package org.menagerie.puppet_master.controls

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.ActiveSpecialEffect
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.SpecialEffect
import org.menagerie.puppet_master.SpecialEffectsManager
import org.menagerie.puppet_master.previews.EffectPreview
import org.menagerie.puppet_master.previews.LivePreview

@Composable
fun SpecialEffectsUI(
    specialEffectsManager: SpecialEffectsManager,
    onSaveEffect: () -> Unit,
    activePuppet: PuppetCharacter?,
    uploadsDir: String,
    preserveState: Boolean,
    onPreserveStateChanged: (Boolean) -> Unit
) {
    var activeEffect by remember(specialEffectsManager.activeEffectIndex, specialEffectsManager.effects.size) {
        mutableStateOf(specialEffectsManager.getActiveEffect())
    }
    var effectName by remember { mutableStateOf(activeEffect?.name ?: "") }
    var vibrationDistance by remember { mutableStateOf(activeEffect?.vibrationDistance ?: 0f) }
    var vibrationSpeed by remember { mutableStateOf(activeEffect?.vibrationSpeed ?: 0f) }
    var glowIntensity by remember { mutableStateOf(activeEffect?.glowIntensity ?: 0f) }
    var scaleX by remember { mutableStateOf(activeEffect?.scaleX ?: 1f) }
    var scaleY by remember { mutableStateOf(activeEffect?.scaleY ?: 1f) }
    var scaleSpeed by remember { mutableStateOf(activeEffect?.scaleSpeed ?: 0f) }
    var spinSpeed by remember { mutableStateOf(activeEffect?.spinSpeed ?: 0f) }
    var spinDirection by remember { mutableStateOf(activeEffect?.spinDirection ?: 1) }
    var isEditingName by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showScaleDetails by remember { mutableStateOf(false) }
    var showVibrationDetails by remember { mutableStateOf(false) }

    LaunchedEffect(activeEffect) {
        activeEffect?.let {
            effectName = it.name
            vibrationDistance = it.vibrationDistance
            vibrationSpeed = it.vibrationSpeed
            glowIntensity = it.glowIntensity
            scaleX = it.scaleX
            scaleY = it.scaleY
            scaleSpeed = it.scaleSpeed
            spinSpeed = it.spinSpeed
            spinDirection = it.spinDirection
        }
    }

    val idleImageName = remember(activePuppet) {
        activePuppet?.states?.find { it.name == "idle" }?.imageName
    }

    EffectPreview(show = showPreview, onDismissRequest = { showPreview = false }) {
        if (idleImageName != null) {
            val previewEffect = remember(vibrationDistance, vibrationSpeed, glowIntensity, scaleX, scaleY, scaleSpeed, spinSpeed, spinDirection) {
                SpecialEffect(
                    name = "preview",
                    vibrationDistance = vibrationDistance,
                    vibrationSpeed = vibrationSpeed,
                    glowIntensity = glowIntensity,
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
                    displayedImageName = idleImageName,
                    uploadsDir = uploadsDir,
                    backgroundColor = Color.Green,
                    serverIp = "",
                    activeSpecialEffect = activePreviewEffect
                )
            }
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Special Effects")
            IconButton(onClick = {
                specialEffectsManager.addEffect()
                activeEffect = specialEffectsManager.getActiveEffect()
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Effect")
            }
            IconButton(onClick = {
                specialEffectsManager.deleteEffect(specialEffectsManager.activeEffectIndex)
                activeEffect = specialEffectsManager.getActiveEffect()
            }, enabled = activeEffect != null) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Effect")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = showPreview,
                onCheckedChange = { showPreview = it },
                enabled = idleImageName != null
            )
            Text("Display Preview")
        }

        activeEffect?.let { effect ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    specialEffectsManager.previousEffect()
                    activeEffect = specialEffectsManager.getActiveEffect()
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
                    specialEffectsManager.nextEffect()
                    activeEffect = specialEffectsManager.getActiveEffect()
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

            Text("Glow (Luminance)")
            Slider(
                value = glowIntensity,
                onValueChange = { glowIntensity = it },
                valueRange = 0f..1f
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spin Speed")
                IconButton(onClick = { spinDirection *= -1 }) {
                    Icon(if (spinDirection > 0) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Toggle Spin Direction")
                }
            }
            Slider(
                value = spinSpeed,
                onValueChange = { spinSpeed = it },
                valueRange = 0f..10f // Increased sensitivity
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        effect.name = effectName
                        effect.vibrationDistance = vibrationDistance
                        effect.vibrationSpeed = vibrationSpeed
                        effect.glowIntensity = glowIntensity
                        effect.scaleX = scaleX
                        effect.scaleY = scaleY
                        effect.scaleSpeed = scaleSpeed
                        effect.spinSpeed = spinSpeed
                        effect.spinDirection = spinDirection
                        onSaveEffect()
                        isEditingName = false
                    },
                    enabled = effectName.isNotBlank() && effectName != "New Effect" && (specialEffectsManager.isNameUnique(effectName) || effectName == effect.name)
                ) {
                    Text("Save Effect")
                }
                Checkbox(
                    checked = preserveState,
                    onCheckedChange = onPreserveStateChanged
                )
                Text("Preserve State")
            }
        }
    }
}
