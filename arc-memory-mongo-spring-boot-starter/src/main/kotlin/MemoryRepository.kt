// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.memory.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.io.Serializable
import java.time.Instant

/**
 * Repository for the memory.
 */
interface MemoryRepository : CoroutineCrudRepository<MemoryEntry, MemoryKey>

/**
 * Entry in memory.
 */
@Document
data class MemoryEntry(
    val owner: String,
    val key: String,
    val value: Any,
    val sessionId: String? = null,
    @Indexed(expireAfterSeconds = 0) val deletionDate: Instant? = null,
    @Id val id: MemoryKey = MemoryKey(owner, key, sessionId),
    val creationDate: Instant = Instant.now(),
)

/**
 * Key for a [MemoryEntry].
 */
data class MemoryKey(val owner: String, val key: String, val sessionId: String? = null) : Serializable
