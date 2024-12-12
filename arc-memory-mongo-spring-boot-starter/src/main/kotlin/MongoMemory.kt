// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.memory.mongo

import org.eclipse.lmos.arc.agents.memory.Memory
import org.slf4j.LoggerFactory.getLogger
import java.time.Duration
import java.time.Instant

class MongoMemory(
    private val memoryRepository: MemoryRepository,
    private val shortTermTTL: Duration,
) : Memory {
    private val log = getLogger(MongoMemory::class.java)

    override suspend fun storeLongTerm(owner: String, key: String, value: Any?) {
        if (value != null) {
            log.debug("Storing $key for $owner in LONG_TERM memory.")
            memoryRepository.save(MemoryEntry(owner, key, value))
        } else {
            log.debug("Deleting $key for $owner in LONG_TERM memory.")
            memoryRepository.deleteById(MemoryKey(owner, key))
        }
    }

    override suspend fun storeShortTerm(owner: String, key: String, value: Any?, sessionId: String) {
        if (value != null) {
            log.debug("Storing $key for $owner in SHORT_TERM memory.")
            memoryRepository.save(
                MemoryEntry(
                    owner,
                    key,
                    value,
                    sessionId,
                    deletionDate = Instant.now().plus(shortTermTTL),
                ),
            )
        } else {
            log.debug("Deleting $key for $owner in SHORT_TERM memory.")
            memoryRepository.deleteById(MemoryKey(owner, key, sessionId))
        }
    }

    override suspend fun fetch(owner: String, key: String, sessionId: String?): Any? {
        return memoryRepository.findById(MemoryKey(owner, key, sessionId))?.value
    }
}
