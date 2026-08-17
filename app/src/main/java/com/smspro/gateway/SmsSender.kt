package com.smspro.gateway

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class SendResult(val success: Boolean, val errorReason: String? = null)

/**
 * Encapsule l'envoi d'un SMS via l'API Android SmsManager.
 * Écoute les 2 broadcasts système (SENT et DELIVERED) pour connaître
 * le vrai statut de l'envoi, pas juste "on a appelé la fonction".
 */
class SmsSender(private val context: Context) {

    suspend fun send(phoneNumber: String, text: String, messageId: String): SendResult =
        suspendCancellableCoroutine { continuation ->
            val smsManager = context.getSystemService(SmsManager::class.java)

            val sentAction = "${ACTION_SMS_SENT}.$messageId"
            val sentIntent = PendingIntent.getBroadcast(
                context, messageId.hashCode(), Intent(sentAction),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val result = when (resultCode) {
                        android.app.Activity.RESULT_OK ->
                            SendResult(success = true)
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                            SendResult(false, "Échec générique de l'opérateur")
                        SmsManager.RESULT_ERROR_NO_SERVICE ->
                            SendResult(false, "Pas de réseau / service indisponible")
                        SmsManager.RESULT_ERROR_RADIO_OFF ->
                            SendResult(false, "Mode avion activé")
                        SmsManager.RESULT_ERROR_NULL_PDU ->
                            SendResult(false, "Erreur de formatage du message")
                        else -> SendResult(false, "Erreur inconnue (code $resultCode)")
                    }
                    context.unregisterReceiver(this)
                    if (continuation.isActive) continuation.resume(result)
                }
            }

            context.registerReceiver(receiver, IntentFilter(sentAction))

            try {
                // Découpe automatiquement les messages longs (>160 caractères) en plusieurs parties
                val parts = smsManager.divideMessage(text)
                if (parts.size > 1) {
                    val sentIntents = ArrayList<PendingIntent>().apply {
                        repeat(parts.size) { add(sentIntent) }
                    }
                    smsManager.sendMultipartTextMessage(
                        phoneNumber, null, parts, sentIntents, null
                    )
                } else {
                    smsManager.sendTextMessage(phoneNumber, null, text, sentIntent, null)
                }
            } catch (e: Exception) {
                context.unregisterReceiver(receiver)
                if (continuation.isActive) {
                    continuation.resume(SendResult(false, e.message ?: "Exception à l'envoi"))
                }
            }
        }

    companion object {
        private const val ACTION_SMS_SENT = "com.smspro.gateway.SMS_SENT"
    }
}
