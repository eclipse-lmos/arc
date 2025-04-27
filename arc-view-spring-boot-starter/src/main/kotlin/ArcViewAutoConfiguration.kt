// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.view

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.RouterFunctions

@AutoConfiguration
open class ArcViewAutoConfiguration {

    @Bean
    @ConditionalOnProperty("arc.chat.ui.enabled", havingValue = "true")
    fun chatResourceRouter() = RouterFunctions.resources("/chat/**", ClassPathResource("chat/"))
}
