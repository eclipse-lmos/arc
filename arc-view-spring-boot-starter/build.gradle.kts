// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-assistants"))
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.4.4")
    implementation("org.springframework.boot:spring-boot-configuration-processor:3.4.4")
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.5.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.0")
}
