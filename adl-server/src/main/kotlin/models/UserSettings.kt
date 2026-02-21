// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.models

import com.expediagroup.graphql.generator.annotations.GraphQLDescription

@GraphQLDescription("User settings containing API key and model name")
data class UserSettings(
    @GraphQLDescription("The API key. When retrieved, only the last 3 characters are visible.")
    val apiKey: String,

    @GraphQLDescription("The model name")
    val modelName: String
)

