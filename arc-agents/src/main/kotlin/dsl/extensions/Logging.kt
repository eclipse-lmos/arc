// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.slf4j.LoggerFactory

/**
 * Extensions for logging
 */
private val log = LoggerFactory.getLogger("ArcDSL")

fun DSLContext.debug(message: String) {
    log.debug(message)
}

fun DSLContext.info(message: String) {
    log.info(message)
}

fun DSLContext.warn(message: String, ex: Exception? = null) {
    log.warn(message, ex)
}

fun DSLContext.error(message: String, ex: Exception? = null) {
    log.error(message, ex)
}
