@file:JvmName("RunKt") // SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner

import picocli.CommandLine.Command
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

@Command(
    name = "arc",
    mixinStandardHelpOptions = true,
    version = ["1.0.0"],
    subcommands = [
        RunArc::class,
        ListAgents::class,
        InstallAgent::class,
        SetProperty::class,
        OpenView::class,
        NewAgent::class,
        NewFunction::class,
        Spring::class,
    ],
    description = ["The Arc CLI."],
)
class Arc

fun home(): File {
    val home = File(System.getProperty("user.home"), ".arc")
    home.mkdirs()
    return home
}

fun loadProperties(): Properties {
    val properties = Properties()
    val propertiesFile = File(home(), "arc.properties")
    if (propertiesFile.exists()) {
        FileInputStream(propertiesFile).use { properties.load(it) }
    }
    return properties
}

fun Properties.storeInHome() {
    val propertiesFile = File(home(), "arc.properties")
    FileOutputStream(propertiesFile).use { store(it, null) }
}
