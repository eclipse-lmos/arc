// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {

    implementation(project(":arc-agents"))
    implementation(project(":arc-result"))
    implementation(project(":arc-api"))

    // Graphql
    implementation("com.expediagroup:graphql-kotlin-spring-server:8.2.1")

    // Spring
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.4.0")

    // Test
    testImplementation(project(":arc-spring-boot-starter"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.0")
}
