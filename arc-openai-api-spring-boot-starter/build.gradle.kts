// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    api(project(":arc-agents"))
    api(project(":arc-api"))
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.configuration.processor)
    implementation(libs.spring.boot.starter.webflux)

    // Tests
    testImplementation(project(":arc-spring-boot-starter"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("dev.langchain4j:langchain4j-open-ai:1.7.1")
}
