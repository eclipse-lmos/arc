// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.memory.mongo

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.time.Duration

@AutoConfiguration
@EnableReactiveMongoRepositories(basePackageClasses = [MemoryRepository::class])
class MemoryAutoConfiguration {

    @Bean
    fun indexCreator(mongoTemplate: ReactiveMongoTemplate) = IndexCreator(mongoTemplate)

    @Bean
    fun memory(
        memoryRepository: MemoryRepository,
        @Value("\${arc.memory.short-term-ttl:PT3H}") shortTermTTL: Duration,
    ) = MongoMemory(memoryRepository, shortTermTTL)
}
