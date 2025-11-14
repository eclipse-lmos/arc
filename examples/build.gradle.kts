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
    implementation("dev.langchain4j:langchain4j-ollama:1.8.0")

    // Test
    testImplementation("org.springframework.ai:spring-ai-mcp-server-webflux-spring-boot-starter:1.0.0-M6")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux:3.4.3")
    testImplementation(project(":arc-graphql-spring-boot-starter"))
    testImplementation("com.expediagroup:graphql-kotlin-spring-server:8.8.1")
}
