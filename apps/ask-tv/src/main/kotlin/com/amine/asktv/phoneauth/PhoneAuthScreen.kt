package com.amine.asktv.phoneauth

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val PhoneBackground = Color(0xFF101820)
private val PhoneSurface = Color(0xFF1B2934)
private val PhoneText = Color(0xFFF3F6F8)
private val PhoneSecondary = Color(0xFFC1CFD9)
private val PhoneAccent = Color(0xFFB9E8DD)

@Composable
internal fun PhoneAuthScreen(
    ui: PhoneAuthUi,
    hasPairingLink: Boolean,
    paste: String,
    message: String?,
    inProgress: Boolean,
    onPaste: (String) -> Unit,
    onUsePaste: () -> Unit,
    onPhotograph: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(ui.deadlineElapsedMs) {
        while (ui.deadlineElapsedMs != null) { now = SystemClock.elapsedRealtime(); delay(1_000) }
    }
    MaterialTheme(colorScheme = darkColorScheme(
        primary = PhoneAccent, onPrimary = PhoneBackground,
        background = PhoneBackground, onBackground = PhoneText,
        surface = PhoneSurface, onSurface = PhoneText,
        onSurfaceVariant = PhoneSecondary, outline = PhoneSecondary,
    )) {
        Box(Modifier.fillMaxSize().background(PhoneBackground).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("AI CHAT · PHONE HELPER", color = PhoneAccent, style = MaterialTheme.typography.labelLarge)
                Text("Sign in on your TV", color = PhoneText, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text("This helper opens ChatGPT in your phone browser and sends authorization to AI Chat on your TV.", color = PhoneSecondary, style = MaterialTheme.typography.bodyLarge)
                if (inProgress) {
                    HorizontalDivider(color = PhoneSecondary.copy(alpha = 0.3f))
                    Text(ui.message, color = PhoneText, style = MaterialTheme.typography.titleMedium)
                    ui.deadlineElapsedMs?.let { deadline ->
                        val seconds = ((deadline - now).coerceAtLeast(0) + 999) / 1_000
                        Text("Time left: ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}", color = PhoneSecondary)
                    }
                } else {
                    if (ui.phase == PhoneAuthPhase.FINISHED) Text(ui.message, color = PhoneText, style = MaterialTheme.typography.titleMedium)
                    Text("Install AI Chat on this phone once. Keep your phone and TV on the same Wi-Fi network.", color = PhoneSecondary)
                    Text("On your TV, choose Continue with ChatGPT, then Use Android phone.", color = PhoneSecondary)
                    Text(if (hasPairingLink) "TV code ready" else "Get the code from your TV", color = PhoneText, style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = onPhotograph, modifier = Modifier.fillMaxWidth()) { Text("Photograph TV code", color = PhoneAccent) }
                    if (!hasPairingLink) {
                        OutlinedTextField(
                            value = paste,
                            onValueChange = onPaste,
                            label = { Text("Paste pairing link") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = PhoneText, unfocusedTextColor = PhoneText,
                                focusedBorderColor = PhoneAccent, unfocusedBorderColor = PhoneSecondary,
                                focusedLabelColor = PhoneAccent, unfocusedLabelColor = PhoneSecondary,
                            ),
                        )
                        OutlinedButton(onClick = onUsePaste, enabled = paste.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Use pasted link") }
                    }
                    Button(
                        onClick = onContinue,
                        enabled = hasPairingLink,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent, contentColor = PhoneBackground),
                    ) { Text("Continue with ChatGPT") }
                }
                message?.let { Text(it, color = PhoneText, style = MaterialTheme.typography.bodyLarge) }
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = PhoneAccent) }
                Spacer(Modifier.height(8.dp))
                Text("Your account stays on your TV. This helper does not save your sign-in details.", color = PhoneSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
