// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.core.closeWith
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result
import java.net.URI

/**
 * Extensions for reading PDF files.
 * @param uriString The URI of the PDF file.
 */
fun DSLContext.pdf(uriString: String) = result<String, ReadPdfException> {
    try {
        val inputStream = URI(uriString).toURL().openStream() closeWith { it.close() }
        val document = Loader.loadPDF(inputStream.readAllBytes()) closeWith { it.close() }
        PDFTextStripper().getText(document)
    } catch (ex: Exception) {
        failWith { ReadPdfException(uriString, ex) }
    }
}

class ReadPdfException(url: String, cause: Exception) : Exception("Failed to read PDF at $url", cause)
