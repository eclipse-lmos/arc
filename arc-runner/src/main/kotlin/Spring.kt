// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import kotlin.text.Charsets.UTF_8

@Command(
    name = "spring",
    mixinStandardHelpOptions = true,
    description = ["Creates a new, empty spring boot host project."],
)
open class Spring : Runnable {

    private val toDelete = listOf(
        ".reuse",
        ".github",
        "LICENSES",
        "CODE_OF_CONDUCT.md",
        "README.md",
    )

    @Parameters(
        index = "0",
        description = ["The name of the project."],
    )
    private var name: String = ""

    override fun run() {
        if (name.isEmpty()) {
            println("Invalid agent name. Please provide a valid agent name.")
        }
        ProcessBuilder("git", "clone", "https://github.com/eclipse-lmos/arc-spring-init", name)
            .start()
            .apply {
                inputStream.bufferedReader().use { reader ->
                    reader.lines().forEach { println(it) }
                }
            }
            .waitFor()
        println(File(name))
        println("Run './gradlew bootRun' to start the server.")
        File(name, "settings.gradle.kts").let {
            it.writeText(it.readText(UTF_8).replace("arc-spring-init", name))
        }
        toDelete.forEach { File(name, it).deleteRecursively() }
    }
}
