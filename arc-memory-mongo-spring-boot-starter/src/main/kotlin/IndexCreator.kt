// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.memory.mongo

import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.IndexDefinition
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver

/**
 * Creates indices for the [MemoryEntry] collection.
 */
class IndexCreator(private val mongoTemplate: ReactiveMongoTemplate) {

    @EventListener(ContextRefreshedEvent::class)
    fun initIndicesAfterStartup() {
        val mappingContext = mongoTemplate.converter.mappingContext
        val resolver = MongoPersistentEntityIndexResolver(mappingContext)
        val indexOps = mongoTemplate.indexOps(MemoryEntry::class.java)
        resolver.resolveIndexFor(MemoryEntry::class.java).forEach { indexDefinition: IndexDefinition? ->
            indexDefinition?.let { indexOps.ensureIndex(it).block() }
        }
    }
}
