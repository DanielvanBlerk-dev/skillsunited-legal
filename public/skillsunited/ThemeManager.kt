package com.dkvb.skillswap

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

object ThemeManager {

    private const val PREFS_NAME = "theme_prefs"
    private lateinit var prefs: SharedPreferences

    // Default colors
    const val DEFAULT_PRIMARY = "#001F5B"
    const val DEFAULT_SECONDARY = "#1976D2"
    const val DEFAULT_BACKGROUND = "#F5F5F5"
    const val DEFAULT_SURFACE = "#FFFFFF"
    const val DEFAULT_TEXT_PRIMARY = "#000000"
    const val DEFAULT_TEXT_SECONDARY = "#555555"
    const val DEFAULT_ACCENT = "#1976D2"
    const val DEFAULT_BUBBLE_SENT = "#001F5B"
    const val DEFAULT_BUBBLE_RECEIVED = "#E0E0E0"
    const val DEFAULT_CARD_BACKGROUND = "#FFFFFF"
    const val DEFAULT_BUTTON_TEXT = "#FFFFFF"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Getters
    fun getPrimary() = prefs.getString("primary", DEFAULT_PRIMARY)!!
    fun getSecondary() = prefs.getString("secondary", DEFAULT_SECONDARY)!!
    fun getBackground() = prefs.getString("background", DEFAULT_BACKGROUND)!!
    fun getSurface() = prefs.getString("surface", DEFAULT_SURFACE)!!
    fun getTextPrimary() = prefs.getString("text_primary", DEFAULT_TEXT_PRIMARY)!!
    fun getTextSecondary() = prefs.getString("text_secondary", DEFAULT_TEXT_SECONDARY)!!
    fun getAccent() = prefs.getString("accent", DEFAULT_ACCENT)!!
    fun getBubbleSent() = prefs.getString("bubble_sent", DEFAULT_BUBBLE_SENT)!!
    fun getBubbleReceived() = prefs.getString("bubble_received", DEFAULT_BUBBLE_RECEIVED)!!
    fun getCardBackground() = prefs.getString("card_background", DEFAULT_CARD_BACKGROUND)!!
    fun getButtonText() = prefs.getString("button_text", DEFAULT_BUTTON_TEXT)!!

    // Setters
    fun setPrimary(color: String) = prefs.edit().putString("primary", color).apply()
    fun setSecondary(color: String) = prefs.edit().putString("secondary", color).apply()
    fun setBackground(color: String) = prefs.edit().putString("background", color).apply()
    fun setSurface(color: String) = prefs.edit().putString("surface", color).apply()
    fun setTextPrimary(color: String) = prefs.edit().putString("text_primary", color).apply()
    fun setTextSecondary(color: String) = prefs.edit().putString("text_secondary", color).apply()
    fun setAccent(color: String) = prefs.edit().putString("accent", color).apply()
    fun setBubbleSent(color: String) = prefs.edit().putString("bubble_sent", color).apply()
    fun setBubbleReceived(color: String) = prefs.edit().putString("bubble_received", color).apply()
    fun setCardBackground(color: String) = prefs.edit().putString("card_background", color).apply()
    fun setButtonText(color: String) = prefs.edit().putString("button_text", color).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    fun parseColor(hex: String): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            Color.parseColor(DEFAULT_PRIMARY)
        }
    }

    // Preset themes
    data class Theme(val name: String, val primary: String, val secondary: String,
                     val background: String, val surface: String, val textPrimary: String,
                     val textSecondary: String, val accent: String, val bubbleSent: String,
                     val bubbleReceived: String, val cardBackground: String, val buttonText: String)

    val presets = listOf(
        Theme("Light", "#001F5B", "#1976D2", "#F5F5F5", "#FFFFFF",
            "#000000", "#555555", "#1976D2", "#001F5B", "#E0E0E0", "#FFFFFF", "#FFFFFF"),
        Theme("Dark", "#1976D2", "#42A5F5", "#121212", "#1E1E1E",
            "#FFFFFF", "#AAAAAA", "#42A5F5", "#1976D2", "#2C2C2C", "#1E1E1E", "#FFFFFF"),
        Theme("Forest", "#1B5E20", "#388E3C", "#F1F8E9", "#FFFFFF",
            "#000000", "#555555", "#388E3C", "#1B5E20", "#E0E0E0", "#FFFFFF", "#FFFFFF"),
        Theme("Sunset", "#E65100", "#FF6D00", "#FFF8F0", "#FFFFFF",
            "#000000", "#555555", "#FF6D00", "#E65100", "#FFE0CC", "#FFFFFF", "#FFFFFF")
    )

    fun applyPreset(theme: Theme) {
        setPrimary(theme.primary)
        setSecondary(theme.secondary)
        setBackground(theme.background)
        setSurface(theme.surface)
        setTextPrimary(theme.textPrimary)
        setTextSecondary(theme.textSecondary)
        setAccent(theme.accent)
        setBubbleSent(theme.bubbleSent)
        setBubbleReceived(theme.bubbleReceived)
        setCardBackground(theme.cardBackground)
        setButtonText(theme.buttonText)
    }

    fun hasAcceptedTerms(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("terms_accepted", false)
    }

    fun setTermsAccepted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("terms_accepted", true).apply()
    }

    fun getTermsAcceptedDate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong("terms_accepted_date", 0L)
        if (timestamp == 0L) return "Unknown"
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun setTermsAcceptedWithDate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("terms_accepted", true)
            .putLong("terms_accepted_date", System.currentTimeMillis())
            .apply()
    }

    fun clearTermsAccepted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("terms_accepted", false)
            .putLong("terms_accepted_date", 0L)
            .apply()
    }
}