package org.menagerie.puppet_master

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.readRemaining
import io.ktor.websocket.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.io.readByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val troupeManager = TroupeManager()
    val stateManager = PuppetStateManager(this, troupeManager)
    val jsonDecoder = Json { ignoreUnknownKeys = true }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json(Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true })
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        val uploadsDir = File("uploads").apply { mkdirs() }
        staticFiles("/uploads", uploadsDir)

        get("/troupe") {
            troupeManager.troupe?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NotFound)
        }

        post("/troupe") {
            val newTroupe = call.receive<PuppetTroupe>()
            troupeManager.updateTroupe(newTroupe)
            stateManager.onTroupeUpdated()
            call.respond(HttpStatusCode.OK)
        }

        post("/upload") {
            val multipart = call.receiveMultipart()
            var fileName = ""
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    fileName = part.originalFileName as String
                    val fileBytes = part.provider().readRemaining().readByteArray()
                    File(uploadsDir, fileName).writeBytes(fileBytes)
                }
                part.dispose()
            }
            call.respondText(fileName)
        }

        webSocket("/mouse") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val jsonElement = jsonDecoder.parseToJsonElement(text).jsonObject
                    when (jsonElement["type"]?.jsonPrimitive?.content) {
                        "pointer" -> {
                            val position = jsonDecoder.decodeFromJsonElement(MousePosition.serializer(), jsonElement)
                            stateManager.onMousePositionChanged(position)
                        }
                        "calibration" -> {
                            val calibrationData = jsonDecoder.decodeFromJsonElement(CalibrationData.serializer(), jsonElement)
                            stateManager.onCalibrationReceived(calibrationData)
                        }
                    }
                }
            }
        }

        webSocket("/audio-input") {
            if (stateManager.obsConnectionCount > 0 && troupeManager.activePuppet != null) {
                for (frame in incoming) {
                    if (stateManager.manualControlActive) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client took control"))
                        break
                    }
                    if (frame is Frame.Text) {
                        frame.readText().toFloatOrNull()?.let { level ->
                            stateManager.onAudioLevelChanged(level)
                        }
                    }
                }
            } else {
                close(CloseReason(CloseReason.Codes.NORMAL, "Server not in headless mode or no active puppet"))
            }
        }

        webSocket("/client-control") {
            if (troupeManager.activePuppet == null) {
                close(CloseReason(CloseReason.Codes.NORMAL, "No active puppet configured on server"))
                return@webSocket
            }
            stateManager.manualControlActive = true
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        stateManager.onClientSentState(frame.readText())
                    }
                }
            } finally {
                stateManager.onClientDisconnected()
            }
        }

        webSocket("/obs") {
            stateManager.obsConnectionCount++
            try {
                stateManager.stateToSend.collectLatest { state ->
                    val json = Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true }
                    send(Frame.Text(json.encodeToString(state)))
                }
            } finally {
                stateManager.obsConnectionCount--
            }
        }
    }
}