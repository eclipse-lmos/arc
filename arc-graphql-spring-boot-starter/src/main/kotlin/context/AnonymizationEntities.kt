// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.context

import org.eclipse.lmos.arc.agents.conversation.AnonymizationEntity
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds a list of anonymized entities.
 */
class AnonymizationEntities(anonymizationEntities: List<AnonymizationEntity>) {
    private val _entities = AtomicReference(anonymizationEntities)
    var entities: List<AnonymizationEntity>
        get() = _entities.get()
        set(value) {
            _entities.set(value)
        }

    fun hasReplacementToken(replacement: String) = entities.any { it.replacement == replacement }

    fun getReplacementFor(name: String) = entities.firstOrNull { it.value == name }
}
