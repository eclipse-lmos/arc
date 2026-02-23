// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionMessageToolCall
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.toJsonString
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.getOrNull
import org.slf4j.MDC
import kotlin.jvm.optionals.getOrNull

/**
 * Helper object to apply attributes to the tags following the OpenInference spec.
 * https://github.com/Arize-ai/openinference/tree/main/spec
 */
object OpenInferenceTags {

    fun applyAttributes(
        tags: Tags,
        config: AIClientConfig,
        settings: ChatCompletionSettings?,
        completions: ChatCompletion,
        inputMessages: List<ChatCompletionMessageParam>,
        functionCallHandler: FunctionCallHandler,
    ) {
        tags.tag("openinference.span.kind", "LLM")
        tags.tag("llm.model_name", config.modelName ?: settings?.deploymentNameOrModel() ?: "unknown")
        tags.tag("llm.provider", "azure")
        tags.tag("llm.system", "openai")
        if (completions.choices().isNotEmpty()) {
            tags.tag(
                "finish_reason",
                completions.choices()[0].finishReason().toString(),
            )
        }
        settings?.let {
            tags.tag(
                "llm.invocation_parameters",
                """
                {"model_name": "${config.modelName ?: settings.deploymentNameOrModel() ?: "unknown"}", "temperature": "${it.temperature}", "seed": "${it.seed}"}
                """.trimIndent(),
            )
        }

        tags.tag("input.mime_type", "text/plain")
        inputMessages.forEachIndexed { i, message ->
            val content: Any? = when {
                message.isUser() -> message.asUser().content().text().getOrNull()
                message.isAssistant() -> message.asAssistant().content().getOrNull()?.text()?.getOrNull()
                message.isSystem() -> message.asSystem().content().text().getOrNull()
                message.isTool() -> message.asTool().content().text().getOrNull()
                else -> null
            }
            val role = when {
                message.isUser() -> "user"
                message.isAssistant() -> "assistant"
                message.isSystem() -> "system"
                message.isTool() -> "tool"
                else -> "unknown"
            }

            tags.tag("llm.input_messages.$i.message.role", role)
            if (content != null) {
                tags.tag("llm.input_messages.$i.message.content", content.toString())
            }
            if (i == inputMessages.size - 1) {
                tags.tag("input.value", content.toString())
            }
        }
        completions.choices().forEachIndexed { i, choice ->
            tags.tag("llm.output_messages.$i.message.role", choice.message()._role().convert(String::class.java) ?: "unknown")
            if (choice.message().content().isPresent) {
                tags.tag("llm.output_messages.$i.message.content", choice.message().content().get())
            }
            choice.message().toolCalls().ifPresent { toolCalls ->
                toolCalls.forEachIndexed { y, call ->
                    val toolCall = call
                    tags.tag("llm.output_messages.$i.message.tool_calls.$y.tool_call.function.name", toolCall.function().name())
                    tags.tag(
                        "llm.output_messages.$i.message.tool_calls.$y.tool_call.function.arguments",
                        toolCall.function().arguments(),
                    )
                }
            }
        }
        functionCallHandler.functions.forEachIndexed { i, tool ->
            tags.tag("llm.tools.$i.tool.name", tool.name)
            tags.tag(
                "llm.tools.$i.tool.json_schema",
                """{"type":"function","function":{"name":"${tool.name}","parameters":${tool.parameters.toJsonString()},"description":"${tool.description}"}}""".replace(
                    "\n",
                    " ",
                ),
            )
        }

        tags.tag("output.value", completions.choices().firstOrNull()?.message()?.content()?.orElse("") ?: "")
        tags.tag("output.mime_type", "text/plain") // TODO
        val usage = completions.usage()
        if (usage.isPresent) {
            tags.tag("llm.token_count.prompt", usage.get().promptTokens())
            tags.tag("llm.token_count.completion", usage.get().completionTokens())
            tags.tag("llm.token_count.total", usage.get().totalTokens())
        }
    }

    fun applyToolAttributes(
        functionName: String,
        toolCall: ChatCompletionMessageToolCall,
        tags: Tags,
    ) {
        val functionArguments = toolCall.function().arguments()
        tags.tag("openinference.span.kind", "TOOL")
        tags.tag("tool_call.id", toolCall.id())
        tags.tag("tool_call.function.name", functionName)
        tags.tag("tool_call.function.arguments", functionArguments)
        tags.tag("input.value", functionArguments)
        tags.tag("input.mime_type", "application/json")
        MDC.get("use_case")?.let { tags.tag("use_case", it) }
    }

    fun applyToolAttributes(function: LLMFunction, tags: Tags) {
        tags.tag("tool.name", function.name)
        tags.tag("tool.id", function.name)
        tags.tag("tool.description", function.description)
        tags.tag("tool.parameters", function.parameters.toJsonString())
        tags.tag(
            "tool.json_schema",
            """{"type":"function","function":{"name":"${function.name}","parameters":${function.parameters.toJsonString()},"description":"${function.description}"}}""".replace(
                "\n",
                " ",
            ),
        )
    }

    fun applyToolAttributes(result: Result<String, ArcException>, tags: Tags) {
        tags.tag("output.value", result.getOrNull() ?: "")
    }
}



