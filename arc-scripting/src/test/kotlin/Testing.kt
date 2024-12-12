// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

/**
 * Reads a script from the resource folder.
 */
fun readScript(name: String) = Thread.currentThread().contextClassLoader.getResourceAsStream(name).use {
    String(it!!.readAllBytes())
}
