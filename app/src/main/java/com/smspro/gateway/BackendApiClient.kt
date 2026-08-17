package com.smspro.gateway

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class PendingMessage(
    val id: String,
    val recipientPhone: String,
    val content: String
)

/**
 * Client HTTP minimal vers le backend. Le token JWT est stocké après
 * connexion (voir module Auth, non inclus ici) et injecté à chaque appel.
 */
class BackendApiClient private constructor(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://api.smspro.exemple.com" // à remplacer par l'URL réelle du backend
    private val jsonMediaType = "application/json".toMediaType()

    private fun authToken(): String =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("jwt_token", "") ?: ""

    /** Récupère un lot de messages en attente d'envoi depuis le backend. */
    suspend fun fetchPendingMessages(batchSize: Int): List<PendingMessage> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/messages/pending?limit=$batchSize")
                .addHeader("Authorization", "Bearer ${authToken()}")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(body)
                (0 until jsonArray.length()).map { i ->
                    val obj = jsonArray.getJSONObject(i)
                    PendingMessage(
                        id = obj.getString("id"),
                        recipientPhone = obj.getString("recipient_phone"),
                        content = obj.getString("content")
                    )
                }
            }
        }

    /** Remonte le statut d'un SMS (SENT / FAILED) au backend pour mise à jour des stats. */
    suspend fun reportStatus(messageId: String, status: String, errorReason: String?) =
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("status", status)
                errorReason?.let { put("error_reason", it) }
            }
            val request = Request.Builder()
                .url("$baseUrl/api/messages/$messageId/status")
                .addHeader("Authorization", "Bearer ${authToken()}")
                .patch(json.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().close()
        }

    companion object {
        @Volatile private var instance: BackendApiClient? = null

        fun getInstance(context: Context): BackendApiClient =
            instance ?: synchronized(this) {
                instance ?: BackendApiClient(context.applicationContext).also { instance = it }
            }
    }
}
