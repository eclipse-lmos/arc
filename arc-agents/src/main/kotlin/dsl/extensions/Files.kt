// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

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
    if (localCache[resource]?.timestamp?.plus(cacheTime.toJavaDuration())?.isBefore(now()) == true) {
        localCache.remove(resource)
    }
    return localCache.computeIfAbsent(resource) {
        val value = Thread.currentThread().contextClassLoader.getResourceAsStream(resource)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: File(resource).takeIf { it.exists() }?.readText()
        CacheEntry(value)
    }.value
}

private val localCache = ConcurrentHashMap<String, CacheEntry>()

data class CacheEntry(val value: String?, val timestamp: LocalDateTime = now())
