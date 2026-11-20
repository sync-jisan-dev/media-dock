package com.mediadock.app.core.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.theme.*

@Composable fun SectionHeading(title: String, subtitle: String = "", action: @Composable (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(CpdRadius.radiusLg),
        color = CpdColor.bgSubtleSecondary,
        border = BorderStroke(CpdBorderWidth.hairline, CpdColor.borderInteractiveSecondary)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(CpdSpace.space4x),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CpdSpace.space3x)
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal, color = CpdColor.textPrimary)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = CpdColor.textSecondary)
            }
            action?.invoke()
        }
    }
}

@Composable fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier,
        shape = RoundedCornerShape(CpdRadius.radiusLg),
        color = CpdColor.bgSubtleSecondary,
        border = BorderStroke(CpdBorderWidth.hairline, CpdColor.borderInteractiveSecondary)
    ) {
        Column(Modifier.padding(CpdSpace.space4x)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = CpdColor.textPrimary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = CpdColor.textSecondary)
        }
    }
}

@Composable fun EditTextDialog(title: String, value: String, onChange: (String) -> Unit, onDismiss: () -> Unit, onSave: () -> Unit, multiline: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = CpdColor.textPrimary) },
        text = {
            OutlinedTextField(
                value,
                onChange,
                Modifier.fillMaxWidth(),
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                maxLines = if (multiline) 8 else 1,
                shape = RoundedCornerShape(CpdRadius.radiusMd),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CpdColor.bgSubtleSecondary,
                    unfocusedContainerColor = CpdColor.bgSubtleSecondary,
                    focusedBorderColor = CpdColor.borderFocused,
                    unfocusedBorderColor = CpdColor.borderInteractiveSecondary
                )
            )
        },
        confirmButton = { TextButton(onClick = onSave, enabled = value.isNotBlank()) { Text("Save", color = CpdColor.CoralRest) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = CpdColor.textSecondary) } },
        containerColor = CpdColor.bgSubtleTertiary,
        shape = RoundedCornerShape(CpdRadius.radiusXl)
    )
}
