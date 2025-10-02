// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.features

import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.assistants.support.usecases.FlowOption
import org.eclipse.lmos.arc.assistants.support.usecases.FlowOptions
import java.time.Instant

data class FlowOptionEvent(
    val useCaseId: String,
    val matchedOption: FlowOption,
    val flowOptions: FlowOptions,
    val referenceUseCase: String? = null,
    override val timestamp: Instant = Instant.now(),
) : Event by BaseEvent()
