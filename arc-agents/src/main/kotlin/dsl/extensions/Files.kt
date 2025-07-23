// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import java.io.File
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Extensions for load text files from the classpath.
 */

/**
 * Load a resource from the classpath or local filesystem.
 * If the resource is on the classpath, it will be loaded otherwise it will try to load it from the local filesystem.
 */
fun DSLContext.local(resource: String, cacheTime: Duration = 30.minutes): String? {
    if (localCache[resource]?.timestamp?.isBefore(now()) == true) {
        localCache.remove(resource)
    }
    return localCache.computeIfAbsent(resource) {
        val value = localResource(resource) ?: localFile(resource)
        CacheEntry(value, timestamp = now().plus(cacheTime.toJavaDuration()))
    }.value
}

fun localResource(resource: String): String? {
    return Thread.currentThread().contextClassLoader.getResourceAsStream(resource)?.use { stream ->
        stream.bufferedReader().readText()
    }
}

fun localFile(resource: String): String? {
    val file = File(resource).takeIf { it.exists() }
    return file?.readText()
}

/**
 * Cache for local resources.
 */
private val scope = CoroutineScope(SupervisorJob())
private val localCache = ConcurrentHashMap<String, CacheEntry>().also {
    scope.launch {
        while (true) {
            it.entries.removeIf { entry -> entry.value.timestamp.isBefore(now()) }
            delay(1.minutes)
        }
    }
}

data class CacheEntry(val value: String?, val timestamp: LocalDateTime = now())
