package org.menagerie.puppet_master

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class MainViewModel : ViewModel() {

    private val uploader = Uploader()
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { 
                ignoreUnknownKeys = true 
            })
        }
    }
    private val gson = Gson()
    private val localConfigFile = File("local_avatar_config.json")

    private val _localAvatarConfig = MutableStateFlow<AvatarConfiguration?>(null)
    val localAvatarConfig: StateFlow<AvatarConfiguration?> = _localAvatarConfig

    init {
        smartLoad()
    }

    private fun smartLoad() {
        viewModelScope.launch {
            val localConfig = loadLocalConfig()
            try {
                val serverConfig = client.get("http://127.0.0.1:$SERVER_PORT/config").body<AvatarConfiguration>()

                if (localConfig != null && localConfig.lastUpdated > serverConfig.lastUpdated) {
                    _localAvatarConfig.value = localConfig
                } else {
                    _localAvatarConfig.value = serverConfig
                    saveLocalConfig(serverConfig) // Sync local with server
                }
            } catch (e: Exception) {
                _localAvatarConfig.value = localConfig
            }
        }
    }

    private fun loadLocalConfig(): AvatarConfiguration? {
        if (!localConfigFile.exists()) return null
        return try {
            gson.fromJson(localConfigFile.readText(), AvatarConfiguration::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveLocalConfig(config: AvatarConfiguration) {
        localConfigFile.writeText(gson.toJson(config))
    }

    fun publishConfiguration() {
        viewModelScope.launch {
            _localAvatarConfig.value?.let {
                try {
                    client.post("http://127.0.0.1:$SERVER_PORT/config") {
                        contentType(ContentType.Application.Json)
                        setBody(it)
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    fun createNewState(stateName: String, imageBytes: ByteArray, localImageName: String) {
        viewModelScope.launch {
            try {
                val serverImageName = uploader.upload(imageBytes, localImageName)
                val newState = AvatarStateInfo(name = stateName, imageName = serverImageName)
                
                val currentConfig = _localAvatarConfig.value
                val newStates = currentConfig?.states.orEmpty() + newState
                val newConfig = AvatarConfiguration(
                    lastUpdated = System.currentTimeMillis(),
                    states = newStates
                )
                
                _localAvatarConfig.value = newConfig
                saveLocalConfig(newConfig)

            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}