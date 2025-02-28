package org.eclipse.lmos.arc.client.langchain4j

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.listener.ChatModelErrorContext
import dev.langchain4j.model.chat.listener.ChatModelListener
import dev.langchain4j.model.chat.listener.ChatModelRequestContext
import dev.langchain4j.model.chat.listener.ChatModelResponseContext
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Scope
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class SpanChatModelListener(private val tracer: Tracer = GlobalOpenTelemetry.getTracer("lmos-arc-langchain4j")) : ChatModelListener {

    override fun onRequest(requestContext: ChatModelRequestContext) {
        val request = requestContext.request()

        val span = tracer.spanBuilder("ChatLanguageModel.chat")
            .setAttribute("gen_ai.operation.name", "chat")
            .setAttribute("gen_ai.system", "az.ai.openai")
            .setAttribute("gen_ai.request.model", request.model())
            .setAttribute("gen_ai.request.temperature", request.temperature() ?: 0.0)
            .setAttribute("gen_ai.request.top_p", request.topP() ?: 0.0)
            .startSpan()

        val scope = span.makeCurrent()

        val combinedPrompt = StringBuilder()
        for (message in request.messages()) {
            val text = message.text()
            if (!text.isNullOrBlank()) {
                when (message) {
                    is SystemMessage -> combinedPrompt.append("System: $text\n")
                    is UserMessage -> combinedPrompt.append("User: $text\n")
                    is AiMessage -> combinedPrompt.append("Agent: $text\n")
                }
            }
        }

        span.addEvent(
            "gen_ai.content.prompt", Attributes.of(
                AttributeKey.stringKey("gen_ai.prompt"), combinedPrompt.toString().trimEnd()
            )
        )

        for (message in request.messages()) {
            when (message) {
                is SystemMessage -> {
                    span.addEvent(
                        "gen_ai.system.message", Attributes.of(
                            AttributeKey.stringKey("content"), message.text()
                        )
                    )
                }
                is UserMessage -> {
                    span.addEvent(
                        "gen_ai.user.message", Attributes.of(
                            AttributeKey.stringKey("content"), message.text()
                        )
                    )
                }
                is AiMessage -> {
                    span.addEvent(
                        "gen_ai.assistant.message", Attributes.of(
                            AttributeKey.stringKey("content"), message.text()
                        )
                    )
                    // Handle tool execution requests
                    if(message.toolExecutionRequests() != null){
                        for (toolExecutionRequest in message.toolExecutionRequests()) {
                            val toolEventAttributes = Attributes.builder()
                                .put("gen_ai.tool.id", toolExecutionRequest.id())
                                .put("gen_ai.tool.function.name", toolExecutionRequest.name())
                                .put("gen_ai.tool.function.arguments", toolExecutionRequest.arguments())
                                .build()
                            span.addEvent("gen_ai.choice", toolEventAttributes)
                        }
                    }
                }
                is ToolExecutionResultMessage -> {
                    // Handle tool execution result messages
                    val toolEventAttributes = Attributes.builder()
                        .put("id", message.id())
                        .put("content", message.text())
                        .put("toolName", message.toolName())
                        .build()
                    span.addEvent("gen_ai.tool.message", toolEventAttributes)
                }
            }

        }

        // Store the scope and span in attributes for later use
        requestContext.attributes()[OTEL_SCOPE_KEY_NAME] = scope
        requestContext.attributes()[OTEL_SPAN_KEY_NAME] = span

    }

    override fun onResponse(responseContext: ChatModelResponseContext) {
        val attributes = responseContext.attributes()
        val span: Span? = attributes[OTEL_SPAN_KEY_NAME] as Span?
        if (span != null) {
            val response = responseContext.response()
            span.setAttribute("gen_ai.response.id", response.id())
                .setAttribute("gen_ai.response.model", response.model())
            if (response.finishReason() != null) {
                span.setAttribute("gen_ai.response.finish_reasons", response.finishReason().toString())
            }

            val message = responseContext.response().aiMessage()

            span.addEvent(
                "gen_ai.content.completion", Attributes.of(
                    AttributeKey.stringKey("gen_ai.completion"), message.text()
                )
            )

            when (message) {
                is AiMessage -> {
                    span.addEvent("gen_ai.assistant.message",
                        Attributes.of(AttributeKey.stringKey("content"), message.text()));
                    // Handle tool execution requests
                    if(message.toolExecutionRequests() != null){
                        for (toolExecutionRequest in message.toolExecutionRequests()) {
                            val toolEventAttributes = Attributes.builder()
                                .put("gen_ai.tool.id", toolExecutionRequest.id())
                                .put("gen_ai.tool.function.name", toolExecutionRequest.name())
                                .put("gen_ai.tool.function.arguments", toolExecutionRequest.arguments())
                                .build()
                            span.addEvent("gen_ai.choice", toolEventAttributes)
                        }
                    }
                }
            }
            val tokenUsage = response.tokenUsage()
            if (tokenUsage != null) {
                span.setAttribute("gen_ai.usage.output_tokens", tokenUsage.outputTokenCount().toLong())
                    .setAttribute("gen_ai.usage.input_tokens", tokenUsage.inputTokenCount().toLong())
            }
            span.end()
        } else {
            // should never happen
            log.warn("No Span found in response")
        }
        safeCloseScope(attributes)
    }

    override fun onError(errorContext: ChatModelErrorContext) {
        val attributes = errorContext.attributes()
        val span: Span? = attributes[OTEL_SPAN_KEY_NAME] as Span?
        if (span != null) {
            span.recordException(errorContext.error())
        } else {
            // should never happen
            log.warn("No Span found in response")
        }
        safeCloseScope(errorContext.attributes())
    }

    private fun safeCloseScope(attributes: Map<Any, Any>) {
        val scope: Scope? = attributes[OTEL_SCOPE_KEY_NAME] as Scope?
        if (scope == null) {
            // should never happen
            log.warn("No Scope found in response")
        } else {
            try {
                scope.close()
            } catch (e: Exception) {
                log.warn("Error closing scope", e)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SpanChatModelListener::class.java)

        private const val OTEL_SCOPE_KEY_NAME = "OTelScope"
        private const val OTEL_SPAN_KEY_NAME = "OTelSpan"
    }
}