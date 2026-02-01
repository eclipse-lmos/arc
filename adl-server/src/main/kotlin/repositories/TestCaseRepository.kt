// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.repositories

import org.eclipse.lmos.adl.server.models.TestCase

/**
 * Repository for managing [TestCase] entities.
 */
interface TestCaseRepository {
    /**
     * Saves a [TestCase].
     * @param testCase The test case to save.
     * @return The saved test case.
     */
    suspend fun save(testCase: TestCase): TestCase

    /**
     * Saves a list of [TestCase]s.
     * @param testCases The test cases to save.
     * @return The saved test cases.
     */
    suspend fun saveAll(testCases: List<TestCase>): List<TestCase>

    /**
     * Finds a [TestCase] by its ID.
     * @param id The ID of the test case.
     * @return The test case if found, null otherwise.
     */
    suspend fun findById(id: String): TestCase?

    /**
     * Retrieves all [TestCase]s.
     * @return A list of all test cases.
     */
    suspend fun findAll(): List<TestCase>

    /**
     * Finds [TestCase]s associated with a specific Use Case ID.
     * @param useCaseId The ID of the use case.
     * @return A list of matching test cases.
     */
    suspend fun findByUseCaseId(useCaseId: String): List<TestCase>

    /**
     * Finds [TestCase]s associated with a specific ADL ID.
     * @param adlId The ID of the ADL.
     * @return A list of matching test cases.
     */
    suspend fun findByADLId(adlId: String): List<TestCase>

    /**
     * Deletes a [TestCase] by its ID.
     * @param id The ID of the test case to delete.
     * @return True if the test case was deleted, false otherwise.
     */
    suspend fun delete(id: String): Boolean
}
