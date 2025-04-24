// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-agents"))
    implementation(project(":arc-graphql-spring-boot-starter"))
    implementation(project(":arc-view-spring-boot-starter"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.static)
    implementation(libs.graphql.kotlin.ktor)
}
