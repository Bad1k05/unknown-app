package com.unknown.qqqqq

import android.content.Context
import android.content.pm.ApplicationInfo
import java.util.Calendar
import kotlin.random.Random

/**
 * Генератор её холодных сообщений.
 * Определяет, что за приложение открыто, и собирает фразу из кусочков:
 * [начало] + основная фраза под приложение + [концовка],
 * плюс особые реплики ночью и когда приложение открывается слишком часто.
 */
object Phrases {

    enum class Cat {
        CALC, GALLERY, CAMERA, MESSENGER, SOCIAL, VIDEO, MUSIC, BROWSER, MAPS, FOOD,
        BANK, SHOP, GAME, DATING, PHONE, MAIL, NOTES, TIME, SETTINGS, WEATHER, OTHER
    }

    // Порядок важен: первое совпадение побеждает.
    private val rules: List<Pair<Cat, List<String>>> = listOf(
        Cat.DATING to listOf("tinder", "badoo", "mamba", "bumble", "hinge", "pure", "twinby", "teamo", "dating", "знакомств"),
        Cat.CALC to listOf("calculator", "calc", "калькулятор"),
        Cat.CAMERA to listOf("camera", "камера"),
        Cat.GALLERY to listOf("gallery", "photos", "album", "галерея", "фото"),
        Cat.MESSENGER to listOf("telegram", "whatsapp", "viber", "discord", "signal", "messag", "messeng", "mms", "sms", "ru.oneme", "skype", "сообщени", "мессендж"),
        Cat.SOCIAL to listOf("instagram", "musically", "tiktok", "vkontakte", "com.vk.", "twitter", "threads", "pinterest", "snapchat", "facebook", "reddit", "ok.android", "likee", "yappy"),
        Cat.VIDEO to listOf("youtube", "twitch", "netflix", "kinopoisk", "ru.ivi", "okko", "wink", "rutube", "kion", "video", "кино"),
        Cat.MUSIC to listOf("music", "spotify", "soundcloud", "zvuk", "deezer", "shazam", "музык"),
        Cat.BROWSER to listOf("chrome", "firefox", "opera", "browser", "brave", "duckduckgo", "sbrowser", "emmx", "браузер"),
        Cat.MAPS to listOf("maps", "navi", "2gis", "dublgis", "taxi", "uber", "yango", "bolt", "metro", "карты", "такси"),
        Cat.FOOD to listOf(".eda", "food", "delivery", "samokat", "dodo", "kfc", "mcdonalds", "vkusno", "vkusvill", "burger", "pizza", "lavka", "glovo", "wolt", "доставк", "еда"),
        Cat.BANK to listOf("bank", "sber", "tinkoff", "tbank", "alfa", "vtb", "raiffeisen", "gazprom", "pochta", "qiwi", "pay", "банк", "сбер"),
        Cat.SHOP to listOf("ozon", "wildberries", "aliexpress", "avito", "market", "lamoda", "shop", "temu", "магазин"),
        Cat.PHONE to listOf("dialer", "contacts", "telecom", "phone", "контакты", "телефон"),
        Cat.MAIL to listOf("mail", "gmail", "outlook", "почта"),
        Cat.NOTES to listOf("notes", "keep", "notion", "obsidian", "memo", "заметк"),
        Cat.TIME to listOf("clock", "alarm", "calendar", "deskclock", "часы", "будильник", "календар"),
        Cat.SETTINGS to listOf("settings", "настройки"),
        Cat.WEATHER to listOf("weather", "погода"),
        Cat.GAME to listOf("game", "steam", "brawl", "pubg", "standoff", "genshin", "roblox", "minecraft", "clash", "supercell", "игр"),
    )

    private val core: Map<Cat, List<String>> = mapOf(
        Cat.CALC to listOf(
            "чего открыл, сам что-ли посчитать не можешь",
            "считаешь, сколько раз я тебе ответила? ноль",
            "калькулятор не посчитает твои шансы. их нет",
            "2+2 осилишь? ну вот, умничка",
            "посчитай, сколько дней я тебе не пишу. и дальше считай",
            "что делишь? меня с тобой не дели",
        ),
        Cat.GALLERY to listOf(
            "хочешь посмотреть на мои фото, просто забудь уже",
            "опять мои фотки листаешь?",
            "удали уже наши фото",
            "там нет ничего для тебя",
            "фотки не вернут то, чего не было",
            "листаешь прошлое? оно тебя не листает",
        ),
        Cat.CAMERA to listOf(
            "селфи? мне не отправляй",
            "убери камеру, я не впечатлюсь",
            "фоткаешь что-то для меня? не надо",
            "сфоткай себя и посмотри, кому ты пишешь по ночам",
        ),
        Cat.MESSENGER to listOf(
            "зачем, ты проверяешь, я не напишу",
            "не пиши мне первым",
            "я видела, что ты онлайн. и что?",
            "проверяешь, была ли я в сети? была. не для тебя",
            "не жди, не напишу",
            "можешь не открывать {app}, от меня там пусто",
            "читаю всех, кроме тебя",
            "печатаешь? стирай",
        ),
        Cat.SOCIAL to listOf(
            "опять мои сторис смотришь?",
            "лайкнешь — заблокирую",
            "листай дальше, меня там нет",
            "я вижу тебя в просмотрах. прекрати",
            "моя страница не для тебя",
            "мои подписки тебя не касаются",
        ),
        Cat.VIDEO to listOf(
            "смотри, смотри. вместе всё равно не посмотрим",
            "включи что-нибудь про то, как отпускать людей",
            "опять залип. как всегда",
            "видео мне не скидывай, я не посмотрю",
            "сериал без меня досмотришь",
        ),
        Cat.MUSIC to listOf(
            "только не грустные песни про меня",
            "ту песню не включай. она ничего не значила",
            "наушники надел и страдаешь? ок",
            "музыка тебе не поможет",
            "плейлист «она» удали",
        ),
        Cat.BROWSER to listOf(
            "загугли «как перестать ей писать»",
            "что ищешь? меня там нет",
            "историю браузера почисти, позорище",
            "мой профиль не ищи",
        ),
        Cat.MAPS to listOf(
            "даже не думай ко мне ехать",
            "маршрут ко мне не построится",
            "куда собрался? надеюсь, не ко мне",
            "я не дома. и не буду",
        ),
        Cat.FOOD to listOf(
            "опять ешь один?",
            "на двоих не заказывай. я не приду",
            "закажи себе, мне не надо",
            "еда лучше меня? наверное",
        ),
        Cat.BANK to listOf(
            "подарки мне не покупай",
            "деньги на меня не трать, не возьму",
            "переводом ничего не исправишь",
            "баланс как наши отношения. на нуле",
        ),
        Cat.SHOP to listOf(
            "мне ничего не надо",
            "цветы не заказывай",
            "очередной подарок, который я не приму?",
            "в корзину меня не добавишь",
        ),
        Cat.GAME to listOf(
            "иди поиграй, у тебя хорошо получается быть одному",
            "опять игры. я не удивлена",
            "в игре хотя бы тебя кто-то ждёт",
            "проиграешь — мне не жалуйся",
        ),
        Cat.DATING to listOf(
            "о, ищешь замену? удачи",
            "моё фото туда не ставь",
            "свайпай, свайпай",
            "ей тоже не пиши по ночам",
        ),
        Cat.PHONE to listOf(
            "не звони",
            "мой номер удали уже",
            "даже не думай набирать",
            "я не возьму трубку",
        ),
        Cat.MAIL to listOf(
            "письма я тоже не читаю",
            "даже по почте мне не пиши",
        ),
        Cat.NOTES to listOf(
            "только стихи мне не пиши",
            "дневник про меня ведёшь? мило. нет",
            "запиши себе: она не напишет",
        ),
        Cat.TIME to listOf(
            "время не лечит, но ты попробуй",
            "нашу дату из календаря удали",
            "смотришь на часы? я не опаздываю. я просто не приду",
        ),
        Cat.SETTINGS to listOf(
            "настройки не помогут настроить нас",
            "поставь меня в беззвучный и забудь",
            "сбрось до заводских. и меня из головы",
        ),
        Cat.WEATHER to listOf(
            "у меня солнечно. без тебя",
            "холодно? мне с тобой так же",
            "зонт возьми. а я не возьму трубку",
        ),
        Cat.OTHER to listOf(
            "чего открыл {app}? думал, я там?",
            "я вижу всё, что ты открываешь. скучно",
            "опять ты",
            "занят? я тоже",
            "делай что хочешь, мне всё равно",
            "открыл {app} и думаешь обо мне. не надо",
            "я не отвечу. просто чтобы ты знал",
            "{app}? серьёзно?",
        ),
    )

    private val openers = listOf("ну и?", "опять ты.", "мм.", "ага.", "серьёзно?", "слушай.", "ой.", "так.")
    private val tails = listOf("не пиши.", "ок.", ")", "...", "мне всё равно", "сам разберёшься", "и не отвечай", "всё.", "пока")

    private val night = listOf(
        "{time} ночи, а ты в {app}. спи уже",
        "ночью мне тоже не пиши",
        "не спится? мне спится. без тебя",
    )
    private val morning = listOf(
        "с утра и сразу {app}? хоть не мне пишешь",
        "доброе утро. не тебе",
    )
    private val often = listOf(
        "{n}-й раз за день открываешь {app}. я не впечатлена",
        "ты {app} уже {n} раз открыл. может, хватит?",
        "{n} раз за сегодня. я считаю, если что",
    )

    fun categorize(context: Context, pkg: String, label: String): Cat {
        val hay = (pkg + " " + label).lowercase()
        for ((cat, keys) in rules) if (keys.any { hay.contains(it) }) return cat
        try {
            val ai = context.packageManager.getApplicationInfo(pkg, 0)
            if (ai.category == ApplicationInfo.CATEGORY_GAME) return Cat.GAME
        } catch (_: Exception) { }
        return Cat.OTHER
    }

    /** Возвращает пару: готовый текст и «ключ» основной фразы для антиповтора. */
    fun make(context: Context, pkg: String, label: String, opensToday: Int, recent: List<String>): Pair<String, String> {
        val cat = categorize(context, pkg, label)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val pool = mutableListOf<String>()
        pool += core[cat].orEmpty()
        pool += core[cat].orEmpty() // профильные фразы в приоритете
        if (cat != Cat.OTHER) pool += core[Cat.OTHER].orEmpty().take(4)
        if (hour in 0..4) pool += night + night
        if (hour in 6..9) pool += morning
        if (opensToday >= 4) pool += often + often

        val fresh = pool.filter { it !in recent }.ifEmpty { pool }
        val key = fresh[Random.nextInt(fresh.size)]

        var text = key
            .replace("{app}", label)
            .replace("{n}", opensToday.toString())
            .replace("{time}", hour.toString())

        val r = Random.nextInt(100)
        if (r < 20) text = openers.random() + " " + text
        else if (r < 50) text = text + ". " + tails.random()

        // иногда без точки и с маленькой буквы — как будто пишет на отвали
        text = text.trim()
        if (Random.nextBoolean()) text = text.trimEnd('.')
        return text to key
    }
}
