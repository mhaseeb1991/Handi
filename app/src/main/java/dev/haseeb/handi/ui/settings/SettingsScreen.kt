package dev.haseeb.handi.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.haseeb.handi.core.designsystem.components.Kicker

/**
 * Entry point for app-level settings — reached from the icon on [dev.haseeb.handi.ui.home.HomeScreen]'s
 * header. Hosts Backup & Restore (its own screen, see [onBackup]) and a thin "About" section —
 * this is the home future entries (account/sign-in, sync status — see the project roadmap)
 * grow into, rather than each bolting its own icon onto Home.
 */
@Composable
fun SettingsScreen(
    onBackup: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SettingsTopBar(onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            // Hidden rather than shown-disabled: Remote Config's "backup_restore_enabled" is a
            // kill switch for a feature that might be pulled entirely, not a "coming soon".
            if (state.backupRestoreEnabled) {
                Kicker("Your data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable(onClick = onBackup),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(Icons.Rounded.SettingsBackupRestore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Export your recipes to a file, or restore from one",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Kicker(
                "About",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = if (state.backupRestoreEnabled) 32.dp else 0.dp),
            )
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Handi", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "A recipe notebook for your own kitchen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Version ${appVersionName()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

/** Matches EditorTopBar/BackupTopBar's shape rather than Material's generic TopAppBar. */
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
        }
        Column(Modifier.padding(start = 4.dp)) {
            Kicker("Your handi")
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun appVersionName(): String {
    val context = LocalContext.current
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "unknown"
}
