// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.System.getenv
import java.net.URI

plugins {
    kotlin("jvm") version "2.0.10" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.dokka") version "1.9.20"
    id("org.cyclonedx.bom") version "1.8.2" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("net.researchgate.release") version "3.0.2"
    id("com.vanniktech.maven.publish") version "0.29.0"
}

subprojects {
    group = "ai.ancf.lmos"

    apply(plugin = "org.cyclonedx.bom")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "kotlinx-serialization")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jetbrains.kotlinx.kover")
    apply(plugin = "com.vanniktech.maven.publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        debug.set(true)
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            freeCompilerArgs += "-Xcontext-receivers"
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
        dependsOn(tasks.dokkaJavadoc)
        from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }

    mavenPublishing {
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()

        pom {
            name = "ARC"
            description = "ARC is an AI framework."
            url = "https://github.com/lmos-ai/arc"
            licenses {
                license {
                    name = "Apache-2.0"
                    distribution = "repo"
                    url = "https://github.com/lmos-ai/arc/blob/main/LICENSES/Apache-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "pat"
                    name = "Patrick Whelan"
                    email = "opensource@telekom.de"
                }
                developer {
                    id = "bharat_bhushan"
                    name = "Bharat Bhushan"
                    email = "opensource@telekom.de"
                }
                developer {
                    id = "merrenfx"
                    name = "Max Erren"
                    email = "opensource@telekom.de"
                }
            }
            scm {
                url = "https://github.com/lmos-ai/arc.git"
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = URI("https://maven.pkg.github.com/lmos-ai/arc")
                credentials {
                    username = findProperty("GITHUB_USER")?.toString() ?: getenv("GITHUB_USER")
                    password = findProperty("GITHUB_TOKEN")?.toString() ?: getenv("GITHUB_TOKEN")
                }
            }
        }
    }

    if (!project.name.endsWith("-bom")) {
        dependencies {
            val kotlinXVersion = "1.8.1"
            "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinXVersion")
            "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinXVersion")
            "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinXVersion")
            "implementation"("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

            // Testing
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
            "testImplementation"("org.assertj:assertj-core:3.26.3")
            "testImplementation"("io.mockk:mockk:1.13.10")
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    tasks.named("dokkaJavadoc") {
        mustRunAfter("checksum")
    }
}

dependencies {
    kover(project("arc-scripting"))
    kover(project("arc-azure-client"))
    kover(project("arc-ollama-client"))
    //kover(project("arc-gemini-client"))
    kover(project("arc-result"))
    kover(project("arc-reader-pdf"))
    kover(project("arc-reader-html"))
    kover(project("arc-agents"))
    kover(project("arc-spring-boot-starter"))
    kover(project("arc-memory-mongo-spring-boot-starter"))
    //kover(project("arc-spring-ai"))
    kover(project("arc-api"))
    kover(project("arc-graphql-spring-boot-starter"))
    kover(project("arc-agent-client"))
    kover(project("arc-assistants"))
    kover(project("arc-langchain4j-client"))
}

repositories {
    mavenCentral()
}

fun Project.java(configure: Action<JavaPluginExtension>): Unit =
    (this as ExtensionAware).extensions.configure("java", configure)

fun String.execWithCode(workingDir: File? = null): Pair<CommandResult, Sequence<String>> {
    ProcessBuilder().apply {
        workingDir?.let { directory(it) }
        command(split(" "))
        redirectErrorStream(true)
        val process = start()
        val result = process.readStream()
        val code = process.waitFor()
        return CommandResult(code) to result
    }
}

class CommandResult(val code: Int) {

    val isFailed = code != 0
    val isSuccess = !isFailed

    fun ifFailed(block: () -> Unit) {
        if (isFailed) block()
    }
}

/**
 * Executes a string as a command.
 */
fun String.exec(workingDir: File? = null) = execWithCode(workingDir).second

fun Project.isBOM() = name.endsWith("-bom")

private fun Process.readStream() = sequence<String> {
    val reader = BufferedReader(InputStreamReader(inputStream))
    try {
        var line: String?
        while (true) {
            line = reader.readLine()
            if (line == null) {
                break
            }
            yield(line)
        }
    } finally {
        reader.close()
    }
}

release {
    buildTasks = listOf("releaseBuild")
    ignoredSnapshotDependencies = listOf("org.springframework.ai:spring-ai-bom")
    newVersionCommitMessage = "New Snapshot-Version:"
    preTagCommitMessage = "Release:"
}

tasks.register("releaseBuild") {
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("build") })
}
