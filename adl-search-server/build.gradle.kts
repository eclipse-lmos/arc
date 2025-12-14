// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.ktor)
}

application {
    mainClass = "org.eclipse.lmos.adl.server.AdlServerKt"
}

tasks.named<KotlinJvmCompile>("compileTestKotlin") {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("21")
    }
}

dependencies {
    implementation(project(":arc-api"))
    implementation(project(":arc-assistants"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.static)
    implementation(libs.graphql.kotlin.ktor)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")

    // Qdrant
    implementation("io.qdrant:client:1.15.0")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("com.google.protobuf:protobuf-java:4.33.2")

    // Embeddings
    implementation("dev.langchain4j:langchain4j:1.9.1")
    implementation("dev.langchain4j:langchain4j-embeddings:1.9.1-beta17")
    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:1.9.1-beta17")

    // OpenTelemetry dependencies
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.55.0"))
    implementation("io.opentelemetry:opentelemetry-extension-kotlin")
    implementation("io.opentelemetry:opentelemetry-context")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")

    // Test dependencies
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.cio.jvm)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:qdrant:1.20.4")
}
