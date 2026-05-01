package com.example.muscuapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.example.muscuapp.MainActivity
import com.example.muscuapp.R
import kotlinx.coroutines.*

class WorkoutTimerService : Service() {

    // --- Ajout pour communiquer avec l'interface (Compose) ---
    companion object {
        val currentTimerSeconds = mutableIntStateOf(0)
        val isTimerRunning = mutableStateOf(false)
        val isAlarmPlaying = mutableStateOf(false)
        val currentSessionId = mutableLongStateOf(-1L)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private var ringtone: Ringtone? = null
    private val CHANNEL_ID = "workout_timer_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Timer prêt")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_TIMER" -> {
                val durationSeconds = intent.getIntExtra("DURATION", 60)
                currentSessionId.longValue = intent.getLongExtra("SESSION_ID", -1)
                startTimer(durationSeconds)
            }
            "STOP_ALARM" -> {
                clearAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            "CANCEL_TIMER" -> {
                stopTimer()
                clearAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(seconds: Int) {
        stopTimer()
        clearAlarm()

        timerJob = serviceScope.launch {
            isTimerRunning.value = true
            var timeLeft = seconds
            while (timeLeft >= 0) {
                currentTimerSeconds.intValue = timeLeft
                updateNotification(timeLeft)
                if (timeLeft == 0) break
                delay(1000)
                timeLeft--
            }
            isTimerRunning.value = false
            playAlarm()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        isTimerRunning.value = false
        currentTimerSeconds.intValue = 0
    }

    private fun updateNotification(timeLeft: Int) {
        val minutes = timeLeft / 60
        val seconds = timeLeft % 60
        val timeStr = String.format("%02d:%02d", minutes, seconds)

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("muscuapp://live/${currentSessionId.longValue}"),
            this,
            MainActivity::class.java
        )
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val cancelIntent = Intent(this, WorkoutTimerService::class.java).apply { action = "CANCEL_TIMER" }
        val cancelPendingIntent = PendingIntent.getService(this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Temps de repos")
            .setContentText("Il reste $timeStr")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Annuler", cancelPendingIntent)
            .build()

        // Mise à jour classique au lieu de startForeground à chaque fois
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun playAlarm() {
        isAlarmPlaying.value = true
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone?.play()

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("muscuapp://live/${currentSessionId.longValue}"),
            this,
            MainActivity::class.java
        )
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(this, WorkoutTimerService::class.java).apply { action = "STOP_ALARM" }
        val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("REPOS FINI !")
            .setContentText("C'est l'heure de la prochaine série !")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "ARRÊTER L'ALARME", stopPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
        } else {
            vibrator.vibrate(longArrayOf(0, 500, 500), 0)
        }
    }

    private fun clearAlarm() {
        isAlarmPlaying.value = false
        ringtone?.stop()
        ringtone = null
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }

    private fun stopAlarm() {
        clearAlarm()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Workout Timer Channel",
                NotificationManager.IMPORTANCE_HIGH // CRUCIAL : HIGH pour que ça sonne et s'affiche !
            ).apply {
                description = "Notification pour le temps de repos"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopTimer()
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }
}