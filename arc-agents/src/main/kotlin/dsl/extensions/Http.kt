// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Performs an HTTP GET request.
 */
suspend fun httpGet(url: String): String {
    val request = HttpRequest.newBuilder().uri(URI(url)).GET().build()
    val response = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
    return response.body()
}
