// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-agents"))
    implementation(project(":arc-azure-client"))
    implementation(project(":arc-server"))
    implementation(project(":arc-mcp"))
    implementation(project(":arc-scripting"))
    implementation(project(":arc-langchain4j-client"))
    implementation("dev.langchain4j:langchain4j-ollama:1.9.1")

    // Test
    testImplementation(libs.spring.ai.starter.mcp.server.webflux)
    testImplementation(libs.spring.boot.starter.webflux)
    testImplementation(project(":arc-graphql-spring-boot-starter"))
    testImplementation(libs.graphql.kotlin.spring)
}
