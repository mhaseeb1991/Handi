package dev.haseeb.handi.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.haseeb.handi.core.designsystem.components.Kicker
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    onClose: () -> Unit,
    onSaved: (id: Long, wasNew: Boolean) -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(event) {
        when (val e = event) {
            is EditorEvent.Saved -> {
                viewModel.consumeEvent()
                onSaved(e.id, e.wasNew)
            }
            null -> Unit
        }
    }

    val requestClose: () -> Unit = {
        if (state.isDirty) {
            confirmDiscard = true
        } else {
            onClose()
        }
    }
    BackHandler(enabled = state.sheet == null) {
        if (!viewModel.previous()) requestClose()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            EditorTopBar(
                state = state,
                onClose = requestClose,
                onStepClick = viewModel::goTo,
            )
        },
        bottomBar = {
            EditorBottomBar(
                state = state,
                onBack = { viewModel.previous() },
                onNext = viewModel::next,
                onSave = viewModel::save,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!state.loading) {
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(tween(280)) { it / 4 * dir } + fadeIn(tween(280)))
                            .togetherWith(slideOutHorizontally(tween(220)) { -it / 4 * dir } + fadeOut(tween(180)))
                    },
                    label = "editor-step",
                ) { step ->
                    when (step) {
                        EditorStep.BASICS -> BasicsStep(
                            state = state,
                            onTitle = viewModel::setTitle,
                            onDescription = viewModel::setDescription,
                            onServings = viewModel::changeServings,
                            onMinutes = viewModel::changeMinutes,
                        )
                        EditorStep.INGREDIENTS -> IngredientsStep(
                            state = state,
                            onCategory = viewModel::selectCategory,
                            onQuery = viewModel::setIngredientQuery,
                            onIngredient = viewModel::openIngredient,
                            onPicked = viewModel::openPicked,
                            onRemovePicked = viewModel::removePicked,
                            onAddCustom = viewModel::addCustomIngredient,
                        )
                        EditorStep.METHOD -> MethodStep(
                            steps = state.steps,
                            onChange = viewModel::updateStep,
                            onAdd = viewModel::addStep,
                            onRemove = viewModel::removeStep,
                            onMove = viewModel::moveStep,
                        )
                        EditorStep.PHOTO -> PhotoStep(
                            state = state,
                            newCameraUri = viewModel::newCameraUri,
                            onPicked = viewModel::onImagePicked,
                            onRemove = viewModel::removeImage,
                            onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } },
                        )
                    }
                }
            }
        }
    }

    state.sheet?.let { sheet ->
        val category = state.categories.firstOrNull { it.id == sheet.categoryId }
        QuantityBottomSheet(
            sheet = sheet,
            category = category,
            onAmount = viewModel::setSheetAmount,
            onNudge = viewModel::nudgeSheetAmount,
            onMeasure = viewModel::setSheetMeasure,
            onNote = viewModel::setSheetNote,
            onConfirm = viewModel::confirmSheet,
            onRemove = viewModel::removeFromSheet,
            onDismiss = viewModel::dismissSheet,
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(if (state.isEditing) "Discard changes?" else "Discard this recipe?") },
            text = { Text("What you’ve written so far won’t be saved.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onClose() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun EditorTopBar(
    state: EditorUiState,
    onClose: () -> Unit,
    onStepClick: (EditorStep) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
            Column(Modifier.padding(start = 4.dp)) {
                Kicker(
                    "Step ${state.step.ordinal + 1} of ${EditorStep.entries.size} · " +
                        if (state.isEditing) "editing" else "new recipe",
                )
                Text(state.step.headline, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EditorStep.entries.forEach { step ->
                val reached = step.ordinal <= state.step.ordinal
                val enabled = state.canOpen(step)
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = enabled) { onStepClick(step) }
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (reached) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                    Text(
                        step.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (step == state.step) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    state: EditorUiState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
) {
    val isLast = state.step == EditorStep.PHOTO
    val hint = when {
        state.step == EditorStep.BASICS && !state.isComplete(EditorStep.BASICS) -> "Give your dish a name to continue"
        state.step == EditorStep.INGREDIENTS && state.picked.isEmpty() -> "Pick at least one ingredient"
        else -> null
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.step != EditorStep.BASICS) {
                    TextButton(onClick = onBack) { Text("Back") }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = if (isLast) onSave else onNext,
                    enabled = state.isComplete(state.step) && !state.saving,
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = ButtonDefaults.ContentPadding,
                    modifier = Modifier.height(52.dp),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(if (isLast) (if (state.isEditing) "Save changes" else "Save recipe") else "Continue")
                        Spacer(Modifier.size(8.dp))
                        Icon(
                            if (isLast) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
