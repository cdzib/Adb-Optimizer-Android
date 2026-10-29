package com.optimizer.tools.feature.localadb.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.optimizer.tools.AdbOptimizerApplication
import com.optimizer.tools.MainActivity
import com.optimizer.tools.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Vinculación de la depuración inalámbrica desde una notificación.
 *
 * Android 11+ muestra el código de 6 dígitos en su propio diálogo; en lugar de obligar
 * al usuario a volver a la app, este servicio pide el código con un `RemoteInput` y,
 * al recibirlo, vincula y abre la sesión local. La pantalla de la app sigue mostrando
 * el mismo formulario como respaldo si las notificaciones están desactivadas.
 */
class LocalAdbPairingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: LibAdbLocalAdbRepository
    private var pairingPort: Int? = null
    private var stopJob: Job? = null
    private var finished = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = (application as AdbOptimizerApplication).container.localAdb
        createChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                startDiscovery()
            }

            ACTION_CODE -> {
                val code =
                    RemoteInput
                        .getResultsFromIntent(intent)
                        ?.getCharSequence(EXTRA_CODE)
                        ?.toString()
                        ?.trim()
                if (code.isNullOrEmpty()) {
                    post(withInput = true, text = getString(R.string.localadb_pairing_code_missing))
                } else {
                    submit(code)
                }
            }

            ACTION_STOP -> {
                finish()
            }
        }
        return START_NOT_STICKY
    }

    private fun startDiscovery() {
        if (finished) return
        startForeground(NOTIFICATION_ID, build(searching = true, withInput = false))
        scope.launch {
            val port = runCatching { repository.discoverPairingPort() }.getOrNull()
            if (port == null) {
                post(withInput = false, text = getString(R.string.localadb_pairing_port_missing))
                scheduleStop()
                return@launch
            }
            pairingPort = port
            post(withInput = true, text = getString(R.string.localadb_pairing_notification_ready, port))
        }
    }

    private fun submit(code: String) {
        val port = pairingPort
        if (port == null) {
            post(withInput = false, text = getString(R.string.localadb_pairing_port_missing))
            scheduleStop()
            return
        }
        scope.launch {
            post(withInput = false, text = getString(R.string.localadb_pairing_notification_working))
            if (runCatching { repository.pair(port, code) }.isFailure) {
                post(withInput = true, text = getString(R.string.localadb_pairing_notification_failed))
                return@launch
            }
            if (runCatching { repository.connect() }.isFailure) {
                post(withInput = false, text = getString(R.string.localadb_pairing_notification_connect_failed))
                scheduleStop()
                return@launch
            }
            post(withInput = false, text = getString(R.string.localadb_pairing_notification_connected))
            scheduleStop()
        }
    }

    private fun post(
        withInput: Boolean,
        text: String,
    ) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, build(searching = false, withInput = withInput, text = text))
    }

    private fun scheduleStop() {
        stopJob?.cancel()
        stopJob =
            scope.launch {
                delay(STOP_DELAY_MS)
                finish()
            }
    }

    private fun finish() {
        finished = true
        stopJob?.cancel()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun build(
        searching: Boolean,
        withInput: Boolean,
        text: String = getString(R.string.localadb_pairing_notification_searching),
    ): Notification {
        val builder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_optimizer)
                .setContentTitle(getString(R.string.localadb_pairing_notification_title))
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setOnlyAlertOnce(!searching)
                .setOngoing(true)
                .setContentIntent(openAppIntent())
        val stopAction =
            NotificationCompat.Action
                .Builder(null, getString(R.string.localadb_pairing_stop), stopIntent())
                .build()
        builder.addAction(stopAction)
        if (withInput) {
            val input =
                RemoteInput
                    .Builder(EXTRA_CODE)
                    .setLabel(getString(R.string.localadb_pairing_code_hint))
                    .setAllowFreeFormInput(true)
                    .build()
            val codeAction =
                NotificationCompat.Action
                    .Builder(null, getString(R.string.localadb_pairing_notification_action), codeIntent())
                    .addRemoteInput(input)
                    .build()
            builder.addAction(codeAction)
        }
        return builder.build()
    }

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun codeIntent(): PendingIntent {
        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getService(
            this,
            1,
            Intent(this, LocalAdbPairingService::class.java).setAction(ACTION_CODE),
            flags,
        )
    }

    private fun stopIntent(): PendingIntent =
        PendingIntent.getService(
            this,
            2,
            Intent(this, LocalAdbPairingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(CHANNEL_ID) == null
        ) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.localadb_pairing_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.optimizer.tools.action.LOCAL_ADB_PAIR"
        const val ACTION_CODE = "com.optimizer.tools.action.LOCAL_ADB_PAIR_CODE"
        const val ACTION_STOP = "com.optimizer.tools.action.LOCAL_ADB_PAIR_STOP"
        private const val EXTRA_CODE = "pairing_code"
        private const val CHANNEL_ID = "local_adb_pairing"
        private const val NOTIFICATION_ID = 42
        private const val STOP_DELAY_MS = 5_000L

        fun start(context: Context) {
            val intent =
                Intent(context, LocalAdbPairingService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
