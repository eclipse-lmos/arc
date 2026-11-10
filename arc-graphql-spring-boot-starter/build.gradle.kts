// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {

    implementation(project(":arc-agents"))
    implementation(project(":arc-result"))
    implementation(project(":arc-api"))

    // Graphql
    implementation(libs.graphql.kotlin.spring)
    implementation(libs.openfeature.sdk)

    // Spring
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.jackson.kotlin)

    // Test
    testImplementation(project(":arc-spring-boot-starter"))
    testImplementation(libs.spring.boot.starter.test)
}
