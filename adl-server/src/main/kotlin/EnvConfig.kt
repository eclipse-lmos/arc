// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server

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
     * Environment variable: ARC_SERVER_PORT
     * Default value: 8080
     */
    val serverPort get() = System.getenv("ADL_SERVER_PORT")?.toInt() ?: 8080
}
