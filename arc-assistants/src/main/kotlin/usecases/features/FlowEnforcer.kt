// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.features

import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.breakWith
import org.eclipse.lmos.arc.agents.dsl.extensions.emit
import org.eclipse.lmos.arc.agents.dsl.extensions.memory
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.functions.FunctionWithContext
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
import org.eclipse.lmos.arc.assistants.support.usecases.output
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
private const val MEMORY_KEY = "flow_options_current_use_case_flow"
private val log = org.slf4j.LoggerFactory.getLogger("usecases.FlowEnforcer")

suspend fun processFlow(
    content: String,
    useCase: UseCase,
    allUseCases: List<UseCase>?,
    usedUseCases: List<String>,
    conditions: Set<String>,
    context: DSLContext,
    model: String? = null,
    noMatchResponse: String? = null,
    optionsAnalyserPrompt: String? = null,
    optionsAnalyser: OptionsAnalyser? = null,
): String {
    val flowStart = extractFlowOptions(content)

    //
    // Only process Use Cases that contain flow options.
    //
    if (flowStart.options.isNotEmpty()) {
        log.debug("Found Flow Options: ${flowStart.options}")

        //
        // If we have started this use case in the last turn, then we are in a flow and should update the instructions.
        //
        if (usedUseCases.lastOrNull() == useCase.id) {
            //
            // Jump to the current step in the flow if one is set.
            //
            val currentFlow = context.memory<FlowProgress>(MEMORY_KEY).also {
                context.setLocal(MEMORY_KEY, it)
            }
            val currentFlowStep = currentFlow?.steps?.lastOrNull()
            val currentCase =
                currentFlowStep?.let { allUseCases?.firstOrNull { it.id == currentFlowStep } }
            val updatedFlowPosition = currentCase?.copy(subUseCase = false)?.flowOptions(conditions) ?: flowStart

            //
            // We have reached the end of a flow, so we clear the current flow progress.
            //
            if (updatedFlowPosition.options.isEmpty()) {
                context.memory(MEMORY_KEY, null)
                context.setLocal(MEMORY_KEY, null)
                return flowStart.contentWithoutOptions
            }

            //
            // Match the user reply to one of the options.
            //
            val messages = context.get<Conversation>().transcript.takeLast(4)
            val userMessage = messages.last().content
            val matchedOption = (if (optionsAnalyser != null) optionsAnalyser.pickOption(
                userMessage,
                updatedFlowPosition
            ) else evalUserReply(updatedFlowPosition, context, optionsAnalyserPrompt, model))
                ?: updatedFlowPosition.options.firstOrNull { it.option == "else" }
            if (matchedOption != null) {
                //
                // The command "RESET" is a special command that clears the current flow progress.
                //
                if (matchedOption.command == "RESET") {
                    context.memory(MEMORY_KEY, null)
                    context.setLocal(MEMORY_KEY, null)
                    return flowStart.contentWithoutOptions
                }

                //
                // If the matched option contains a reference to another use case,
                // then we update the instructions to those of the referenced use case.
                //
                matchedOption.getReferencedUseCase(allUseCases)?.let { referenceUseCase ->
                    // Emit event
                    context.emit(
                        FlowOptionEvent(
                            useCaseId = useCase.id,
                            matchedOption = matchedOption,
                            flowOptions = updatedFlowPosition,
                            referenceUseCase = referenceUseCase.id,
                        ),
                    )

                    if (referenceUseCase.subUseCase) {
                        // Store the current step.
                        val currentFlowProgress = FlowProgress(
                            useCaseId = useCase.id,
                            steps = currentFlow?.steps?.plus(referenceUseCase.id) ?: listOf(referenceUseCase.id),
                        )
                        context.memory(MEMORY_KEY, currentFlowProgress)
                        context.setLocal(MEMORY_KEY, currentFlowProgress)

                        // Check if the instruction is to use static text.
                        val instructions = referenceUseCase.toInstructions(conditions)
                        if (instructions.trim().startsWith("\"") && instructions.trim().endsWith("\"")) {
                            context.breakWith(
                                instructions.substringAfter("\"")
                                    .substringBeforeLast("\""), reason = "Following flow option."
                            )
                        }

                        // Update the instructions to those of the referenced use case.
                        return referenceUseCase.solution.output(useCase, conditions).removeFlowOptions()
                    }
                } ?: run {
                    context.emit(
                        FlowOptionEvent(
                            useCaseId = useCase.id,
                            matchedOption = matchedOption,
                            flowOptions = updatedFlowPosition,
                        ),
                    )
                }

                //
                // No reference to another case, just return the instructions.
                //
                return matchedOption.command.output(useCase, conditions)
            }

            //
            // We could not match the user reply to an option.
            // If the user has changed the topic, then this would automatically trigger a different use cases
            // and this would not be needed.
            //
            log.warn("Could not match user reply to any flow option. noMatchResponse = $noMatchResponse")
            return noMatchResponse?.output(useCase, conditions) ?: updatedFlowPosition.contentWithoutOptions
        }
        return flowStart.contentWithoutOptions
    }
    return content
}

/**
 * Converts the Use Case solution to instructions.
 * Applies the given conditionals and removes any flow options.
 */
private suspend fun UseCase.toInstructions(conditions: Set<String>) = StringBuilder().let {
    this.solution.output(conditions, it)
    it.toString().removeFlowOptions().trim()
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
    prompt: String?,
    model: String? = null,
): FlowOption? {
    val messages = context.get<Conversation>().transcript.takeLast(4)

    // Try to match the user reply to one of the options with code first.
    val userMessage = messages.last().content
    options.options.firstOrNull { it.option.equals(userMessage.trim(), ignoreCase = true) }?.let {
        log.debug("Matched user input directly...")
        return it
    }

    val optionList = options.options.filter { it.option != "" }.joinToString("\n") { "- ${it.option}" }

    // Otherwise use the LLM to match the user reply to one of the options.
    val option =
        context
            .llmMessages(
                model = model,
                system = (prompt ?: optionsAnalyserPrompt),
                messages = listOf(
                    UserMessage(
                        """
                    User input: "$userMessage"
                    
                    Options:
                    $optionList
                        """.trimIndent(),
                    ),
                ),
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
    val functionsWithContext =
        functions?.map { if (it is FunctionWithContext) it.withContext(this@llmMessages) else it } ?: functions
    return chatCompleter.complete(messages, functionsWithContext, settings = settings)
}

@Serializable
data class FlowProgress(val useCaseId: String, val steps: List<String>)

/**
 * Retrieves the current flow progress from the local context.
 * If no current use case is set or no flow progress is found, `null` is returned.
 * This function will only be able to return after the processFlow was called.
 *
 * @return The current FlowProgress if available, otherwise `null`.
 */
suspend fun DSLContext.getCurrentFlowProgress(): FlowProgress? {
    return getLocal(MEMORY_KEY) as? FlowProgress? ?: memory<FlowProgress>(MEMORY_KEY)
}

fun interface OptionsAnalyser {
    fun pickOption(userInput: String, options: FlowOptions): FlowOption?
}