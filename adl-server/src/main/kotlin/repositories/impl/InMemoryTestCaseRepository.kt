// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.repositories.impl

import org.eclipse.lmos.adl.server.repositories.TestCaseRepository
import org.eclipse.lmos.adl.server.models.TestCase
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of [TestCaseRepository].
 */
class InMemoryTestCaseRepository : TestCaseRepository {
    private val store = ConcurrentHashMap<String, TestCase>()

    override suspend fun save(testCase: TestCase): TestCase {
        store[testCase.id] = testCase
        return testCase
    }

    override suspend fun findById(id: String): TestCase? {
        return store[id]
    }

    override suspend fun findAll(): List<TestCase> {
        return store.values.toList()
    }

    override suspend fun findByUseCaseId(useCaseId: String): List<TestCase> {
        return store.values.filter { it.useCaseId == useCaseId }
    }

    override suspend fun delete(id: String): Boolean {
        return store.remove(id) != null
    }
}
