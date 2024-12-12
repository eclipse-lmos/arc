// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner

import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

@Command(
    name = "set",
    mixinStandardHelpOptions = true,
    description = ["Sets a property for the runner. The following properties are supported: ARC_AI_KEY, ARC_AI_URL, ARC_MODEL, ARC_CLIENT."],
)
open class SetProperty : Runnable {

    @Parameters(
        index = "0",
        description = ["The name of the property to set."],
    )
    private var name: String = ""

    @Parameters(
        index = "1",
        description = ["The value of the property to set."],
    )
    private var value: String = ""

    private val propertyNames: Set<String> = setOf("ARC_AI_KEY", "ARC_AI_URL", "ARC_MODEL", "ARC_CLIENT")

    override fun run() {
        if (!propertyNames.contains(name)) {
            println("Invalid property name. Please provide a valid property name: $propertyNames")
        }
        if (value.isEmpty()) {
            println("Invalid property value. Property value cannot be empty.")
        }
        val properties = loadProperties()
        properties[name] = value
        properties.storeInHome()
    }
}
