package com.crystal.alarm.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.crystal.alarm.presentation.format.AlarmFormat
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun DayPicker(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AlarmFormat.WEEK.forEach { day ->
            val isOn = day in selected
            val container by animateColorAsState(if (isOn) CrystalColors.Accent else CrystalColors.Surface, label = "day")
            val content by animateColorAsState(if (isOn) CrystalColors.Background else CrystalColors.TextMuted, label = "dayText")
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .pressScale(interaction)
                    .clip(CircleShape)
                    .background(container)
                    .clickable(interactionSource = interaction, indication = null, role = Role.Checkbox) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onToggle(day)
                    }
                    .semantics {
                        this.selected = isOn
                        contentDescription = AlarmFormat.dayName(day, TextStyle.FULL)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(AlarmFormat.dayLetter(day), style = CrystalType.Label, color = content)
            }
        }
    }
}
