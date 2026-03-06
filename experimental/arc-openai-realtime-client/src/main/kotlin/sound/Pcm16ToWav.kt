// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client.ws.sound

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Converts PCM data to a WAV file.
 */
fun pcm16ToWav(pcmData: ByteArray, sampleRate: Int = 24000, channels: Int = 1): ByteArray {
    val byteRate = sampleRate * channels * 2 // Bytes per second
    val blockAlign = channels * 2 // Bytes per sample
    val chunkSize = 36 + pcmData.size // Chunk size, including header

    val header = ByteBuffer.allocate(44)
    header.order(ByteOrder.LITTLE_ENDIAN)
    header.put("RIFF".toByteArray())
    header.putInt(chunkSize)
    header.put("WAVE".toByteArray())
    header.put("fmt ".toByteArray())
    header.putInt(16) // Format chunk size
    header.putShort(1.toShort()) // Audio format (1 = PCM)
    header.putShort(channels.toShort())
    header.putInt(sampleRate)
    header.putInt(byteRate)
    header.putShort(blockAlign.toShort())
    header.putShort(16.toShort()) // Bits per sample
    header.put("data".toByteArray())
    header.putInt(pcmData.size)

    val outputFile = ByteArrayOutputStream()
    outputFile.write(header.array())
    outputFile.write(pcmData)
    return outputFile.toByteArray()
}
