package org.menagerie.puppet_master.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.menagerie.puppet_master.ActiveSpecialEffect
import org.menagerie.puppet_master.AnimationState
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.SpecialEffect
import org.menagerie.puppet_master.SpecialEffectsManager
import org.menagerie.puppet_master.localisation.Strings
import org.menagerie.puppet_master.previews.EffectPreview
import org.menagerie.puppet_master.previews.LivePreview
import org.menagerie.puppet_master.rememberImageFromUrl

/**
 * A composable that provides a UI for managing and editing special effects.
 *
 * This UI allows for the creation, deletion, and modification of special effects.
 * It includes controls for various effect properties such as scale, vibration, glow, and spin.
 * A live preview of the effect is also displayed.
 *
 * @param specialEffectsManager The [SpecialEffectsManager] that holds the state of the special effects.
 * @param onSpecialEffectsManagerChanged Callback invoked when the [SpecialEffectsManager] is modified.
 * @param activePuppet The currently active [PuppetCharacter], used for the effect preview.
 * @param uploadsDir The directory where uploaded images are stored, used for the effect preview.
 * @param preserveState A boolean indicating whether the current effect should be preserved across puppet changes.
 * @param onPreserveStateChanged Callback invoked when the preserveState value is changed.
 * @param window The application window, used for the effect preview.
 * @param onFocusChange Callback to report whether any input fields within the controls have focus.
 * @param rootFocusRequester A [FocusRequester] to return focus to the main application area after an action is completed.
 * @param backgroundColor The background color of the effect preview.
 */
@Composable
fun SpecialEffectsUI(
    specialEffectsManager: SpecialEffectsManager,
    onSpecialEffectsManagerChanged: (SpecialEffectsManager) -> Unit,
    activePuppet: PuppetCharacter?,
    uploadsDir: String,
    preserveState: Boolean,
    onPreserveStateChanged: (Boolean) -> Unit,
    window: Any?,
    onFocusChange: (Boolean) -> Unit,
    rootFocusRequester: FocusRequester,
    backgroundColor: Color,
) {
    val activeEffect = remember(specialEffectsManager) { specialEffectsManager.getActiveEffect() }

    var effectName by remember { mutableStateOf(activeEffect?.name ?: "") }
    var vibrationDistance by remember { mutableFloatStateOf(activeEffect?.vibrationDistance ?: 0f) }
    var vibrationSpeed by remember { mutableFloatStateOf(activeEffect?.vibrationSpeed ?: 0f) }
    var glowIntensity by remember { mutableFloatStateOf(activeEffect?.glowIntensity ?: 0f) }
    var glowColor by remember { mutableStateOf(Color(activeEffect?.glowColor ?: 0xFFFFFFFF.toInt())) }
    var scaleX by remember { mutableFloatStateOf(activeEffect?.scaleX ?: 1f) }
    var scaleY by remember { mutableFloatStateOf(activeEffect?.scaleY ?: 1f) }
    var scaleSpeed by remember { mutableFloatStateOf(activeEffect?.scaleSpeed ?: 0f) }
    var spinSpeed by remember { mutableFloatStateOf(activeEffect?.spinSpeed ?: 0f) }
    var spinDirection by remember { mutableIntStateOf(activeEffect?.spinDirection ?: 1) }
    var showPreview by remember { mutableStateOf(false) }
    var showScaleDetails by remember { mutableStateOf(false) }
    var showVibrationDetails by remember { mutableStateOf(false) }
    var showGlowColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(activeEffect) {
        activeEffect?.let {
            effectName = it.name
            vibrationDistance = it.vibrationDistance
            vibrationSpeed = it.vibrationSpeed
            glowIntensity = it.glowIntensity ?: 1f
            glowColor = Color(it.glowColor ?: 0xffffff)
            scaleX = it.scaleX
            scaleY = it.scaleY
            scaleSpeed = it.scaleSpeed
            spinSpeed = it.spinSpeed
            spinDirection = it.spinDirection
        }
    }

    val onValuesChanged = {
        if (specialEffectsManager.isCreatingEffect) {
            val effect = activeEffect?.copy(
                name = effectName,
                vibrationDistance = vibrationDistance,
                vibrationSpeed = vibrationSpeed,
                glowIntensity = glowIntensity,
                glowColor = glowColor.toArgb(),
                scaleX = scaleX,
                scaleY = scaleY,
                scaleSpeed = scaleSpeed,
                spinSpeed = spinSpeed,
                spinDirection = spinDirection
            )
            if (effect != null) {
                onSpecialEffectsManagerChanged(specialEffectsManager.updateNewEffect(effect))
            }
        }
    }

    val idleState = remember(activePuppet) {
        activePuppet?.states?.find { it.name == "idle" }
    }

    val idleImageUrl = remember(idleState, uploadsDir) {
        idleState?.imageName?.let { "file://$uploadsDir/$it" }
    }
    val idleImageBitmap = idleImageUrl?.let { rememberImageFromUrl(it) }

    EffectPreview(show = showPreview, onDismissRequest = { showPreview = false }) {
        if (idleState != null) {
            val previewEffect = remember(vibrationDistance, vibrationSpeed, glowIntensity, glowColor, scaleX, scaleY, scaleSpeed, spinSpeed, spinDirection) {
                SpecialEffect(
                    name = "preview",
                    vibrationDistance = vibrationDistance,
                    vibrationSpeed = vibrationSpeed,
                    glowIntensity = glowIntensity,
                    glowColor = glowColor.toArgb(),
                    scaleX = scaleX,
                    scaleY = scaleY,
                    scaleSpeed = scaleSpeed,
                    spinSpeed = spinSpeed,
                    spinDirection = spinDirection
                )
            }
            var activePreviewEffect by remember { mutableStateOf<ActiveSpecialEffect?>(null) }
            var animationState by remember { mutableStateOf(AnimationState()) }

            LaunchedEffect(previewEffect) {
                activePreviewEffect = activePreviewEffect?.copyWithPreservedStartTime(previewEffect) ?: ActiveSpecialEffect(previewEffect)
            }

            LaunchedEffect(activePreviewEffect) {
                if (activePreviewEffect != null) {
                    while (true) {
                        val effect = activePreviewEffect ?: break
                        val offset = effect.getVibrationOffset(1920f / 20f)
                        animationState = AnimationState(
                            rotation = effect.getRotation(),
                            scaleX = effect.getScaleX(),
                            scaleY = effect.getScaleY(),
                            translationX = offset.x,
                            translationY = offset.y,
                            glowColor = effect.getGlowColor(),
                            glowIntensity = effect.getGlow()
                        )
                        delay(16) // roughly 60 fps
                    }
                } else {
                    animationState = AnimationState()
                }
            }

            Box(Modifier.height(300.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (idleImageBitmap != null) {
                    LivePreview(
                        operatingMode = OperatingMode.OFFLINE,
                        puppetState = idleState,
                        isBlinking = false,
                        uploadsDir = uploadsDir,
                        backgroundColor = backgroundColor,
                        serverIp = "",
                        animationState = animationState,
                        window = window,
                        isAudienceCheckForced = false,
                        displayedImageName = idleState.imageName,
                        idleImage = idleImageBitmap,
                        onFocusPointUpdate = {}
                    )
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showGlowColorPicker) {
        AlertDialog(
            onDismissRequest = { showGlowColorPicker = false },
            title = { Text(Strings.getString(Strings.Keys.BG_COLOR_TEXT)) },
            text = { ColorPicker(true) { glowColor = it; showGlowColorPicker = false; onValuesChanged() } },
            confirmButton = { Button(onClick = { showGlowColorPicker = false }) { Text(Strings.getString(Strings.Keys.CANCEL_BUTTON)) } }
        )
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Strings.getString(Strings.Keys.SPECIAL_EFFECTS_TITLE))
            IconButton(onClick = {
                onSpecialEffectsManagerChanged(specialEffectsManager.startCreatingEffect())
            }, enabled = !specialEffectsManager.isCreatingEffect) {
                Icon(Icons.Filled.Add, contentDescription = Strings.getString(Strings.Keys.ADD_EFFECT_BUTTON))
            }
            IconButton(
                onClick = { onSpecialEffectsManagerChanged(specialEffectsManager.deleteEffect(specialEffectsManager.activeEffectIndex)) },
                enabled = activeEffect != null && !specialEffectsManager.isCreatingEffect
            ) {
                Icon(Icons.Filled.Delete, contentDescription = Strings.getString(Strings.Keys.DELETE_EFFECT_BUTTON))
            }
        }

        if(activeEffect != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showPreview,
                    onCheckedChange = { showPreview = it },
                    enabled = idleState != null
                )
                Text(Strings.getString(Strings.Keys.DISPLAY_PREVIEW_CHECKBOX))
            }
        }

        activeEffect?.let { effect ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onSpecialEffectsManagerChanged(specialEffectsManager.previousEffect()) },
                    enabled = !specialEffectsManager.isCreatingEffect
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.getString(Strings.Keys.PREVIOUS_EFFECT_BUTTON))
                }
                EditableText(
                    text = effect.name,
                    onValueChange = { newName ->
                        effectName = newName
                        onValuesChanged()
                    },
                    onFocusChange = onFocusChange,
                    rootFocusRequester = rootFocusRequester
                )
                IconButton(
                    onClick = { onSpecialEffectsManagerChanged(specialEffectsManager.nextEffect()) },
                    enabled = !specialEffectsManager.isCreatingEffect
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = Strings.getString(Strings.Keys.NEXT_EFFECT_BUTTON))
                }
            }

            // Scale Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.getString(Strings.Keys.SCALE_SLIDER))
                IconButton(onClick = { showScaleDetails = !showScaleDetails }) {
                    Icon(
                        if (showScaleDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = Strings.getString(Strings.Keys.TOGGLE_SCALE_DETAILS_BUTTON)
                    )
                }
            }
            Slider(
                value = (scaleX + scaleY) / 2,
                onValueChange = {
                    scaleX = it
                    scaleY = it
                },
                onValueChangeFinished = onValuesChanged,
                valueRange = 0.25f..2f
            )
            if (showScaleDetails) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    EditableFloatText(label = Strings.getString(Strings.Keys.SCALE_X_SLIDER), value = scaleX, onValueChange = { scaleX = it; onValuesChanged() }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = scaleX, onValueChange = { scaleX = it }, onValueChangeFinished = onValuesChanged, valueRange = 0.25f..2f)
                    EditableFloatText(label = Strings.getString(Strings.Keys.SCALE_Y_SLIDER), value = scaleY, onValueChange = { scaleY = it; onValuesChanged() }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = scaleY, onValueChange = { scaleY = it }, onValueChangeFinished = onValuesChanged, valueRange = 0.25f..2f)
                    EditableFloatText(label = Strings.getString(Strings.Keys.SCALE_SPEED_SLIDER), value = scaleSpeed, onValueChange = { scaleSpeed = it; onValuesChanged() }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = scaleSpeed, onValueChange = { scaleSpeed = it }, onValueChangeFinished = onValuesChanged, valueRange = 0f..1f)
                }
            }

            // Vibration Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.getString(Strings.Keys.VIBRATION_SLIDER))
                IconButton(onClick = { showVibrationDetails = !showVibrationDetails }) {
                    Icon(
                        if (showVibrationDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = Strings.getString(Strings.Keys.TOGGLE_VIBRATION_DETAILS_BUTTON)
                    )
                }
            }
            Slider(
                value = (vibrationDistance + vibrationSpeed) / 2,
                onValueChange = {
                    vibrationDistance = it
                    vibrationSpeed = it
                },
                onValueChangeFinished = onValuesChanged,
                valueRange = 0f..1f
            )
            if (showVibrationDetails) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    EditableFloatText(label = Strings.getString(Strings.Keys.VIBRATION_DISTANCE_SLIDER), value = vibrationDistance, onValueChange = { vibrationDistance = it; onValuesChanged() }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = vibrationDistance, onValueChange = { vibrationDistance = it }, onValueChangeFinished = onValuesChanged, valueRange = 0f..1f)
                    EditableFloatText(label = Strings.getString(Strings.Keys.VIBRATION_SPEED_SLIDER), value = vibrationSpeed, onValueChange = { vibrationSpeed = it; onValuesChanged() }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = vibrationSpeed, onValueChange = { vibrationSpeed = it }, onValueChangeFinished = onValuesChanged, valueRange = 0f..1f)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditableFloatText(label = Strings.getString(Strings.Keys.GLOW_LUMINANCE_SLIDER), value = glowIntensity, onValueChange = { glowIntensity = it; onValuesChanged() }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(glowColor)
                        .clickable { showGlowColorPicker = true }
                )
            }
            Slider(
                value = glowIntensity,
                onValueChange = { glowIntensity = it },
                onValueChangeFinished = onValuesChanged,
                valueRange = 0f..6f
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditableFloatText(label = Strings.getString(Strings.Keys.SPIN_SPEED_SLIDER), value = spinSpeed, onValueChange = { spinSpeed = it; onValuesChanged() }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
            }
            Slider(
                value = spinSpeed,
                onValueChange = { spinSpeed = it },
                onValueChangeFinished = onValuesChanged,
                valueRange = 0f..20f
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.getString(Strings.Keys.SPIN_DIRECTION_LABEL))
                RadioButton(selected = spinDirection < 0, onClick = { spinDirection = -1; onValuesChanged() })
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = Strings.getString(Strings.Keys.SPIN_LEFT_ICON),
                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                )
                RadioButton(selected = spinDirection > 0, onClick = { spinDirection = 1; onValuesChanged() })
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = Strings.getString(Strings.Keys.SPIN_RIGHT_ICON)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = preserveState, onCheckedChange = onPreserveStateChanged)
            Text(Strings.getString(Strings.Keys.PRESERVE_STATE_CHECKBOX))
        }

        if (specialEffectsManager.isCreatingEffect) {
            Row {
                Button(
                    onClick = { onSpecialEffectsManagerChanged(specialEffectsManager.saveNewEffect()) },
                    enabled = effectName != "New Effect" && specialEffectsManager.isNameUnique(effectName)
                ) {
                    Text(Strings.getString(Strings.Keys.SAVE_BUTTON))
                }
                Button(onClick = { onSpecialEffectsManagerChanged(specialEffectsManager.discardNewEffect()) }) {
                    Text(Strings.getString(Strings.Keys.CANCEL_BUTTON))
                }
            }
        } else if (activeEffect != null) {
            Button(
                onClick = {
                    val updatedEffect = activeEffect.copy(
                        name = effectName,
                        vibrationDistance = vibrationDistance,
                        vibrationSpeed = vibrationSpeed,
                        glowIntensity = glowIntensity,
                        glowColor = glowColor.toArgb(),
                        scaleX = scaleX,
                        scaleY = scaleY,
                        scaleSpeed = scaleSpeed,
                        spinSpeed = spinSpeed,
                        spinDirection = spinDirection
                    )
                    onSpecialEffectsManagerChanged(
                        specialEffectsManager.updateEffect(specialEffectsManager.activeEffectIndex, updatedEffect)
                    )
                },
                enabled = effectName != "New Effect" && specialEffectsManager.isNameUnique(effectName, specialEffectsManager.activeEffectIndex)
            ) {
                Text(Strings.getString(Strings.Keys.SAVE_BUTTON))
            }
        }

        if(effectName == "New Effect" || !specialEffectsManager.isNameUnique(effectName, specialEffectsManager.activeEffectIndex)) {
            Text(
                text = if(effectName == "New Effect") Strings.getString(Strings.Keys.RENAME_BEFORE_SAVING_TEXT) else Strings.getString(Strings.Keys.NAME_MUST_BE_UNIQUE_TEXT),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

    }
}
