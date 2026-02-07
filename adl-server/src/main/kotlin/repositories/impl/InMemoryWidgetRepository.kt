// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.repositories.impl

import org.eclipse.lmos.adl.server.models.Widget
import org.eclipse.lmos.adl.server.repositories.WidgetRepository
import java.util.concurrent.ConcurrentHashMap

class InMemoryWidgetRepository : WidgetRepository {
    private val widgets = ConcurrentHashMap<String, Widget>()

    override fun save(widget: Widget): Widget {
        widgets[widget.id] = widget
        return widget
    }

    override fun findById(id: String): Widget? {
        return widgets[id]
    }

    override fun findAll(): List<Widget> {
        return widgets.values.toList()
    }
}

