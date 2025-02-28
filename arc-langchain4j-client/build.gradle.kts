// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.47.0"))
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.13.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")

    // LangChain4J
    val langchain4jVersion = "1.0.0-beta1"
    compileOnly("dev.langchain4j:langchain4j-bedrock:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-google-ai-gemini:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")
    compileOnly("dev.langchain4j:langchain4j-azure-open-ai:$langchain4jVersion")

    testImplementation("dev.langchain4j:langchain4j-bedrock:$langchain4jVersion")
}
