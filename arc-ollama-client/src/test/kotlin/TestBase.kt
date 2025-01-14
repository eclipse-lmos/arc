// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.ollama

import io.ktor.network.sockets.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.net.ServerSocket

open class TestBase {

    companion object {

        lateinit var server: ApplicationEngine

        @JvmStatic
        protected var port: Int = 0

        @JvmStatic
        fun getFreePort(): Int {
            val socket = ServerSocket(0)
            val port = socket.localPort
            socket.close()
            return port
        }

        @BeforeAll
        @JvmStatic
        fun setup() {
            this.port = getFreePort()
            server = embeddedServer(Netty, port = this.port) {
                routing {
                    post("/api/chat") {
                        require(call.receiveText().contains(""""test question"""))
                        call.respondText(
                            Json.encodeToString(
                                ChatResponse(
                                    50,
                                    100,
                                    ChatMessage(role = "assistant", content = "answer to test"),
                                ),
                            ),
                        )
                    }
                    post("/api/embeddings") {
                        require(call.receiveText().contains(""""model":"llama3:8b""""))
                        call.respondText(""" {  "embedding": [0.0, 0.1] } """)
                    }
                }
            }.start()
        }

        @AfterAll
        @JvmStatic
        fun shutdown() {
            server.stop()
        }
    }
}
