@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.optimizer.tools.feature.tvlauncher

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.optimizer.tools.R
import com.optimizer.tools.app.tv.LocalTelevisionUi
import com.optimizer.tools.core.ui.resolve

@Composable
private fun InstallLauncherCard(
    state: TvLauncherState,
    onPick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.tv_launcher_install_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.tv_launcher_install_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onPick, enabled = !state.installing) {
                    Text(stringResource(R.string.tv_launcher_install_pick))
                }
                if (state.installing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            state.installProgress?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            state.installMessage?.let {
                Text(
                    it.resolve(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
internal fun TvLauncherScreen(
    state: TvLauncherState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (String) -> Unit,
    onConfirmChange: () -> Unit,
    onDismissChange: () -> Unit,
    onInstall: (Uri) -> Unit,
) {
    val apkPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) onInstall(uri)
        }
    val selectedLauncher = state.launchers.firstOrNull { it.packageName == state.pending }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!LocalTelevisionUi.current) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            }
            Text(stringResource(R.string.tv_launcher_title), style = MaterialTheme.typography.titleLarge)
        }
        Text(
            stringResource(R.string.tv_launcher_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InstallLauncherCard(
            state = state,
            onPick = { apkPicker.launch(arrayOf("application/vnd.android.package-archive")) },
        )
        when {
            state.loading -> {
                Box(
                    Modifier.fillMaxWidth().padding(28.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }

            state.error != null -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(state.error.resolve())
                        OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.action_retry)) }
                    }
                }
            }

            state.launchers.isEmpty() -> {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.tv_launcher_empty))
                        OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.action_refresh)) }
                    }
                }
            }

            else -> {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.launchers, key = { it.packageName }) { launcher ->
                        val isCurrent = launcher.packageName == state.current?.packageName
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = state.updating == null) {
                                        onSelect(launcher.packageName)
                                    }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(launcher.packageName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(
                                            if (isCurrent) {
                                                R.string.tv_launcher_in_use
                                            } else {
                                                R.string.tv_launcher_available
                                            },
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color =
                                            if (isCurrent) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                }
                                if (state.updating == launcher.packageName) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    TextButton(
                                        onClick = { onSelect(launcher.packageName) },
                                        enabled = !isCurrent && state.updating == null,
                                    ) {
                                        Text(
                                            stringResource(
                                                if (isCurrent) {
                                                    R.string.tv_launcher_current
                                                } else {
                                                    R.string.tv_launcher_use
                                                },
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onRefresh, enabled = !state.loading && state.updating == null) {
            Text(stringResource(R.string.tv_launcher_refresh_list))
        }
    }

    if (selectedLauncher != null) {
        AlertDialog(
            onDismissRequest = onDismissChange,
            title = { Text(stringResource(R.string.tv_launcher_change_title)) },
            text = {
                Text(stringResource(R.string.tv_launcher_change_body, selectedLauncher.packageName))
            },
            confirmButton = {
                Button(onClick = onConfirmChange, enabled = state.updating == null) {
                    Text(stringResource(R.string.tv_launcher_set))
                }
            },
            dismissButton = { TextButton(onClick = onDismissChange) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
