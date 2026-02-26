// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound.mutation

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Mutation
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.formatToString
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases

/**
 * GraphQL query for compiling ADL code.
 * Takes ADL code as input (string) and returns compiled output as a formatted string.
 */
class AdlCompilerMutation : Mutation {

    /**
     * Compiles the given ADL code and returns the formatted output.
     * @param adl The ADL code to compile.
     * @return An object containing the compiled output as a string in the field 'compiledOutput'.
     */
    @GraphQLDescription("Compiles the given ADL code.")
    suspend fun compile(adl: String, conditionals: List<String> = emptyList()): CompileResult {
        val useCases: List<UseCase> = adl.toUseCases()
        val compiledOutput = useCases.formatToString(conditions = conditionals.toSet())
        return CompileResult(compiledOutput)
    }
}

/**
 * Data class for the compile result.
 */
data class CompileResult(val compiledOutput: String)

data class AdlSource(val adl: String)
