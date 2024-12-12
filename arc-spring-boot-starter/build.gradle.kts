// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
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

    compileOnly("io.micrometer:micrometer-registry-atlas:1.14.1")
    compileOnly("com.azure:azure-identity:1.13.1")

    val langchain4jVersion = "0.36.2"
    compileOnly("dev.langchain4j:langchain4j-bedrock:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-google-ai-gemini:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")

    implementation("org.springframework.boot:spring-boot-autoconfigure:3.4.0")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.4.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.0")
    testImplementation("org.springframework.boot:spring-boot-starter:3.4.0")
}
