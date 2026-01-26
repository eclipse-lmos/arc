// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.repositories.impl

import org.eclipse.lmos.adl.server.repositories.AdlRepository
import org.eclipse.lmos.adl.server.model.Adl
import java.util.concurrent.ConcurrentHashMap

class InMemoryAdlRepository : AdlRepository {
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

    override suspend fun deleteById(id: String) {
        storage.remove(id)
    }
}