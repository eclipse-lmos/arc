// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.cio.jvm)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.otel)
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.54.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.54.0")

    implementation(project(":arc-api"))

    testImplementation(project(":arc-result"))
    testImplementation(project(":arc-agents"))
    testImplementation(project(":arc-graphql-spring-boot-starter"))
    testImplementation(project(":arc-spring-boot-starter"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter)
}
