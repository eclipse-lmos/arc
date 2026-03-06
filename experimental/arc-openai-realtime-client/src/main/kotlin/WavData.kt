// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client.ws

import org.eclipse.lmos.arc.agents.conversation.BinaryData
import org.eclipse.lmos.arc.agents.conversation.DataStream

/**
 * Creates a [BinaryData] object with the given wav audio data.
 */
fun wavData(audio: DataStream) = BinaryData(mimeType = "audio/wav", stream = audio)
