// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.query

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.eclipse.lmos.adl.server.models.Widget
import org.eclipse.lmos.adl.server.repositories.WidgetRepository

class WidgetQuery(private val widgetRepository: WidgetRepository) : Query {

    @GraphQLDescription("Get all widgets")
    fun widgets(): List<Widget> {
        return widgetRepository.findAll()
    }

    @GraphQLDescription("Get a widget by ID")
    fun widget(id: String): Widget? {
        return widgetRepository.findById(id)
    }
}

