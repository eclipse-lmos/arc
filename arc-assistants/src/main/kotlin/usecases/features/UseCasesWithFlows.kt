// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.usecases.features

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.useCases
import org.eclipse.lmos.arc.assistants.support.usecases.OutputOptions
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import java.io.File

/**
 * Process use cases that use flow options.
 */
suspend fun DSLContext.useCasesWithFlows(
    name: String,
    fallbackLimit: Int = 2,
    conditions: Set<String> = emptySet(),
    useCaseFolder: File? = null,
    exampleLimit: Int = 4,
    outputOptions: OutputOptions = OutputOptions(),
    filter: (UseCase) -> Boolean = { true },
    model: String? = null,
): String {
    return useCases(
        name,
        fallbackLimit = fallbackLimit,
        conditions = conditions,
        useCaseFolder = useCaseFolder,
        exampleLimit = exampleLimit,
        outputOptions = outputOptions,
        filter = filter,
        formatter = { content, useCase, useCases, usedUseCases ->
            processFlow(
                content = content,
                useCase = useCase,
                allUseCases = useCases,
                usedUseCases = usedUseCases,
                conditions = conditions,
                context = this,
                model = model,
            )
        },
    )
}
