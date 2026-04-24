// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

dependencies {
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.cio.jvm)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.otel)
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.56.0")
    implementation("io.opentelemetry:opentelemetry-sdk:1.56.0")

    api(project(":arc-api"))

    testImplementation(project(":arc-result"))
    testImplementation(project(":arc-agents"))
    testImplementation(project(":arc-graphql-spring-boot-starter"))
    testImplementation(project(":arc-spring-boot-starter"))
    // Aligned with arc-graphql-spring-boot-starter: graphql-kotlin 8.5.0 is incompatible
    // with Spring Boot 4.0 (JacksonAutoConfiguration was removed/relocated). Pin to 3.5.0 for tests.
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.0")
    testImplementation("org.springframework.boot:spring-boot-starter:3.5.0")
}

// Force Spring Boot 3.5.0 across the test runtime classpath because graphql-kotlin 8.5.0
// (transitively pulled in via arc-graphql-spring-boot-starter) references the
// pre-Spring-Boot-4 location of JacksonAutoConfiguration.
configurations.matching { it.name.startsWith("test") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.springframework.boot") {
            useVersion("3.5.0")
            because("graphql-kotlin 8.5.0 requires the Spring Boot 3.x JacksonAutoConfiguration")
        }
    }
}

