// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.ktor)
}

application {
    mainClass = "org.eclipse.lmos.adl.server.AdlServerKt"
}

dependencies {
    implementation(project(":arc-assistants"))
    implementation(project(":adl-kotlin-runner"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.static)
    implementation(libs.graphql.kotlin.ktor)

    // OpenTelemetry dependencies
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.55.0"))
    implementation("io.opentelemetry:opentelemetry-extension-kotlin")
    implementation("io.opentelemetry:opentelemetry-context")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
}
