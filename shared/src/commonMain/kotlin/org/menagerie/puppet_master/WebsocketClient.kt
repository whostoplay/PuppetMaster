package org.menagerie.puppet_master

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.HttpMethod
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

class WebsocketClient {
    private val client = HttpClient { install(WebSockets) }
    private var session: DefaultClientWebSocketSession? = null

    private val _messages = MutableStateFlow<String>("")
    val messages: StateFlow<String> = _messages

    suspend fun connect() {
        if (session != null) return
        try {
            client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = SERVER_PORT, path = "/ws") {
                session = this
                for (frame in incoming) {
                    frame as? Frame.Text ?: continue
                    val receivedText = frame.readText()
                    _messages.value = receivedText
                }
            }
        } catch (e: Exception) {
            _messages.value = "Error: ${e.message}"
        }
    }

    suspend fun send(state: AvatarState) {
        val json = Json.encodeToString(AvatarState.serializer(), state)
        session?.send(json)
    }

    suspend fun close() {
        session?.close()
        session = null
        client.close()
    }
}
