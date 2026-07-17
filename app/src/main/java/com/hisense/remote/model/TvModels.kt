package com.hisense.remote.model

/**
 * TV connection state
 */
data class TvState(
    val connected: Boolean = false,
    val paired: Boolean = false,
    val host: String = "",
    val mac: String = "",
    val name: String = "Not Connected",
    val power: String = "unknown",
    val volume: Int = 0,
    val muted: Boolean = false,
    val source: String = "",
    val channel: String = "",
    val pairingCode: String = "",
    val errorMessage: String = "",
)

/**
 * Discovered TV on the network
 */
data class DiscoveredTv(
    val ip: String,
    val name: String = "",
    val port: Int = 36669,
)

/**
 * Key codes for the Hisense Vidaa TV remote protocol
 */
object KeyCodes {
    val keys = mapOf(
        "power" to "KEY_POWER",
        "home" to "KEY_HOME",
        "back" to "KEY_BACK",
        "exit" to "KEY_EXIT",
        "menu" to "KEY_MENU",
        "settings" to "KEY_SETTINGS",
        "info" to "KEY_INFO",
        "source" to "KEY_SOURCE",
        "up" to "KEY_UP",
        "down" to "KEY_DOWN",
        "left" to "KEY_LEFT",
        "right" to "KEY_RIGHT",
        "ok" to "KEY_OK",
        "enter" to "KEY_ENTER",
        "volume_up" to "KEY_VOLUMEUP",
        "volume_down" to "KEY_VOLUMEDOWN",
        "mute" to "KEY_MUTE",
        "channel_up" to "KEY_CHANNELUP",
        "channel_down" to "KEY_CHANNELDOWN",
        "0" to "KEY_0", "1" to "KEY_1", "2" to "KEY_2",
        "3" to "KEY_3", "4" to "KEY_4", "5" to "KEY_5",
        "6" to "KEY_6", "7" to "KEY_7", "8" to "KEY_8", "9" to "KEY_9",
        "play" to "KEY_PLAY",
        "pause" to "KEY_PAUSE",
        "stop" to "KEY_STOP",
        "rewind" to "KEY_REWIND",
        "fastforward" to "KEY_FF",
        "record" to "KEY_RECORD",
        "next" to "KEY_NEXT",
        "previous" to "KEY_PREVIOUS",
        "netflix" to "KEY_NETFLIX",
        "youtube" to "KEY_YOUTUBE",
        "prime" to "KEY_AMAZON",
        "disney" to "KEY_DISNEY",
        "text" to "KEY_TEXT",
        "red" to "KEY_RED",
        "green" to "KEY_GREEN",
        "yellow" to "KEY_YELLOW",
        "blue" to "KEY_BLUE",
        "delete" to "KEY_DELETE",
        "backspace" to "KEY_BACKSPACE",
        "space" to "KEY_SPACE",
        "clear" to "KEY_CLEAR",
        "input" to "KEY_INPUT",
        "page_up" to "KEY_PAGEUP",
        "page_down" to "KEY_PAGEDOWN",
        "guide" to "KEY_GUIDE",
        "search" to "KEY_SEARCH",
    )

    fun get(key: String): String = keys[key] ?: key
}

/**
 * Character to key code mapping for text input
 */
object CharToKey {
    val map = buildMap {
        // Lowercase letters
        for (c in 'a'..'z') put(c.toString(), "KEY_${c.uppercase()}")
        // Uppercase letters (same key - TV handles shift)
        for (c in 'A'..'Z') put(c.toString(), "KEY_${c}")
        // Numbers
        for (c in '0'..'9') put(c.toString(), "KEY_$c")
        // Symbols
        put(" ", "KEY_SPACE")
        put(".", "KEY_DOT")
        put(",", "KEY_COMMA")
        put("-", "KEY_MINUS")
        put("_", "KEY_MINUS")
        put("=", "KEY_EQUALS")
        put("+", "KEY_EQUALS")
        put("/", "KEY_SLASH")
        put("?", "KEY_SLASH")
        put("\\", "KEY_BACKSLASH")
        put("|", "KEY_BACKSLASH")
        put(";", "KEY_SEMICOLON")
        put(":", "KEY_SEMICOLON")
        put("'", "KEY_APOSTROPHE")
        put("\"", "KEY_APOSTROPHE")
        put("`", "KEY_GRAVE")
        put("~", "KEY_GRAVE")
        put("!", "KEY_EXCLAMATION")
        put("@", "KEY_AT")
        put("#", "KEY_HASH")
        put("\$", "KEY_DOLLAR")
        put("%", "KEY_PERCENT")
        put("^", "KEY_CARET")
        put("&", "KEY_AMPERSAND")
        put("*", "KEY_ASTERISK")
        put("(", "KEY_LEFT_PAREN")
        put(")", "KEY_RIGHT_PAREN")
        put("[", "KEY_LEFT_BRACKET")
        put("]", "KEY_RIGHT_BRACKET")
        put("\n", "KEY_ENTER")
        put("\t", "KEY_TAB")
    }

    fun get(char: String): String? = map[char]
}
