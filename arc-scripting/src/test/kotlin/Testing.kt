// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

/**
 * Reads a script from the resource folder.
 */
fun readScript(name: String) = Thread.currentThread().contextClassLoader.getResourceAsStream(name).use {
    String(it!!.readAllBytes())
}
