package com.crystal.alarm.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.crystal.alarm.presentation.theme.CrystalColors
import com.crystal.alarm.presentation.theme.CrystalType

/** Single-line field in the app style: filled, rounded, accent border when focused. */
@Composable
fun CrystalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = CrystalType.BodyStrong.copy(color = CrystalColors.Text),
        cursorBrush = SolidColor(CrystalColors.Accent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .clip(shape)
                    .background(CrystalColors.SurfaceHigh)
                    .border(1.dp, if (focused) CrystalColors.Accent else CrystalColors.SurfaceHigh, shape)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, style = CrystalType.Body, color = CrystalColors.TextFaint)
                inner()
            }
        },
    )
}
