// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
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
