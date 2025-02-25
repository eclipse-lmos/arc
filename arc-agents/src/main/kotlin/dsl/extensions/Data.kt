// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventPublisher

/**
 * Local variable where the LLM data is stored.
 */
private const val LOCAL_LLM_DATA = "LOCAL_LLM_DATA"

/**
 * The piece of data that is feed to the LLM.
 */
data class Data(val name: String, val data: String)

/**
 * Gets and sets the current data sources.
 */
fun DSLContext.getData(): List<Data>? = getLocal(LOCAL_LLM_DATA) as? List<Data>?
suspend fun DSLContext.addData(data: Data) {
    val dataList = getLocal(LOCAL_LLM_DATA) as? List<Data>? ?: emptyList()
    setLocal(LOCAL_LLM_DATA, dataList + data)
    getOptional<EventPublisher>()?.publish(DataAddedEvent(data.name, data.data))
}

/**
 * Event that is published when new data is added.
 */
data class DataAddedEvent(val name: String, val data: String) : Event by BaseEvent()
