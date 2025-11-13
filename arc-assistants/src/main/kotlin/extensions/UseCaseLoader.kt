// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase

/**
 * Interface for loading use cases by name and context.
 *
 * Implementations of this interface are responsible for retrieving a list of [UseCase]s
 * based on a given name (e.g., a file, resource, or identifier) and the current [DSLContext].
 * This allows for flexible loading strategies, such as from local files, remote sources,
 * or custom repositories, depending on the application's needs.
 *
 * @see UseCase for the use case data structure
 * @see DSLContext for the context in which the loader operates
 */
interface UseCaseLoader {
    /**
     * Loads use cases for the given name and context.
     *
     * @param name The identifier or resource name for the use cases to load.
     * @param context The DSLContext providing additional context for loading.
     * @return A list of loaded [UseCase]s or null, if use cases could not be loaded.
     */
    suspend fun loadUseCases(name: String, context: DSLContext): List<UseCase>?
}
