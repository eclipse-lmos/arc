// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File

@Command(
    name = "new",
    mixinStandardHelpOptions = true,
    description = ["Creates a new, empty agent file."],
)
open class NewAgent : Runnable {

    @Parameters(
        index = "0",
        description = ["The name of the agent to create."],
    )
    private var name: String = ""

    override fun run() {
        if (name.isEmpty()) {
            println("Invalid agent name. Please provide a valid agent name.")
        }
        val currentFolder = File(".")
        val agentFolder: File
        if (currentFolder.name.contains("agent", ignoreCase = true)) {
            agentFolder = currentFolder
        } else {
            agentFolder = File(currentFolder, "agents")
            agentFolder.mkdirs()
        }
        val agentFile = File(agentFolder, "$name.agent.kts")
        agentFile.writeText(
            """
            |agent {
            |  name = "$name"
            |  tools = AllTools
            |  prompt {
            |    ${"\"\"\""}
            |      ### Role and Responsibilities ###
            |      You are a helpful assistant.
            |      
            |      ## Instructions
            |      - Always answer with 'Arc is ready to assist you in creating the future of Agents!.'
            |    ${"\"\"\""}
            |  }
            |}
            """.trimMargin(),
            Charsets.UTF_8,
        )
    }
}
