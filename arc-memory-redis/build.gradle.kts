// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    api(project(":arc-agents"))

    implementation(libs.redis.lettuce)
    implementation(libs.slf4j.api)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.databind)

    testImplementation("com.redis:testcontainers-redis:2.2.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
}
