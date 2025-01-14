// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.future.await
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.core.closeWith
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.result
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Extensions for reading html files.
 * @param url The url of the html file.
 */
suspend fun DSLContext.htmlDocument(url: String, followRedirects: Boolean = true, enableCookies: Boolean = true) =
    result<Document, ReadHtmlException> {
        try {
            val htmlText = readUrl(url, followRedirects, enableCookies).getOrThrow()
            Jsoup.parse(htmlText)
        } catch (ex: Exception) {
            failWith { ReadHtmlException(url, ex) }
        }
    }

fun String.htmlDocument() = result<Document, ReadHtmlException> {
    try {
        Jsoup.parse(this@htmlDocument)
    } catch (ex: Exception) {
        failWith { ReadHtmlException("unknown", ex) }
    }
}

suspend fun DSLContext.html(url: String, followRedirects: Boolean = true, enableCookies: Boolean = true) =
    result<String, ReadHtmlException> {
        val doc = htmlDocument(url, followRedirects, enableCookies) failWith { it }
        doc.text()
    }

class ReadHtmlException(url: String, cause: Exception) : Exception("Failed to read html at $url", cause)

private suspend fun DSLContext.readUrl(
    uriString: String,
    followRedirects: Boolean = true,
    enableCookies: Boolean = true,
) =
    result<String, Exception> {
        val request = HttpRequest.newBuilder()
            .header("content-type", "text/html;charset=UTF-8")
            .uri(URI(uriString)).GET().build()
        val client = HttpClient.newBuilder()
            .apply {
                if (enableCookies) cookieHandler(java.net.CookieManager())
                if (followRedirects) followRedirects(HttpClient.Redirect.ALWAYS)
            }
            .build() closeWith { it.close() }
        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        response.body()
    }
