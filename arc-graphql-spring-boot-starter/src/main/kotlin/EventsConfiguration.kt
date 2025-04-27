// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import org.eclipse.lmos.arc.graphql.inbound.EventSubscription
import org.eclipse.lmos.arc.graphql.inbound.EventSubscriptionHolder
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase

@Configuration(proxyBeanMethods = false)
@Conditional(DevModeOrEnabled::class)
open class EventsConfiguration {

    @Bean
    fun eventSubscriptionHolder() = EventSubscriptionHolder()

    @Bean
    fun eventSubscription(eventSubscriptionHolder: EventSubscriptionHolder) = EventSubscription(eventSubscriptionHolder)
}

class DevModeOrEnabled : AnyNestedCondition(ConfigurationPhase.PARSE_CONFIGURATION) {
    @ConditionalOnProperty("arc.chat.ui.enabled", havingValue = "true")
    class UIEnabled

    @ConditionalOnProperty("arc.subscriptions.events.enable", havingValue = "true")
    class Enable
}
