// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.memory.redis

import com.redis.testcontainers.RedisContainer
import io.lettuce.core.RedisClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Testcontainers
open class TestBase {

    companion object {
        private val redisImage: DockerImageName = DockerImageName.parse("redis:7.0.12")

        @Container
        private val redisContainer = RedisContainer(redisImage).withExposedPorts(6379).also { it.start() }
    }

    val memory = RedisMemory(shortTermTTL = Duration.ofSeconds(2), redisClient = createRedisClient())

    protected fun createRedisClient(): RedisClient {
        val port = redisContainer.getMappedPort(6379)
        val redisURI = "redis://localhost:$port"
        return RedisClient.create(redisURI)
    }
}

data class TestValue(val nr: Int, val value: String)
