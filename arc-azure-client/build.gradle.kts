// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.25")

    // Azure
    api("com.azure:azure-ai-openai:1.0.0-beta.7")
    api("com.azure:azure-core-tracing-opentelemetry:1.0.0-beta.52")
}
