// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    api(project(":arc-agents"))
    implementation(project(":arc-scripting"))
    implementation(project(":arc-langchain4j-client"))

    compileOnly(project(":arc-azure-client"))
    compileOnly(project(":arc-ollama-client"))
    compileOnly(project(":arc-gen"))
    compileOnly(project(":arc-openai-client"))

    compileOnly("io.micrometer:micrometer-registry-atlas:1.14.1")
    compileOnly("com.azure:azure-identity:1.15.0")

    val langchain4jVersion = "1.0.0-beta1"
    compileOnly("dev.langchain4j:langchain4j-bedrock:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-google-ai-gemini:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-azure-open-ai:$langchain4jVersion")

    implementation("org.springframework.boot:spring-boot-autoconfigure:3.4.2")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.4.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.2")
    testImplementation("org.springframework.boot:spring-boot-starter:3.4.2")
}
