// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.memory.mongo

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    properties = ["arc.memory.short-term-ttl=PT3S"],
)
@ActiveProfiles("test")
@TestInstance(PER_CLASS)
open class TestBase {

    @Autowired
    lateinit var memory: MongoMemory

    @Autowired
    lateinit var mongoTemplate: ReactiveMongoTemplate

    @AfterEach
    fun setup() {
        mongoTemplate.collectionNames.map {
            mongoTemplate.dropCollection(it)
        }.blockFirst()
    }
}

data class TestValue(val nr: Int, val value: String)
