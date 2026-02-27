// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    api(project(":arc-agents"))
    api(project(":arc-mcp"))
    implementation(project(":arc-scripting"))

    compileOnly(project(":arc-langchain4j-client"))
    compileOnly(project(":arc-azure-client"))
    compileOnly(project(":arc-openai-client"))
    compileOnly(project(":arc-gen"))

    // Azure
    compileOnly("com.azure:azure-identity:1.18.2")

    // MCP
    compileOnly(libs.spring.ai.starter.mcp.server.webflux)
    compileOnly(libs.mcp.spring.webflux)

    // Micrometer
    compileOnly("io.micrometer:micrometer-registry-atlas:1.14.6")
    implementation(platform("io.micrometer:micrometer-tracing-bom:1.4.5"))
    compileOnly("io.micrometer:micrometer-tracing")
    compileOnly("io.micrometer:micrometer-registry-otlp")

    val langchain4jVersion = "1.9.1"
    compileOnly("dev.langchain4j:langchain4j-bedrock:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-google-ai-gemini:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")

    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.configuration.processor)
    implementation(libs.spring.boot.starter.webflux)

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.0")
    testImplementation("org.springframework.boot:spring-boot-starter:3.5.0")
    testImplementation(project(":arc-langchain4j-client"))
    testImplementation(project(":arc-azure-client"))
    testImplementation("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
}
