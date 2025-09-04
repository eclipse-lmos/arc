// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.tracing

import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.dsl.DSLContext

/**
 * Interface for adding tags to the "generated response" span.
 * Simple add any implementation to the context of the agent.
 */
interface GenerateResponseTagger {

    fun tag(tags: Tags, outputMessage: AssistantMessage, dslContext: DSLContext)
}
