// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")

    // Kotlin Scripting
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.1.20")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.1.20")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.1.21")
    // implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies")
    // implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")
}
