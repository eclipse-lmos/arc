// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("ktlint")

package org.eclipse.lmos.arc.runner

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Command(
    name = "install",
    mixinStandardHelpOptions = true,
    description = ["Installs a Agents."],
)
class InstallAgent : Runnable {

    @Parameters(
        index = "0",
        description = ["The name of the Agent to install."],
    )
    private var agent: String = ""

    override fun run() {
        if (agent.isEmpty()) {
            println("Invalid Agent name.")
            return
        }
        if (agent.contains(".") || agent.contains("/")) {
            println("Invalid Agent name. Please provide a valid Agent name without '.' or '/'...")
            return
        }
        println("Installing Arc Runner...")
        val fullName = "$agent.agent.kts"
        val `in` = URL("https://raw.githubusercontent.com/eclipse-lmos/arc/main/arc-runner/$fullName").openStream()
        Files.copy(`in`, File(home(), fullName).toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}