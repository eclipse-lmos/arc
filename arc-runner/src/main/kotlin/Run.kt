// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner

import org.eclipse.lmos.arc.runner.server.AIClientConfig
import org.eclipse.lmos.arc.runner.server.AppConfig
import org.eclipse.lmos.arc.runner.server.runApp
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File

@Command(
    name = "run",
    mixinStandardHelpOptions = true,
    description = ["Starts the Arc Runner."],
)
class RunArc : Runnable {

    @Parameters(
        index = "0",
        defaultValue = "HOME",
        description = ["The name of the folder containing the agents to run. Defaults to HOME."],
    )
    private var agentFolder: String = ""

    override fun run() {
        println("Staring Arc Runner...")

        val properties = loadProperties()

        val accessKey = System.getenv("ARC_AI_ACCESS_KEY") ?: properties.getProperty("ARC_AI_ACCESS_KEY")
        val accessSecret = System.getenv("ARC_AI_ACCESS_SECRET") ?: properties.getProperty("ARC_AI_ACCESS_SECRET")

        val aiKey = System.getenv("ARC_AI_KEY") ?: properties.getProperty("ARC_AI_KEY")
        val aiUrl = System.getenv("ARC_AI_URL") ?: properties.getProperty("ARC_AI_URL")
        if (aiUrl == null && aiKey == null) {
            println("Please set either ARC_AI_URL and ARC_AI_KEY environment variables...")
            return
        }

        val model = System.getenv("ARC_MODEL") ?: properties.getProperty("ARC_MODEL")
        if (model == null) {
            println("Please set ARC_MODEL. For example: 'gpt-4o'...")
            return
        }
        val client = System.getenv("ARC_CLIENT") ?: properties.getProperty("ARC_CLIENT")
        if (client == null) {
            println("Please set ARC_CLIENT. For example: 'azure', 'openai', 'gemini' or 'ollma'...")
            return
        }

        val agentsHome = if (agentFolder == "HOME" || agentFolder == "") home() else File(agentFolder)

        runApp(
            AppConfig(
                scriptFolder = agentsHome,
                clientConfig = AIClientConfig(
                    id = model,
                    modelName = model,
                    client = client,
                    url = aiUrl,
                    apiKey = aiKey,
                    accessSecret = accessSecret,
                    accessKey = accessKey,
                ),
            ),
        )
    }
}
