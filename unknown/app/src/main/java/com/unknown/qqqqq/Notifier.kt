package com.unknown.qqqqq

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat

object Notifier {
    const val CH_MSG = "msgs"
    const val CH_BG = "bg"
    const val FG_ID = 1
    private var nextId = 100

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val msg = NotificationChannel(CH_MSG, "Сообщения", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Сообщения от ?????"
            enableVibration(true)
        }
        val bg = NotificationChannel(CH_BG, "Фон (можно скрыть)", NotificationManager.IMPORTANCE_MIN).apply {
            description = "Служебное уведомление, чтобы приложение работало в фоне"
            setShowBadge(false)
        }
        nm.createNotificationChannel(msg)
        nm.createNotificationChannel(bg)
    }

    private fun openApp(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun foreground(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, CH_BG)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("?????")
            .setContentText("в сети")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openApp(ctx))
            .build()

    fun sendMessage(ctx: Context, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val her = Person.Builder().setName("?????").setKey("unknown").build()
        val me = Person.Builder().setName("Я").setKey("me").build()
        val style = NotificationCompat.MessagingStyle(me)
            .addMessage(text, System.currentTimeMillis(), her)

        val n = NotificationCompat.Builder(ctx, CH_MSG)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(0xFFE0245E.toInt())
            .setContentTitle("?????")
            .setContentText(text)
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(nextId++, n)
        } catch (_: SecurityException) { }
        if (nextId > 130) nextId = 100

        Prefs(ctx).pushHistory(text)
    }
}
