// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting.functions

import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event

/**
 * Events published when a function is loaded.
 */
data class FunctionLoadedEvent(
    val name: String,
    val errorMessage: String? = null,
) : Event by BaseEvent()
