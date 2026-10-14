package com.unknown.qqqqq

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlin.random.Random

/**
 * Фоновая служба: раз в 1.5 секунды смотрит, какое приложение вышло на передний план,
 * и иногда присылает от «?????» реплику под это приложение.
 */
class WatcherService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var usm: UsageStatsManager
    private lateinit var prefs: Prefs

    private var currentPkg: String? = null
    private var lastSeenTs = 0L
    private var lastAnyMsg = 0L
    private val lastPerApp = HashMap<String, Long>()
    private val ignored = HashSet<String>()

    private val tick = object : Runnable {
        override fun run() {
            try { poll() } catch (_: Exception) { }
            handler.postDelayed(this, 1500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        Notifier.createChannels(this)

        val n = Notifier.foreground(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifier.FG_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifier.FG_ID, n)
        }

        // Не реагируем на рабочий стол, шторку, клавиатуру и само себя
        ignored += packageName
        ignored += "android"
        ignored += "com.android.systemui"
        try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            packageManager.queryIntentActivities(home, 0).forEach { ignored += it.activityInfo.packageName }
        } catch (_: Exception) { }

        lastSeenTs = System.currentTimeMillis()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.enabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun poll() {
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 15_000, now)
        val e = UsageEvents.Event()
        var newest: String? = null
        var newestTs = lastSeenTs
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND && e.timeStamp > newestTs) {
                newestTs = e.timeStamp
                newest = e.packageName
            }
        }
        lastSeenTs = newestTs
        val pkg = newest ?: return
        if (pkg == currentPkg) return
        currentPkg = pkg
        if (pkg in ignored || pkg.startsWith("com.android.inputmethod") || pkg.contains("launcher")) return
        onAppOpened(pkg)
    }

    private fun onAppOpened(pkg: String) {
        val opens = prefs.bumpOpenCount(pkg)
        val now = System.currentTimeMillis()

        // частота: шанс сработать и пауза для одного и того же приложения
        val (chance, perAppPause) = when (prefs.frequency) {
            0 -> 30 to 15 * 60_000L
            2 -> 100 to 30_000L
            else -> 60 to 4 * 60_000L
        }
        if (now - lastAnyMsg < 20_000) return
        if (now - (lastPerApp[pkg] ?: 0L) < perAppPause) return
        if (Random.nextInt(100) >= chance) return

        lastAnyMsg = now
        lastPerApp[pkg] = now

        val label = appLabel(this, pkg)
        val (text, key) = Phrases.make(this, pkg, label, opens, prefs.recent())
        prefs.pushRecent(key)

        // небольшая пауза — будто она «заметила» и пишет
        handler.postDelayed({ Notifier.sendMessage(this, text) }, 1200L + Random.nextLong(2500L))
    }

    companion object {
        fun appLabel(ctx: Context, pkg: String): String = try {
            val ai = ctx.packageManager.getApplicationInfo(pkg, 0)
            ctx.packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast('.')
        }

        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, WatcherService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, WatcherService::class.java))
        }
    }
}
