// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

/**
 * Extensions to make using kotlin string templating easier.
 */

/**
 * Returns the string if the predicate is true.
 * For example,
 * """ Some string template ${someVariableThatResultToTrue then "Hello"} """
 * will return "Some string template Hello".
 * This is simply a shorthand for
 * """ Some string template ${if(someVariableThatResultToTrue) "Hello" else "" } """.
 */
infix fun Any?.then(out: String) = if (this == true) out else ""

/**
 * Adds a newline character to the end of the string.
**/
fun String.newline() = this + "\n"

/**
 * Prints a list of strings as a markdown list.
 * Example:
 * listOf("one", "two", "three").printAsList()
 * will return
 * - one
 * - two
 * - three
 */
fun List<String>.markdown() = this.joinToString("") { "- $it".newline() }
