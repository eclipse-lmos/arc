// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.25")

    // ktor
    val ktorVersion = "2.3.13"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")

    // Ktor Server for tests
    testImplementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
}
