// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.kotlin.runner

import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget

object ScriptConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(
            "org.eclipse.lmos.arc.kotlin.runner.extensions.*",
        )

        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }

        jvm {
            jvmTarget("21")
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
    },
)
