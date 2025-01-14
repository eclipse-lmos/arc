// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint")

package org.eclipse.lmos.arc.agents.gen

import org.eclipse.lmos.arc.agents.dsl.AgentDefinitionContext

class Agents {

    context(AgentDefinitionContext)
    fun build() {
        agent {
            name = "weather-gen"
        }
    }
}
