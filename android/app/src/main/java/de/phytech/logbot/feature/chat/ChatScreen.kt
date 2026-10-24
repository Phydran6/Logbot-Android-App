/**
 * KI-Assistent: Fragen in Alltagssprache ("Ist im WLAN alles gut?"). Der Server
 * sieht selbst in den Logs nach und antwortet; unter jeder Antwort steht, wie
 * viele Logzeilen von welchen Geräten dafür an die KI gingen.
 *
 * Optional auf ein Gerät beschränkt – dann sieht die KI nur dessen Logs.
 */
package de.phytech.logbot.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.phytech.logbot.core.ui.UiState
import de.phytech.logbot.data.api.ChatResponseDto
import de.phytech.logbot.feature.common.ErrorState
import de.phytech.logbot.feature.common.LoadingState
import java.util.Locale

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    when (val s = viewModel.status) {
        UiState.Loading -> LoadingState()
        is UiState.Error -> ErrorState(s.message, onRetry = viewModel::retryStatus)
        is UiState.Success -> when {
            !s.data.available -> NotAvailable(
                "Die KI ist auf diesem Server nicht eingerichtet. Ein Administrator kann sie in der " +
                    "Weboberfläche unter System → KI-Auswertung anbinden – an Claude, ChatGPT, eine eigene " +
                    "KI über Open WebUI oder einen n8n-Workflow.",
            )
            !s.data.allowed -> NotAvailable(
                "Die KI ist auf diesem Server nur für Administratoren freigegeben.",
            )
            else -> Conversation(viewModel, s.data.providerLabel)
        }
    }
}

@Composable
private fun NotAvailable(text: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun Conversation(viewModel: ChatViewModel, providerLabel: String) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val count = viewModel.entries.size + if (viewModel.sending) 1 else 0
    LaunchedEffect(count) {
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    val send = {
        viewModel.send(input)
        input = ""
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScopeBar(viewModel)
        Box(Modifier.weight(1f)) {
            if (viewModel.entries.isEmpty() && !viewModel.sending) {
                Suggestions(viewModel.host, providerLabel) { viewModel.send(it) }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(viewModel.entries) { index, entry ->
                        Bubble(
                            entry = entry,
                            onRetry = if (entry.failed && index == viewModel.entries.lastIndex) viewModel::retry else null,
                        )
                    }
                    if (viewModel.sending) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    "Sehe in den Logs nach …",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Frag etwas zu deinen Geräten …") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = send, enabled = input.isNotBlank() && !viewModel.sending) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Senden")
            }
        }
    }
}

@Composable
private fun ScopeBar(viewModel: ChatViewModel) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            FilterChip(
                selected = viewModel.host.isNotBlank(),
                onClick = { open = true },
                leadingIcon = { Icon(Icons.Filled.Devices, contentDescription = null, modifier = Modifier.size(18.dp)) },
                label = { Text(viewModel.host.ifBlank { "Alle Geräte" }) },
            )
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text("Alle Geräte") }, onClick = { viewModel.selectHost(""); open = false })
                viewModel.knownHosts.forEach { h ->
                    DropdownMenuItem(text = { Text(h) }, onClick = { viewModel.selectHost(h); open = false })
                }
            }
        }
        Box(Modifier.weight(1f))
        if (viewModel.entries.isNotEmpty()) {
            TextButton(onClick = viewModel::newConversation, enabled = !viewModel.sending) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Neu", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(host: String, providerLabel: String, onAsk: (String) -> Unit) {
    val questions = if (host.isBlank()) {
        listOf(
            "Ist im WLAN alles gut?",
            "Gab es heute kritische Fehler?",
            "Welche Geräte sind offline?",
            "Läuft das Internet stabil?",
            "Gab es verdächtige Anmeldeversuche?",
            "Laufen die Backups?",
        )
    } else {
        listOf(
            "Ist mit $host alles in Ordnung?",
            "Was waren die letzten Fehler auf $host?",
            "Wurde $host neu gestartet?",
            "Gibt es Probleme mit Platte oder Speicher?",
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp))
        Text("Frag einfach", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Die KI sieht selbst in den Logs nach und antwortet mit Belegen. " +
                if (providerLabel.isNotBlank()) "Angebunden: $providerLabel." else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            questions.forEach { q -> AssistChip(onClick = { onAsk(q) }, label = { Text(q) }) }
        }
    }
}

@Composable
private fun Bubble(entry: ChatEntry, onRetry: (() -> Unit)?) {
    val clipboard = LocalClipboardManager.current
    val mine = entry.fromUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Surface(
            color = when {
                entry.failed -> MaterialTheme.colorScheme.errorContainer
                mine -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            SelectionContainer {
                Text(
                    text = if (mine || entry.failed) AnnotatedString(entry.text) else lightMarkdown(entry.text),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("Erneut versuchen") }
        }
        entry.meta?.let { meta ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = metaLine(meta),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f, fill = false),
                )
                IconButton(onClick = { clipboard.setText(AnnotatedString(entry.text)) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Antwort kopieren", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** Was für diese Antwort an die KI ging – Transparenz über die Daten. */
private fun metaLine(meta: ChatResponseDto): String = buildString {
    append(
        when (meta.logsSent) {
            0 -> "Keine Logzeilen gesendet"
            1 -> "1 Logzeile"
            else -> "${meta.logsSent} Logzeilen"
        },
    )
    if (meta.hosts.isNotEmpty()) {
        append(" von ")
        append(if (meta.hosts.size <= 3) meta.hosts.joinToString(", ") else "${meta.hosts.size} Geräten")
    }
    meta.providerLabel.takeIf { it.isNotBlank() }?.let { append(" · ").append(it.substringBefore(" —")) }
    if (meta.durationSeconds > 0) append(" · ").append(String.format(Locale.GERMANY, "%.1f s", meta.durationSeconds))
}

/**
 * Sehr kleines Markdown: **fett**, Überschriften (#) und Aufzählungen (- / *).
 * Mehr braucht eine Antwort am Handy nicht, und eine Bibliothek dafür lohnt nicht.
 */
private fun lightMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    text.lines().forEachIndexed { index, rawLine ->
        if (index > 0) append('\n')
        var line = rawLine
        val heading = line.trimStart().startsWith("#")
        if (heading) line = line.trimStart().trimStart('#').trimStart()
        if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
            line = "• " + line.trimStart().drop(2)
        }
        if (heading) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(line) }
            return@forEachIndexed
        }
        val parts = line.split("**")
        parts.forEachIndexed { i, part ->
            if (i % 2 == 1 && i < parts.lastIndex) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
            } else {
                if (i % 2 == 1) append("**")
                append(part)
            }
        }
    }
}
