package com.m3u.smartphone.ui.common.internal

import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.m3u.smartphone.ui.material.components.FontFamilies
import com.m3u.smartphone.ui.material.components.withFontFamily
import com.m3u.smartphone.ui.material.model.LocalSpacing
import com.m3u.smartphone.ui.material.model.Spacing
import com.m3u.smartphone.ui.material.model.Theme

@Composable
internal fun PreviewTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    Theme(
        argb = 0xFF3C8C68.toInt(),
        useDynamicColors = false,
        useDarkTheme = useDarkTheme,
        typography = Typography().withFontFamily(FontFamilies.GoogleSans)
    ) {
        CompositionLocalProvider(LocalSpacing provides Spacing.REGULAR) {
            Surface(content = content)
        }
    }
}
