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
val configFile = File("puppet_config.json")

fun saveConfiguration(config: PuppetConfiguration) {
    configFile.writeText(json.encodeToString(config))
}

fun loadConfiguration(): PuppetConfiguration {
    if (!configFile.exists() || configFile.readText().isBlank()) {
        // Create a default config if one doesn't exist
        return PuppetConfiguration(
            lastUpdated = System.currentTimeMillis(),
            states = listOf(
                PuppetStateInfo("idle", "idle.png"),
                PuppetStateInfo("talking", "talking.png")
            )
        )
    }
    return try {
        json.decodeFromString<PuppetConfiguration>(configFile.readText())
    } catch (e: Exception) {
        // If file is corrupt, create a default config.
        PuppetConfiguration(
            lastUpdated = System.currentTimeMillis(),
            states = listOf(
                PuppetStateInfo("idle", "idle.png"),
                PuppetStateInfo("talking", "talking.png")
            )
        )
    }
}

// --- Main Application ---

var obsConnectionCount = 0

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val uploadsDir = File("uploads").apply { mkdirs() }
    var puppetConfiguration = loadConfiguration()
    val activeState = MutableStateFlow(puppetConfiguration.states.find { it.name == "idle" } ?: puppetConfiguration.states.first())

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
            if (state.blinkImageName != null) {
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

        get("/config") {
            call.respond(puppetConfiguration)
        }

        post("/config") {
            val newConfig = call.receive<PuppetConfiguration>()
            puppetConfiguration = newConfig
            saveConfiguration(puppetConfiguration)
            if (!isHeadless()) {
                activeState.value = puppetConfiguration.states.find { it.name == "idle" } ?: puppetConfiguration.states.first()
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
            if (isHeadless()) {
                for (frame in incoming) {
                    if (!isHeadless()) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client took control"))
                        break
                    }

                    if (frame is Frame.Text) {
                        val isSpeaking = frame.readText().toBoolean()
                        val targetStateName = if (isSpeaking) "talking" else "idle"
                        puppetConfiguration.states.find { it.name == targetStateName }?.let {
                            activeState.value = it
                        }
                    }
                }
            } else {
                close(CloseReason(CloseReason.Codes.NORMAL, "Server not in headless mode"))
            }
        }

        webSocket("/client-control") {
            manualControlActive = true
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val receivedImageName = frame.readText()
                        // Find the base state this image belongs to, so we know the logical state.
                        val baseState = puppetConfiguration.states.find { it.imageName == receivedImageName || it.blinkImageName == receivedImageName }
                        if (baseState != null) {
                            // Update the logical active state IF it's different
                            if (activeState.value != baseState) {
                                activeState.value = baseState
                            }
                            // CRITICAL: Pass the client's exact image name (including blinks) to the state that OBS sees.
                            if (stateToSend.value.imageName != receivedImageName) {
                                // Create a temporary state with the exact image name for OBS
                                stateToSend.value = baseState.copy(imageName = receivedImageName)
                            }
                        }
                    }
                }
            } finally {
                manualControlActive = false
                // When client disconnects, revert to idle. The collector will pick this up and handle blinking if needed.
                activeState.value = puppetConfiguration.states.find { it.name == "idle" } ?: puppetConfiguration.states.first()
            }
        }

        webSocket("/obs") {
            obsConnectionCount++
            try {
                var lastSentImage: String? = null
                while(true) {
                    val state = stateToSend.value
                    val imageFile = File(uploadsDir, state.imageName)
                    val imageUrl = if (imageFile.exists()) {
                        "http://127.0.0.1:$SERVER_PORT/uploads/${state.imageName}"
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
