// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

/**
 * Provides ChatCompleter based on their model name.
 * ChatCompleterProviders must provide a default ChatCompleter if no model is explicitly defined.
 */
fun interface ChatCompleterProvider {

    fun provideByModel(model: String?): ChatCompleter
}

/**
 * Provides an implementation of ChatCompleterProvider that is backed by a map of ChatCompleters.
 */
class MapChatCompleterProvider(private val map: Map<String, ChatCompleter>) : ChatCompleterProvider {

    override fun provideByModel(model: String?): ChatCompleter {
        return map[model] ?: map.values.first()
    }
}

fun Map<String, ChatCompleter>.toChatCompleterProvider() = MapChatCompleterProvider(this)
