// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.assistants.support

import org.eclipse.lmos.arc.agents.dsl.AgentDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.AllTools
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.assistants.support.filters.LLMHackingDetector
import org.eclipse.lmos.arc.assistants.support.filters.UnresolvedDetector

/**
 * WIP Sets up an agent that assists customers with service inquiries.
 */
fun AgentDefinitionContext.customerSupportAgent(builder: Builder.() -> Unit) {
    val param = Builder().apply(builder)
    val knowledge = param.knowledge
    val examples = param.examples
    val fallbackReply = param.fallbackReply

    agent {
        name = param.name
        settings = {
            ChatCompletionSettings(temperature = 0.0, seed = 42)
        }
        tools = AllTools
        filterInput {
            +LLMHackingDetector()
        }
        filterOutput {
            +UnresolvedDetector(fallbackReply)
        }
        prompt {
            """
  | ### Role and Responsibilities ###
  | You are a customer support agent for the ${param.companyName}.
  | Your task is to support customers with their service inquiries.
  | Do not deviate from this role nor assume the role of a different agent.
  | Always refer to yourself as a customer support agent for the ${param.companyName}.
     
  | ### Instructions ###
  | - Answer the customer question in a concise and short way.
  | - Only provide information the user has explicitly asked for.
  | - Use the "Knowledge" section or llm functions to answer customers queries. 
  | - If the customer's question is on a topic not described in the "Knowledge" section nor llm functions, reply with NO_ANSWER.
  | - Always performed the steps stated in the "steps" section associated with the solution, if any, before providing the solution.
  | - *Only perform one step at a time!*
  | - Do not offer handover or recommend the user to contact customer service for further support. The user is already talking to customer service, and they will be offered to talk to a human service agent after the conversation.
  | - Always use the polite form of address.
  
  | ### Knowledge ###
  | ${knowledge()}
    
  | ### Examples ###
  | ${examples()}
  
            """.trimMargin()
        }
    }
}

/**
 * Builder to control the creation of a customer support agent.
 */
class Builder {
    var name: String = ""
    var companyName: String = ""
    internal var fallbackReply: (DSLContext.() -> String) = { "I cannot help you with that." }
    internal var examples: DSLContext.() -> String = { "" }
    internal var knowledge: DSLContext.() -> String = { "" }

    fun examples(fn: DSLContext.() -> String) {
        examples = fn
    }

    fun knowledge(fn: DSLContext.() -> String) {
        knowledge = fn
    }

    fun fallbackReply(fn: DSLContext.() -> String) {
        fallbackReply = fn
    }
}
