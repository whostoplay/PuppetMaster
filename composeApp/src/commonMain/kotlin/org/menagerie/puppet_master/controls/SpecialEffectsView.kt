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
import org.menagerie.puppet_master.ActiveSpecialEffect
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.SpecialEffect
import org.menagerie.puppet_master.SpecialEffectsManager
import org.menagerie.puppet_master.previews.EffectPreview
import org.menagerie.puppet_master.previews.LivePreview
import org.menagerie.puppet_master.rememberImageFromUrl

@Composable
fun SpecialEffectsUI(
    specialEffectsManager: SpecialEffectsManager,
    onSpecialEffectsManagerChanged: (SpecialEffectsManager) -> Unit,
    onSaveEffect: (effect: SpecialEffect) -> Unit,
    activePuppet: PuppetCharacter?,
    uploadsDir: String,
    preserveState: Boolean,
    onPreserveStateChanged: (Boolean) -> Unit,
    window: Any?,
    onFocusChange: (Boolean) -> Unit,
    rootFocusRequester: FocusRequester,
    backgroundColor: Color,
) {
    val activeEffect = remember(specialEffectsManager.activeEffectIndex, specialEffectsManager.effects) {
        specialEffectsManager.getActiveEffect()
    }

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

            LaunchedEffect(previewEffect) {
                activePreviewEffect = activePreviewEffect?.copyWithPreservedStartTime(previewEffect) ?: ActiveSpecialEffect(previewEffect)
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
                        activeSpecialEffect = activePreviewEffect,
                        window = window,
                        isAudienceCheckForced = false,
                        displayedImageName = null,
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
            title = { Text("Select Glow Color") },
            text = { ColorPicker(true) { glowColor = it; showGlowColorPicker = false } },
            confirmButton = { Button(onClick = { showGlowColorPicker = false }) { Text("Close") } }
        )
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Special Effects")
            IconButton(onClick = {
                onSpecialEffectsManagerChanged(specialEffectsManager.addEffect())
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Effect")
            }
            IconButton(onClick = {
                onSpecialEffectsManagerChanged(specialEffectsManager.deleteEffect(specialEffectsManager.activeEffectIndex))
            }, enabled = activeEffect != null) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Effect")
            }
        }

        if(activeEffect != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showPreview,
                    onCheckedChange = { showPreview = it },
                    enabled = idleState != null
                )
                Text("Display Preview")
            }
        }

        activeEffect?.let { effect ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    onSpecialEffectsManagerChanged(specialEffectsManager.previousEffect())
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Effect")
                }
                EditableText(
                    text = effect.name,
                    onValueChange = { newName ->
                        effectName = newName
                        val updatedEffect = effect.copy(name = newName)
                        val updatedManager = specialEffectsManager.updateEffect(specialEffectsManager.activeEffectIndex, updatedEffect)
                        onSpecialEffectsManagerChanged(updatedManager)
                    },
                    onFocusChange = onFocusChange,
                    rootFocusRequester = rootFocusRequester
                )
                IconButton(onClick = {
                    onSpecialEffectsManagerChanged(specialEffectsManager.nextEffect())
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Effect")
                }
            }

            // Scale Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scale")
                IconButton(onClick = { showScaleDetails = !showScaleDetails }) {
                    Icon(
                        if (showScaleDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Scale Details"
                    )
                }
            }
            Slider(
                value = (scaleX + scaleY) / 2,
                onValueChange = {
                    scaleX = it
                    scaleY = it
                },
                valueRange = 0.25f..2f
            )
            if (showScaleDetails) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    EditableFloatText(label = "Scale X", value = scaleX, onValueChange = { scaleX = it }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = scaleX, onValueChange = { scaleX = it }, valueRange = 0.25f..2f)
                    EditableFloatText(label = "Scale Y", value = scaleY, onValueChange = { scaleY = it }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = scaleY, onValueChange = { scaleY = it }, valueRange = 0.25f..2f)
                    EditableFloatText(label = "Scale Speed", value = scaleSpeed, onValueChange = { scaleSpeed = it }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = scaleSpeed, onValueChange = { scaleSpeed = it }, valueRange = 0f..1f)
                }
            }

            // Vibration Controls
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vibration")
                IconButton(onClick = { showVibrationDetails = !showVibrationDetails }) {
                    Icon(
                        if (showVibrationDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Vibration Details"
                    )
                }
            }
            Slider(
                value = (vibrationDistance + vibrationSpeed) / 2,
                onValueChange = {
                    vibrationDistance = it
                    vibrationSpeed = it
                 },
                valueRange = 0f..1f
            )
            if (showVibrationDetails) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    EditableFloatText(label = "Distance", value = vibrationDistance, onValueChange = { vibrationDistance = it }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = vibrationDistance, onValueChange = { vibrationDistance = it }, valueRange = 0f..1f)
                    EditableFloatText(label = "Speed", value = vibrationSpeed, onValueChange = { vibrationSpeed = it }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
                    Slider(value = vibrationSpeed, onValueChange = { vibrationSpeed = it }, valueRange = 0f..1f)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditableFloatText(label = "Glow (Luminance)", value = glowIntensity, onValueChange = { glowIntensity = it }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
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
                valueRange = 0f..6f
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditableFloatText(label = "Spin Speed", value = spinSpeed, onValueChange = { spinSpeed = it }, onFocusChange = onFocusChange, rootFocusRequester = rootFocusRequester)
            }
            Slider(
                value = spinSpeed,
                onValueChange = { spinSpeed = it },
                valueRange = 0f..20f
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spin Direction")
                RadioButton(selected = spinDirection < 0, onClick = { spinDirection = -1 })
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Spin Left",
                    modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                )
                RadioButton(selected = spinDirection > 0, onClick = { spinDirection = 1 })
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Spin Right"
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = preserveState, onCheckedChange = onPreserveStateChanged)
            Text("Preserve this state across puppet changes")
        }
        Button(
            onClick = {
                activeEffect?.let {
                    onSaveEffect(
                        it.copy(
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
                    )
                }
            },
            enabled = effectName != "New Effect" && specialEffectsManager.isNameUnique(effectName, specialEffectsManager.activeEffectIndex)
        ) {
            Text("Save")
        }
        if(effectName == "New Effect" || !specialEffectsManager.isNameUnique(effectName, specialEffectsManager.activeEffectIndex)) {
            Text(
                text = if(effectName == "New Effect") "Rename Before Saving" else "Name Must Be Unique",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

    }
}
