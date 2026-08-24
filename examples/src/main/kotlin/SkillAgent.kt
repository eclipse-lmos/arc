// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.examples.skills

import kotlinx.coroutines.runBlocking
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.getChatAgent
import org.eclipse.lmos.arc.agents.agents
import org.eclipse.lmos.arc.agents.agent.ask
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrThrow

/**
 * Demonstrates the Agent Skills DSL with an in-memory skill document.
 *
 * The agent declares a skill source, exposes its metadata through `$SKILLS`, and receives an
 * automatically registered `activate_skill` tool. The fixed completer invokes that
 * tool so this example runs locally without model credentials.
 *
 * Dependencies:
 *  - implementation(project(":arc-agents"))
 */
fun main(): Unit = runBlocking {
    println(runSkillAgent())
}

suspend fun runSkillAgent(): String {
    val agentSystem = agents(
        chatCompleterProvider = ChatCompleterProvider { SkillDemoCompleter() },
        skills = {
            skill {
                name = "release-notes"
                description = "Creates concise, user-facing release notes."
                """
                # Release notes

                When summarizing a release, do not invent version numbers, dates, or changes.
                """
            }
        },
    ) {
        agent {
            name = "release-notes"
            skills {
                +"release-notes"
            }
            prompt {
                """
                You write release notes.

                $SKILLS
                """
            }
        }
    }

    val response = agentSystem.getChatAgent("release-notes")
        .ask("Summarize the latest release.")
        .getOrThrow()

    return response
}

private class SkillDemoCompleter : ChatCompleter {
    override suspend fun complete(
        messages: List<ConversationMessage>,
        functions: List<LLMFunction>?,
        settings: ChatCompletionSettings?,
        eventPublisher: EventPublisher?,
    ): Result<AssistantMessage, ArcException> {
        val systemPrompt = (messages.first { it is SystemMessage } as SystemMessage).content
        check("name: release-notes" in systemPrompt) { "The prompt must contain the available skill name." }
        check("description: Creates concise, user-facing release notes." in systemPrompt) {
            "The prompt must contain the available skill description."
        }

        val activateSkill = functions.orEmpty().single { it.name == "activate_skill" }
        val instructions = activateSkill.execute(mapOf("name" to "release-notes")).getOrThrow()
        return Success(AssistantMessage("Loaded skill instructions:\n$instructions"))
    }
}



