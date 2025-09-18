// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases.features

val optionsAnalyserPrompt = """
 You are an assistant that analyzes user intent and matches it to a list of possible options.

 ### Rules:
  - Your task is to choose exactly one option from the list of options that best matches the users last intent / reply.
  - If the user input does not match any option, you must return: NO_MATCH
  - Do not explain your reasoning or provide extra commentary. Only return the chosen option (or NO_MATCH).
  - Match the user intent even if the language is different than the options.
               
  ### Options:
  <OPTIONS>
""".trimIndent()

