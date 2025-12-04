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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
    activeSpecialEffect: ActiveSpecialEffect?,
    pointerPosition: Offset? = null,
    maxPupilRadius: Float = 75f
) {
    var frame by remember { mutableLongStateOf(0L) }

    val displayedImageName = if (isBlinking) puppetState?.blinkImageName else puppetState?.imageName
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

            val scaledWidthPx = image.width * imageScaleFactor
            val scaledHeightPx = image.height * imageScaleFactor
            val imageTopLeftX = (with(density) { maxWidth.toPx() } - scaledWidthPx) / 2f
            val imageTopLeftY = (with(density) { maxHeight.toPx() } - scaledHeightPx) / 2f

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
                        val finalPointerInImage: Offset? =
                            if (it.eyes.followCursor && pointerPosition != null) {
                                val rotationInDegrees = activeSpecialEffect?.getRotation() ?: 0f

                                val pointerInImageXUnrotated = (pointerPosition.x - imageTopLeftX) / imageScaleFactor
                                val pointerInImageYUnrotated = (pointerPosition.y - imageTopLeftY) / imageScaleFactor

                                if (rotationInDegrees == 0f) {
                                    Offset(pointerInImageXUnrotated, pointerInImageYUnrotated)
                                } else {
                                    val rotationInRadians = Math.toRadians(rotationInDegrees.toDouble())

                                    val imageCenterX = image.width / 2f
                                    val imageCenterY = image.height / 2f

                                    val pointerRelToCenterX = pointerInImageXUnrotated - imageCenterX
                                    val pointerRelToCenterY = pointerInImageYUnrotated - imageCenterY

                                    val cosAngle = cos(-rotationInRadians).toFloat()
                                    val sinAngle = sin(-rotationInRadians).toFloat()

                                    val rotatedPointerRelToCenterX =
                                        pointerRelToCenterX * cosAngle - pointerRelToCenterY * sinAngle
                                    val rotatedPointerRelToCenterY =
                                        pointerRelToCenterX * sinAngle + pointerRelToCenterY * cosAngle

                                    val finalPointerInImageX = rotatedPointerRelToCenterX + imageCenterX
                                    val finalPointerInImageY = rotatedPointerRelToCenterY + imageCenterY
                                    Offset(finalPointerInImageX, finalPointerInImageY)
                                }
                            } else {
                                null
                            }

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

                            var pupilModifier = leftEyeModifier
                            if (finalPointerInImage != null && leftEyePupilImage != null) {
                                val angle = atan2(
                                    finalPointerInImage.y - leftEye.position.y,
                                    finalPointerInImage.x - leftEye.position.x
                                )
                                val x = leftEye.position.x + cos(angle) * (maxPupilRadius * leftEye.scale)
                                val y = leftEye.position.y + sin(angle) * (maxPupilRadius * leftEye.scale)

                                pupilModifier = Modifier.offset(
                                    x = (x * imageScaleFactor).dp,
                                    y = (y * imageScaleFactor).dp
                                )
                                    .graphicsLayer(
                                        scaleX = leftEye.scale * imageScaleFactor,
                                        scaleY = leftEye.scale * imageScaleFactor,
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    )
                            }

                            if (leftEyePupilImage != null) {
                                Image(
                                    bitmap = leftEyePupilImage,
                                    contentDescription = "Left Eye Pupil",
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                    modifier = pupilModifier
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

                            var pupilModifier = rightEyeModifier
                            if (finalPointerInImage != null && rightEyePupilImage != null) {
                                val angle = atan2(
                                    finalPointerInImage.y - rightEye.position.y,
                                    finalPointerInImage.x - rightEye.position.x
                                )
                                val x = rightEye.position.x + cos(angle) * (maxPupilRadius * rightEye.scale)
                                val y = rightEye.position.y + sin(angle) * (maxPupilRadius * rightEye.scale)

                                pupilModifier = Modifier.offset(
                                    x = (x * imageScaleFactor).dp,
                                    y = (y * imageScaleFactor).dp
                                )
                                    .graphicsLayer(
                                        scaleX = rightEye.scale * imageScaleFactor,
                                        scaleY = rightEye.scale * imageScaleFactor,
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    )
                            }

                            if (rightEyePupilImage != null) {
                                Image(
                                    bitmap = rightEyePupilImage,
                                    contentDescription = "Right Eye Pupil",
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix),
                                    modifier = pupilModifier
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
