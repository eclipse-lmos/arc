// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.core.Result

interface ErrorHandler {

    fun handleError(e: Exception): Result<AssistantMessage, Exception>
}
