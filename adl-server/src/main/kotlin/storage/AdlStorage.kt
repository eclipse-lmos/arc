// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.storage
import org.eclipse.lmos.adl.server.model.Adl
interface AdlStorage {
    suspend fun store(adl: Adl): Adl
    suspend fun get(id: String): Adl?
    suspend fun list(): List<Adl>
}
