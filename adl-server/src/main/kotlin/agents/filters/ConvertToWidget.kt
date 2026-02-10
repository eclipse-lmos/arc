package org.eclipse.lmos.adl.server.agents.filters

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.mustachejava.DefaultMustacheFactory
import org.eclipse.lmos.adl.server.repositories.WidgetRepository
import org.eclipse.lmos.adl.server.templates.TemplateLoader
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentOutputFilter
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.extensions.getCurrentUseCases
import org.eclipse.lmos.arc.agents.dsl.extensions.llm
import org.eclipse.lmos.arc.core.getOrThrow
import java.io.StringReader
import java.io.StringWriter

/**
 * An [AgentOutputFilter] that converts the agent's output into a widget format (HTML) using a specific JSON schema.
 *
 * This filter checks if there is a current use case context. If so, it:
 * 1. Retrieves a widget definition from the [WidgetRepository].
 * 2. Uses an LLM call to convert the original output message into JSON data conforming to the widget's schema.
 * 3. Renders the widget's HTML template with the generated JSON data using Mustache.
 * 4. Updates the output message content with the rendered HTML.
 *
 * @property widgetRepository Repository to fetch widget definitions.
 */
class ConvertToWidget(private val widgetRepository: WidgetRepository) : AgentOutputFilter {

    private val mapper = jacksonObjectMapper()
    private val mustacheFactory = DefaultMustacheFactory()
    private val mapType = object : TypeReference<HashMap<String?, Any?>?>() {}
    private val prompt = TemplateLoader().loadResource("/json_converter.md")

    /**
     * Filters the agent's conversation message.
     *
     * @param message The conversation message to be filtered.
     * @param context The context for the output filter, providing access to LLM and other utilities.
     * @return The updated [ConversationMessage] containing the widget HTML, or null if no update occurred.
     */
    override suspend fun filter(
        message: ConversationMessage,
        context: OutputFilterContext
    ): ConversationMessage? {
        if (true) return null // currently disabled
        return context.getCurrentUseCases()?.currentUseCase()?.let {
            val widget = widgetRepository.findAll().first()

            val data = context.llm(
                system = prompt.replace("{{schema}}", widget.jsonSchema),
                user = context.outputMessage.content
            ).getOrThrow().content

            val m = mustacheFactory.compile(StringReader(widget.html), "widget")
            val writer = StringWriter()

            m.execute(writer, mapper.readValue(data, mapType)).flush()
            val html: String = writer.toString()
            context.outputMessage.update(html)
        }
    }
}