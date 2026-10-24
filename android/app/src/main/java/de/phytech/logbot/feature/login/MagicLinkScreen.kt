/**
 * Anmeldung per E-Mail-Link: Adresse eingeben, Link in der Mail antippen – die
 * App öffnet sich und meldet an. Der Link gehört aufs Handy, das ihn angefordert
 * hat; nur dort liegt das Geheimnis, mit dem er sich einlösen lässt.
 */
package de.phytech.logbot.feature.login

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.navigation.LinkInbox
import kotlinx.coroutines.delay

@Composable
fun MagicLinkScreen(
    linkInbox: LinkInbox,
    onAuthenticated: () -> Unit,
    onMfaRequired: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MagicLinkViewModel = hiltViewModel(),
) {
    val incoming by linkInbox.magicLink.collectAsState()
    LaunchedEffect(incoming) {
        linkInbox.consumeMagicLink()?.let(viewModel::redeem)
    }
    when (val s = viewModel.step) {
        MagicLinkStep.Done -> LaunchedEffect(Unit) { onAuthenticated() }
        is MagicLinkStep.Mfa -> LaunchedEffect(s.token) { onMfaRequired(s.token) }
        else -> Unit
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.MarkEmailUnread,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.height(12.dp))
            when (val s = viewModel.step) {
                MagicLinkStep.Enter -> EnterEmail(viewModel, onBack)
                is MagicLinkStep.Waiting -> Waiting(viewModel, s)
                else -> {
                    Text("Anmeldung läuft …", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
            viewModel.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun EnterEmail(viewModel: MagicLinkViewModel, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf(viewModel.suggestedEmail) }
    Text("Anmelde-Link anfordern", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Gib die E-Mail-Adresse deines Kontos ein. Du bekommst einen Link, mit dem du dich " +
            "in der App anmeldest – ohne Passwort.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("E-Mail-Adresse") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { viewModel.request(email) }),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { viewModel.request(email) },
        enabled = !viewModel.loading,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Link schicken") }
    if (viewModel.loading) {
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
    }
    TextButton(onClick = onBack, enabled = !viewModel.loading) { Text("Zurück") }
}

@Composable
private fun Waiting(viewModel: MagicLinkViewModel, step: MagicLinkStep.Waiting) {
    val context = LocalContext.current
    var now by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(step) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remaining = ((step.pending.expiresAtMillis - now) / 1000).coerceAtLeast(0)
    LaunchedEffect(remaining == 0L) {
        if (remaining == 0L) viewModel.expired()
    }

    Text("Schau in dein Postfach", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Falls es zu ${step.pending.email} ein Konto gibt, ist ein Anmelde-Link unterwegs. " +
            "Tippe ihn auf diesem Handy an – die App öffnet sich und meldet dich an.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
    )
    Button(
        onClick = {
            val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                // Keine Mail-App als Standard eingerichtet – dann eben von Hand.
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Mail-App öffnen") }

    Spacer(Modifier.height(20.dp))
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text(
        text = "Der Link ist noch ${remaining / 60}:${"%02d".format(remaining % 60)} min gültig",
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = 8.dp),
    )

    Spacer(Modifier.height(12.dp))
    val canResend = now >= viewModel.resendAtMillis
    OutlinedButton(
        onClick = { viewModel.request(step.pending.email) },
        enabled = canResend && !viewModel.loading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            if (canResend) "Neuen Link schicken"
            else "Neuen Link schicken (in ${(viewModel.resendAtMillis - now) / 1000} s)",
        )
    }
    TextButton(onClick = viewModel::cancel) { Text("Abbrechen") }
}
