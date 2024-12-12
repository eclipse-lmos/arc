// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.llm

/**
 * Provides TextEmbedder based on their model name.
 * TextEmbedderProviders must provide a default TextEmbedder if no model is explicitly defined.
 */
fun interface TextEmbedderProvider {

    fun provideByModel(model: String?): TextEmbedder
}
