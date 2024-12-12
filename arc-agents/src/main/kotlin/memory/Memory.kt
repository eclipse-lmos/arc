// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.memory

import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit.HOURS
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface for storing data in memory.
 */
interface Memory {

    /**
     * Store a value in LONG_TERM memory.
     * @param owner The owner of the memory. For example, the user id.
     * @param key The key to store the value under.
     * @param value The value to store. If null, the value is removed from memory.
     */
    suspend fun storeLongTerm(owner: String, key: String, value: Any?)

    /**
     * Store a value in SHORT_TERM memory.
     * @param owner The owner of the memory. For example, the user id.
     * @param key The key to store the value under.
     * @param value The value to store. If null, the value is removed from memory.
     * @param sessionId The session id to store the value under.
     */
    suspend fun storeShortTerm(owner: String, key: String, value: Any?, sessionId: String)

    /**
     * Fetch a value from memory.
     * @param owner The owner of the memory. For example, the user id.
     * @param key The key to fetch the value for.
     * @param sessionId The session id to fetch the value for. Only used if the value was stored under SHORT_TERM memory.
     * @return The value stored under the key, or null if no value is stored.
     */
    suspend fun fetch(owner: String, key: String, sessionId: String? = null): Any?
}

/**
 * In-memory implementation of [Memory].
 * This class is designed for to testing and development. It is
 * not sufficient for production.
 */
class InMemoryMemory : Memory {

    private val log = LoggerFactory.getLogger(javaClass)

    private val shortTermMemory = ConcurrentHashMap<String, MemoryShortTermEntry>()
    private val longTermMemory = ConcurrentHashMap<String, Any>()

    override suspend fun storeLongTerm(owner: String, key: String, value: Any?) {
        validate(owner, key)
        if (value == null) {
            log.debug("Removing $key for $owner from LONG_TERM memory.")
            longTermMemory.remove("$owner $key")
            return
        }
        log.debug("Storing $value with $key for $owner in LONG_TERM memory.")
        longTermMemory["$owner $key"] = value
        cleanShortTermMemory()
    }

    override suspend fun storeShortTerm(owner: String, key: String, value: Any?, sessionId: String) {
        validate(owner, key)
        if (value == null) {
            log.debug("Removing $key for $owner from SHORT_TERM memory.")
            shortTermMemory.remove("$sessionId $owner $key")
            return
        }
        log.debug("Storing $value with $key for $owner in SHORT_TERM memory with session id $sessionId.")
        shortTermMemory["$sessionId $owner $key"] = MemoryShortTermEntry(value)
        cleanShortTermMemory()
    }

    private fun validate(owner: String, key: String) {
        if (owner.isEmpty() || key.isEmpty()) {
            log.error("Owner and key must not be empty.")
            error("Owner and key must not be empty!")
        }
    }

    private fun cleanShortTermMemory() {
        val now = Instant.now()
        shortTermMemory.entries.removeIf { (_, entry) ->
            entry.creationDate.plus(6, HOURS).isBefore(now)
        }
    }

    override suspend fun fetch(owner: String, key: String, sessionId: String?) =
        if (sessionId != null) {
            shortTermMemory["$sessionId $owner $key"]?.value
                ?: longTermMemory["$owner $key"]
        } else {
            longTermMemory["$owner $key"]
        }
}

private data class MemoryShortTermEntry(val value: Any?, val creationDate: Instant = Instant.now())
