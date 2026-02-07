// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.repositories

import org.eclipse.lmos.adl.server.models.Widget

interface WidgetRepository {
    fun save(widget: Widget): Widget
    fun findById(id: String): Widget?
    fun findAll(): List<Widget>
}

