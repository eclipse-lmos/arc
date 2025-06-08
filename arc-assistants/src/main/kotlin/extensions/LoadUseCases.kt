// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.assistants.support.extensions.LoadedUseCases
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import org.eclipse.lmos.arc.assistants.support.usecases.formatToString
import org.eclipse.lmos.arc.assistants.support.usecases.toUseCases
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.random.Random.Default.nextInt

private val log = LoggerFactory.getLogger("UseCasesLoader")

/**
 * Local variables stored by the Use Case extensions.
 */
private const val LOCAL_USE_CASES = "LOCAL_USE_CASES"

/**
 * Loads the use case file with the given name.
 * This will report the loaded use cases to the tracer and store them in the local context.
 */
suspend fun DSLContext.useCases(
    name: String,
    fallbackLimit: Int = 2,
    conditions: Set<String> = emptySet(),
    useCaseFolder: File? = null,
): String {
    return tracer().withSpan("load $name") { tags, _ ->
        tags.tag("openinference.span.kind", "RETRIEVER")
        val requestUseCase = system("usecase", defaultValue = "").takeIf { it.isNotEmpty() }
        var useCases =
            (requestUseCase ?: local(name))?.toUseCases() ?: kotlin.error("No use case file found with the name $name!")

        if (useCaseFolder != null) {
            useCases = useCases.resolveReferences(useCaseFolder)
        }

        val usedUseCases = memory("usedUseCases") as List<String>? ?: emptyList()
        val fallbackCases = usedUseCases.groupingBy { it }.eachCount().filter { it.value >= fallbackLimit }.keys
        val filteredUseCases =
            useCases.formatToString(usedUseCases.toSet(), fallbackCases, loadConditions() + conditions)
        log.info("Loaded use cases: ${useCases.map { it.id }} Fallback cases: $fallbackCases")

        setLocal(LOCAL_USE_CASES, LoadedUseCases(name = name, useCases, usedUseCases, filteredUseCases))
        tags.tag("retrieval.documents.0.document.id", name)
        tags.tag("retrieval.documents.0.document.content", filteredUseCases)
        tags.tag("retrieval.documents.0.document.score", "1.0")
        tags.tag(
            "retrieval.documents.0.document.meta",
            """
                {"version": "${useCases.firstOrNull()?.version ?: "1.0.0"}", "fallbackLimit": "$fallbackLimit", "conditions": "${
                conditions.joinToString(
                    ",",
                )
            }"}
                """,
        )

        filteredUseCases
    }
}

/**
 * Extension function to format a list of use cases into a string representation.
 *
 * This function applies filtering based on previously used use cases and conditions,
 * and tracks the loaded use cases in the local storage.
 *
 * @param useCases The list of use cases to format
 * @param fallbackLimit The minimum number of times a use case must have been used to be considered a fallback
 * @param conditions Optional set of conditions to filter use cases
 * @return A formatted string representation of the use cases
 */
suspend fun DSLContext.processUseCases(
    useCases: List<UseCase>,
    fallbackLimit: Int = 2,
    conditions: Set<String> = emptySet(),
): String {
    val usedUseCases = memory("usedUseCases") as List<String>? ?: emptyList()
    val fallbackCases =
        usedUseCases
            .groupingBy { it }
            .eachCount()
            .filter { it.value >= fallbackLimit }
            .keys
    val filteredUseCases =
        useCases.formatToString(usedUseCases.toSet(), fallbackCases, conditions)
    log.info("Loaded use cases: ${useCases.map { it.id }} Fallback cases: $fallbackCases")

    setLocal(LOCAL_USE_CASES, LoadedUseCases(name = "all", useCases, usedUseCases, filteredUseCases))

    return filteredUseCases
}

/**
 * Gets and sets the current use cases from the local context.
 */
fun DSLContext.getCurrentUseCases(): LoadedUseCases? = getLocal(LOCAL_USE_CASES) as LoadedUseCases?
fun DSLContext.setCurrentUseCases(cases: LoadedUseCases) {
    setLocal(LOCAL_USE_CASES, cases)
}

/**
 * Loads default conditions.
 */
private fun loadConditions(): Set<String> = buildSet {
    // Add a condition with a 1 in 4 chance.
    // Helps to add some variety to the behaviour of the Agent.
    nextInt(1, 4).let { if (it == 2) add("sometimes") }
}

/**
 * Loads use cases from a specified folder or file.
 * Files should have the ".md" extension and not start with "base_".
 * @return A map of use case IDs to their corresponding UseCase objects.
 */
fun loadUseCasesFromFiles(folderOrFile: File): Map<String, UseCase> {
    val useCaseFiles = if (folderOrFile.isFile) arrayOf(folderOrFile) else folderOrFile.listFiles()
    return useCaseFiles
        ?.filter { !it.name.startsWith("base_") && it.name.endsWith(".md") }
        ?.flatMap { file -> file.readText().toUseCases() }
        ?.associateBy { it.id } ?: emptyMap()
}

/**
 * Loads "base" use cases from a specified folder or file.
 * Files should have the ".md" extension and start with "base_".
 * @return A map of use case IDs to their corresponding UseCase objects.
 */
fun loadBaseUseCasesFromFiles(folderOrFile: File): Map<String, UseCase> {
    val useCaseFiles = if (folderOrFile.isFile) arrayOf(folderOrFile) else folderOrFile.listFiles()
    return useCaseFiles
        ?.filter { it.name.startsWith("base_") && it.name.endsWith(".md") }
        ?.flatMap { file -> file.readText().toUseCases() }
        ?.associateBy { it.id } ?: emptyMap()
}

/**
 * Resolves all use case references using the provided folder.
 */
fun List<UseCase>.resolveReferences(folderOrFile: File): List<UseCase> = buildList {
    val currentUseCases = this@resolveReferences
    addAll(currentUseCases)

    var references =
        currentUseCases.flatMap { it.extractReferences() }.filter { ref -> currentUseCases.firstOrNull { it.id == ref } == null }
            .toSet()
    if (references.isEmpty()) return@buildList

    val useCaseFiles = if (folderOrFile.isFile) arrayOf(folderOrFile) else folderOrFile.listFiles()
    useCaseFiles
        ?.filter { !it.name.startsWith("base_") && it.name.endsWith(".md") }
        ?.forEach { file ->
            val useCases = file.readText().toUseCases().filter {
                if (references.contains(it.id)) {
                    references = references - it.id
                    true
                } else {
                    false
                }
            }
            addAll(useCases)
            if (references.isEmpty()) return@buildList
        }
}
