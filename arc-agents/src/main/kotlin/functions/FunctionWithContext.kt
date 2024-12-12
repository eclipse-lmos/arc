// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.eclipse.lmos.arc.agents.dsl.DSLContext

/**
 * Tags a function that requires a DSLContext to be executed.
 */
interface FunctionWithContext {

    /**
     * Returns a new instance of the function that executes with the given context.
     */
    fun withContext(context: DSLContext): LLMFunction
}
