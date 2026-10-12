package com.berkahrejeki.pos.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppTheme(val label: String) {
    BIRU("Biru Profesional"),
    HIJAU("Hijau Islami"),
    UNGU("Ungu Elegan"),
    ORANGE("Oranye Ceria"),
    MERAH("Merah Berkah"),
    DARK("Mode Gelap")
}

object ThemeManager {
    private const val PREF = "theme_pref"
    private const val KEY = "app_theme"

    fun save(ctx: Context, theme: AppTheme) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY, theme.name).apply()
    }

    fun load(ctx: Context): AppTheme {
        val name = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, AppTheme.HIJAU.name) ?: AppTheme.HIJAU.name
        return runCatching { AppTheme.valueOf(name) }.getOrDefault(AppTheme.HIJAU)
    }
}

fun themeColors(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.BIRU -> lightColorScheme(
        primary = Color(0xFF1565C0), onPrimary = Color.White,
        secondary = Color(0xFF0288D1), tertiary = Color(0xFF00ACC1),
        background = Color(0xFFF5F9FF), surface = Color.White
    )
    AppTheme.HIJAU -> lightColorScheme(
        primary = Color(0xFF2E7D32), onPrimary = Color.White,
        secondary = Color(0xFF00897B), tertiary = Color(0xFF7CB342),
        background = Color(0xFFF3FBF4), surface = Color.White
    )
    AppTheme.UNGU -> lightColorScheme(
        primary = Color(0xFF6A1B9A), onPrimary = Color.White,
        secondary = Color(0xFF8E24AA), tertiary = Color(0xFFD81B60),
        background = Color(0xFFF9F4FF), surface = Color.White
    )
    AppTheme.ORANGE -> lightColorScheme(
        primary = Color(0xFFEF6C00), onPrimary = Color.White,
        secondary = Color(0xFFF57C00), tertiary = Color(0xFFFFA000),
        background = Color(0xFFFFF8F0), surface = Color.White
    )
    AppTheme.MERAH -> lightColorScheme(
        primary = Color(0xFFC62828), onPrimary = Color.White,
        secondary = Color(0xFFD32F2F), tertiary = Color(0xFFF4511E),
        background = Color(0xFFFFF5F5), surface = Color.White
    )
    AppTheme.DARK -> darkColorScheme(
        primary = Color(0xFF80CBC4), onPrimary = Color(0xFF00382F),
        secondary = Color(0xFF81D4FA), tertiary = Color(0xFFFFCC80)
    )
}
