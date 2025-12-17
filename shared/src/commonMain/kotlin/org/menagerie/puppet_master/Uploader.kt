package org.menagerie.puppet_master

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/**
 * A client for uploading files to the server.
 */
class Uploader(private val client: HttpClient) {

    /**
     * Uploads a file to the server as a byte array.
     *
     * @param bytes The file content as a byte array.
     * @param fileName The name of the file to upload.
     * @param serverIp The IP address of the server.
     * @return The name of the file on the server.
     */
    suspend fun upload(bytes: ByteArray, fileName: String, serverIp: String): String {
        val contentType = when {
            fileName.endsWith(".troupe") || fileName.endsWith(".puppet") -> "application/zip"
            else -> "image/png"
        }
        val response = client.submitFormWithBinaryData(
            url = "http://$serverIp:$SERVER_PORT/upload",
            formData = formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        )
        return response.body<String>()
    }
}
