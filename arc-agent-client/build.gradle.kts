// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.cio.jvm)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.core)

    implementation(project(":arc-api"))

    testImplementation(project(":arc-result"))
    testImplementation(project(":arc-agents"))
    testImplementation(project(":arc-graphql-spring-boot-starter"))
    testImplementation(project(":arc-spring-boot-starter"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter)
}
