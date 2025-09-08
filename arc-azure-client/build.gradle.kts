// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation(libs.slf4j.api)

    // Azure
    api("com.azure:azure-ai-openai:1.0.0-beta.16")
    api("com.azure:azure-core-tracing-opentelemetry:1.0.0-beta.60")
    implementation("com.azure:azure-identity:1.15.4")

    // Tests
    testImplementation(libs.slf4j.jdk14)
}
