package dev.haseeb.handi.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.haseeb.handi.core.designsystem.components.Kicker

@Composable
fun MethodStep(
    steps: List<StepDraft>,
    onChange: (Long, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
    onMove: (Long, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
    ) {
        item("intro") {
            Kicker(
                "Write one action per step — it reads better while cooking",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            )
        }
        itemsIndexed(steps, key = { _, s -> s.id }) { index, step ->
            Row(Modifier.padding(vertical = 8.dp)) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(48.dp).padding(top = 10.dp, start = 4.dp),
                )
                OutlinedTextField(
                    value = step.text,
                    onValueChange = { onChange(step.id, it) },
                    placeholder = {
                        Text(
                            when (index) {
                                0 -> "Heat oil in a karahi and add the chicken…"
                                1 -> "Add tomatoes, ginger and green chillies…"
                                else -> "Describe this step…"
                            },
                        )
                    },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f),
                )
                Column {
                    IconButton(onClick = { onMove(step.id, true) }, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move step up")
                    }
                    IconButton(onClick = { onMove(step.id, false) }, enabled = index < steps.lastIndex, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move step down")
                    }
                    IconButton(onClick = { onRemove(step.id) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete step",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item("add") {
            OutlinedButton(
                onClick = onAdd,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 36.dp, top = 8.dp)
                    .height(52.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Add step", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
