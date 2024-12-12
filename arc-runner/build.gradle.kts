// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    val ktorVersion = "2.3.13"
    val langchain4jVersion = "0.36.2"
    val graphqlKotlinVersion = "8.2.1"
    val logbackVersion = "1.5.12"

    implementation(project(":arc-agents"))
    implementation(project(":arc-result"))
    implementation(project(":arc-api"))
    implementation(project(":arc-azure-client"))
    implementation(project(":arc-langchain4j-client"))
    implementation(project(":arc-scripting"))
    implementation(project(":arc-graphql-spring-boot-starter"))
    implementation(project(":arc-reader-html"))
    implementation(project(":arc-reader-pdf"))

    // Picocli
    implementation("info.picocli:picocli:4.7.6")

    // Azure
    implementation("com.azure:azure-identity:1.13.1")
    implementation("com.azure:azure-core-tracing-opentelemetry:1.0.0-beta.52")

    // GraphQL
    implementation("com.expediagroup:graphql-kotlin-ktor-server:$graphqlKotlinVersion")

    // Langchain4j
    implementation("dev.langchain4j:langchain4j-bedrock:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-ollama:$langchain4jVersion")
    implementation("dev.langchain4j:langchain4j-open-ai:$langchain4jVersion")

    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Tests
    testImplementation(project(":arc-agent-client"))
}
