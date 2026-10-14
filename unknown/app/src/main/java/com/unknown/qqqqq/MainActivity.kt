package com.unknown.qqqqq

import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val bg = Color.parseColor("#0E0E12")
    private val card = Color.parseColor("#1A1A22")
    private val accent = Color.parseColor("#E0245E")
    private val textMain = Color.parseColor("#EDEDF2")
    private val textDim = Color.parseColor("#8A8A99")

    private lateinit var prefs: Prefs
    private lateinit var usageBtn: Button
    private lateinit var notifBtn: Button
    private lateinit var batteryBtn: Button
    private lateinit var toggleBtn: Button
    private lateinit var freqBtns: List<Button>
    private lateinit var historyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        Notifier.createChannels(this)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }

        root.addView(TextView(this).apply {
            text = "?????"
            textSize = 34f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(accent)
        })
        root.addView(label("она всё видит. и ей всё равно.", textDim, 14f, bottom = 20))

        root.addView(label("1. Разрешения", textMain, 16f, bold = true, bottom = 8))
        usageBtn = button { openUsageSettings() }
        notifBtn = button { askNotifications() }
        batteryBtn = button { askBattery() }
        root.addView(usageBtn); root.addView(notifBtn); root.addView(batteryBtn)
        root.addView(label("Без «Доступа к истории использования» она не узнает, что ты открыл.", textDim, 12f, bottom = 20))

        root.addView(label("2. Как часто писать", textMain, 16f, bold = true, bottom = 8))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val names = listOf("Редко", "Иногда", "Всегда")
        freqBtns = names.mapIndexed { i, n ->
            button(n) { prefs.frequency = i; refresh() }.also {
                row.addView(it, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = if (i < 2) dp(6) else 0
                })
            }
        }
        root.addView(row)
        root.addView(label("", textDim, 4f, bottom = 12))

        root.addView(label("3. Запуск", textMain, 16f, bold = true, bottom = 8))
        toggleBtn = button { toggle() }
        root.addView(toggleBtn)
        root.addView(button("Проверить: прислать сообщение") {
            val (text, key) = Phrases.make(this, "com.example", "Калькулятор", 1, prefs.recent())
            prefs.pushRecent(key)
            Notifier.sendMessage(this, text)
            refresh()
        })
        root.addView(label("", textDim, 4f, bottom = 12))

        root.addView(label("Что она писала", textMain, 16f, bold = true, bottom = 8))
        historyView = label("", textMain, 14f).apply {
            background = rounded(card, 16)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        root.addView(historyView)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(root)
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val usage = hasUsageAccess(this)
        val notif = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val pm = getSystemService(PowerManager::class.java)
        val battery = pm.isIgnoringBatteryOptimizations(packageName)

        style(usageBtn, (if (usage) "✓ " else "✗ ") + "Доступ к истории использования", usage)
        style(notifBtn, (if (notif) "✓ " else "✗ ") + "Уведомления", notif)
        style(batteryBtn, (if (battery) "✓ " else "✗ ") + "Работа в фоне без ограничений", battery)
        freqBtns.forEachIndexed { i, b -> style(b, b.text.toString(), prefs.frequency == i) }

        val on = prefs.enabled
        toggleBtn.text = if (on) "Включено — нажми, чтобы выключить" else "Выключено — нажми, чтобы включить"
        toggleBtn.background = rounded(if (on) accent else card, 24)
        toggleBtn.setTextColor(textMain)

        val h = prefs.history().takeLast(15).reversed()
        historyView.text = if (h.isEmpty()) "пока ничего. и не надейся" else h.joinToString("\n\n") { "— $it" }
    }

    private fun toggle() {
        if (!prefs.enabled) {
            if (!hasUsageAccess(this)) {
                Toast.makeText(this, "Сначала дай доступ к истории использования", Toast.LENGTH_LONG).show()
                openUsageSettings()
                return
            }
            prefs.enabled = true
            WatcherService.start(this)
        } else {
            prefs.enabled = false
            WatcherService.stop(this)
        }
        refresh()
    }

    private fun openUsageSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            openAppNotificationSettings()
        }
    }

    private fun openAppNotificationSettings() {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        try { startActivity(i) } catch (_: Exception) { }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            openAppNotificationSettings()
        }
        refresh()
    }

    @Suppress("BatteryLife")
    private fun askBattery() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) { }
        }
    }

    // ---------- маленькие помощники для интерфейса ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun label(t: String, color: Int, size: Float, bold: Boolean = false, bottom: Int = 0) =
        TextView(this).apply {
            text = t
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(bottom))
        }

    private fun button(t: String = "", onClick: () -> Unit) = Button(this).apply {
        text = t
        isAllCaps = false
        textSize = 15f
        gravity = Gravity.CENTER
        stateListAnimator = null
        setTextColor(textMain)
        background = rounded(card, 24)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) }
    }

    private fun style(b: Button, t: String, ok: Boolean) {
        b.text = t
        b.background = rounded(if (ok) Color.parseColor("#2A1520") else card, 24)
        b.setTextColor(if (ok) accent else textMain)
    }

    companion object {
        fun hasUsageAccess(ctx: Context): Boolean {
            val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
