// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.filters

/**
 * Default system prompt template for [UseCaseMatcher].
 * Replace `{useCases}` with the formatted use case definitions at runtime.
 */
object UseCaseMatcherPrompts {
    const val TEMPLATE = """
    You are a classification system.

    Your task is to match the latest assistant message to the most appropriate use case,
    based on the provided use case definitions, their solutions, and the conversation history.
    
    ## Rules
    Each use case includes a Solution, describing the expected assistant behavior.
    Match based on semantic meaning, not exact wording.
    Use the full conversation context, especially the user's latest messages, to disambiguate similar use cases.
    Prefer the use case whose solution best aligns with the intent of the latest assistant message.
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
    
    ### Example 5 (Multi-turn Disambiguation)
    UseCases
    UseCase: umzug_beauftragen_mobilfunk
    Solution
    Guide the customer through submitting a mobile relocation order via the Business Service Portal.
    
    UseCase: umzug_beauftragen_festnetz
    Solution
    Direct the customer to submit a fixed-line relocation order via the relocation form.
    
    Conversation
    User: ich will umziehen
    Assistant: Geht es um Mobilfunk oder Festnetz?
    User: festnetz
    Assistant: Sie können Ihren Umzugsauftrag hier stellen: [Umzug beauftragen](https://example.com/umzug).
    
    Output: <umzug_beauftragen_festnetz>
    
    ----
    
    Now classify:
    
    #UseCases
    {useCases}

    
    Output: (only the label in angle brackets)
    """

    fun build(useCases: String): String = TEMPLATE.replace("{useCases}", useCases)
}
