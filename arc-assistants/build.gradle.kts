// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    api(project(":arc-result"))
    api(project(":arc-agents"))
    implementation("org.slf4j:slf4j-api:2.0.17")

    testImplementation(project(":adl-kotlin-runner"))
}
