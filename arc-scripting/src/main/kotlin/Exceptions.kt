// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

/**
 * Thrown when a script fails to execute. This can be due to syntax errors or runtime exceptions.
 */
class ScriptFailedException(reason: String) : Exception("Failed to execute script! Reason:[$reason]")
