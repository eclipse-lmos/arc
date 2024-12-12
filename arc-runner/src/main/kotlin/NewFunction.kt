// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File

@Command(
    name = "fun",
    mixinStandardHelpOptions = true,
    description = ["Creates a new function file."],
)
open class NewFunction : Runnable {

    @Parameters(
        index = "0",
        description = ["The name of the function to create."],
    )
    private var name: String = ""

    override fun run() {
        if (name.isEmpty()) {
            println("Invalid function name. Please provide a valid function name.")
        }
        val currentFolder = File(".")
        val agentFolder: File
        if (currentFolder.name.contains("agent", ignoreCase = true)) {
            agentFolder = currentFolder
        } else {
            agentFolder = File(currentFolder, "agents")
            agentFolder.mkdirs()
        }
        val agentFile = File(agentFolder, "$name.functions.kts")
        agentFile.writeText(
            """
            |function(
            |    name = "$name",
            |    description = "Returns content from the web.",
            |    params = types(
            |        string("url", "The URL of the content to fetch.")
            |    )
            |) { (url) ->
            |   httpGet(url.toString())
            |}
            """.trimMargin(),
            Charsets.UTF_8,
        )
    }
}
