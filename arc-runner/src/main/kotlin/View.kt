// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.awt.Desktop
import java.net.URI

@Command(
    name = "view",
    description = ["Opens the Arc View in the browser."],
)
open class OpenView : Runnable {

    @Parameters(
        index = "0",
        description = ["The port the Agent server is running on. Defaults to 8080."],
        defaultValue = "8080",
    )
    private var port: String = "8080"

    override fun run() {
        println("Opening Arc View...")
        try {
            Desktop.getDesktop().browse(URI("http://localhost:$port/chat/index.html"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
