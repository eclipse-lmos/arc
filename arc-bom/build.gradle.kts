// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    constraints {
        api(project(":arc-scripting"))
        api(project(":arc-azure-client"))
        api(project(":arc-ollama-client"))
        api(project(":arc-gemini-client"))
        api(project(":arc-reader-html"))
        api(project(":arc-reader-pdf"))
        api(project(":arc-result"))
        api(project(":arc-agents"))
        api(project(":arc-spring-boot-starter"))
        api(project(":arc-memory-mongo-spring-boot-starter"))
        api(project(":arc-spring-ai"))
        api(project(":arc-api"))
        api(project(":arc-agent-client"))
        api(project(":arc-graphql-spring-boot-starter"))
    }
}
