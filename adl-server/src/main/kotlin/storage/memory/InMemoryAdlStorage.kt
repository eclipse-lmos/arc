// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.storage.memory

import org.eclipse.lmos.adl.server.model.Adl
import org.eclipse.lmos.adl.server.storage.AdlStorage
import java.util.concurrent.ConcurrentHashMap

class InMemoryAdlStorage : AdlStorage {
    private val storage = ConcurrentHashMap<String, Adl>()

    override suspend fun store(adl: Adl): Adl {
        storage[adl.id] = adl
        return adl
    }

    override suspend fun get(id: String): Adl? {
        return storage[id]
    }

    override suspend fun list(): List<Adl> {
        return storage.values.toList()
    }
}
