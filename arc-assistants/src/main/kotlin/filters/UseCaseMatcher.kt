// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.extensions.llm
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.onFailure
import org.slf4j.LoggerFactory

class UseCaseMatcher(private val model: String? = null) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    private fun system(useCases: String) = """
    You are a classification system.

    Your task is to match an assistant message to the most appropriate use case, 
    based on the provided use case definitions and their solutions.
    
    ## Rules
    Each use case includes a Solution, describing the expected assistant behavior.
    Match based on semantic meaning, not exact wording.
    Prefer the use case whose solution best aligns with the intent of the assistant message.
    If no use case clearly matches, return <UNKNOWN>.
    Output must be only one label in angle brackets.
    
    ----
    
    ## Examples
    
    ### Example 1
    UseCases
    UseCase: password_reset
    Solution
    Ask the customer to restart their router to reset their router.
    
    #### UseCase: password_forgotton
    Solution
    Ask the customer to send an email to customer support.
    
    Assistant
    Please restart your router.
    
    Output:<password_reset>
    
    ### Example 2
    UseCases
    UseCase: billing_issue
    Solution
    Ask the customer to check their invoice or contact billing support.
    
    UseCase: technical_issue
    Solution
    Guide the customer through troubleshooting steps.
    
    Assistant
    Please review your invoice or reach out to billing support.
    
    Output: <billing_issue>
    
    ### Example 3
    UseCases
    UseCase: account_locked
    Solution
    Tell the customer to wait 15 minutes before trying again.
    
    UseCase: password_reset
    Solution
    Ask the customer to reset their password using the reset link.
   
    Assistant
    You can reset your password using the link we sent you.
    
    Output: <password_reset>
    
    ### Example 4 (No Match)
    UseCases
    UseCase: shipping_delay
    Solution
    Inform the customer about delayed delivery.
    
    UseCase: refund_request
    Solution
    Guide the customer through requesting a refund.
    
    Assistant
    Our office hours are Monday to Friday, 9am–5pm.
    
    Output: <UNKNOWN>
    
    ----
    
    Now classify:
    
    #UseCases
    $useCases

    
    Output: (only the label in angle brackets)
    """

    suspend fun matchUseCase(
        message: String,
        useCases: String,
        context: DSLContext
    ): String? {
        val result = context.llm(
            system = system(useCases),
            user = "Assistant Message: $message",
            model = model
        ).onFailure {
            log.warn("Failure while matching UseCase: $it")
        }.getOrNull() ?: return null
        log.debug("Matched UseCase: $result")
        val useCase = result.content.substringAfter("<", missingDelimiterValue = "")
            .substringBefore(">", missingDelimiterValue = "")
        if (useCase.isBlank()) return null
        if (!useCases.contains(useCase)) return null
        log.info("Matched UseCase found: $result")
        return useCase
    }
}