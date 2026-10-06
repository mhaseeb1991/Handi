package dev.haseeb.handi.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.haseeb.handi.core.designsystem.components.Hairline
import dev.haseeb.handi.core.designsystem.components.Kicker

@Composable
fun BasicsStep(
    state: EditorUiState,
    onTitle: (String) -> Unit,
    onDescription: (String) -> Unit,
    onServings: (Int) -> Unit,
    onMinutes: (Int) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (state.title.isEmpty()) runCatching { focus.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Kicker("Recipe name", color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = state.title,
            onValueChange = onTitle,
            textStyle = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp)
                .focusRequester(focus),
            decorationBox = { inner ->
                Box {
                    if (state.title.isEmpty()) {
                        Text(
                            "e.g. Chicken Karahi",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    inner()
                }
            },
        )
        Hairline()

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescription,
            label = { Text("A line about it (optional)") },
            placeholder = { Text("Smoky, tomato-rich, Peshawari style") },
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 24.dp),
        ) {
            StepperCard(
                label = "Serves",
                value = "${state.servings}",
                onMinus = { onServings(-1) },
                onPlus = { onServings(1) },
                modifier = Modifier.weight(1f),
            )
            StepperCard(
                label = "Cook time",
                value = "${state.cookMinutes}m",
                onMinus = { onMinutes(-5) },
                onPlus = { onMinutes(5) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StepperCard(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Kicker(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease $label")
                }
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase $label")
                }
            }
        }
    }
}
