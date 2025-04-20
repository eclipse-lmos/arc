// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-agents"))
    implementation(project(":arc-azure-client"))
    implementation(project(":arc-langchain4j-client"))
    implementation("dev.langchain4j:langchain4j-ollama:0.36.2")
    implementation(libs.slf4j.jdk14)
}
