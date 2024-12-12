// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import java.io.File

/**
 * Extensions for load text files from the classpath.
 */

/**
 * Load a resource from the classpath or local filesystem.
 * If the resource is on the classpath, it will be loaded otherwise it will try to load it from the local filesystem.
 */
fun DSLContext.local(resource: String): String? {
    return Thread.currentThread().contextClassLoader.getResourceAsStream(resource)?.use { stream ->
        stream.bufferedReader().readText()
    } ?: File(resource).takeIf { it.exists() }?.readText()
}
