// SPDX-FileCopyrightText: 2026 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.llm

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class CompleterLoaderTest {

    @AfterEach
    fun clearProperties() {
        listOf(
            "ARC_CLIENT",
            "ARC_MODEL",
            "ARC_AI_SERVICE_TIER",
            "ARC_IGNORE_ARC_PROPERTIES",
        ).forEach(System::clearProperty)
    }

    @Test
    fun `loads service tier from configuration`() {
        System.setProperty("ARC_IGNORE_ARC_PROPERTIES", "true")
        System.setProperty("ARC_CLIENT", "openai")
        System.setProperty("ARC_MODEL", "gpt-5")
        System.setProperty("ARC_AI_SERVICE_TIER", "flex")

        val config = loadConfigFromEnv().single()

        assertThat(config.serviceTier).isEqualTo("flex")
    }
}

