// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.events

/**
 * EventHandler interface.
 */
interface EventHandler<T : Event> {
    fun onEvent(event: T)
}
