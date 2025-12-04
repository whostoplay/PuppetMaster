package org.menagerie.puppet_master.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.ActiveSpecialEffect
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.SpecialEffect
import org.menagerie.puppet_master.SpecialEffectsManager
import org.menagerie.puppet_master.previews.EffectPreview
import org.menagerie.puppet_master.previews.LivePreview

@Composable
fun SpecialEffectsUI(
    specialEffectsManager: SpecialEffectsManager,
    onSpecialEffectsManagerChanged: (SpecialEffectsManager) -> Unit,
    onSaveEffect: () -> Unit,
    activePuppet: PuppetCharacter?,
    uploadsDir: String,
    preserveState: Boolean,
    onPreserveStateChanged: (Boolean) -> Unit
) {
    val activeEffect = remember(specialEffectsManager.activeEffectIndex, specialEffectsManager.effects) {
        specialEffectsManager.getActiveEffect()
    }

    var effectName by remember { mutableStateOf(activeEffect?.name ?: "") }
    var vibrationDistance by remember { mutableStateOf(activeEffect?.vibrationDistance ?: 0f) }
    var vibrationSpeed by remember { mutableStateOf(activeEffect?.vibrationSpeed ?: 0f) }
    var glowIntensity by remember { mutableStateOf(activeEffect?.glowIntensity ?: 2f) }
    var glowColor by remember { mutableStateOf(Color(activeEffect?.glowColor ?: 0xFFFFFFFF.toInt())) }
    var scaleX by remember { mutableStateOf(activeEffect?.scaleX ?: 1f) }
    var scaleY by remember { mutableStateOf(activeEffect?.scaleY ?: 1f) }
    var scaleSpeed by remember { mutableStateOf(activeEffect?.scaleSpeed ?: 0f) }
    var spinSpeed by remember { mutableStateOf(activeEffect?.spinSpeed ?: 0f) }
    var spinDirection by remember { mutableStateOf(activeEffect?.spinDirection ?: 1) }
    var isEditingName by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showScaleDetails by remember { mutableStateOf(false) }
    var showVibrationDetails by remember { mutableStateOf(false) }
    var showGlowColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(activeEffect) {
        activeEffect?.let {
            effectName = it.name
            vibrationDistance = it.vibrationDistance
            vibrationSpeed = it.vibrationSpeed
            glowIntensity = it.glowIntensity
            glowColor = Color(it.glowColor)
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
            val activePreviewEffect = remember(previewEffect) {
                ActiveSpecialEffect(previewEffect)
            }
            Box(Modifier.height(300.dp).fillMaxWidth()) {
                LivePreview(
                    operatingMode = OperatingMode.OFFLINE,
                    puppetState = idleState,
                    isBlinking = false,
                    uploadsDir = uploadsDir,
                    backgroundColor = Color.Green,
                    serverIp = "",
                    activeSpecialEffect = activePreviewEffect
                )
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = showPreview,
                onCheckedChange = { showPreview = it },
                enabled = idleState != null
            )
            Text("Display Preview")
        }

        activeEffect?.let { effect ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    onSpecialEffectsManagerChanged(specialEffectsManager.previousEffect())
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Effect")
                }
                if (isEditingName) {
                    TextField(
                        value = effectName,
                        onValueChange = { effectName = it },
                        singleLine = true,
                    )
                } else {
                    Text(effect.name, modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { isEditingName = true })
                    })
                }
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
                    Text("Scale X: %.2f".format(scaleX))
                    Slider(value = scaleX, onValueChange = { scaleX = it }, valueRange = 0.25f..2f)
                    Text("Scale Y: %.2f".format(scaleY))
                    Slider(value = scaleY, onValueChange = { scaleY = it }, valueRange = 0.25f..2f)
                    Text("Scale Speed: %.2f".format(scaleSpeed))
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
                    Text("Distance: %.2f".format(vibrationDistance))
                    Slider(value = vibrationDistance, onValueChange = { vibrationDistance = it }, valueRange = 0f..1f)
                    Text("Speed: %.2f".format(vibrationSpeed))
                    Slider(value = vibrationSpeed, onValueChange = { vibrationSpeed = it }, valueRange = 0f..1f)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Glow (Luminance)")
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
                Text("Spin Speed")
            }
            Slider(
                value = spinSpeed,
                onValueChange = { spinSpeed = it },
                valueRange = 0f..10f // Increased sensitivity
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spin Direction")
                IconButton(onClick = { spinDirection *= -1 }) {
                    Icon(if (spinDirection > 0) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Toggle Spin Direction")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val updatedEffect = effect.copy(
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
                        onSpecialEffectsManagerChanged(specialEffectsManager.updateEffect(specialEffectsManager.activeEffectIndex, updatedEffect))
                        isEditingName = false
                        onSaveEffect()
                    }
                ) {
                    Text("Save Effect")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = preserveState, onCheckedChange = onPreserveStateChanged)
                    Text("Preserve on change")
                }
            }
        }
    }
}
