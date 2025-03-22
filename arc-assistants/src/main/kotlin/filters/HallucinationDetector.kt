// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.HallucinationDetectedException
import org.eclipse.lmos.arc.agents.conversation.ConversationMessage
import org.eclipse.lmos.arc.agents.dsl.AgentFilter
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.extensions.breakWith
import org.eclipse.lmos.arc.agents.dsl.getData
import org.eclipse.lmos.arc.assistants.support.HallucinationDetected
import org.slf4j.LoggerFactory

context(OutputFilterContext)
class HallucinationDetector(private val returnValue: String? = null) : AgentFilter {
    private val log = LoggerFactory.getLogger(javaClass)
    private val urlPattern: Regex = Regex("https?://[\\w-]+(\\.[\\w-]+)+(/[^\\s\\[\\]{}().\"]*(?<!\"))?")
    private val ibanRegex = Regex("[A-Z]{2}\\d{2}( )?(?:\\d{4}( )?){3}\\d{4}(?:( )?\\d\\d?)?")
    private val emailPattern =
        Regex("""([a-zA-Z0-9.!#${'$'}%&'*+/=?^_`{|}~-]+)@([a-zA-Z0-9](?:[a-zA-Z0-9-]{0,62}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}""")

    override suspend fun filter(message: ConversationMessage): ConversationMessage {
        val data = getData()?.joinToString { it.data } ?: return message
        val urlList = extract(message.content, urlPattern)
        val ibanList = extract(message.content, ibanRegex)
        val emailList = extract(message.content, emailPattern)

        for (url in urlList) {
            if (!data.contains(url)) {
                hallucinationDetected("Fabricated link detected, url is : $url")
            }
        }

        for (email in emailList) {
            if (!data.contains(email)) {
                hallucinationDetected("Fabricated email detected, email is : $email")
            }
        }

        for (iban in ibanList) {
            if (!data.contains(iban)) {
                hallucinationDetected(" Fabricated Iban detected, iban is : $iban")
            }
        }

        return message
    }

    private suspend fun hallucinationDetected(reason: String) {
        log.warn("Hallucination detected: $reason")
        returnValue?.let {
            breakWith(returnValue, reason = reason, classification = HallucinationDetected)
        } ?: throw HallucinationDetectedException(reason)
    }

    private fun extract(input: String, regex: Regex): MutableList<String> {
        val links = mutableListOf<String>()
        val matcher = regex.findAll(input)
        matcher.forEach {
            links.add(it.value)
        }
        return links
    }
}
