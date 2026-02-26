// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.repositories

import org.eclipse.lmos.adl.server.model.Adl
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.eclipse.lmos.arc.assistants.support.usecases.Conditional

interface AdlRepository {
    suspend fun store(adl: Adl): Adl
    suspend fun get(id: String): Adl?

    suspend fun getAsUseCases(id: String): List<UseCase> {
        val adl = get(id) ?: return emptyList()
        val useCases = adl.content.toUseCases()
        return if (adl.output != null) {
            useCases.map { it.copy(output = listOf(Conditional(adl.output))) }
        } else {
            useCases
        }
    }

    suspend fun list(): List<Adl>
    suspend fun deleteById(id: String)
}