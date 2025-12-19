package org.menagerie.puppet_master.previews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.AnimationState
import org.menagerie.puppet_master.Constants
import org.menagerie.puppet_master.Eye
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.Strings
import org.menagerie.puppet_master.rememberGlobalPointerPosition
import org.menagerie.puppet_master.rememberImageFromUrl
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A composable that displays a live preview of the puppet, including eye movements, blinking, and special effects.
 *
 * @param operatingMode The current operating mode of the application (online or offline).
 * @param puppetState The current state of the puppet to be displayed.
 * @param isBlinking Whether the puppet should be blinking.
 * @param uploadsDir The directory where uploaded images are stored.
 * @param backgroundColor The background color of the preview.
 * @param serverIp The IP address of the server (if in online mode).
 * @param animationState The current animation state for special effects.
 * @param isAudienceCheckForced Whether to force an audience check.
 * @param window The window object, used for tracking the global pointer position.
 * @param displayedImageName The name of the image to be displayed.
 * @param idleImage The image to display when no other image is available.
 * @param onFocusPointUpdate A callback to be invoked when the focus point of the eyes is updated.
 */
@Composable
fun LivePreview(
    operatingMode: OperatingMode,
    puppetState: PuppetStateInfo?,
    isBlinking: Boolean,
    uploadsDir: String,
    backgroundColor: Color,
    serverIp: String,
    animationState: AnimationState,
    isAudienceCheckForced: Boolean,
    window: Any?,
    displayedImageName: String?,
    idleImage: ImageBitmap,
    onFocusPointUpdate: (SerializableOffset?) -> Unit,
) {
    var jitter by remember { mutableStateOf(Offset.Zero) }
    var isCheckingAudience by remember { mutableStateOf(false) }
    var audienceCheckBlink by remember { mutableStateOf(false) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    val mousePosition = rememberGlobalPointerPosition(window)

    val eyeState = puppetState?.eyeState

    fun getImageUrl(imageName: String?): String? {
        return when {
            imageName.isNullOrBlank() -> null
            operatingMode == OperatingMode.ONLINE -> "http://$serverIp:${Constants.Server.PORT}/uploads/$imageName"
            else -> "file://$uploadsDir/$imageName"
        }
    }

    val loadedBody = getImageUrl(displayedImageName)?.let { rememberImageFromUrl(it) }
    val loadedBlinkBody = getImageUrl(puppetState?.blinkImageName)?.let { rememberImageFromUrl(it) }
    val loadedLeftOpenEye = getImageUrl(eyeState?.eyes?.left?.openState)?.let { rememberImageFromUrl(it) }
    val loadedLeftClosedEye = getImageUrl(eyeState?.eyes?.left?.closedState)?.let { rememberImageFromUrl(it) }
    val loadedLeftPupil = getImageUrl(eyeState?.eyes?.left?.pupil)?.let { rememberImageFromUrl(it) }
    val loadedRightOpenEye = getImageUrl(eyeState?.eyes?.right?.openState)?.let { rememberImageFromUrl(it) }
    val loadedRightClosedEye = getImageUrl(eyeState?.eyes?.right?.closedState)?.let { rememberImageFromUrl(it) }
    val loadedRightPupil = getImageUrl(eyeState?.eyes?.right?.pupil)?.let { rememberImageFromUrl(it) }

    LaunchedEffect(
        eyeState?.eyes?.checkOnAudience,
        eyeState?.eyes?.audienceCheckRate,
        eyeState?.eyes?.audienceCheckDuration,
        isAudienceCheckForced,
        loadedBlinkBody
    ) {
        val checkOnAudience = eyeState?.eyes?.checkOnAudience == true
        if (checkOnAudience || isAudienceCheckForced) {
            val audienceCheckRate = eyeState?.eyes?.audienceCheckRate ?: 0
            val audienceCheckDuration = eyeState?.eyes?.audienceCheckDuration ?: 0
            if (audienceCheckRate > 0 && audienceCheckDuration > 0) {
                while (true) {
                    kotlinx.coroutines.delay(audienceCheckRate)
                    if (loadedBlinkBody != null) {
                        audienceCheckBlink = true
                        kotlinx.coroutines.delay(100)
                        audienceCheckBlink = false
                    }
                    isCheckingAudience = true
                    kotlinx.coroutines.delay(audienceCheckDuration)
                    if (loadedBlinkBody != null) {
                        audienceCheckBlink = true
                        kotlinx.coroutines.delay(100)
                        audienceCheckBlink = false
                    }
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
                val randomAngle = Random.nextFloat() * Math.PI
                val randomRadius = Random.nextFloat() * 5f
                jitter = Offset(
                    x = (cos(randomAngle) * randomRadius).toFloat(),
                    y = (sin(randomAngle) * randomRadius).toFloat()
                )
                kotlinx.coroutines.delay(250)
            }
        } else {
            jitter = Offset.Zero
        }
    }

    // --- Rendering ---
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(backgroundColor).padding(8.dp)
            .pointerInput(Unit) {
                forEachGesture {
                    awaitPointerEventScope {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            touchPosition = event.changes.firstOrNull()?.position
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        touchPosition = null
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val isEffectivelyBlinking = isBlinking || audienceCheckBlink
        val image = (if (isEffectivelyBlinking) loadedBlinkBody else loadedBody) ?: idleImage

        val glowColor = Color(animationState.glowColor)
        val glowIntensity = animationState.glowIntensity

        val colorMatrix = ColorMatrix(
            floatArrayOf(
                glowIntensity * glowColor.red, 0f, 0f, 0f, glowColor.red * 0.2f,
                0f, glowIntensity * glowColor.green, 0f, 0f, glowColor.green * 0.2f,
                0f, 0f, glowIntensity * glowColor.blue, 0f, glowColor.blue * 0.2f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val imageScaleFactor = if (image.width > 0 && image.height > 0) {
            min(maxWidth.value / image.width, maxHeight.value / image.height)
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

        val puppetModifier = Modifier.graphicsLayer(
            scaleX = animationState.scaleX,
            scaleY = animationState.scaleY,
            rotationZ = animationState.rotation,
            translationX = animationState.translationX,
            translationY = animationState.translationY,
            shadowElevation = glowIntensity * 30f,
            ambientShadowColor = glowColor,
            spotShadowColor = glowColor
        )

        Box(
            modifier = Modifier.size(scaledWidth, scaledHeight).then(puppetModifier)
        ) {
            Image(
                bitmap = image,
                contentDescription = Strings.getString(Strings.Keys.LIVE_PREVIEW_CONTENT_DESCRIPTION),
                colorFilter = if (glowIntensity > 0) ColorFilter.colorMatrix(colorMatrix) else null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            eyeState?.let { eyeData ->
                val leftEye = eyeData.eyes.left
                val rightEye = eyeData.eyes.right

                val leftEyeModifier = Modifier
                    .offset {
                        IntOffset(
                            (leftEye.position.x * imageScaleFactor).roundToInt(),
                            (leftEye.position.y * imageScaleFactor).roundToInt()
                        )
                    }
                    .graphicsLayer(
                        scaleX = leftEye.scaleX * imageScaleFactor,
                        scaleY = leftEye.scaleY * imageScaleFactor,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )

                val rightEyeModifier = Modifier
                    .offset {
                        IntOffset(
                            (rightEye.position.x * imageScaleFactor).roundToInt(),
                            (rightEye.position.y * imageScaleFactor).roundToInt()
                        )
                    }
                    .graphicsLayer(
                        scaleX = rightEye.scaleX * imageScaleFactor,
                        scaleY = rightEye.scaleY * imageScaleFactor,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )

                val leftEyeToDisplay = if (isEffectivelyBlinking) loadedLeftClosedEye else loadedLeftOpenEye
                leftEyeToDisplay?.let {
                    Image(bitmap = it, contentDescription = Strings.getString(Strings.Keys.LEFT_EYE), modifier = leftEyeModifier, colorFilter = if (glowIntensity > 0) ColorFilter.colorMatrix(colorMatrix) else null)
                }

                val rightEyeToDisplay = if (isEffectivelyBlinking) loadedRightClosedEye else loadedRightOpenEye
                rightEyeToDisplay?.let {
                    Image(bitmap = it, contentDescription = Strings.getString(Strings.Keys.RIGHT_EYE), modifier = rightEyeModifier, colorFilter = if (glowIntensity > 0) ColorFilter.colorMatrix(colorMatrix) else null)
                }

                if (!isEffectivelyBlinking) {
                    val focusPointOnScreen: Offset? = when {
                        isCheckingAudience || isAudienceCheckForced -> null
                        eyeData.eyes.focusOnGame -> Offset(x = imageTopLeftX + (eyeData.eyes.gameScreenLocation.x * scaledWidthPx), y = imageTopLeftY + (eyeData.eyes.gameScreenLocation.y * scaledHeightPx))
                        eyeData.eyes.followCursor -> {
                            if (operatingMode == OperatingMode.ONLINE) {
                                eyeData.cursorPosition?.let { Offset(it.x, it.y) }
                            } else {
                                mousePosition ?: touchPosition
                            }
                        }

                        else -> null
                    }

                    val finalFocusPointInImage: SerializableOffset? = if (focusPointOnScreen != null) {
                        val rotationInDegrees = animationState.rotation
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
                            val rotatedPointRelToCenterX = pointRelToCenterX * cosAngle - pointRelToCenterY * sinAngle
                            val rotatedPointRelToCenterY = pointRelToCenterX * sinAngle + pointRelToCenterY * cosAngle
                            val finalPointInImageX = rotatedPointRelToCenterX + imageCenterX
                            val finalPointInImageY = rotatedPointRelToCenterY + imageCenterY
                            SerializableOffset(finalPointInImageX, finalPointInImageY)
                        }
                    } else {
                        null
                    }

                    onFocusPointUpdate(finalFocusPointInImage)

                    val pupilFocusPoint = if (isCheckingAudience || isAudienceCheckForced) {
                        null
                    } else {
                        finalFocusPointInImage
                    }

                    val leftPupilPosition = getPupilPosition(
                        pupilFocusPoint, leftEye, loadedLeftPupil, loadedLeftOpenEye
                    )
                    leftPupilPosition?.let { position ->
                        loadedLeftPupil?.let {
                            Image(
                                bitmap = it,
                                contentDescription = Strings.getString(Strings.Keys.LEFT_PUPIL_CONTENT_DESCRIPTION),
                                modifier = Modifier.offset {
                                    IntOffset(
                                        ((position.x + jitter.x) * imageScaleFactor).roundToInt(),
                                        ((position.y + jitter.y) * imageScaleFactor).roundToInt()
                                    )
                                }
                                    .graphicsLayer(
                                        scaleX = leftEye.scaleX * imageScaleFactor,
                                        scaleY = leftEye.scaleY * imageScaleFactor,
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    ),
                                colorFilter = if (glowIntensity > 0) ColorFilter.colorMatrix(colorMatrix) else null
                            )
                        }
                    }

                    val rightPupilPosition = getPupilPosition(
                        pupilFocusPoint, rightEye, loadedRightPupil, loadedRightOpenEye
                    )
                    rightPupilPosition?.let { position ->
                        loadedRightPupil?.let {
                            Image(
                                bitmap = it,
                                contentDescription = Strings.getString(Strings.Keys.RIGHT_PUPIL_CONTENT_DESCRIPTION),
                                modifier = Modifier.offset {
                                    IntOffset(
                                        ((position.x + jitter.x) * imageScaleFactor).roundToInt(),
                                        ((position.y + jitter.y) * imageScaleFactor).roundToInt()
                                    )
                                }
                                    .graphicsLayer(
                                        scaleX = rightEye.scaleX * imageScaleFactor,
                                        scaleY = rightEye.scaleY * imageScaleFactor,
                                        transformOrigin = TransformOrigin(0f, 0f)
                                    ),
                                colorFilter = if (glowIntensity > 0) ColorFilter.colorMatrix(colorMatrix) else null
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculates the position of the pupil within the eye, based on the focus point.
 *
 * @param focusPoint The point in the image where the eye should be looking.
 * @param eyeInfo The information about the eye, including its position, scale, and maximum pupil radius.
 * @param pupilImage The image of the pupil.
 * @param eyeImage The image of the eye.
 * @return The offset of the pupil from the top-left corner of the eye.
 */
private fun getPupilPosition(
    focusPoint: SerializableOffset?,
    eyeInfo: Eye,
    pupilImage: ImageBitmap?,
    eyeImage: ImageBitmap?
): Offset? {
    if (pupilImage == null || eyeImage == null) return null

    val eyeImageWidth = eyeImage.width * eyeInfo.scaleX
    val eyeImageHeight = eyeImage.height * eyeInfo.scaleY

    val eyeCenterX = eyeInfo.position.x + eyeImageWidth / 2f
    val eyeCenterY = eyeInfo.position.y + eyeImageHeight / 2f

    val pupilOffsetX: Float
    val pupilOffsetY: Float

    if (focusPoint == null) {
        pupilOffsetX = 0f
        pupilOffsetY = 0f
    } else {
        val focusRelativeToEyeX = focusPoint.x - eyeCenterX
        val focusRelativeToEyeY = focusPoint.y - eyeCenterY

        val pupilMajorRadius = eyeInfo.maxPupilRadiusX * eyeInfo.scaleX
        val pupilMinorRadius = eyeInfo.maxPupilRadiusY * eyeInfo.scaleY

        if (pupilMajorRadius > 0f && pupilMinorRadius > 0f) {
            val normalizedX = focusRelativeToEyeX / pupilMajorRadius
            val normalizedY = focusRelativeToEyeY / pupilMinorRadius
            val ellipseValue = normalizedX * normalizedX + normalizedY * normalizedY

            if (ellipseValue > 1f) {
                val scale = 1f / sqrt(ellipseValue)
                pupilOffsetX = focusRelativeToEyeX * scale
                pupilOffsetY = focusRelativeToEyeY * scale
            } else {
                pupilOffsetX = focusRelativeToEyeX
                pupilOffsetY = focusRelativeToEyeY
            }
        } else {
            pupilOffsetX = 0f
            pupilOffsetY = 0f
        }
    }

    val pupilWidth = pupilImage.width * eyeInfo.scaleX
    val pupilHeight = pupilImage.height * eyeInfo.scaleY

    return Offset(
        eyeInfo.position.x + (eyeImageWidth - pupilWidth) / 2f + pupilOffsetX,
        eyeInfo.position.y + (eyeImageHeight - pupilHeight) / 2f + pupilOffsetY
    )
}
