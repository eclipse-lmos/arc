// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.search

/**
 * Configuration object that provides access to environment-based settings for the ARC server.
 * This object reads configuration values from environment variables and provides default values
 * when the environment variables are not set.
 */
object EnvConfig {
    /**
     * Indicates whether the server is running in development mode.
     *
     * Environment variable: ARC_DEV_MODE
     * Default value: false
     */
    val isDevMode get() = System.getenv("ARC_DEV_MODE")?.toBoolean() ?: false

    /**
     * The port on which the server should listen for incoming connections.
     *
     * Environment variable: ADL_SERVER_PORT
     * Default value: 8080
     */
    val serverPort get() = System.getenv("ADL_SERVER_PORT")?.toInt() ?: 8080

    /**
     * Qdrant vector database host.
     *
     * Environment variable: QDRANT_HOST
     * Default value: localhost
     */
    val qdrantHost get() = System.getenv("QDRANT_HOST") ?: "localhost"

    /**
     * Qdrant vector database port.
     *
     * Environment variable: QDRANT_PORT
     * Default value: 6334
     */
    val qdrantPort get() = System.getenv("QDRANT_PORT")?.toInt() ?: 6334

    /**
     * Qdrant collection name for UseCase embeddings.
     *
     * Environment variable: QDRANT_COLLECTION_NAME
     * Default value: usecase_embeddings
     */
    val qdrantCollectionName get() = System.getenv("QDRANT_COLLECTION_NAME") ?: "usecase_embeddings"

    /**
     * Vector size for embeddings.
     *
     * Environment variable: QDRANT_VECTOR_SIZE
     * Default value: 384
     */
    val qdrantVectorSize get() = System.getenv("QDRANT_VECTOR_SIZE")?.toInt() ?: 384
}

/**
 * Configuration for the Qdrant UseCase Embeddings Store.
 */
data class QdrantConfig(
    val host: String = EnvConfig.qdrantHost,
    val port: Int = EnvConfig.qdrantPort,
    val collectionName: String = EnvConfig.qdrantCollectionName,
    val vectorSize: Int = EnvConfig.qdrantVectorSize,
)
