// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.templates

/**
 * Loads template resources for system prompt generation.
 */
class TemplateLoader {

    companion object {
        private const val ASSISTANT_TEMPLATE_PATH = "/assistant.md"
        private const val ROLE_TEMPLATE_PATH = "/role.md"

        private const val ROLE_PLACEHOLDER = "\$\$ROLE\$\$"
        private const val TIME_PLACEHOLDER = "\$\$TIME\$\$"
        private const val USE_CASES_PLACEHOLDER = "\$\$USE_CASES\$\$"
    }

    private val assistantTemplate: String by lazy {
        loadResource(ASSISTANT_TEMPLATE_PATH)
    }

    private val roleTemplate: String by lazy {
        loadResource(ROLE_TEMPLATE_PATH)
    }

    /**
     * Renders the system prompt template with the given placeholder values.
     * @param time The current time string to inject.
     * @param useCases The compiled use cases string to inject.
     * @return The rendered system prompt.
     */
    fun render(time: String, useCases: String): String {
        return assistantTemplate
            .replace(ROLE_PLACEHOLDER, roleTemplate)
            .replace(TIME_PLACEHOLDER, time)
            .replace(USE_CASES_PLACEHOLDER, useCases)
    }

    private fun loadResource(path: String): String {
        return TemplateLoader::class.java.getResourceAsStream(path)?.bufferedReader()?.readText()
            ?: error("Could not load resource: $path")
    }
}
