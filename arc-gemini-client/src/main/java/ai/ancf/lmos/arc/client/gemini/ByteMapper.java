// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.gemini;

import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.PartMaker;

/**
 * Workaround for the mapping kotlin byte arrays to java byte arrays.
 */
public class ByteMapper {

    /**
     * Maps a byte array to a Part.
     *
     * @param mimeType The mime type of the byte array.
     * @param data The byte array.
     * @return a Gemini Part instance.
     */
    public static Part map(String mimeType, byte[] data) {
        return PartMaker.fromMimeTypeAndData(mimeType, data);
    }
}
