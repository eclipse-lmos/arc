// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    api(project(":arc-agents"))
    implementation(project(":arc-scripting"))
    implementation(project(":arc-mcp"))
    implementation(project(":arc-langchain4j-client"))

    compileOnly(project(":arc-azure-client"))
    compileOnly(project(":arc-ollama-client"))
    compileOnly(project(":arc-gen"))
    compileOnly(project(":arc-openai-client"))

    // Azure
    compileOnly("com.azure:azure-identity:1.15.0")

    // MCP
    compileOnly("org.springframework.ai:spring-ai-mcp-server-webflux-spring-boot-starter:1.0.0-M6")

    // Micrometer
    compileOnly("io.micrometer:micrometer-registry-atlas:1.14.5")
    implementation(platform("io.micrometer:micrometer-tracing-bom:1.4.3"))
    compileOnly("io.micrometer:micrometer-tracing")
    compileOnly("io.micrometer:micrometer-registry-otlp")

    val langchain4jVersion = "0.36.2"
    compileOnly("dev.langchain4j:langchain4j-bedrock:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-google-ai-gemini:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")

    implementation("org.springframework.boot:spring-boot-autoconfigure:3.4.2")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.4.3")
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.4.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.4")
    testImplementation("org.springframework.boot:spring-boot-starter:3.4.4")
}
