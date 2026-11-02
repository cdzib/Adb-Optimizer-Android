package com.optimizer.tools.app.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.optimizer.tools.R
import com.optimizer.tools.core.ui.AdbOptimizerTheme
import com.optimizer.tools.core.ui.AppIcon
import com.optimizer.tools.core.ui.DesignChip
import com.optimizer.tools.core.ui.formatBytes
import com.optimizer.tools.core.ui.resolve
import com.optimizer.tools.feature.apps.AppsState
import com.optimizer.tools.feature.apps.visibleApps
import dev.adboptimizer.core.model.AppAction
import dev.adboptimizer.core.model.AppFilter
import dev.adboptimizer.core.model.AppTarget
import dev.adboptimizer.core.model.ManagedApp

/**
 * Cuadricula de apps para television: cinco columnas, foco con anillo visible y acciones
 * de la app enfocada. Nunca ejecuta acciones destructivas por si sola: el host conserva
 * los dialogos de confirmacion.
 */
@Composable
fun TvAppsGrid(
    state: AppsState,
    onDetails: (ManagedApp) -> Unit,
    onReload: () -> Unit,
    onAction: (ManagedApp, AppAction) -> Unit = { _, _ -> },
    onInspect: (String) -> Unit = {},
    onFilter: (AppFilter) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val apps = remember(state.apps, state.query, state.filter, state.sort) { visibleApps(state) }
    var focusedApp by remember(apps) { mutableStateOf<ManagedApp?>(null) }
    var sheetApp by remember { mutableStateOf<ManagedApp?>(null) }
    val systemCount = state.apps.count { it.isSystem }
    val userCount = state.apps.count { !it.isSystem }
    // Cada chip sustituye el filtro por un conjunto exacto; onFilter alterna.
    val toggleFilter: (Set<AppFilter>) -> Unit = { desired ->
        (state.filter - desired).forEach { onFilter(it) }
        (desired - state.filter).forEach { onFilter(it) }
    }
    Column(modifier.fillMaxSize()) {
        TvScreenHeader(
            title = stringResource(R.string.tv_apps_title),
            subtitle = stringResource(R.string.tv_apps_subtitle, apps.size, systemCount),
            actions = {
                if (!state.privileged) {
                    Text(
                        stringResource(R.string.apps_read_only_title),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = onReload, enabled = !state.loading && state.busy == null) {
                    Text(stringResource(R.string.apps_refresh_list))
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DesignChip(
                text = stringResource(R.string.apps_filter_all_count, state.apps.size),
                selected = state.filter.isEmpty(),
                onClick = { toggleFilter(emptySet()) },
            )
            DesignChip(
                text = stringResource(R.string.apps_filter_user_count, userCount),
                selected = state.filter == setOf(AppFilter.USER),
                onClick = { toggleFilter(setOf(AppFilter.USER)) },
            )
            DesignChip(
                text = stringResource(R.string.apps_filter_system_count, systemCount),
                selected = state.filter == setOf(AppFilter.SYSTEM),
                onClick = { toggleFilter(setOf(AppFilter.SYSTEM)) },
            )
        }
        state.message?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        when {
            state.loading && apps.isEmpty() -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.padding(4.dp))
                    Text(stringResource(R.string.apps_loading))
                }
            }

            apps.isEmpty() -> {
                Text(stringResource(R.string.apps_empty), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.apps_empty_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        var focused by remember(app.packageName) { mutableStateOf(false) }
                        Surface(
                            onClick = {
                                focusedApp = app
                                sheetApp = app
                            },
                            enabled = state.busy == null && !state.loading,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged {
                                        focused = it.isFocused
                                        if (it.isFocused) focusedApp = app
                                    },
                            shape = RoundedCornerShape(14.dp),
                            color =
                                if (focused) {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            border = if (focused) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                        ) {
                            Column(
                                Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                AppIcon(app, local = state.target == AppTarget.LOCAL, size = 44)
                                Text(
                                    app.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    minLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (app.sizeBytes > 0) formatBytes(app.sizeBytes) else stringResource(R.string.apps_system),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                sheetApp?.let { app ->
                    AlertDialog(
                        onDismissRequest = { sheetApp = null },
                        title = { Text(app.displayName) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(
                                    onClick = {
                                        sheetApp = null
                                        onAction(app, AppAction.OPEN)
                                    },
                                    enabled = state.busy == null,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) { Text(stringResource(R.string.apps_open)) }
                                OutlinedButton(
                                    onClick = {
                                        sheetApp = null
                                        onDetails(app)
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) { Text(stringResource(R.string.apps_details)) }
                                OutlinedButton(
                                    onClick = {
                                        sheetApp = null
                                        if (state.privileged) onInspect(app.packageName) else onDetails(app)
                                    },
                                    enabled = state.busy == null,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) { Text(stringResource(R.string.apps_inspect)) }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { sheetApp = null }) {
                                Text(stringResource(R.string.apps_close))
                            }
                        },
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 640, heightDp = 400, showBackground = true)
@Composable
private fun TvAppsGridEmptyPreview() {
    AdbOptimizerTheme { TvTheme { TvAppsGrid(AppsState(), {}, {}) } }
}
