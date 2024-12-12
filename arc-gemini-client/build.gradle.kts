// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.25")

    // vertexai
    api("com.google.cloud:google-cloud-vertexai:0.7.0")
    // api("com.google.ai.client.generativeai:generativeai:0.3.0")
}
