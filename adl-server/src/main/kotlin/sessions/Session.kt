// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.sessions

import java.util.concurrent.ConcurrentHashMap

/**
 * Stores session objects.
 */
interface Sessions {
    fun getOrCreate(sessionId: String): Session
    fun incrementTurn(sessionId: String): Session
    fun get(sessionId: String): Session?
}

/**
 * Stores session objects in memory using a concurrent hash map.
 */
class InMemorySessions : Sessions {
    private val sessions = ConcurrentHashMap<String, Session>()

    /**
     * Gets an existing session or creates a new one with turn 0.
     */
    fun getOrCreate(sessionId: String): Session {
        return sessions.getOrPut(sessionId) { Session(sessionId, 0) }
    }

    /**
     * Increments the turn number for a session.
     * @return The updated session.
     */
    fun incrementTurn(sessionId: String): Session {
        return sessions.compute(sessionId) { _, existing ->
            existing?.copy(turn = existing.turn + 1) ?: Session(sessionId, 1)
        }!!
    }

    /**
     * Gets a session by ID or returns null if not found.
     */
    fun get(sessionId: String): Session? = sessions[sessionId]

    /**
     * Clears all sessions (useful for testing).
     */
    fun clear() = sessions.clear()
}

data class Session(val id: String, val turn: Int)
