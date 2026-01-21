// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.ktor)
    id("sh.ondr.koja") version "0.4.6"
    id("org.graalvm.buildtools.native") version "0.11.3"
}

application {
    mainClass = "org.eclipse.lmos.adl.server.AdlServerKt"
}

graalvmNative {
    binaries {
        named("main") {
            fallback.set(false)
            verbose.set(true)

            buildArgs.add("--initialize-at-build-time=ch.qos.logback")
            buildArgs.add("--initialize-at-build-time=io.ktor,kotlin")
            buildArgs.add("--initialize-at-build-time=org.slf4j.LoggerFactory")

            buildArgs.add("--initialize-at-build-time=org.slf4j.helpers.Reporter")
            buildArgs.add("--initialize-at-build-time=kotlinx.io.bytestring.ByteString")
            buildArgs.add("--initialize-at-build-time=kotlinx.io.SegmentPool")

            buildArgs.add("--initialize-at-build-time=kotlinx.serialization.json.Json")
            buildArgs.add("--initialize-at-build-time=kotlinx.serialization.json.JsonImpl")
            buildArgs.add("--initialize-at-build-time=kotlinx.serialization.json.ClassDiscriminatorMode")
            buildArgs.add("--initialize-at-build-time=kotlinx.serialization.modules.SerializersModuleKt")

            buildArgs.add("-H:+InstallExitHandlers")
            buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")
            buildArgs.add("-H:+ReportExceptionStackTraces")

            imageName.set("graalvm-server")
        }

        named("test"){
            fallback.set(false)
            verbose.set(true)

            buildArgs.add("--initialize-at-build-time=ch.qos.logback")
            buildArgs.add("--initialize-at-build-time=io.ktor,kotlin")
            buildArgs.add("--initialize-at-build-time=org.slf4j.LoggerFactory")

            buildArgs.add("--initialize-at-build-time=org.slf4j.helpers.Reporter")
            buildArgs.add("--initialize-at-build-time=kotlinx.io.bytestring.ByteString")
            buildArgs.add("--initialize-at-build-time=kotlinx.io.SegmentPool")

            buildArgs.add("--initialize-at-build-time=kotlinx.serialization.json.Json")
            buildArgs.add("--initialize-at-build-time=kotlinx.serialization.json.JsonImpl")
            buildArgs.add("--initialize-at-build-time=kotlinx.serialization.json.ClassDiscriminatorMode")
            buildArgs.add("--initialize-at-build-time=kotlinx.serialization.modules.SerializersModuleKt")

            buildArgs.add("-H:+InstallExitHandlers")
            buildArgs.add("-H:+ReportUnsupportedElementsAtRuntime")
            buildArgs.add("-H:+ReportExceptionStackTraces")

            val path = "${projectDir}/src/test/resources/META-INF/native-image/"
            buildArgs.add("-H:ReflectionConfigurationFiles=${path}reflect-config.json")
            buildArgs.add("-H:ResourceConfigurationFiles=${path}resource-config.json")

            imageName.set("adl-server-test")
        }
    }
}

dependencies {
    implementation(project(":arc-assistants"))
    implementation(project(":arc-api"))
    implementation(project(":adl-kotlin-runner"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.static)
    implementation(libs.ktor.server.cors)
    implementation(libs.graphql.kotlin.ktor)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.10.2")

    // Logback
    implementation(libs.logback.classic)

    // LLM Clients
    implementation(project(":arc-azure-client"))
    // implementation("dev.langchain4j:langchain4j-ollama:1.8.0")

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
    testImplementation("org.testcontainers:testcontainers:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:qdrant:1.21.4")
}
