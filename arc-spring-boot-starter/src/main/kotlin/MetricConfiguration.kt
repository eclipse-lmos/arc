// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@ConditionalOnClass(MeterRegistry::class)
class MetricConfiguration {

    @Bean
    fun metricsHandler(meterRegistry: MeterRegistry) = MetricsHandler(meterRegistry)

    @Bean
    @ConditionalOnMissingBean
    fun meterRegistry() = SimpleMeterRegistry()
}
