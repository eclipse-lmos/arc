// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Azure
    api("com.azure:azure-ai-openai:1.0.0-beta.7")
    api("com.azure:azure-core-tracing-opentelemetry:1.0.0-beta.52")
}
