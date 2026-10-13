package com.crystal.alarm.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crystal.alarm.R
import com.crystal.alarm.domain.model.DismissMethod
import com.crystal.alarm.domain.model.WakeUpRecord
import com.crystal.alarm.domain.usecase.WakeUpStats
import com.crystal.alarm.presentation.components.CrystalCard
import com.crystal.alarm.presentation.components.ScreenHeader
import com.crystal.alarm.presentation.components.SectionLabel
import com.crystal.alarm.presentation.format.AlarmFormat
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType
import java.time.ZoneId

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stats = state.stats

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CrystalColors.Background)
            .navigationBarsPadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.history_title), onBack = onBack)
        if (stats == null) return@Column

        LazyColumn(modifier = Modifier.padding(horizontal = 24.dp)) {
            item {
                Spacer(Modifier.height(8.dp))
                SectionLabel(stringResource(R.string.history_section_summary))
                SummaryCard(stats)
            }
            if (stats.records.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(28.dp))
                    SectionLabel(stringResource(R.string.history_section_list))
                }
                itemsIndexed(stats.records) { index, record ->
                    RecordRow(record, state.zone, isFirst = index == 0)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SummaryCard(stats: WakeUpStats) {
    val res = LocalResources.current
    val average = stats.average
    val best = stats.best
    CrystalCard {
        if (average == null || best == null) {
            Text(stringResource(R.string.history_empty), style = CrystalType.Body, color = CrystalColors.TextMuted)
            return@CrystalCard
        }
        Text(AlarmFormat.preciseDuration(res, average), style = CrystalType.Display, color = CrystalColors.Text)
        Text(
            stringResource(R.string.history_average_caption, minOf(stats.records.size, WakeUpStats.RECENT_COUNT)),
            style = CrystalType.Caption,
            color = CrystalColors.TextMuted,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            listOf(
                stringResource(R.string.history_best, AlarmFormat.preciseDuration(res, best)),
                stringResource(R.string.history_rescue_count, stats.rescueCount),
            ).joinToString(" · "),
            style = CrystalType.Label,
            color = CrystalColors.Accent,
        )
    }
}

/** One morning: date, alarm time and method on the left, time taken to stop it on the right. */
@Composable
private fun RecordRow(record: WakeUpRecord, zone: ZoneId, isFirst: Boolean) {
    val res = LocalResources.current
    if (!isFirst) HorizontalDivider(color = CrystalColors.Stroke)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(AlarmFormat.date(record.rangAt, zone), style = CrystalType.BodyStrong, color = CrystalColors.Text)
            Text(
                stringResource(
                    R.string.history_row_detail,
                    AlarmFormat.time(record.alarmTime),
                    stringResource(
                        when (record.method) {
                            DismissMethod.Code -> R.string.history_method_code
                            DismissMethod.Rescue -> R.string.history_method_rescue
                        },
                    ),
                ),
                style = CrystalType.Caption,
                color = CrystalColors.TextMuted,
            )
        }
        Text(
            AlarmFormat.preciseDuration(res, record.duration),
            style = CrystalType.BodyStrong.copy(fontFeatureSettings = "tnum"),
            color = CrystalColors.Text,
        )
    }
}
