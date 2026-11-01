package com.example.c001apk.compose.ui.component.settings

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SwitchListItem(
    value: Boolean,
    leadingImageVector: ImageVector,
    headlineText: String,
    supportingText: String? = null,
    onValueChanged: (value: Boolean) -> Unit
) {
    BasicListItem(
        leadingImageVector = leadingImageVector,
        headlineText = headlineText,
        supportingText = supportingText,
        trailingContent = {
            Switch(
                checked = value,
                onCheckedChange = {
                    onValueChanged(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759),
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE5E5EA),
                    uncheckedBorderColor = Color.Transparent,
                )
            )
        }
    ) {
        onValueChanged(!value)
    }
}
