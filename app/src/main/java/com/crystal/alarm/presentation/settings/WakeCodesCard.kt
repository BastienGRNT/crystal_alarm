package com.crystal.alarm.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crystal.alarm.R
import com.crystal.alarm.domain.model.WakeCode
import com.crystal.alarm.presentation.components.CardShape
import com.crystal.alarm.presentation.components.CrystalTextField
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType

/** Saved codes: tap to make one active, rename or delete it, or add a new one. */
@Composable
fun WakeCodesCard(
    codes: List<WakeCode>,
    activeId: String?,
    onSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
) {
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CrystalColors.Surface),
    ) {
        codes.forEach { code ->
            CodeRow(
                code = code,
                active = code.id == activeId,
                onSelect = { onSelect(code.id) },
                onRename = { renaming = code.id },
                onDelete = { deleting = code.id },
            )
            HorizontalDivider(color = CrystalColors.Background, thickness = 1.dp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAdd)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CrystalColors.Ice.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(rememberVectorPainter(Icons.Rounded.Add), null, tint = CrystalColors.Ice, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.action_add_code), style = CrystalType.BodyStrong, color = CrystalColors.Ice)
        }
    }

    codes.firstOrNull { it.id == renaming }?.let { code ->
        RenameDialog(
            initial = code.name,
            onConfirm = { name ->
                onRename(code.id, name)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    codes.firstOrNull { it.id == deleting }?.let { code ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = CrystalColors.SurfaceHigh,
            title = { Text(stringResource(R.string.code_delete_title, code.name), style = CrystalType.Title) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(code.id)
                    deleting = null
                }) { Text(stringResource(R.string.action_delete), color = CrystalColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel), color = CrystalColors.Text) }
            },
        )
    }
}

@Composable
private fun CodeRow(
    code: WakeCode,
    active: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val bubble by animateColorAsState(if (active) CrystalColors.Accent else CrystalColors.SurfaceHigh, label = "codeBubble")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bubble),
            contentAlignment = Alignment.Center,
        ) {
            if (active) {
                Icon(rememberVectorPainter(Icons.Rounded.Check), null, tint = CrystalColors.Background, modifier = Modifier.size(20.dp))
            } else {
                Icon(painterResource(R.drawable.ic_qr), null, tint = CrystalColors.TextMuted, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(code.name, style = CrystalType.BodyStrong, color = CrystalColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                text = listOf(code.format.ifBlank { null }, maskCode(code.value)).filterNotNull().joinToString(" · ") +
                    if (active) " · " + stringResource(R.string.code_active) else "",
                style = CrystalType.Caption,
                color = if (active) CrystalColors.Accent else CrystalColors.TextMuted,
                maxLines = 1,
            )
        }
        IconButton(onClick = onRename) {
            Icon(rememberVectorPainter(Icons.Rounded.Edit), stringResource(R.string.action_rename), tint = CrystalColors.TextMuted, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(rememberVectorPainter(Icons.Rounded.Delete), stringResource(R.string.action_delete), tint = CrystalColors.TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CrystalColors.SurfaceHigh,
        title = { Text(stringResource(R.string.code_rename_title), style = CrystalType.Title) },
        text = {
            CrystalTextField(
                value = name,
                onValueChange = { name = it.take(WakeCode.MAX_NAME_LENGTH) },
                placeholder = stringResource(R.string.register_name_placeholder),
                onDone = { if (name.isNotBlank()) onConfirm(name) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save), color = CrystalColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = CrystalColors.Text) }
        },
    )
}

internal fun maskCode(value: String): String =
    if (value.length <= 4) value else "•••• " + value.takeLast(4)
