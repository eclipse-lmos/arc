// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

/**
 * Indicates that the ChatCompleter can be used for multiple models.
 */
const val ANY_MODEL = "*"

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
        return model?.let {
            map[model] ?: map[ANY_MODEL] ?: error("Cannot find a ChatCompleter for $model! ChatCompleters:[$map]")
        } ?: map[ANY_MODEL] ?: map.values.first()
    }

    override fun toString(): String {
        return "MapChatCompleterProvider(map=$map)"
    }
}

fun Map<String, ChatCompleter>.toChatCompleterProvider() = MapChatCompleterProvider(this)
