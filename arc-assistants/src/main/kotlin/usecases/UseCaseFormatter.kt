// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases

/**
 * Formats the given use cases to a string.
 *
 * The useAlternatives set is used to filter solutions.
 * Solutions with the headline "#### Alternative Solution" are filtered
 * unless the use case name is contained in the given set of alternatives.
 * In this case, the "primary" solution is filtered.
 */
suspend fun List<UseCase>.formatToString(
    useAlternatives: Set<String> = emptySet(),
    useFallbacks: Set<String> = emptySet(),
    conditions: Set<String> = emptySet(),
    exampleLimit: Int = 10_000,
    outputOptions: OutputOptions = OutputOptions(),
    usedUseCases: List<String> = emptyList(),
    allUseCases: List<UseCase>? = null,
    formatter: suspend (String, UseCase, List<UseCase>?, List<String>) -> String = { s, _, _, _ -> s },
): String = buildString {
    this@formatToString.filter { !it.subUseCase }.filter { it.matches(conditions) }.forEach { useCase ->
        val useAlternative = useAlternatives.contains(useCase.id) && useCase.alternativeSolution.isNotEmpty()
        val useFallback = useFallbacks.contains(useCase.id) && useCase.fallbackSolution.isNotEmpty()
        val allConditions = conditions + "step_${usedUseCases.count { useCase.id == it } + 1}"
        val temp = StringBuilder()

        temp.append(
            """
            |### UseCase: ${useCase.id}
            |#### Description
            |${useCase.description}
            |
            """.trimMargin(),
        )
        if (useCase.steps.isNotEmpty() && outputOptions.outputSolution != false) {
            temp.append("#### Steps\n")
            useCase.steps.output(allConditions, temp)
        }
        if (!useAlternative && !useFallback && outputOptions.outputSolution != false) {
            temp.append("#### Solution\n")
            useCase.solution.output(allConditions, temp)
        }
        if (useAlternative && !useFallback && outputOptions.outputSolution != false) {
            temp.append("#### Solution\n")
            useCase.alternativeSolution.output(allConditions, temp)
        }
        if (useFallback && outputOptions.outputSolution != false) {
            temp.append("#### Solution\n")
            useCase.fallbackSolution.output(allConditions, temp)
        }
        if (useCase.examples.isNotEmpty() && outputOptions.outputExamples != false) {
            temp.append("#### Examples\n")
            useCase.examples.split("\n").take(exampleLimit).forEach { example ->
                temp.appendLine(example)
            }
        }
        temp.append("\n----\n\n")
        append(formatter(temp.toString(), useCase, allUseCases, usedUseCases))
    }
}.replace("\n\n\n", "\n\n")

suspend fun UseCase.formatToString(
    useAlternatives: Set<String> = emptySet(),
    useFallbacks: Set<String> = emptySet(),
    conditions: Set<String> = emptySet(),
    exampleLimit: Int = 10_000,
    outputOptions: OutputOptions = OutputOptions(),
    usedUseCases: List<String> = emptyList(),
    allUseCases: List<UseCase>? = null,
    formatter: suspend (String, UseCase, List<UseCase>?, List<String>) -> String = { s, _, _, _ -> s },
): String = listOf(this).formatToString(
    useAlternatives,
    useFallbacks,
    conditions,
    exampleLimit,
    outputOptions,
    usedUseCases,
    allUseCases,
    formatter,
)

/**
 * Outputs the conditionals to the given StringBuilder based on the provided conditions.
 *
 * This function processes a list of conditionals and appends the relevant text to the output
 * StringBuilder if the conditions match. It handles grouping of conditionals and ensures that
 * only the appropriate text is included based on the current set of conditions.
 *
 * @receiver List of Conditional objects to be processed.
 * @param conditions Set of conditions to match against.
 * @param output StringBuilder to which the matched text will be appended.
 */
fun List<Conditional>.output(conditions: Set<String>, output: StringBuilder) {
    var currentConditionals = emptySet<String>()
    var temp = StringBuilder()
    forEach {
        if (it.conditions.isEmpty() && it.endConditional) {
            if (currentConditionals.matches(conditions)) {
                output.append(temp.toString())
                if (it.text.isNotBlank()) output.append("${it.text}\n")
            }
            temp = StringBuilder()
            currentConditionals = emptySet()
        } else {
            if (it.conditions.isNotEmpty()) {
                currentConditionals = it.conditions
                output.append(temp.toString())
                temp = StringBuilder()
            }
            if (it.matches(conditions)) temp.append("${it.text}\n")
        }
    }
    output.append(temp.toString())
}

/**
 * Options for outputting use case results.
 *
 * @property outputSolution Whether to output the solution of the use case.
 * @property outputExamples Whether to output examples related to the use case.
 */
data class OutputOptions(
    val outputSolution: Boolean? = null,
    val outputExamples: Boolean? = null,
)
