// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.kotlin.runner.extensions

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse

/**
 * Performs a HTTP GET request.
 */
suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
    val request = HttpRequest.newBuilder().uri(URI(url)).apply {
        headers.forEach { (k, v) -> setHeader(k, v) }
    }.GET().build()
    val response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
    return response.body()
}

/**
 * Performs a HTTP POST request.
 */
suspend fun httpPost(url: String, body: String, headers: Map<String, String> = emptyMap()): String {
    val request = HttpRequest.newBuilder().uri(URI(url)).apply {
        headers.forEach { (k, v) -> setHeader(k, v) }
    }.POST(BodyPublishers.ofString(body)).build()
    val response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
    return response.body()
}
