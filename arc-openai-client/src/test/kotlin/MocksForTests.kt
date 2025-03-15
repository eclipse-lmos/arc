package org.eclipse.lmos.arc.client.openai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.openai.client.OpenAIClient
import com.openai.client.OpenAIClientAsync
import com.openai.core.RequestOptions
import com.openai.core.http.AsyncStreamResponse
import com.openai.models.responses.*
import com.openai.services.async.*
import com.openai.services.async.responses.InputItemServiceAsync
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventPublisher
import java.util.concurrent.CompletableFuture

class EventPublisherForTests: EventPublisher {
    override fun publish(event: Event) {
        // Do nothing
    }
}

enum class Mock {
    WEB_SEARCH,
    WEATHER_TOOL
}

class MockOpenAIClient(private val mockId: Mock): OpenAIClientAsync {
    override fun audio(): AudioServiceAsync {
        TODO("Not yet implemented")
    }

    override fun batches(): BatchServiceAsync {
        TODO("Not yet implemented")
    }

    override fun beta(): BetaServiceAsync {
        TODO("Not yet implemented")
    }

    override fun chat(): ChatServiceAsync {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun completions(): CompletionServiceAsync {
        TODO("Not yet implemented")
    }

    override fun embeddings(): EmbeddingServiceAsync {
        TODO("Not yet implemented")
    }

    override fun files(): FileServiceAsync {
        TODO("Not yet implemented")
    }

    override fun fineTuning(): FineTuningServiceAsync {
        TODO("Not yet implemented")
    }

    override fun images(): ImageServiceAsync {
        TODO("Not yet implemented")
    }

    override fun models(): ModelServiceAsync {
        TODO("Not yet implemented")
    }

    override fun moderations(): ModerationServiceAsync {
        TODO("Not yet implemented")
    }

    override fun responses(): ResponseServiceAsync {
        return MockResponseServiceAsync(mockId)
    }

    override fun sync(): OpenAIClient {
        TODO("Not yet implemented")
    }

    override fun uploads(): UploadServiceAsync {
        TODO("Not yet implemented")
    }

    override fun vectorStores(): VectorStoreServiceAsync {
        TODO("Not yet implemented")
    }

    override fun withRawResponse(): OpenAIClientAsync.WithRawResponse {
        TODO("Not yet implemented")
    }

}

class MockResponseServiceAsync(private val mockId: Mock): ResponseServiceAsync {
    override fun withRawResponse(): ResponseServiceAsync.WithRawResponse {
        TODO("Not yet implemented")
    }

    override fun create(params: ResponseCreateParams, requestOptions: RequestOptions): CompletableFuture<Response> {
        val mapper = ObjectMapper().registerKotlinModule()
        val mock = mockMap.getValue(mockId)
        return CompletableFuture.completedFuture(mapper.readValue(mock, Response::class.java))
    }

    override fun createStreaming(
        params: ResponseCreateParams,
        requestOptions: RequestOptions
    ): AsyncStreamResponse<ResponseStreamEvent> {
        TODO("Not yet implemented")
    }

    override fun delete(params: ResponseDeleteParams, requestOptions: RequestOptions): CompletableFuture<Void?> {
        TODO("Not yet implemented")
    }

    override fun inputItems(): InputItemServiceAsync {
        TODO("Not yet implemented")
    }

    override fun retrieve(params: ResponseRetrieveParams, requestOptions: RequestOptions): CompletableFuture<Response> {
        TODO("Not yet implemented")
    }

    private val mockMap = mutableMapOf(
        Mock.WEB_SEARCH to """
       {
    "id": "resp_67d533a7cb9481918bbe9cd3ce404e8701f8172a11e95da9",
    "object": "response",
    "created_at": 1742025639,
    "status": "completed",
    "error": null,
    "incomplete_details": null,
    "instructions": null,
    "max_output_tokens": null,
    "model": "gpt-4o-2024-08-06",
    "output": [
        {
            "type": "message",
            "id": "msg_67d533a84da08191940e7695a6ada42801f8172a11e95da9",
            "status": "completed",
            "role": "assistant",
            "content": [
                {
                    "type": "output_text",
                    "text": "What is OpenAI Responses API?",
                    "annotations": []
                }
            ]
        }
    ],
    "parallel_tool_calls": true,
    "previous_response_id": null,
    "reasoning": {
        "effort": null,
        "generate_summary": null
    },
    "store": true,
    "temperature": 1.0,
    "text": {
        "format": {
            "type": "text"
        }
    },
    "tool_choice": "auto",
    "tools": [
        {
            "type": "web_search_preview",
            "search_context_size": "medium",
            "user_location": {
                "type": "approximate",
                "city": null,
                "country": "US",
                "region": null,
                "timezone": null
            }
        }
    ],
    "top_p": 1.0,
    "truncation": "disabled",
    "usage": {
        "input_tokens": 326,
        "input_tokens_details": {
            "cached_tokens": 0
        },
        "output_tokens": 89,
        "output_tokens_details": {
            "reasoning_tokens": 0
        },
        "total_tokens": 415
    },
    "user": null,
    "metadata": {}
}
    """,
        Mock.WEATHER_TOOL to """
           {
    "id": "resp_67d5337a93548191b9e71b5047988edd017ba49167235035",
    "object": "response",
    "created_at": 1742025594,
    "status": "completed",
    "error": null,
    "incomplete_details": null,
    "instructions": null,
    "max_output_tokens": 2048,
    "model": "gpt-4o-2024-08-06",
    "output": [
        {
            "type": "function_call",
            "id": "fc_67d5337b1be0819191fb4a37c324c6eb017ba49167235035",
            "call_id": "call_ipmP7tg49unJmR6eNC4EWPff",
            "name": "request",
            "arguments": "{\"model\":\"sonar\",\"stream\":false,\"messages\":[{\"role\":\"user\",\"content\":\"What is Responses API?\"}]}",
            "status": "completed"
        }
    ],
    "parallel_tool_calls": true,
    "previous_response_id": null,
    "reasoning": {
        "effort": null,
        "generate_summary": null
    },
    "store": false,
    "temperature": 1.0,
    "text": {
        "format": {
            "type": "text"
        }
    },
    "tool_choice": "auto",
    "tools": [
        {
            "type": "function",
            "description": "Request to interact with the 'sonar' model and retrieve information.",
            "name": "request",
            "parameters": {
                "type": "object",
                "required": [
                    "model",
                    "messages",
                    "stream"
                ],
                "properties": {
                    "model": {
                        "type": "string",
                        "description": "The model to be used for the request, in this case 'sonar'."
                    },
                    "stream": {
                        "type": "boolean",
                        "description": "Indicates whether the response should be streamed."
                    },
                    "messages": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "required": [
                                "role",
                                "content"
                            ],
                            "properties": {
                                "role": {
                                    "type": "string",
                                    "description": "The role of the message sender (e.g. 'user' or 'assistant')."
                                },
                                "content": {
                                    "type": "string",
                                    "description": "The content of the message being sent."
                                }
                            },
                            "additionalProperties": false
                        },
                        "description": "A list of messages to be sent within the request."
                    }
                },
                "additionalProperties": false
            },
            "strict": true
        }
    ],
    "top_p": 1.0,
    "truncation": "disabled",
    "usage": {
        "input_tokens": 366,
        "input_tokens_details": {
            "cached_tokens": 0
        },
        "output_tokens": 35,
        "output_tokens_details": {
            "reasoning_tokens": 0
        },
        "total_tokens": 401
    },
    "user": null,
    "metadata": {}
}     
        """
    )
}