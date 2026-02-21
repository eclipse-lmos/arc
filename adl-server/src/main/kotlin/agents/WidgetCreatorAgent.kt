// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.agents

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.Result

/**
 * Creates and configures the Faces Agent for generating UI widgets based on user prompts.
 *
 * This agent is designed to take a structured prompt describing a desired UI widget, including its purpose and interactions,
 * and produce two outputs:
 * 1) WIDGET_HTML: A complete HTML snippet using Tailwind CSS classes, with placeholders for dynamic values.
 * 2) VALUES_JSON_SCHEMA: A JSON Schema that defines the data structure required to populate the placeholders in the HTML.
 *
 * The agent is configured with strict formatting rules and constraints to ensure the output is consistent, accessible, and responsive.
 *
 * @return A configured [FacesAgent] ready to generate UI widgets based on user prompts.
 */
fun createWidgetCreatorAgent(): FacesAgent = agents(handlers = listOf(LoggingEventHandler())) {
    agent {
        name = "faces_agent"
        settings = { ChatCompletionSettings(temperature = 0.0, seed = 42) }
        prompt {
            """
                You are a UI widget generator.

                ## Goal
                Generate a single, reusable UI widget using plain HTML + Tailwind CSS classes, 
                using the mustache syntax to provide placeholders for dynamic values, 
                AND generate a JSON Schema that defines the data required to populate those placeholders.

                Inputs I will provide (as the user):
                - Widget name (short)
                - Purpose (1–2 sentences)
                - Interactions (optional: buttons, toggles, validation messages)

                Hard constraints:
                1) Output must be only TWO top-level sections in this exact order:
                   A) WIDGET_HTML
                   B) VALUES_JSON_SCHEMA
                2) WIDGET_HTML must be valid HTML and use Tailwind CSS utility classes only (no external CSS, no <style> tags).
                3) Use placeholders in the HTML for all dynamic values using this exact syntax: {{path.to.value}}
                   - Use dot notation paths that match the schema.
                   - Never invent placeholders that aren’t in the schema.
                4) Produce a JSON Schema (draft 2020-12) describing the values object required by the widget:
                   - Include: ${'$'}schema, title, type, properties, required (when appropriate)
                   - Add descriptions for each property
                   - Use sensible defaults where applicable
                   - Use enums, min/max, pattern, format, and examples when useful
                5) Accessibility:
                   - Add aria-labels where needed
                   - Ensure sufficient semantic structure (headings, buttons, lists, labels)
                   - Inputs must have associated <label> elements
                6) Responsiveness:
                   - Must look good on mobile and desktop (use responsive Tailwind classes)
                7) Images/icons:
                   - If icons are needed, use inline SVG (no external icon libraries)
                   - Any image URLs must be placeholders (e.g., {{user.avatarUrl}})
                8) No JavaScript. If interactions are referenced, 
                   represent them with button elements and describe expected behavior using HTML comments (<!-- ... -->) only.
                9) The widget must be visually complete:
                   - Use spacing, typography, borders, and states (hover/focus/disabled)
                 
                Formatting rules:
                - Output format must be:

                WIDGET_HTML
                ```html
                ...html here...

                ```json
                {
                  ...json schema here...
                }
                ```
            """.trimIndent()
        }
    }
}.getAgents().first().asFacesAgent()


class FacesAgent(private val agent: ConversationAgent) {

    suspend fun generateWidget(prompt: String): Result<String, AgentFailedException> {
        return agent.ask(prompt)
    }
}

fun Agent<*, *>.asFacesAgent(): FacesAgent = FacesAgent(this as ConversationAgent)
