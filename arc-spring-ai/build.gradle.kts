// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(project(":arc-agents"))
    implementation(project(":arc-result"))
    implementation("org.slf4j:slf4j-api:1.7.25")

    // spring.ai
    implementation(platform("org.springframework.ai:spring-ai-bom:1.0.0-SNAPSHOT"))
    compileOnly("org.springframework.ai:spring-ai-core")

    // testing
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.0")
    testImplementation("org.springframework.boot:spring-boot-starter:3.4.0")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux:3.4.0")
    testImplementation("org.springframework.ai:spring-ai-core")
    testImplementation("org.springframework.ai:spring-ai-ollama")

    // Ktor Server for tests
    testImplementation("io.ktor:ktor-server-core-jvm:2.3.11")
    testImplementation("io.ktor:ktor-server-netty-jvm:2.3.11")
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}
