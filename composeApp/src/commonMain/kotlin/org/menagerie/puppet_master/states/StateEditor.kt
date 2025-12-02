package org.menagerie.puppet_master.states

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.SpecialEffectsManager

/**
 * A composable that provides a UI for editing the properties of a puppet state.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param selectedState The currently selected puppet state.
 * @param specialEffectsManager The manager for special effects.
 * @param onBlinkRateChanged A callback that is invoked when the blink rate is changed.
 * @param onApplyEffect A callback that is invoked when a special effect is applied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateEditor(
    modifier: Modifier,
    selectedState: PuppetStateInfo?,
    specialEffectsManager: SpecialEffectsManager,
    onBlinkRateChanged: (LongRange) -> Unit,
    onApplyEffect: (String?) -> Unit
) {
    val sliderValueRange = 200f..10000f
    var blinkRateRange by remember(selectedState) {
        val start = selectedState?.minBlinkRate?.toFloat()?.coerceIn(sliderValueRange) ?: 2000f
        val end = selectedState?.maxBlinkRate?.toFloat()?.coerceIn(sliderValueRange) ?: 8000f
        mutableStateOf(start..end)
    }
    var showEffectDropdown by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(8.dp)) {
        Text(text = "Edit State: ${selectedState?.name ?: ""}")
        selectedState?.let { state ->
            if (state.blinkImageName != null) {
                Text(text = "Blink Rate Range: ${blinkRateRange.start.toLong()} - ${blinkRateRange.endInclusive.toLong()} ms")
                RangeSlider(
                    value = blinkRateRange,
                    onValueChange = { newRange -> blinkRateRange = newRange },
                    onValueChangeFinished = { onBlinkRateChanged(blinkRateRange.start.toLong()..blinkRateRange.endInclusive.toLong()) },
                    valueRange = sliderValueRange
                )
            }

            Box {
                Text(
                    text = "Applied Effect: ${state.appliedEffectName ?: "None"}",
                    modifier = Modifier.fillMaxWidth().clickable { showEffectDropdown = true }
                )
                DropdownMenu(
                    expanded = showEffectDropdown,
                    onDismissRequest = { showEffectDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            onApplyEffect(null)
                            showEffectDropdown = false
                        }
                    )
                    specialEffectsManager.effects.forEach { effect ->
                        DropdownMenuItem(
                            text = { Text(effect.name) },
                            onClick = {
                                onApplyEffect(effect.name)
                                showEffectDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}
