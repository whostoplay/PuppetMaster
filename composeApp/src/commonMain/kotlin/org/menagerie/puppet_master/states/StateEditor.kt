package org.menagerie.puppet_master.states

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.PuppetStateInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun StateEditor(
    modifier: Modifier,
    selectedState: PuppetStateInfo?,
    onBlinkRateChanged: (LongRange) -> Unit,
    onHover: (Boolean) -> Unit
) {
    val sliderValueRange = 200f..10000f
    var blinkRateRange by remember(selectedState) {
        val start = selectedState?.minBlinkRate?.toFloat()?.coerceIn(sliderValueRange) ?: 2000f
        val end = selectedState?.maxBlinkRate?.toFloat()?.coerceIn(sliderValueRange) ?: 8000f
        mutableStateOf(start..end)
    }

    val isHoveringOnItems = remember { mutableStateMapOf<String, Boolean>() }
    val isHovering = isHoveringOnItems.values.any { it }

    LaunchedEffect(isHovering) {
        onHover(isHovering)
    }

    Column(modifier = modifier.padding(8.dp)) {
        Text(
            text = "Edit State: ${selectedState?.name ?: ""}",
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["title"] = true }
                .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["title"] = false }
        )
        selectedState?.let {
            if (it.blinkImageName != null) {
                Text(
                    text = "Blink Rate Range: ${blinkRateRange.start.toLong()} - ${blinkRateRange.endInclusive.toLong()} ms",
                    modifier = Modifier
                        .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["rangeLabel"] = true }
                        .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["rangeLabel"] = false }
                )
                RangeSlider(
                    value = blinkRateRange,
                    onValueChange = { newRange -> blinkRateRange = newRange },
                    onValueChangeFinished = { onBlinkRateChanged(blinkRateRange.start.toLong()..blinkRateRange.endInclusive.toLong()) },
                    valueRange = sliderValueRange,
                    modifier = Modifier
                        .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["slider"] = true }
                        .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["slider"] = false }
                )
            }
        }
    }
}
