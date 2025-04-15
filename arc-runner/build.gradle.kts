// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    val ktorVersion = "3.1.2"
    val langchain4jVersion = "0.36.2"
    val graphqlKotlinVersion = "8.3.0"
    val logbackVersion = "1.5.17"
    val arcVersion = "0.121.0"

    // Arc
    implementation(project(":arc-view-spring-boot-starter"))
    implementation("org.eclipse.lmos:arc-azure-client:$arcVersion")
    implementation("org.eclipse.lmos:arc-spring-boot-starter:$arcVersion")
    implementation("org.eclipse.lmos:arc-reader-pdf:$arcVersion")
    implementation("org.eclipse.lmos:arc-reader-html:$arcVersion")
    implementation("org.eclipse.lmos:arc-assistants:$arcVersion")
    implementation("org.eclipse.lmos:arc-reader-html:$arcVersion")
    implementation("org.eclipse.lmos:arc-api:$arcVersion")
    implementation("org.eclipse.lmos:arc-graphql-spring-boot-starter:$arcVersion")
    implementation("org.eclipse.lmos:arc-agents:$arcVersion")
    implementation("org.eclipse.lmos:arc-result:$arcVersion")
    implementation("org.eclipse.lmos:arc-langchain4j-client:$arcVersion")
    implementation("org.eclipse.lmos:arc-openai-client:$arcVersion")
    implementation("org.eclipse.lmos:arc-scripting:$arcVersion")

    // Picocli
    implementation("info.picocli:picocli:4.7.6")

    // Azure
    implementation("com.azure:azure-identity:1.15.4")
    implementation("com.azure:azure-core-tracing-opentelemetry:1.0.0-beta.56")

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
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Tests
    testImplementation(project(":arc-agent-client"))
}
