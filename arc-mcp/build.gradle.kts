// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")

    // MCP
    implementation(platform("io.modelcontextprotocol.sdk:mcp-bom:0.7.0"))
    implementation("io.modelcontextprotocol.sdk:mcp")

    // Test
    testImplementation(project(":arc-spring-boot-starter"))
    testImplementation("org.springframework.ai:spring-ai-mcp-server-webflux-spring-boot-starter:1.0.0-M6")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux:3.4.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.3")
}
