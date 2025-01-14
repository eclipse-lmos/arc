// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.api

import kotlinx.serialization.Serializable

@Serializable
data class AnonymizationEntity(val type: String, val value: String, val replacement: String)
