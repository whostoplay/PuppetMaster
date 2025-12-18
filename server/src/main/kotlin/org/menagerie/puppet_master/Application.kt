package org.menagerie.puppet_master

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.deserialize
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
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
import io.ktor.websocket.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.Duration
import java.util.zip.ZipInputStream

/**
 * Main entry point for the Puppet Master server application.
 */
fun main() {
    embeddedServer(Netty, port = Constants.Server.PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/**
 * Main module for the Ktor application.
 * Configures plugins and defines routing.
 */
fun Application.module() {
    val troupeManager = TroupeManager()
    val stateManager = PuppetStateManager(this, troupeManager)
    val jsonDecoder = Json { ignoreUnknownKeys = true; allowStructuredMapKeys = true }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    install(ContentNegotiation) {
        json(Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true })
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true })
    }

    routing {
        val uploadsDir = File("uploads").apply { mkdirs() }

        /**
         * Serves uploaded files from the 'uploads' directory.
         * Responds with a 404 Not Found if the file does not exist.
         * It disables caching for the served files.
         */
        get("/uploads/{fileName}") {
            val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val file = File(uploadsDir, fileName)
            if (file.exists()) {
                call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
                call.response.header(HttpHeaders.Pragma, "no-cache")
                call.response.header(HttpHeaders.Expires, "0")
                call.respondBytes(file.readBytes())
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        /**
         * Gets the current [PuppetTroupe].
         * Responds with the troupe or 404 Not Found if no troupe is active.
         */
        get("/troupe") {
            troupeManager.troupe?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NotFound)
        }

        /**
         * Updates the current [PuppetTroupe].
         * The new troupe is received from the request body.
         */
        post("/troupe") {
            val newTroupe = call.receive<PuppetTroupe>()
            troupeManager.updateTroupe(newTroupe)
            stateManager.onTroupeUpdated()
            call.respond(HttpStatusCode.OK)
        }

        /**
         * Handles file uploads.
         * If the uploaded file is a `.troupe` or `.puppet` zip archive, it extracts the contents.
         * For `.troupe` files, it reads `troupe.json` and updates the current troupe.
         * Other files are saved directly to the 'uploads' directory.
         */
        post("/upload") {
            val multipart = call.receiveMultipart()
            var fileName = ""
            var troupeJsonFile: File? = null
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    fileName = part.originalFileName as String
                    val fileBytes = part.streamProvider().readBytes()
                    if (fileName.endsWith(".troupe") || fileName.endsWith(".puppet")) {
                        // It's a zip archive, so we need to extract it
                        ZipInputStream(fileBytes.inputStream()).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val file = File(uploadsDir, entry.name)
                                if (entry.isDirectory) {
                                    file.mkdirs()
                                } else {
                                    file.parentFile?.mkdirs()
                                    file.outputStream().use { fos ->
                                        zis.copyTo(fos)
                                    }
                                    if (file.name == "troupe.json") {
                                        troupeJsonFile = file
                                    }
                                }
                                entry = zis.nextEntry
                            }
                        }

                        if (fileName.endsWith(".troupe")) {
                            troupeJsonFile?.let {
                                if (it.exists()) {
                                    val troupe = jsonDecoder.decodeFromString(PuppetTroupe.serializer(), it.readText())
                                    troupeManager.updateTroupe(troupe)
                                    stateManager.onTroupeUpdated()
                                }
                            }
                        }
                    } else {
                        // It's a single file, so we just write it
                        File(uploadsDir, fileName).writeBytes(fileBytes)
                    }
                }
                part.dispose()
            }
            call.respondText(fileName)
        }

        /**
         * WebSocket endpoint for receiving mouse tracking data.
         * It handles 'pointer' messages with [MousePosition] and 'calibration' messages with [CalibrationData].
         */
        webSocket("/mouse") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val jsonElement = jsonDecoder.parseToJsonElement(text).jsonObject
                    when (jsonElement["type"]?.jsonPrimitive?.content) {
                        "pointer" -> {
                            val position = jsonDecoder.decodeFromJsonElement(MousePosition.serializer(), jsonElement)
                            stateManager.onMousePositionChanged(SerializableOffset(position.x.toFloat(), position.y.toFloat()))
                        }

                        "calibration" -> {
                            val calibrationData = jsonDecoder.decodeFromJsonElement(CalibrationData.serializer(), jsonElement)
                            stateManager.onCalibrationReceived(calibrationData)
                        }
                    }
                }
            }
        }

        /**
         * WebSocket endpoint for receiving audio input levels for an active puppet.
         * The connection is closed if the server is not in headless mode or no puppet is active.
         * Also closes if a client takes manual control.
         */
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

        /**
         * WebSocket endpoint for a client to take manual control of the puppet.
         * The server will receive [ServerState] objects from the client and update accordingly.
         * The connection is closed if no puppet is active.
         */
        webSocket("/client-control") {
            if (troupeManager.activePuppet == null) {
                close(CloseReason(CloseReason.Codes.NORMAL, "No active puppet configured on server"))
                return@webSocket
            }
            stateManager.manualControlActive = true
            try {
                for (frame in incoming) {
                    val serverState = converter!!.deserialize<ServerState>(frame)
                    stateManager.onClientSentState(serverState)
                }
            } finally {
                stateManager.onClientDisconnected()
            }
        }

        /**
         * WebSocket endpoint for OBS clients to receive [ServerState] updates.
         * Manages the number of connected OBS clients.
         */
        webSocket("/obs") {
            stateManager.obsConnectionCount++
            try {
                stateManager.stateToSend.collectLatest { state ->
                    sendSerialized(state)
                }
            } finally {
                stateManager.obsConnectionCount--
            }
        }
    }
}
