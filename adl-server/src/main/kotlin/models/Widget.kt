// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("A UI widget")
data class Widget(
    @GraphQLDescription("The unique identifier of the widget")
    val id: String,
    @GraphQLDescription("The widget name")
    val name: String,
    @GraphQLDescription("The HTML content of the widget")
    val html: String,
    @GraphQLDescription("The JSON schema")
    val jsonSchema: String,
    @GraphQLDescription("Preview image or data")
    val preview: String? = null
)

