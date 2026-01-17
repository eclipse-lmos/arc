// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.model

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("A representation of an Agent Definition Language (ADL) file.")
data class Adl(
    @GraphQLDescription("Unique identifier for the ADL")
    val id: String,

    @GraphQLDescription("The content of the ADL")
    val content: String,

    @GraphQLDescription("Tags associated with the ADL")
    val tags: List<String>,

    @GraphQLDescription("Timestamp when the ADL was created")
    val createdAt: String,

    @GraphQLDescription("Examples included in the ADL")
    val examples: List<String> = emptyList()
)
