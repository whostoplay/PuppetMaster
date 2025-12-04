package org.menagerie.puppet_master.previews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.menagerie.puppet_master.ActiveSpecialEffect
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.SERVER_PORT
import org.menagerie.puppet_master.rememberImageFromUrl
import kotlin.math.min

/**
 * A composable that displays a live preview of the puppet.
 *
 * @param operatingMode The current operating mode.
 * @param puppetState The current puppet state.
 * @param isBlinking Whether the puppet is currently blinking.
 * @param uploadsDir The directory where uploaded images are stored.
 * @param backgroundColor The background color of the preview.
 * @param serverIp The IP address of the server.
 * @param activeSpecialEffect The currently active special effect.
 */
@Composable
fun LivePreview(
    operatingMode: OperatingMode,
    puppetState: PuppetStateInfo?,
    isBlinking: Boolean,
    uploadsDir: String,
    backgroundColor: Color,
    serverIp: String,
    activeSpecialEffect: ActiveSpecialEffect?
) {
    var frame by remember { mutableStateOf(0L) }

    val displayedImageName = if (isBlinking) puppetState?.blinkImageName else  puppetState?.imageName
    val eyeState = puppetState?.eyeState

    LaunchedEffect(activeSpecialEffect) {
        if (activeSpecialEffect != null) {
            while (true) {
                frame = System.currentTimeMillis()
                delay(16) // roughly 60 fps
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(backgroundColor).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val imageUrl = when {
            displayedImageName.isNullOrBlank() -> ""
            operatingMode == OperatingMode.ONLINE -> "http://$serverIp:$SERVER_PORT/uploads/$displayedImageName"
            else -> "file://$uploadsDir/$displayedImageName"
        }
        val image = rememberImageFromUrl(imageUrl)

        if (image != null) {
            val offset = activeSpecialEffect?.getVibrationOffset(maxWidth.value / 20f)
            val glowColor = activeSpecialEffect?.getGlowColor()?.let { Color(it) } ?: Color.White
            val glowIntensity = activeSpecialEffect?.getGlow() ?: 1f

            val colorMatrix = ColorMatrix().apply {
                setToScale(
                    redScale = glowIntensity * glowColor.red,
                    greenScale = glowIntensity * glowColor.green,
                    blueScale = glowIntensity * glowColor.blue,
                    alphaScale = 1f
                )
            }

            val imageScaleFactor = if (image.width > 0 && image.height > 0) {
                min(
                    maxWidth.value / image.width,
                    maxHeight.value / image.height
                )
            } else {
                1.0f
            }

            val density = LocalDensity.current
            val scaledWidth = with(density) { (image.width * imageScaleFactor).toDp() }
            val scaledHeight = with(density) { (image.height * imageScaleFactor).toDp() }

            val puppetModifier =
                Modifier
                    .graphicsLayer(
                        scaleX = activeSpecialEffect?.getScaleX() ?: 1f,
                        scaleY = activeSpecialEffect?.getScaleY() ?: 1f,
                        rotationZ = activeSpecialEffect?.getRotation() ?: 0f,
                        translationX = offset?.x ?: 0f,
                        translationY = offset?.y ?: 0f,
                        shadowElevation = glowIntensity * 30f,
                        ambientShadowColor = glowColor,
                        spotShadowColor = glowColor
                    )
                    .let { if (frame > 0) it else it } // force recomposition

            Box(
                modifier = Modifier
                    .size(scaledWidth, scaledHeight)
                    .then(puppetModifier)
            ) {
                Image(
                    bitmap = image,
                    contentDescription = "Live Preview",
                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                eyeState?.let {
                    val leftEye = it.eyes.left
                    val rightEye = it.eyes.right

                    val leftEyeModifier =
                        Modifier
                            .offset(
                                x = (leftEye.position.x * imageScaleFactor).dp,
                                y = (leftEye.position.y * imageScaleFactor).dp
                            )
                            .graphicsLayer(
                                scaleX = leftEye.scale * imageScaleFactor,
                                scaleY = leftEye.scale * imageScaleFactor,
                                transformOrigin = TransformOrigin(0f, 0f)
                            )

                    val rightEyeModifier =
                        Modifier
                            .offset(
                                x = (rightEye.position.x * imageScaleFactor).dp,
                                y = (rightEye.position.y * imageScaleFactor).dp
                            )
                            .graphicsLayer(
                                scaleX = rightEye.scale * imageScaleFactor,
                                scaleY = rightEye.scale * imageScaleFactor,
                                transformOrigin = TransformOrigin(0f, 0f)
                            )

                    if (isBlinking && leftEye.closedState != null && rightEye.closedState != null) {
                        // Blinking state - show closed eyes
                        val leftEyeClosedImageUrl =
                            when {
                                operatingMode == OperatingMode.ONLINE ->
                                    "http://$serverIp:$SERVER_PORT/uploads/${leftEye.closedState}"
                                else -> "file://$uploadsDir/${leftEye.closedState}"
                            }
                        val leftEyeClosedImage = rememberImageFromUrl(leftEyeClosedImageUrl)
                        if (leftEyeClosedImage != null) {
                            Image(
                                bitmap = leftEyeClosedImage,
                                contentDescription = "Left Eye Closed",
                                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                modifier = leftEyeModifier
                            )
                        }

                        val rightEyeClosedImageUrl =
                            when {
                                operatingMode == OperatingMode.ONLINE ->
                                    "http://$serverIp:$SERVER_PORT/uploads/${rightEye.closedState}"
                                else -> "file://$uploadsDir/${rightEye.closedState}"
                            }
                        val rightEyeClosedImage = rememberImageFromUrl(rightEyeClosedImageUrl)
                        if (rightEyeClosedImage != null) {
                            Image(
                                bitmap = rightEyeClosedImage,
                                contentDescription = "Right Eye Closed",
                                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                modifier = rightEyeModifier
                            )
                        }
                    } else {
                        // Open state - show open eyes and pupils
                        val leftEyeOpenImageUrl =
                            when {
                                operatingMode == OperatingMode.ONLINE ->
                                    "http://$serverIp:$SERVER_PORT/uploads/${leftEye.openState}"
                                else -> "file://$uploadsDir/${leftEye.openState}"
                            }
                        val leftEyeOpenImage = rememberImageFromUrl(leftEyeOpenImageUrl)

                        if (leftEyeOpenImage != null) {
                            Image(
                                bitmap = leftEyeOpenImage,
                                contentDescription = "Left Eye",
                                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                modifier = leftEyeModifier
                            )
                        }

                        leftEye.pupil?.let { pupilImageName ->
                            val leftEyePupilImageUrl =
                                when {
                                    operatingMode == OperatingMode.ONLINE ->
                                        "http://$serverIp:$SERVER_PORT/uploads/$pupilImageName"
                                    else -> "file://$uploadsDir/$pupilImageName"
                                }
                            val leftEyePupilImage = rememberImageFromUrl(leftEyePupilImageUrl)

                            if (leftEyePupilImage != null) {
                                Image(
                                    bitmap = leftEyePupilImage,
                                    contentDescription = "Left Eye Pupil",
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                    modifier = leftEyeModifier
                                )
                            }
                        }

                        val rightEyeOpenImageUrl =
                            when {
                                operatingMode == OperatingMode.ONLINE ->
                                    "http://$serverIp:$SERVER_PORT/uploads/${rightEye.openState}"
                                else -> "file://$uploadsDir/${rightEye.openState}"
                            }
                        val rightEyeOpenImage = rememberImageFromUrl(rightEyeOpenImageUrl)

                        if (rightEyeOpenImage != null) {
                            Image(
                                bitmap = rightEyeOpenImage,
                                contentDescription = "Right Eye",
                                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                modifier = rightEyeModifier
                            )
                        }

                        rightEye.pupil?.let { pupilImageName ->
                            val rightEyePupilImageUrl =
                                when {
                                    operatingMode == OperatingMode.ONLINE ->
                                        "http://$serverIp:$SERVER_PORT/uploads/$pupilImageName"
                                    else -> "file://$uploadsDir/$pupilImageName"
                                }
                            val rightEyePupilImage = rememberImageFromUrl(rightEyePupilImageUrl)

                            if (rightEyePupilImage != null) {
                                Image(
                                    bitmap = rightEyePupilImage,
                                    contentDescription = "Right Eye Pupil",
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                    modifier = rightEyeModifier
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text("No Active Image")
        }
    }
}
