package org.menagerie.puppet_master

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.readRemaining
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.seconds

// --- Data Models & Persistence ---

val gson = Gson()
val configFile = File("avatar_config.json")

fun saveConfiguration(config: AvatarConfiguration) {
    configFile.writeText(gson.toJson(config))
}

fun loadConfiguration(): AvatarConfiguration {
    if (!configFile.exists()) {
        // Create a default config if one doesn't exist
        return AvatarConfiguration(
            lastUpdated = System.currentTimeMillis(),
            states = listOf(
                AvatarStateInfo("idle", "idle.png"),
                AvatarStateInfo("talking", "talking.png")
            )
        )
    }
    val type = object : TypeToken<AvatarConfiguration>() {}.type
    return gson.fromJson(configFile.readText(), type)
}

// --- Main Application ---

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val uploadsDir = File("uploads").apply { mkdirs() }
    var avatarConfig = loadConfiguration()

    val activeState = MutableStateFlow(avatarConfig.states.find { it.name == "idle" } ?: avatarConfig.states.first())

    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        staticFiles("/uploads", uploadsDir)

        // --- Configuration API for the Client ---

        get("/config") {
            call.respond(avatarConfig)
        }

        post("/config") {
            val newConfig = call.receive<AvatarConfiguration>()
            avatarConfig = newConfig
            saveConfiguration(avatarConfig)
            activeState.value = avatarConfig.states.find { it.name == "idle" } ?: avatarConfig.states.first()
            call.respond(HttpStatusCode.OK)
        }

        post("/upload") {
            val multipart = call.receiveMultipart()
            var fileName = ""
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    fileName = part.originalFileName as String
                    val fileBytes = part.provider().readRemaining().readBytes()
                    File(uploadsDir, fileName).writeBytes(fileBytes)
                }
                part.dispose()
            }
            call.respondText(fileName)
        }

        // --- Real-time Endpoints ---

        webSocket("/audio-input") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val isSpeaking = frame.readText().toBoolean()
                    val targetStateName = if (isSpeaking) "talking" else "idle"
                    avatarConfig.states.find { it.name == targetStateName }?.let {
                        activeState.value = it
                    }
                }
            }
        }

        webSocket("/obs") {
            activeState.asStateFlow().collect { state ->
                val imageFile = File(uploadsDir, state.imageName)
                if (imageFile.exists()) {
                    val imageUrl = "http://127.0.0.1:$SERVER_PORT/uploads/${state.imageName}"
                    outgoing.send(Frame.Text(imageUrl))
                } else {
                    // If file doesn't exist, clear the image in OBS
                    outgoing.send(Frame.Text(""))
                }
            }
        }
    }
}