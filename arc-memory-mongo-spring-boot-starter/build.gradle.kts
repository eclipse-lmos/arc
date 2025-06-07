// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    api(project(":arc-agents"))

    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.configuration.processor)
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive:3.5.0")

    testImplementation("org.springframework.boot:spring-boot-testcontainers:3.5.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.0")
    testImplementation("org.springframework.boot:spring-boot-starter:3.5.0")
    testImplementation("org.testcontainers:mongodb:1.21.1")
    testImplementation("org.testcontainers:junit-jupiter:1.15.1")
}
