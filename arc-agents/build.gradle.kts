// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(platform("io.opentelemetry:opentelemetry-bom:1.47.0"))
    implementation(project(":arc-result"))
    implementation(libs.slf4j.api)
    implementation("io.opentelemetry:opentelemetry-extension-kotlin")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.13.1")
}
