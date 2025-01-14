// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.ai

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
open class TestBase {

    @Autowired
    lateinit var springChatClient: SpringChatClient

    companion object {

        lateinit var server: ApplicationEngine

        @JvmStatic
        protected var port: Int = 0

        @BeforeAll
        @JvmStatic
        fun setup() {
            server = embeddedServer(Netty, port = 8888) {
                routing {
                    post("/api/chat") {
                        require(call.receiveText().contains(""""test question"""))
                        call.response.header("content-type", "application/json")
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

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null,
)

@Serializable
data class ChatResponse(
    @SerialName("prompt_eval_count")
    val promptTokenCount: Int = -1,
    @SerialName("eval_count")
    val responseTokenCount: Int,
    val message: ChatMessage,
)
