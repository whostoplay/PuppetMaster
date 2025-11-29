package org.menagerie.puppet_master

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

// --- Data Models & Persistence ---

val json = Json {
    prettyPrint = true
    isLenient = true
    ignoreUnknownKeys = true
}
val troupeFile = File("troupe.json")
val legacyConfigFile = File("puppet_config.json")

fun saveTroupe(troupe: Troupe) {
    troupeFile.writeText(json.encodeToString(troupe))
}

fun loadTroupe(): Troupe? {
    if (troupeFile.exists()) {
        return try {
            json.decodeFromString<Troupe>(troupeFile.readText())
        } catch (e: Exception) {
            // If troupe file is corrupt, treat as non-existent
            null
        }
    }

    // If the new file doesn't exist, try to migrate from the old format
    if (legacyConfigFile.exists()) {
        return try {
            val legacyConfig = json.decodeFromString<PuppetConfiguration>(legacyConfigFile.readText())
            val defaultPuppet = Puppet("Default", legacyConfig.lastUpdated, legacyConfig.states)
            val troupe = Troupe("Default", listOf(defaultPuppet))
            saveTroupe(troupe)
            legacyConfigFile.delete() // remove old file after successful migration
            troupe
        } catch (e: Exception) {
            null
        }
    }

    return null
}

// --- Main Application ---

var obsConnectionCount = 0

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val uploadsDir = File("uploads").apply { mkdirs() }
    var troupe: Troupe? = loadTroupe()
    var activePuppet: Puppet? = troupe?.puppets?.find { it.name == troupe?.activePuppetName }
    val activeState = MutableStateFlow<PuppetStateInfo?>(activePuppet?.states?.find { it.name == "idle" } ?: activePuppet?.states?.firstOrNull())

    var manualControlActive = false
    fun isHeadless() = obsConnectionCount > 0 && !manualControlActive

    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val stateToSend = MutableStateFlow(activeState.value)
    var blinkingJob: Job? = null

    // This collector now ONLY handles blinking for headless mode.
    launch {
        activeState.collect { state ->
            blinkingJob?.cancel()
            stateToSend.value = state // Immediately update the display state.
            if (state?.blinkImageName != null) {
                blinkingJob = launch {
                    while (true) {
                        delay(Random.nextLong(2000, 8000))
                        // This is the key condition: ONLY blink if in headless mode.
                        if (isHeadless() && activeState.value == state) {
                            val blinkState = state.copy(imageName = state.blinkImageName!!)
                            stateToSend.value = blinkState
                            delay(150)
                            stateToSend.value = state
                        }
                    }
                }
            }
        }
    }

    routing {
        staticFiles("/uploads", uploadsDir)

        // --- Configuration API for the Client ---

        get("/troupe") {
            troupe?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NotFound)
        }

        post("/troupe") {
            val newTroupe = call.receive<Troupe>()
            troupe = newTroupe
            saveTroupe(newTroupe)
            activePuppet = troupe?.puppets?.find { it.name == newTroupe.activePuppetName }
            if (!isHeadless()) {
                activeState.value = activePuppet?.states?.find { it.name == "idle" } ?: activePuppet?.states?.firstOrNull()
            }
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

        post("/state") {
            call.respond(HttpStatusCode.Forbidden, "State updates must be sent via the /client-control WebSocket.")
        }

        // --- Real-time Endpoints ---

        webSocket("/audio-input") {
            if (isHeadless() && activePuppet != null) {
                for (frame in incoming) {
                    if (!isHeadless()) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client took control"))
                        break
                    }

                    if (frame is Frame.Text) {
                        val isSpeaking = frame.readText().toBoolean()
                        val targetStateName = if (isSpeaking) "talking" else "idle"
                        activePuppet?.states?.find { it.name == targetStateName }?.let {
                            activeState.value = it
                        }
                    }
                }
            } else {
                close(CloseReason(CloseReason.Codes.NORMAL, "Server not in headless mode or no active puppet"))
            }
        }

        webSocket("/client-control") {
            if (activePuppet == null) {
                close(CloseReason(CloseReason.Codes.NORMAL, "No active puppet configured on server"))
                return@webSocket
            }
            manualControlActive = true
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val receivedImageName = frame.readText()
                        val baseState = activePuppet?.states?.find { it.imageName == receivedImageName || it.blinkImageName == receivedImageName }
                        if (baseState != null) {
                            if (activeState.value != baseState) {
                                activeState.value = baseState
                            }
                            if (stateToSend.value?.imageName != receivedImageName) {
                                // Create a temporary state with the correct image name (for blinks)
                                val tempState = baseState.copy(imageName = receivedImageName)
                                stateToSend.value = tempState
                            }
                        }
                    }
                }
            } finally {
                manualControlActive = false
                activeState.value = activePuppet?.states?.find { it.name == "idle" } ?: activePuppet?.states?.firstOrNull()
            }
        }

        webSocket("/obs") {
            obsConnectionCount++
            try {
                var lastSentImage: String? = null
                while(true) {
                    val state = stateToSend.value
                    val imageUrl = if (state != null) {
                        val imageFile = File(uploadsDir, state.imageName)
                        if (imageFile.exists()) {
                            "http://127.0.0.1:$SERVER_PORT/uploads/${state.imageName}"
                        } else {
                            ""
                        }
                    } else {
                        ""
                    }

                    if (imageUrl != lastSentImage) {
                        outgoing.send(Frame.Text(imageUrl))
                        lastSentImage = imageUrl
                    }
                    delay(16) // ~60fps
                }
            } finally {
                obsConnectionCount--
            }
        }
    }
}
