// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-result"))
    implementation(project(":arc-agents"))

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.25")

    // Kotlin Scripting
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:2.1.0")
    // implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies")
    // implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies-maven")
}
