package org.menagerie.puppet_master

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
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
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun SpecialEffectsUI(
    specialEffectsManager: SpecialEffectsManager,
    onSaveEffect: () -> Unit
) {
    var activeEffect by remember(specialEffectsManager.activeEffectIndex, specialEffectsManager.effects.size) {
        mutableStateOf(specialEffectsManager.getActiveEffect())
    }
    var effectName by remember {
        mutableStateOf(activeEffect?.name ?: "")
    }
    var vibrationIntensity by remember {
        mutableStateOf(activeEffect?.vibrationIntensity ?: 0f)
    }
    var glowIntensity by remember {
        mutableStateOf(activeEffect?.glowIntensity ?: 0f)
    }
    var scale by remember {
        mutableStateOf(activeEffect?.scale ?: 1f)
    }
    var spinSpeed by remember {
        mutableStateOf(activeEffect?.spinSpeed ?: 0f)
    }
    var isEditingName by remember { mutableStateOf(false) }

    LaunchedEffect(activeEffect) {
        activeEffect?.let {
            effectName = it.name
            vibrationIntensity = it.vibrationIntensity
            glowIntensity = it.glowIntensity
            scale = it.scale
            spinSpeed = it.spinSpeed
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

            Text("Vibration Intensity")
            Slider(
                value = vibrationIntensity,
                onValueChange = { vibrationIntensity = it },
                valueRange = 0f..1f
            )

            Text("Glow Intensity")
            Slider(
                value = glowIntensity,
                onValueChange = { glowIntensity = it },
                valueRange = 0f..1f
            )

            Text("Scale")
            Slider(
                value = scale,
                onValueChange = { scale = it },
                valueRange = 0f..2f
            )

            Text("Spin Speed")
            Slider(
                value = spinSpeed,
                onValueChange = { spinSpeed = it },
                valueRange = 0f..1f
            )

            Button(
                onClick = {
                    effect.name = effectName
                    effect.vibrationIntensity = vibrationIntensity
                    effect.glowIntensity = glowIntensity
                    effect.scale = scale
                    effect.spinSpeed = spinSpeed
                    onSaveEffect()
                    isEditingName = false
                },
                enabled = effectName.isNotBlank() && effectName != "New Effect" && (specialEffectsManager.isNameUnique(effectName) || effectName == effect.name)
            ) {
                Text("Save Effect")
            }
        }
    }
}