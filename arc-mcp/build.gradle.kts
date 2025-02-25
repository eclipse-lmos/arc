// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")

    // MCP
    implementation(platform("io.modelcontextprotocol.sdk:mcp-bom:0.7.0"))
    implementation("io.modelcontextprotocol.sdk:mcp")
}
