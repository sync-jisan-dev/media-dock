package com.mediadock.app.core.downloads

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mediadock.app.MainActivity
import com.mediadock.app.R
import java.util.concurrent.ConcurrentHashMap

class ForegroundTransferService : Service() {

    companion object {
        const val CHANNEL_ID = "mediadock_transfer_channel"
        const val CHANNEL_COMPLETE_ID = "mediadock_completed_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1001

        const val ACTION_START = "action_start_transfer"
        const val ACTION_UPDATE = "action_update_transfer"
        const val ACTION_COMPLETE = "action_complete_transfer"
        const val ACTION_FAIL = "action_fail_transfer"
        const val ACTION_STOP = "action_stop_transfer"

        const val EXTRA_JOB_ID = "extra_job_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_ERROR = "extra_error"

        fun startService(context: Context, jobId: String, title: String) {
            val intent = Intent(context, ForegroundTransferService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_TITLE, title)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun updateProgress(
            context: Context,
            jobId: String,
            title: String,
            progress: Int,
            statusText: String = "",
            speed: String = ""
        ) {
            val intent = Intent(context, ForegroundTransferService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_STATUS, statusText)
                putExtra(EXTRA_SPEED, speed)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun completeDownload(context: Context, jobId: String, title: String) {
            val intent = Intent(context, ForegroundTransferService::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_TITLE, title)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun failDownload(context: Context, jobId: String, title: String, error: String) {
            val intent = Intent(context, ForegroundTransferService::class.java).apply {
                action = ACTION_FAIL
                putExtra(EXTRA_JOB_ID, jobId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ERROR, error)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ForegroundTransferService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private lateinit var notificationManager: NotificationManager
    private val activeJobTitles = ConcurrentHashMap<String, String>()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val jobId = intent.getStringExtra(EXTRA_JOB_ID) ?: "default_job"
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Downloading media"

        when (action) {
            ACTION_START -> {
                activeJobTitles[jobId] = title
                val notif = buildProgressNotification(title, 5, "Connecting to media server...", "")
                promoteToForeground(notif)
            }
            ACTION_UPDATE -> {
                activeJobTitles[jobId] = title
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                val statusText = intent.getStringExtra(EXTRA_STATUS) ?: ""
                val speed = intent.getStringExtra(EXTRA_SPEED) ?: ""
                val notif = buildProgressNotification(title, progress, statusText, speed)
                promoteToForeground(notif)
                notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notif)
            }
            ACTION_COMPLETE -> {
                activeJobTitles.remove(jobId)
                val completeNotif = buildCompleteNotification(title)
                notificationManager.notify(jobId.hashCode(), completeNotif)
                if (activeJobTitles.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            ACTION_FAIL -> {
                activeJobTitles.remove(jobId)
                val error = intent.getStringExtra(EXTRA_ERROR) ?: "Download failed"
                val failNotif = buildFailNotification(title, error)
                notificationManager.notify(jobId.hashCode(), failNotif)
                if (activeJobTitles.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                activeJobTitles.clear()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressChannel = NotificationChannel(
                CHANNEL_ID,
                "Media Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active media download progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(progressChannel)

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Completed Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when media downloads finish"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    private fun buildProgressNotification(
        title: String,
        progress: Int,
        statusText: String,
        speed: String
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openQueue", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = when {
            statusText.isNotBlank() && speed.isNotBlank() -> "$statusText • $speed ($progress%)"
            statusText.isNotBlank() -> "$statusText ($progress%)"
            speed.isNotBlank() -> "$progress% • $speed"
            else -> "Downloading • $progress%"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(displayText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun buildCompleteNotification(title: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openQueue", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
            .setContentTitle("Download Complete")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun buildFailNotification(title: String, error: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openQueue", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
            .setContentTitle("Download Failed")
            .setContentText("$title: $error")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
