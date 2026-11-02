@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.optimizer.tools.feature.tvtuning.presentation

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.optimizer.tools.R
import com.optimizer.tools.core.ui.resolve
import com.optimizer.tools.feature.tvtuning.domain.TvTuningCategory
import com.optimizer.tools.feature.tvtuning.domain.TvTuningPreset
import com.optimizer.tools.feature.tvtuning.domain.TvTuningRisk

@Composable
internal fun TvTuningScreen(
    state: TvTuningState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPreset: (TvTuningPreset) -> Unit,
    onToggleSetting: (String) -> Unit,
    onToggleBundle: (String) -> Unit,
    onApply: () -> Unit,
    onUndo: () -> Unit,
    onDismissMessage: () -> Unit,
    onDismissError: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tv_tuning_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    state.profile?.let { stringResource(R.string.tv_tuning_model, it.brand, it.model) }
                        ?: stringResource(R.string.tv_tuning_no_model),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRefresh, enabled = !state.running && !state.loading) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.tv_tuning_refresh))
            }
        }

        when {
            !state.deviceAvailable || !state.isTv -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(
                            if (!state.deviceAvailable) R.string.tv_tuning_no_device else R.string.tv_tuning_not_tv,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
        }

        state.error?.let { Notice(it.resolve(), MaterialTheme.colorScheme.error, onDismissError) }
        state.message?.let { Notice(it.resolve(), MaterialTheme.colorScheme.primary, onDismissMessage) }

        Text(
            stringResource(R.string.tv_tuning_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TvTuningPreset.entries.forEach { preset ->
                FilterChip(
                    selected = state.preset == preset,
                    onClick = { onPreset(preset) },
                    enabled = !state.running,
                    label = { Text(stringResource(preset.label())) },
                )
            }
        }

        if (state.running) {
            Text(
                stringResource(R.string.tv_tuning_applying, state.done, state.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 6.dp))
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TvTuningCategory.entries.forEach { category ->
                val items = state.byCategory(category)
                if (items.isNotEmpty()) {
                    item(key = "header-${category.name}") {
                        Text(
                            stringResource(category.label()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(items, key = { it.setting.id }) { item ->
                        SettingRow(
                            item = item,
                            enabled = !state.running,
                            onToggle = { onToggleSetting(item.setting.id) },
                        )
                    }
                }
            }
            if (state.bundles.isNotEmpty()) {
                item(key = "header-bundles") {
                    Text(
                        stringResource(R.string.tv_tuning_bundles),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(state.bundles, key = { it.bundle.id }) { item ->
                    BundleRow(
                        item = item,
                        enabled = !state.running,
                        onToggle = { onToggleBundle(item.bundle.id) },
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onApply,
                enabled = !state.running && state.selectionCount > 0,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.tv_tuning_apply, state.selectionCount))
            }
            OutlinedButton(
                onClick = onUndo,
                enabled = !state.running && (state.lastApplied.isNotEmpty() || state.lastRun != null),
            ) {
                Text(stringResource(R.string.tv_tuning_undo))
            }
        }
    }
}

@Composable
private fun SettingRow(
    item: TvSettingState,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = item.selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            androidx.compose.material3.Checkbox(checked = item.selected, onCheckedChange = null, enabled = enabled)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(stringResourceName(item.setting.titleKey)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    RiskBadge(item.setting.risk)
                }
                Text(
                    stringResource(stringResourceName(item.setting.summaryKey)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.current.isNotEmpty()) {
                    val unset = stringResource(R.string.tv_tuning_unset)
                    Text(
                        stringResource(
                            R.string.tv_tuning_current,
                            item.current.joinToString(" · ") { it ?: unset },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun BundleRow(
    item: TvBundleState,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = item.selected, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            androidx.compose.material3.Checkbox(checked = item.selected, onCheckedChange = null, enabled = enabled)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(stringResourceName(item.bundle.titleKey)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    RiskBadge(item.bundle.risk)
                }
                Text(
                    stringResource(R.string.tv_tuning_packages, item.bundle.packages.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RiskBadge(risk: TvTuningRisk) {
    Text(
        stringResource(
            if (risk == TvTuningRisk.SAFE) R.string.tv_tuning_risk_safe else R.string.tv_tuning_risk_advanced,
        ),
        style = MaterialTheme.typography.labelSmall,
        color =
            if (risk == TvTuningRisk.SAFE) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun Notice(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit,
) {
    val containerColor = color.copy(alpha = 0.12f)
    Card(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    }
}

private fun TvTuningPreset.label(): Int =
    when (this) {
        TvTuningPreset.RECOMMENDED -> R.string.tv_tuning_preset_recommended
        TvTuningPreset.PERFORMANCE -> R.string.tv_tuning_preset_performance
        TvTuningPreset.PRIVACY -> R.string.tv_tuning_preset_privacy
        TvTuningPreset.CUSTOM -> R.string.tv_tuning_preset_custom
    }

private fun TvTuningCategory.label(): Int =
    when (this) {
        TvTuningCategory.PERFORMANCE -> R.string.tv_tuning_category_performance
        TvTuningCategory.DISPLAY -> R.string.tv_tuning_category_display
        TvTuningCategory.PRIVACY -> R.string.tv_tuning_category_privacy
        TvTuningCategory.CLEANUP -> R.string.tv_tuning_category_cleanup
    }

/** Los ajustes guardan el nombre del recurso para no depender de `R` en el dominio. */
private fun stringResourceName(key: String): Int =
    when (key) {
        "tv_tuning_animations_title" -> R.string.tv_tuning_animations_title
        "tv_tuning_animations_summary" -> R.string.tv_tuning_animations_summary
        "tv_tuning_screensaver_title" -> R.string.tv_tuning_screensaver_title
        "tv_tuning_screensaver_summary" -> R.string.tv_tuning_screensaver_summary
        "tv_tuning_timeout_title" -> R.string.tv_tuning_timeout_title
        "tv_tuning_timeout_summary" -> R.string.tv_tuning_timeout_summary
        "tv_tuning_dexopt_title" -> R.string.tv_tuning_dexopt_title
        "tv_tuning_dexopt_summary" -> R.string.tv_tuning_dexopt_summary
        "tv_tuning_trim_title" -> R.string.tv_tuning_trim_title
        "tv_tuning_trim_summary" -> R.string.tv_tuning_trim_summary
        "tv_tuning_fixed_performance_title" -> R.string.tv_tuning_fixed_performance_title
        "tv_tuning_fixed_performance_summary" -> R.string.tv_tuning_fixed_performance_summary
        "tv_tuning_dns_title" -> R.string.tv_tuning_dns_title
        "tv_tuning_dns_summary" -> R.string.tv_tuning_dns_summary
        "tv_tuning_ad_tracking_title" -> R.string.tv_tuning_ad_tracking_title
        "tv_tuning_ad_tracking_summary" -> R.string.tv_tuning_ad_tracking_summary
        "tv_tuning_background_title" -> R.string.tv_tuning_background_title
        "tv_tuning_background_summary" -> R.string.tv_tuning_background_summary
        "tv_bundle_telemetry" -> R.string.tv_bundle_telemetry
        "tv_bundle_ads" -> R.string.tv_bundle_ads
        "tv_bundle_preload" -> R.string.tv_bundle_preload
        else -> R.string.tv_tuning_title
    }
