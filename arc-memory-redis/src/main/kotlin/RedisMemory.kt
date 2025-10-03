// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.memory.redis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.lettuce.core.AbstractRedisClient
import io.lettuce.core.RedisClient
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands
import io.lettuce.core.api.reactive.RedisStringReactiveCommands
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.eclipse.lmos.arc.agents.memory.Memory
import org.eclipse.lmos.arc.agents.memory.MemoryException
import org.slf4j.LoggerFactory.getLogger
import java.time.Duration

/**
 * Memory implementation using Redis.
 */
class RedisMemory(
    private val shortTermTTL: Duration,
    private val longTermTTL: Duration = Duration.ofDays(180),
    private val redisClient: AbstractRedisClient,
) : Memory {
    private val log = getLogger(RedisMemory::class.java)

    private val redisConnection: StatefulConnection<String, String> = when (redisClient) {
        is RedisClusterClient -> redisClient.connect()
        else -> (redisClient as RedisClient).connect()
    }

    private val json = jacksonObjectMapper().apply {
        enableDefaultTyping()
    }

    override suspend fun storeLongTerm(owner: String, key: String, value: Any?): Unit = withClient { commands, keys ->
        val compositeKey = json.writeValueAsString(MemoryKey(owner, key))
        if (value != null) {
            log.debug("Storing $key for $owner in LONG_TERM memory.")
            val valueJson = json.writeValueAsString(MemoryEntry(value))
            val setArgs = SetArgs().ex(longTermTTL.toSeconds())
            commands.set(compositeKey, valueJson, setArgs).awaitSingle().also {
                if (it != "OK") {
                    throw MemoryException("Failed to store $key for $owner in LONG_TERM memory.")
                }
            }
        } else {
            log.debug("Deleting $key for $owner in LONG_TERM memory.")
            keys.del(compositeKey).awaitSingle()
        }
    }

    override suspend fun storeShortTerm(owner: String, key: String, value: Any?, sessionId: String): Unit =
        withClient { commands, keys ->
            val compositeKey = json.writeValueAsString(MemoryKey(owner, key, sessionId))
            if (value != null) {
                log.debug("Storing $key for $owner in SHORT_TERM memory.")
                val setArgs = SetArgs().ex(shortTermTTL.toSeconds())
                commands.set(compositeKey, json.writeValueAsString(MemoryEntry(value)), setArgs).awaitSingle()
            } else {
                log.debug("Deleting $key for $owner in SHORT_TERM memory.")
                keys.del(compositeKey).awaitSingle()
            }
        }

    override suspend fun <T> fetch(owner: String, key: String, sessionId: String?): T? = withClient { commands, _ ->
        val compositeKey = json.writeValueAsString(MemoryKey(owner, key, sessionId))
        val value = commands.get(compositeKey).awaitSingleOrNull()
        if (value != null) {
            json.readValue<MemoryEntry<T>>(value).value
        } else {
            null
        }
    }

    private suspend fun <T> withClient(fn: suspend (RedisStringReactiveCommands<String, String>, RedisKeyReactiveCommands<String, String>) -> T): T {
        val commands = when (redisConnection) {
            is StatefulRedisClusterConnection -> redisConnection.reactive()
            else -> (redisConnection as StatefulRedisConnection).reactive()
        }
        val result = try {
            fn(commands, commands)
        } catch (ex: Exception) {
            log.error("Error while executing Redis command!", ex)
            if (ex is MemoryException) throw ex
            throw MemoryException("Error while executing Redis command!", ex)
        }
        return result
    }

    fun close() {
        redisConnection.close()
        redisClient.shutdown()
    }
}
