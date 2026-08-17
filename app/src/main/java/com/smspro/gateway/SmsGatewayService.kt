package com.smspro.gateway

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Service Foreground = coeur de la passerelle SMS.
 * Tourne en continu, interroge le backend pour récupérer la file d'envoi,
 * envoie les SMS via SmsSender avec un débit contrôlé, et remonte les statuts.
 */
class SmsGatewayService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var smsSender: SmsSender
    private lateinit var apiClient: BackendApiClient

    // Débit d'envoi : intervalle mini entre 2 SMS pour éviter un blocage anti-spam opérateur
    private val sendIntervalMs = 3000L
    // Intervalle de polling du backend quand la file est vide
    private val pollIntervalMs = 10000L

    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        smsSender = SmsSender(applicationContext)
        apiClient = BackendApiClient.getInstance(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Passerelle SMS active"))
        if (!isRunning) {
            isRunning = true
            startQueueLoop()
        }
        // START_STICKY : Android relance le service s'il est tué (manque de mémoire, etc.)
        return START_STICKY
    }

    /**
     * Boucle principale : tant que le service tourne, on va chercher les SMS
     * en attente sur le backend et on les envoie un par un avec un délai
     * entre chaque envoi pour respecter le débit max.
     */
    private fun startQueueLoop() {
        scope.launch {
            while (isRunning) {
                try {
                    val pendingMessages = apiClient.fetchPendingMessages(batchSize = 20)

                    if (pendingMessages.isEmpty()) {
                        updateNotification("En attente de messages…")
                        delay(pollIntervalMs)
                        continue
                    }

                    for (message in pendingMessages) {
                        if (!isRunning) break

                        updateNotification("Envoi en cours (${message.recipientPhone})")

                        val result = smsSender.send(
                            phoneNumber = message.recipientPhone,
                            text = message.content,
                            messageId = message.id
                        )

                        // On remonte immédiatement le statut au backend
                        apiClient.reportStatus(
                            messageId = message.id,
                            status = if (result.success) "SENT" else "FAILED",
                            errorReason = result.errorReason
                        )

                        delay(sendIntervalMs)
                    }
                } catch (e: Exception) {
                    // Erreur réseau ou backend indisponible : on attend avant de réessayer
                    updateNotification("Connexion au serveur impossible, nouvelle tentative…")
                    delay(pollIntervalMs)
                }
            }
        }
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- Notification (obligatoire pour un Foreground Service) ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Passerelle SMS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Statut de la passerelle SMS en arrière-plan"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Pro — Passerelle active")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(content))
    }

    companion object {
        private const val CHANNEL_ID = "sms_gateway_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
