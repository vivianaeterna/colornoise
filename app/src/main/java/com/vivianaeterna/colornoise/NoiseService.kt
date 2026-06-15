package com.vivianaeterna.colornoise // <-- CHANGE THIS

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat // Still used briefly for permission compat, but we build native below
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NoiseService : Service() {

    private val CHANNEL_ID = "ColorNoiseChannel"
    private val NOTIFICATION_ID = 1

    private var mediaSession: MediaSession? = null
    private var isServiceRunning = false // NEW: Track if service is active

    // NEW: Coroutine scope to observe state changes
    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Main +
                kotlinx.coroutines.SupervisorJob()
    )

    private val presetNames = listOf("White", "Pink", "Brown", "Blue", "Violet", "Green", "Grey")

    // Custom Actions for our Notification Buttons
    companion object {
        const val ACTION_PLAY = "com.vivianaeterna.colornoise.PLAY"
        const val ACTION_PAUSE = "com.vivianaeterna.colornoise.PAUSE"
        const val ACTION_STOP = "com.vivianaeterna.colornoise.STOP"
        const val ACTION_NEXT = "com.vivianaeterna.colornoise.NEXT"
        const val ACTION_PREV = "com.vivianaeterna.colornoise.PREV"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSession(this, "ColorNoiseSession")
        mediaSession?.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { AudioEngine.play(); updateNotification() }
            override fun onPause() { AudioEngine.stop(); updateNotification() }
            override fun onStop() { AudioEngine.stop(); stopSelf() }
            override fun onSkipToNext() { cyclePreset(1) }
            override fun onSkipToPrevious() { cyclePreset(-1) }
        })

        // Observe the preset name. When UI changes it, update the notification!
        serviceScope.launch {
            NoiseAppState.currentPresetName.collect { newName ->
                if (isServiceRunning) {
                    updateNotification()
                }
            }
        }
    }

    private fun cyclePreset(direction: Int) {
        val currentIndex = presetNames.indexOf(NoiseAppState.currentPresetName.value)
        val newIndex = (currentIndex + direction).let {
            if (it < 0) presetNames.size - 1 else if (it >= presetNames.size) 0 else it
        }

        val newName = presetNames[newIndex]
        val newBands = ColorPresets.getBandsByName(newName)

        AudioEngine.updateBands(newBands)
        NoiseAppState.setPresetName(newName)
        updateNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle our custom notification button clicks
        when (intent?.action) {
            ACTION_PLAY -> { AudioEngine.play(); updateNotification() }
            ACTION_PAUSE -> { AudioEngine.stop(); updateNotification() }
            ACTION_STOP -> { AudioEngine.stop(); stopSelf() }
            ACTION_NEXT -> cyclePreset(1)
            ACTION_PREV -> cyclePreset(-1)
        }

        val notification = buildNotification()

        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        isServiceRunning = true

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val isPlaying = NoiseAppState.isPlaying.value
        val presetName = NoiseAppState.currentPresetName.value

        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPendingIntent = contentIntent?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        // Create PendingIntents for our buttons
        val prevIntent = PendingIntent.getService(this, 0, Intent(this, NoiseService::class.java).setAction(ACTION_PREV), PendingIntent.FLAG_IMMUTABLE)
        val playPauseIntent = PendingIntent.getService(this, 0, Intent(this, NoiseService::class.java).setAction(if (isPlaying) ACTION_PAUSE else ACTION_PLAY), PendingIntent.FLAG_IMMUTABLE)
        val nextIntent = PendingIntent.getService(this, 0, Intent(this, NoiseService::class.java).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE)

        // Using the native Notification.Builder (No deprecation!)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Color Noise")
            .setContentText("Playing: $presetName")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_previous, "Previous", prevIntent).build())
            .addAction(Notification.Action.Builder(if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, if (isPlaying) "Pause" else "Play", playPauseIntent).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_next, "Next", nextIntent).build())
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Noise Playback", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioEngine.stop()
        mediaSession?.release()
        isServiceRunning = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}