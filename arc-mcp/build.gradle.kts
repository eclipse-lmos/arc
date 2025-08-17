// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")

    // MCP
    implementation(libs.mcp.sdk)

    // Test
    testImplementation(project(":arc-spring-boot-starter"))
    testImplementation(libs.spring.ai.starter.mcp.server.webflux)
    testImplementation(libs.spring.boot.starter.webflux)
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.0")
}
