package com.unknown.qqqqq

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Настройки и небольшая «память» приложения. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("qqqqq", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    /** 0 — редко, 1 — иногда, 2 — всегда. */
    var frequency: Int
        get() = sp.getInt("frequency", 1)
        set(v) = sp.edit().putInt("frequency", v).apply()

    /** Последние фразы, чтобы не повторяться. */
    fun recent(): List<String> =
        sp.getString("recent", "")!!.split('\n').filter { it.isNotBlank() }

    fun pushRecent(core: String) {
        val list = (recent() + core).takeLast(25)
        sp.edit().putString("recent", list.joinToString("\n")).apply()
    }

    /** История её сообщений для экрана приложения. */
    fun history(): List<String> =
        sp.getString("history", "")!!.split('\n').filter { it.isNotBlank() }

    fun pushHistory(line: String) {
        val list = (history() + line.replace('\n', ' ')).takeLast(40)
        sp.edit().putString("history", list.joinToString("\n")).apply()
    }

    /** Сколько раз сегодня открывалось приложение pkg (включая этот раз). */
    fun bumpOpenCount(pkg: String): Int {
        val day = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        if (sp.getString("count_day", "") != day) {
            val e = sp.edit()
            sp.all.keys.filter { it.startsWith("cnt_") }.forEach { e.remove(it) }
            e.putString("count_day", day).apply()
        }
        val n = sp.getInt("cnt_$pkg", 0) + 1
        sp.edit().putInt("cnt_$pkg", n).apply()
        return n
    }
}
