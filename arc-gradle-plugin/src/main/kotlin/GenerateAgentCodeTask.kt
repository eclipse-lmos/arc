// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.text.Charsets.UTF_8

/**
 * Converts agent and function scripts into Kotlin code.
 */
open class GenerateAgentCodeTask : DefaultTask() {

    @get:InputDirectory
    lateinit var input: Directory

    @Internal // @get:OutputDirectory
    lateinit var output: Directory

    @TaskAction
    fun deploy() {
        println("Generating Agent code...")

        val agentCode = Code()
        input.asFileTree.filter {
            it.name.endsWith(".agent.kts")
        }.forEach { writeCode(it, agentCode, "agent\\s*\\{.*") }
        write("Agents.kt", agentCode, "AgentTemplate.kt.txt")

        val functionCode = Code()
        input.asFileTree.filter {
            it.name.endsWith(".functions.kts")
        }.forEach { writeCode(it, functionCode, "function\\s*\\(.*") }
        write("Functions.kt", functionCode, "FunctionTemplate.kt.txt")
    }

    private fun write(name: String, code: Code, template: String) {
        val outputFile = File(output.asFile, name)
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            readTemplate(template)
                .replace("//@@IMPORTS@@", code.imports.joinToString(""))
                .replace("//@@CODE@@", code.code)
                .replace("//@@OUTER_CODE@@", code.outerCode),
        )
    }

    private fun writeCode(file: File, code: Code, codeStart: String) {
        var reading: String? = null
        file.readLines(charset = UTF_8).forEach { line ->
            if (line.trim().startsWith("// Code")) {
                code.outerCode += line + "\n"
                reading = "outerCode"
            } else if (line.trim().matches(codeStart.toRegex())) {
                code.code += line + "\n"
                reading = "code"
            } else if (reading != null) {
                when (reading) {
                    "outerCode" -> code.outerCode += line + "\n"
                    "code" -> code.code += line + "\n"
                }
            } else if (line.trim().startsWith("import ")) {
                code.imports += line + "\n"
            }
        }
    }

    private fun readTemplate(name: String): String {
        return Thread.currentThread().contextClassLoader.getResource(name)?.readText()!!
    }
}

data class Code(
    var imports: Set<String> = emptySet(),
    var code: String = "",
    var outerCode: String = "",
)
