// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.features

import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.memory
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.assistants.support.usecases.Conditional
import org.eclipse.lmos.arc.assistants.support.usecases.FlowOption
import org.eclipse.lmos.arc.assistants.support.usecases.FlowOptions
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.extractFlowOptions
import org.eclipse.lmos.arc.assistants.support.usecases.flowOptions
import org.eclipse.lmos.arc.assistants.support.usecases.formatToString
import org.eclipse.lmos.arc.assistants.support.usecases.parseUseCaseRefs
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.result

/**
 * Processes Use Cases that contain "Flow options".
 *
 * Flow options are denoted using the so-called box syntax: [option] command.
 *
 * Example:
 * ```
 * ### UseCase: some_use_case
 * #### Description
 * A description of the use case.
 *
 * #### Solution
 * As the customer if there would like to buy an iPhone.
 *
 * [yes] Great! Please visit our store, here https://www.telekom.de/iphone/
 * [no] Ok, maybe next time.
 *
 * ```
 *
 * Options denoted in this manner can be identified and re-enforced using a combination of static and LLM-based matching.
 *
 * In this implementation, the solution of
 * the use case is updated in every turn of the conversation with the current step of the flow.
 * This ensures that the LLM cannot get distracted or confused by multiple instructions.
 */
suspend fun processFlow(
    content: String,
    useCase: UseCase,
    allUseCases: List<UseCase>?,
    usedUseCases: List<String>,
    conditions: Set<String>,
    context: DSLContext,
    model: String? = null,
    noMatchResponse: String? = null,
): String {
    var flowOptions = extractFlowOptions(content)

    //
    // Only process Use Cases that contain flow options.
    //
    if (flowOptions.options.isNotEmpty()) {
        //
        // If we have started this use case in the last turn, then we are in a flow and should update the instructions.
        //
        if (usedUseCases.lastOrNull() == useCase.id) {
            //
            // Jump to the current step in the flow if one is set.
            //
            val currentFlowStep = context.memory<String>("${useCase.id}#current_flow_step")
            val currentCase =
                currentFlowStep?.let { allUseCases?.firstOrNull { it.id == currentFlowStep } }
            if (currentCase != null) flowOptions = currentCase.copy(subUseCase = false).flowOptions(conditions)

            //
            // Match the user reply to one of the options.
            //
            val matchedOption = evalUserReply(flowOptions, context, model)
            if (matchedOption != null) {
                //
                // If the matched option contains a reference to another use case,
                // then we update the instructions to those of the referenced use case.
                //
                matchedOption.getReferencedUseCase(allUseCases)?.let { referenceUseCase ->
                    // Store the current step.
                    context.memory("${useCase.id}#current_flow_step", referenceUseCase.id)

                    // Remove possible nested flow options.
                    return referenceUseCase.solution.output(useCase, conditions).removeFlowOptions()
                }

                //
                // No reference to another case, just return the instructions.
                //
                return matchedOption.command.output(useCase)
            }

            //
            // We could not match the user reply to an option, so we ask the user to repeat.
            // If the user has changed the topic, then this would automatically trigger different use cases
            // and this would not be needed.
            //
            return (noMatchResponse ?: "Kindly ask the customer to repeat. You did not understand their reply.").output(
                useCase
            )
        }
        return flowOptions.contentWithoutOptions
    }
    return content
}

/**
 * Outputs the string as the solution to a Use Case.
 */
suspend fun String.output(
    useCase: UseCase,
    conditions: Set<String> = emptySet(),
): String = useCase.copy(solution = listOf(Conditional(this))).formatToString(conditions = conditions)

/**
 * Outputs the conditionals as a string.
 */
suspend fun List<Conditional>.output(
    useCase: UseCase,
    conditions: Set<String>,
): String = listOf(useCase.copy(solution = this)).formatToString(conditions = conditions)

/**
 * Removes flow options from a string.
 */
fun String.removeFlowOptions(): String = extractFlowOptions(this).contentWithoutOptions

/**
 * Returns the referenced use case if the matched option contains a reference to another use case.
 */
fun FlowOption.getReferencedUseCase(allUseCases: List<UseCase>?): UseCase? {
    if (allUseCases == null) return null
    val (_, references) = command.parseUseCaseRefs()
    return if (references.isNotEmpty()) allUseCases.firstOrNull { it.id == references.firstOrNull() } else null
}

/**
 * Returns the option that best matches the user reply.
 *
 * Uses an LLM to match user reply to one of the box options
 * This can be optimized by trying to use static code first.
 * TODO Refine the prompt.
 */
suspend fun evalUserReply(
    options: FlowOptions,
    context: DSLContext,
    model: String? = null,
): FlowOption? {
    val messages = context.get<Conversation>().transcript.takeLast(4)
    val option =
        context
            .llmMessages(
                model = model,
                system = """
                    Examine the user's reply and return the option that best matches the user's intent.
                    Only return one of the following options or NO_MATCH if none of the options match.
                    Do not ask follow-up questions.
                    
                    Options:
                    ${options.options.filter { it.option != "" }.joinToString("\n") { "- ${it.option}" }}
                """,
                messages = messages,
            ).getOrThrow()
            .content
            .replace("-", "")
            .trim()
    val matched = options.options.firstOrNull { it.option.contains(option, ignoreCase = true) }
    return matched ?: options.options.firstOrNull { it.option == "" }
}

suspend fun DSLContext.llmMessages(
    messages: List<ConversationMessage>,
    system: String? = null,
    model: String? = null,
    settings: ChatCompletionSettings? = null,
    functions: List<LLMFunction>? = null,
) = result<AssistantMessage, ArcException> {
    val chatCompleterProvider = get<ChatCompleterProvider>()
    val chatCompleter = chatCompleterProvider.provideByModel(model = model)
    val messages =
        buildList {
            if (system != null) add(SystemMessage(system))
            addAll(messages)
        }
    return chatCompleter.complete(messages, functions, settings = settings)
}
