// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation(libs.slf4j.api)

    // jsoup
    implementation("org.jsoup:jsoup:1.19.1")
    implementation("org.apache.pdfbox:pdfbox:3.0.3")
}
