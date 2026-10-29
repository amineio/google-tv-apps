package com.amine.battleship.localization

import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.amine.battleship.R
import java.util.Locale

private val LocalGameResources = staticCompositionLocalOf<Resources?> { null }

/** Scopes language changes to this composition without restarting the Activity or match. */
@Composable
fun ProvideGameLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val currentConfiguration = LocalConfiguration.current
    val resources = remember(context, language, currentConfiguration) {
        val configuration = Configuration(currentConfiguration)
        configuration.setLocale(Locale.forLanguageTag(language.tag))
        context.createConfigurationContext(configuration).resources
    }
    CompositionLocalProvider(LocalGameResources provides resources, content = content)
}

@Composable
fun gameText(@StringRes id: Int, vararg args: Any): String {
    val resources = LocalGameResources.current ?: LocalResources.current
    return if (args.isEmpty()) resources.getString(id) else resources.getString(id, *args)
}

/** Legacy save warnings remain English identifiers in session state. */
@Composable
fun localizedSaveWarning(message: String): String = when (message) {
    "The saved match did not open. Start a new game." -> gameText(R.string.save_recovered)
    "The match cannot be saved. Free some storage and try again." -> gameText(R.string.save_failed)
    else -> message
}
