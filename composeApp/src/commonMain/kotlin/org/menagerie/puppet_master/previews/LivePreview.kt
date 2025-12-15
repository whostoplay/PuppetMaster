package org.menagerie.puppet_master.previews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import org.menagerie.puppet_master.ActiveSpecialEffect
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.SERVER_PORT
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.rememberGlobalPointerPosition
import org.menagerie.puppet_master.rememberImageFromUrl

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
    window: Any?,
    displayedImageName: String?,
    idleImage: ImageBitmap,
    onFocusPointUpdate: (SerializableOffset?) -> Unit,
) {

    var frame by remember { mutableLongStateOf(0L) }
    var jitter by remember { mutableStateOf(Offset.Zero) }
    var isCheckingAudience by remember { mutableStateOf(false) }
    val pointerPosition = rememberGlobalPointerPosition(window)

    val eyeState = puppetState?.eyeState

    fun getImageUrl(imageName: String?): String? {
        return when {
            imageName.isNullOrBlank() -> null
            operatingMode == OperatingMode.ONLINE -> "http://$serverIp:$SERVER_PORT/uploads/$imageName"
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

    // --- General Effects ---
    LaunchedEffect(activeSpecialEffect) {
        if (activeSpecialEffect != null) {
            while (true) {
                frame = System.currentTimeMillis()
                delay(16) // roughly 60 fps
            }
        }
    }

    LaunchedEffect(
        eyeState?.eyes?.checkOnAudience,
        eyeState?.eyes?.audienceCheckRate,
        eyeState?.eyes?.audienceCheckDuration,
        isAudienceCheckForced
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
                val randomAngle = Random.nextFloat()  * Math.PI
                val randomRadius = Random.nextFloat() * 5f
                jitter = Offset(
                    x = (cos(randomAngle) * randomRadius).toFloat(),
                    y = (sin(randomAngle) * randomRadius).toFloat()
                )
                delay(250)
            }
        } else {
            jitter = Offset.Zero
        }
    }

    // --- Rendering ---
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(backgroundColor).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val image = (if (isBlinking) loadedBlinkBody else loadedBody) ?: idleImage

        val offset = activeSpecialEffect?.getVibrationOffset(maxWidth.value / 20f)
        val glowColor = activeSpecialEffect?.getGlowColor()?.let { Color(it) } ?: Color.White
        val glowIntensity = activeSpecialEffect?.getGlow() ?: 1f

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
            scaleX = activeSpecialEffect?.getScaleX() ?: 1f,
            scaleY = activeSpecialEffect?.getScaleY() ?: 1f,
            rotationZ = activeSpecialEffect?.getRotation() ?: 0f,
            translationX = offset?.x ?: 0f,
            translationY = offset?.y ?: 0f,
            shadowElevation = glowIntensity * 30f,
            ambientShadowColor = glowColor,
            spotShadowColor = glowColor
        ).let { if (frame > 0) it else it } // force recomposition

        Box(
            modifier = Modifier.size(scaledWidth, scaledHeight).then(puppetModifier)
        ) {
            Image(
                bitmap = image,
                contentDescription = "Live Preview",
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            eyeState?.let { eyeData ->
                val leftEye = eyeData.eyes.left
                val rightEye = eyeData.eyes.right

                val leftEyeModifier = Modifier
                    .offset(x = (leftEye.position.x * imageScaleFactor).dp, y = (leftEye.position.y * imageScaleFactor).dp)
                    .graphicsLayer(
                        scaleX = leftEye.scaleX * imageScaleFactor,
                        scaleY = leftEye.scaleY * imageScaleFactor,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )

                val rightEyeModifier = Modifier
                    .offset(x = (rightEye.position.x * imageScaleFactor).dp, y = (rightEye.position.y * imageScaleFactor).dp)
                    .graphicsLayer(
                        scaleX = rightEye.scaleX * imageScaleFactor,
                        scaleY = rightEye.scaleY * imageScaleFactor,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )

                val leftEyeToDisplay = if (isBlinking) loadedLeftClosedEye else loadedLeftOpenEye
                leftEyeToDisplay?.let {
                    Image(bitmap = it, contentDescription = "Left Eye", colorFilter = ColorFilter.colorMatrix(colorMatrix), modifier = leftEyeModifier)
                }

                val rightEyeToDisplay = if (isBlinking) loadedRightClosedEye else loadedRightOpenEye
                rightEyeToDisplay?.let {
                    Image(bitmap = it, contentDescription = "Right Eye", colorFilter = ColorFilter.colorMatrix(colorMatrix), modifier = rightEyeModifier)
                }

                if (!isBlinking) {
                    val focusPointOnScreen: Offset? = when {
                        isCheckingAudience || isAudienceCheckForced -> null
                        eyeData.eyes.focusOnGame -> Offset(x = imageTopLeftX + (eyeData.eyes.gameScreenLocation.x * scaledWidthPx), y = imageTopLeftY + (eyeData.eyes.gameScreenLocation.y * scaledHeightPx))
                        eyeData.eyes.followCursor -> {
                            if (operatingMode == OperatingMode.ONLINE) {
                                eyeData.cursorPosition?.let { Offset(it.x, it.y) }
                            } else {
                                pointerPosition
                            }
                        }
                        else -> null
                    }

                    val finalFocusPointInImage: SerializableOffset? = if (focusPointOnScreen != null) {
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
                            val rotatedPointRelToCenterX = pointRelToCenterX * cosAngle - pointRelToCenterY * sinAngle
                            val rotatedPointRelToCenterY = pointRelToCenterX * sinAngle + pointRelToCenterY * cosAngle
                            val finalPointInImageX = rotatedPointRelToCenterX + imageCenterX
                            val finalPointInImageY = rotatedPointRelToCenterY + imageCenterY
                            SerializableOffset(finalPointInImageX, finalPointInImageY)
                        }
                    } else {
                        null
                    }

                    onFocusPointUpdate(pointerPosition?.let { SerializableOffset(it.x, it.y) })

                    loadedLeftPupil?.let { pupilBitmap ->
                        val leftPupilAngle = if (finalFocusPointInImage != null) {
                            atan2((finalFocusPointInImage.y - leftEye.position.y).toDouble(), (finalFocusPointInImage.x - leftEye.position.x).toDouble()).toFloat()
                        } else {
                            0f
                        }
                        var pupilModifier = leftEyeModifier
                        if (finalFocusPointInImage != null) {
                            val x = leftEye.position.x + cos(leftPupilAngle) * (leftEye.maxPupilRadiusX * leftEye.scaleX)
                            val y = leftEye.position.y + sin(leftPupilAngle) * (leftEye.maxPupilRadiusY * leftEye.scaleY)

                            pupilModifier = Modifier.offset(
                                x = ((x + jitter.x) * imageScaleFactor).dp,
                                y = ((y + jitter.y) * imageScaleFactor).dp
                            )
                                .graphicsLayer(
                                    scaleX = leftEye.scaleX * imageScaleFactor,
                                    scaleY = leftEye.scaleY * imageScaleFactor,
                                    transformOrigin = TransformOrigin(0f, 0f)
                                )
                        }
                        Image(bitmap = pupilBitmap, contentDescription = "Left Pupil", colorFilter = ColorFilter.colorMatrix(colorMatrix), modifier = pupilModifier)
                    }

                    loadedRightPupil?.let { pupilBitmap ->
                        val rightPupilAngle = if (finalFocusPointInImage != null) {
                            atan2((finalFocusPointInImage.y - rightEye.position.y).toDouble(), (finalFocusPointInImage.x - rightEye.position.x).toDouble()).toFloat()
                        } else {
                            0f
                        }
                        var pupilModifier = rightEyeModifier
                        if (finalFocusPointInImage != null) {
                            val x = rightEye.position.x + cos(rightPupilAngle) * (rightEye.maxPupilRadiusX * rightEye.scaleX)
                            val y = rightEye.position.y + sin(rightPupilAngle) * (rightEye.maxPupilRadiusY * rightEye.scaleY)

                            pupilModifier = Modifier.offset(
                                x = ((x + jitter.x) * imageScaleFactor).dp,
                                y = ((y + jitter.y) * imageScaleFactor).dp
                            )
                                .graphicsLayer(
                                    scaleX = rightEye.scaleX * imageScaleFactor,
                                    scaleY = rightEye.scaleY * imageScaleFactor,
                                    transformOrigin = TransformOrigin(0f, 0f)
                                )
                        }
                        Image(bitmap = pupilBitmap, contentDescription = "Right Pupil", colorFilter = ColorFilter.colorMatrix(colorMatrix), modifier = pupilModifier)
                    }
                }
            }
        }
    }
}
