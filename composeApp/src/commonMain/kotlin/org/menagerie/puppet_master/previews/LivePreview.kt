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
import androidx.compose.ui.graphics.ImageBitmap
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
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.rememberGlobalPointerPosition
import org.menagerie.puppet_master.rememberImageFromUrl
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

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
    isAudienceCheckForced: Boolean,
    window: Any?
) {
    var frame by remember { mutableLongStateOf(0L) }
    var jitter by remember { mutableStateOf(Offset.Zero) }
    var isCheckingAudience by remember { mutableStateOf(false) }
    val pointerPosition = rememberGlobalPointerPosition(window)

    val displayedImageName = if (isBlinking) puppetState?.blinkImageName else puppetState?.imageName
    val eyeState = puppetState?.eyeState

    var displayedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentImageUrl by remember { mutableStateOf<String?>(null) }

    val nextImageUrl = when {
        displayedImageName.isNullOrBlank() -> null
        operatingMode == OperatingMode.ONLINE -> "http://$serverIp:$SERVER_PORT/uploads/$displayedImageName"
        else -> "file://$uploadsDir/$displayedImageName"
    }

    val loadedImage = rememberImageFromUrl(currentImageUrl ?: "")

    LaunchedEffect(nextImageUrl) {
        // This effect runs ONLY when the target URL changes.
        // It updates `currentImageUrl` to trigger the load.
        currentImageUrl = nextImageUrl
    }

    LaunchedEffect(loadedImage) {
        // This effect runs ONLY when the `loadedImage` itself changes.
        if (loadedImage != null) {
            // A new image has finished loading, so we display it.
            displayedImage = loadedImage
        } else if (currentImageUrl == null) {
            // If the URL is null and there's no loaded image, clear the display.
            // This handles the case where no image should be shown.
            displayedImage = null
        }
    }


    LaunchedEffect(activeSpecialEffect) {
        if (activeSpecialEffect != null) {
            while (true) {
                frame = System.currentTimeMillis()
                delay(16) // roughly 60 fps
            }
        }
    }

    LaunchedEffect(
        eyeState?.eyes?.checkOnAudience, eyeState?.eyes?.audienceCheckRate, eyeState?.eyes?.audienceCheckDuration, isAudienceCheckForced
    ) {
        val checkOnAudience = eyeState?.eyes?.checkOnAudience == true
        if (checkOnAudience || isAudienceCheckForced) {
            val audienceCheckRate = eyeState?.eyes?.audienceCheckRate ?: 0
            val audienceCheckDuration = eyeState?.eyes?.audienceCheckDuration ?: 0
            if (audienceCheckRate > 0 && audienceCheckDuration > 0) {
                while (true) {
                    delay(audienceCheckRate)
                    isCheckingAudience = true
                    delay(audienceCheckDuration)
                    isCheckingAudience = false
                }
            }
        } else {
            isCheckingAudience = false
        }
    }

    LaunchedEffect(eyeState?.eyes?.focusOnGame, isCheckingAudience) {
        if (eyeState?.eyes?.focusOnGame == true && !isCheckingAudience) {
            while (true) {
                val randomAngle = Random.nextFloat() * 2 * Math.PI
                val randomRadius = Random.nextFloat() * 10f
                jitter = Offset(
                    x = (cos(randomAngle) * randomRadius).toFloat(),
                    y = (sin(randomAngle) * randomRadius).toFloat()
                )
                delay(100)
            }
        } else {
            jitter = Offset.Zero
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(backgroundColor).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val image = displayedImage

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
                        val focusPointOnScreen: Offset? = when {
                            isCheckingAudience || isAudienceCheckForced -> null
                            it.eyes.focusOnGame -> {
                                Offset(
                                    x = imageTopLeftX + (it.eyes.gameScreenLocation.x * scaledWidthPx),
                                    y = imageTopLeftY + (it.eyes.gameScreenLocation.y * scaledHeightPx)
                                )
                            }
                            it.eyes.followCursor && pointerPosition != null -> pointerPosition
                            else -> null
                        }

                        val finalFocusPointInImage: SerializableOffset? =
                            if (focusPointOnScreen != null) {
                                val rotationInDegrees = activeSpecialEffect?.getRotation() ?: 0f

                                val pointInImageXUnrotated = (focusPointOnScreen.x - imageTopLeftX) / imageScaleFactor
                                val pointInImageYUnrotated = (focusPointOnScreen.y - imageTopLeftY) / imageScaleFactor

                                if (rotationInDegrees == 0f) {
                                    SerializableOffset(pointInImageXUnrotated, pointInImageYUnrotated)
                                } else {
                                    val rotationInRadians = Math.toRadians(rotationInDegrees.toDouble())
                                    val imageCenterX = image.width / 2f
                                    val imageCenterY = image.height / 2f

                                    val pointRelToCenterX = pointInImageXUnrotated - imageCenterX
                                    val pointRelToCenterY = pointInImageYUnrotated - imageCenterY

                                    val cosAngle = cos(-rotationInRadians).toFloat()
                                    val sinAngle = sin(-rotationInRadians).toFloat()
                                    val rotatedPointRelToCenterX =
                                        pointRelToCenterX * cosAngle - pointRelToCenterY * sinAngle
                                    val rotatedPointRelToCenterY =
                                        pointRelToCenterX * sinAngle + pointRelToCenterY * cosAngle

                                    val finalPointInImageX = rotatedPointRelToCenterX + imageCenterX
                                    val finalPointInImageY = rotatedPointRelToCenterY + imageCenterY

                                    SerializableOffset(finalPointInImageX, finalPointInImageY)
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
                            if (finalFocusPointInImage != null && leftEyePupilImage != null) {
                                val angle = atan2(
                                    finalFocusPointInImage.y - leftEye.position.y + jitter.y,
                                    finalFocusPointInImage.x - leftEye.position.x + jitter.x
                                )
                                val x = leftEye.position.x + cos(angle) * (leftEye.maxPupilRadiusX * leftEye.scale)
                                val y = leftEye.position.y + sin(angle) * (leftEye.maxPupilRadiusY * leftEye.scale)

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
                            if (finalFocusPointInImage != null && rightEyePupilImage != null) {
                                val angle = atan2(
                                    finalFocusPointInImage.y - rightEye.position.y + jitter.y,
                                    finalFocusPointInImage.x - rightEye.position.x + jitter.x
                                )
                                val x = rightEye.position.x + cos(angle) * (rightEye.maxPupilRadiusX * rightEye.scale)
                                val y = rightEye.position.y + sin(angle) * (rightEye.maxPupilRadiusY * rightEye.scale)

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
        }
    }
}
