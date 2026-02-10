// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.adl.server.agents.FacesAgent
import org.eclipse.lmos.adl.server.models.Widget
import org.eclipse.lmos.adl.server.repositories.WidgetRepository
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import java.util.UUID

class WidgetsMutation(
    private val facesAgent: FacesAgent,
    private val widgetRepository: WidgetRepository,
) : Mutation {

    @GraphQLDescription("Generates a UI widget using the Faces Agent")
    suspend fun generateWidget(
        @GraphQLDescription("The widget requirements") input: WidgetInput,
    ): WidgetOutput {
        val prompt = buildString {
            append("Widget name: ${input.name}\n")
            append("Purpose: ${input.purpose}\n")
            if (input.interactions != null) {
                append("Interactions: ${input.interactions}\n")
            }
        }
        val result = facesAgent.generateWidget(prompt)
        return when (result) {
            is Success -> {
                val output = result.value.substringAfter("```html").substringBefore("```").trim()
                val json = result.value.substringAfter("```json").substringBefore("```").trim()
                WidgetOutput(
                    html = output,
                    jsonSchema = json
                )
            }

            is Failure -> throw result.reason
        }
    }

    @GraphQLDescription("Save a widget")
    fun saveWidget(
        @GraphQLDescription("The widget to save") input: SaveWidgetInput
    ): Widget {
        val id = input.id ?: UUID.randomUUID().toString()
        val widget = Widget(
            id = id,
            name = input.name,
            description = input.description,
            html = input.html,
            jsonSchema = input.jsonSchema,
            preview = input.preview
        )
        return widgetRepository.save(widget)
    }

    @GraphQLDescription("Delete a widget")
    fun deleteWidget(
        @GraphQLDescription("The ID of the widget to delete") id: String
    ): Boolean {
        return widgetRepository.delete(id)
    }
}

@GraphQLDescription("Output for generated UI widget")
data class WidgetOutput(
    @GraphQLDescription("The HTML content of the widget")
    val html: String,
    @GraphQLDescription("The JSON schema defining the data structure for the widget's dynamic values")
    val jsonSchema: String
)

@GraphQLDescription("Input for generating a UI widget")
data class WidgetInput(
    @GraphQLDescription("The name of the widget (short)")
    val name: String,
    @GraphQLDescription("The purpose of the widget (1-2 sentences)")
    val purpose: String,
    @GraphQLDescription("Optional interactions like buttons, toggles, etc.")
    val interactions: String? = null
)

@GraphQLDescription("Input for saving a widget")
data class SaveWidgetInput(
    @GraphQLDescription("The unique identifier of the widget (optional)")
    val id: String? = null,
    @GraphQLDescription("The widget name")
    val name: String,
    @GraphQLDescription("The widget description")
    val description: String? = null,
    @GraphQLDescription("The HTML content of the widget")
    val html: String,
    @GraphQLDescription("The JSON schema")
    val jsonSchema: String,
    @GraphQLDescription("Preview image or data")
    val preview: String? = null
)
