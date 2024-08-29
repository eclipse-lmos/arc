// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.agents.dsl.extensions

import ai.ancf.lmos.arc.agents.dsl.DSLContext
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
