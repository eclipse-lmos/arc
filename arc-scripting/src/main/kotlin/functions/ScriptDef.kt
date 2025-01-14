// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting.functions

import org.eclipse.lmos.arc.agents.dsl.FunctionDefinitionContext
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget

/**
 * Configure Script Execution.
 */
@KotlinScript(
    fileExtension = "functions.kts",
    compilationConfiguration = ScriptConfiguration::class,
)
abstract class ScriptDeps

object ScriptConfiguration : ScriptCompilationConfiguration(
    {
        implicitReceivers(FunctionDefinitionContext::class)

        compilerOptions("-Xcontext-receivers")

        defaultImports(
            "org.eclipse.lmos.arc.agents.dsl.get",
            "org.eclipse.lmos.arc.agents.dsl.*",
            "org.eclipse.lmos.arc.core.*",
            "org.eclipse.lmos.arc.agents.dsl.extensions.*",
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
