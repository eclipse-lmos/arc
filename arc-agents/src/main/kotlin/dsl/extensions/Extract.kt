// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.agents.dsl.extensions

import ai.ancf.lmos.arc.agents.conversation.ConversationMessage
import ai.ancf.lmos.arc.agents.dsl.DSLContext
import ai.ancf.lmos.arc.agents.dsl.InputFilterContext
import ai.ancf.lmos.arc.agents.dsl.OutputFilterContext

/**
 * Extensions for extracting data from messages.
 */

/**
 * Extracts email addresses from a message.
 */
val EMAIL_PATTERN =
    Regex("""([a-zA-Z0-9.!#${'$'}%&'*+/=?^_`{|}~-]+)@([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,62}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}""")

fun DSLContext.extractEmail(message: ConversationMessage): List<String> {
    return EMAIL_PATTERN.findAll(message.content).map { it.value }.toList()
}

/**
 * Extracts urls from a message.
 */
val URL_PATTERN =
    Regex("""(https|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]""")

fun DSLContext.extractUrl(message: ConversationMessage): List<String> {
    return URL_PATTERN.findAll(message.content).map { it.value }.toList()
}

/**
 * Extracts patterns from input or output.
 */
fun OutputFilterContext.extract(pattern: Regex): List<String> {
    return pattern.findAll(outputMessage.content).map { it.value }.toList()
}
fun InputFilterContext.extract(pattern: Regex): List<String> {
    return pattern.findAll(inputMessage.content).map { it.value }.toList()
}
