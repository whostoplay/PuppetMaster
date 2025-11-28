package org.menagerie.puppet_master

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class Uploader {
    private val client = HttpClient()

    // Uploads the file and returns the server-side filename as a string
    suspend fun upload(bytes: ByteArray, fileName: String): String {
        val response = client.submitFormWithBinaryData(
            url = "http://127.0.0.1:$SERVER_PORT/upload",
            formData = formData {
                append("image", bytes, Headers.build { 
                    append(HttpHeaders.ContentType, "image/png")
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        )
        return response.body<String>()
    }
}