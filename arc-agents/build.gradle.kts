// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    implementation(libs.slf4j.api)

    implementation("dev.openfeature:sdk:1.15.1")
    implementation("dev.openfeature.contrib.providers:flagd:0.11.10")
}
