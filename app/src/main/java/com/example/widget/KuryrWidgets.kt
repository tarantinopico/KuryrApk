package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize as glanceFillMaxSize
import androidx.glance.layout.fillMaxWidth as glanceFillMaxWidth
import androidx.glance.layout.padding as glancePadding
import androidx.glance.text.FontWeight as GlanceFontWeight
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextStyle as GlanceTextStyle
import com.example.MainActivity
import com.example.data.local.AccountEntity
import com.example.data.local.EmailMessageEntity
import com.example.data.local.FolderEntity
import com.example.data.local.KuryrDatabase
import com.example.data.model.FolderType
import com.example.ui.theme.KuryrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pomocný objekt pro hromadnou aktualizaci obou Jetpack Glance widgetů.
 */
object KuryrWidgetUpdater {
    suspend fun updateAllWidgets(context: Context) {
        runCatching {
            UnifiedInboxGlanceWidget().updateAll(context)
            AccountsUnreadGlanceWidget().updateAll(context)
        }
    }
}

/**
 * Widget (A): Unified Inbox postavený na Jetpack Glance.
 * Zobrazuje nejnovější nepřečtené a doručené zprávy ze všech účtů.
 */
class UnifiedInboxGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = KuryrDatabase.getInstance(context)
        val dao = db.kuryrDao()
        val messages: List<EmailMessageEntity> = withContext(Dispatchers.IO) {
            runCatching { dao.observeAllMessages().first().take(5) }.getOrDefault(emptyList())
        }
        val unreadTotal = messages.count { !it.isRead }

        provideContent {
            GlanceTheme {
                androidx.glance.layout.Column(
                    modifier = GlanceModifier
                        .glanceFillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .glancePadding(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    androidx.glance.layout.Row(
                        modifier = GlanceModifier.glanceFillMaxWidth()
                    ) {
                        GlanceText(
                            text = "Kurýr • Sjednocená pošta",
                            style = GlanceTextStyle(
                                color = GlanceTheme.colors.primary,
                                fontWeight = GlanceFontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        GlanceText(
                            text = "  ($unreadTotal nepřečtených)",
                            style = GlanceTextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }

                    if (messages.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.glanceFillMaxSize().glancePadding(top = 12.dp)
                        ) {
                            GlanceText(
                                text = "Žádné zprávy v lokální mezipaměti. Klepnutím otevřete Kurýr.",
                                style = GlanceTextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    } else {
                        messages.forEach { msg ->
                            androidx.glance.layout.Column(
                                modifier = GlanceModifier
                                    .glanceFillMaxWidth()
                                    .glancePadding(vertical = 4.dp)
                            ) {
                                GlanceText(
                                    text = "${if (!msg.isRead) "● " else ""}${msg.fromName.ifBlank { msg.fromEmail }}",
                                    style = GlanceTextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontWeight = if (!msg.isRead) GlanceFontWeight.Bold else GlanceFontWeight.Normal,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 1
                                )
                                GlanceText(
                                    text = msg.subject,
                                    style = GlanceTextStyle(
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class UnifiedInboxWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UnifiedInboxGlanceWidget()
}

/**
 * Widget (B): Přehled účtů s počty nepřečtených zpráv postavený na Jetpack Glance.
 */
class AccountsUnreadGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = KuryrDatabase.getInstance(context)
        val dao = db.kuryrDao()
        val accounts: List<AccountEntity> = withContext(Dispatchers.IO) {
            runCatching { dao.getAllAccountsOnce() }.getOrDefault(emptyList())
        }
        val folders: List<FolderEntity> = withContext(Dispatchers.IO) {
            runCatching { dao.observeAllFolders().first() }.getOrDefault(emptyList())
        }

        provideContent {
            GlanceTheme {
                androidx.glance.layout.Column(
                    modifier = GlanceModifier
                        .glanceFillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .glancePadding(12.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    GlanceText(
                        text = "Kurýr • Přehled účtů",
                        style = GlanceTextStyle(
                            color = GlanceTheme.colors.primary,
                            fontWeight = GlanceFontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )

                    if (accounts.isEmpty()) {
                        GlanceText(
                            text = "Žádný účet není nastaven. Klepněte pro přidání účtu.",
                            style = GlanceTextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 13.sp
                            ),
                            modifier = GlanceModifier.glancePadding(top = 8.dp)
                        )
                    } else {
                        accounts.forEach { account ->
                            val unread = folders
                                .filter { it.accountId == account.id && it.type == FolderType.INBOX }
                                .sumOf { it.unreadCount }

                            androidx.glance.layout.Row(
                                modifier = GlanceModifier
                                    .glanceFillMaxWidth()
                                    .glancePadding(vertical = 4.dp)
                            ) {
                                GlanceText(
                                    text = "${account.accountName} (${account.protocol.name})",
                                    style = GlanceTextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        fontWeight = GlanceFontWeight.Medium,
                                        fontSize = 13.sp
                                    ),
                                    maxLines = 1
                                )
                                GlanceText(
                                    text = "  • $unread nepřečtených",
                                    style = GlanceTextStyle(
                                        color = GlanceTheme.colors.primary,
                                        fontWeight = GlanceFontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class AccountsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AccountsUnreadGlanceWidget()
}

/**
 * Konfigurační aktivita pro domovské widgety aplikace Kurýr.
 */
class WidgetConfigurationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        setContent {
            KuryrTheme {
                var onlyUnread by remember { mutableStateOf(false) }
                var showProtocolBadge by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()

                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Nastavení widgetu Kurýr",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zvolte způsob zobrazení přehledu doručené pošty a účtů na domovské obrazovce.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Zobrazit primárně nepřečtené zprávy")
                                    Switch(
                                        checked = onlyUnread,
                                        onCheckedChange = { onlyUnread = it },
                                        modifier = Modifier.testTag("widget_only_unread_switch")
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Zobrazit štítek protokolu (IMAP/JMAP/POP3)")
                                    Switch(
                                        checked = showProtocolBadge,
                                        onCheckedChange = { showProtocolBadge = it },
                                        modifier = Modifier.testTag("widget_protocol_badge_switch")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    KuryrWidgetUpdater.updateAllWidgets(applicationContext)
                                    val resultValue = Intent().putExtra(
                                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                                        appWidgetId
                                    )
                                    setResult(RESULT_OK, resultValue)
                                    finish()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_widget_config_button")
                        ) {
                            Text("Uložit a přidat widget")
                        }
                    }
                }
            }
        }
    }
}
