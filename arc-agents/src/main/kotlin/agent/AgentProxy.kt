// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.core.getOrThrow
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.kotlinFunction

/**
 * JSON serializer/deserializer for agent input and output.
 * Mirroring the configuration from org.eclipse.lmos.arc.agents.agent.agentInputJson
 */
@PublishedApi
internal val proxyJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

/**
 * Extension function to create a dynamic proxy for the Agents.
 *
 * Allows casting an Agent to an interface that accepts an object and returns an object.
 * The interface must have a single suspend function with one parameter (the input) and a return type (the output).
 *
 * @return The proxy instance implementing interface T.
 */
inline fun <reified T : Any> Agent<*, *>.proxy(): T {
    val kClass = T::class.java
    return Proxy.newProxyInstance(kClass.classLoader, arrayOf(kClass)) { proxy, method, args ->
        val kFunction = method.kotlinFunction
        val arguments = args ?: emptyArray()
        val agent = this as ConversationAgent

        if (kFunction != null && kFunction.isSuspend) {
            // Handle suspend function
            val continuation = arguments.last() as Continuation<Any?>
            val realArgs = arguments.dropLast(1)
            val input = realArgs.firstOrNull() ?: ""
            // Parameter 0 is likely the instance, 1 is the first argument
            val inputType = kFunction.parameters.getOrNull(1)?.type
                ?: throw IllegalArgumentException("Method must have at least one argument")
            val returnType = kFunction.returnType

            val suspendBlock: suspend () -> Any? = {
                val inputString = input as? String ?: proxyJson.encodeToString(serializer(inputType), input)

                val conversation = Conversation() + UserMessage(inputString)
                val resultConversation = agent.execute(conversation).getOrThrow()

                val lastMessage = resultConversation.transcript.filterIsInstance<AssistantMessage>().lastOrNull()
                    ?: throw IllegalStateException("Agent did not return an AssistantMessage")
                val resultString = lastMessage.content

                if (returnType.isSubtypeOf(String::class.starProjectedType)) {
                    resultString
                } else {
                    proxyJson.decodeFromString(serializer(returnType), resultString)
                }
            }

            suspendBlock.startCoroutineUninterceptedOrReturn(continuation)
        } else {
            // Handle standard Object methods
            when (method.name) {
                "toString" -> "AgentProxy for $agent"
                "hashCode" -> agent.hashCode()
                "equals" -> arguments.getOrNull(0) === proxy
                else -> throw UnsupportedOperationException("Only suspend functions are supported in Agent Proxy")
            }
        }
    } as T
}

