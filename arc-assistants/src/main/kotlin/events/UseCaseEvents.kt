package org.eclipse.lmos.arc.assistants.support.events

import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event

/**
 * Emitted when a use case was triggered.
 */
data class UseCaseEvent(val name: String) : Event by BaseEvent()
