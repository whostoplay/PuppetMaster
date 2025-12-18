
    const puppetElement = document.getElementById('puppet');
    const leftEyeElement = document.getElementById('left-eye');
    const rightEyeElement = document.getElementById('right-eye');
    const leftPupilElement = document.getElementById('left-pupil');
    const rightPupilElement = document.getElementById('right-pupil');
    const socket = new WebSocket('ws://127.0.0.1:8080/obs');

    let pointerPosition = null;
    let currentPuppetStateInfo = null;
    let currentAnimationState = null;
    let baseImage = { width: 0, height: 0, src: '' };

    // State for eyes and pupils
    let leftEyeImage = { width: 0, height: 0 };
    let rightEyeImage = { width: 0, height: 0 };
    let leftPupilImage = { width: 0, height: 0 };
    let rightPupilImage = { width: 0, height: 0 };

    // State for dynamic behaviors
    let lastTime = Date.now();
    let isCheckingAudience = false;
    let audienceCheckTimer = 0;
    let audienceCheckEndTime = 0;

    let jitter = { x: 0, y: 0 };
    let jitterTimer = 0;

    socket.onmessage = function(event) {
        const serverState = JSON.parse(event.data);
        currentPuppetStateInfo = serverState.puppetStateInfo;
        if (serverState.puppetStateInfo && serverState.puppetStateInfo.eyeState) {
            pointerPosition = serverState.puppetStateInfo.eyeState.cursorPosition;
        } else {
            pointerPosition = null;
        }
        currentAnimationState = serverState.animationState;

        if (!currentPuppetStateInfo || !currentPuppetStateInfo.imageName) {
            puppetElement.style.backgroundImage = '';
            leftEyeElement.style.display = 'none';
            rightEyeElement.style.display = 'none';
            return;
        }

        // Update main puppet image
        const newImgSrc = 'http://127.0.0.1:8080/uploads/' + currentPuppetStateInfo.imageName;
        if (baseImage.src !== newImgSrc) {
            const newImg = new Image();
            newImg.onload = () => {
                baseImage = { width: newImg.width, height: newImg.height, src: newImgSrc };
                puppetElement.style.backgroundImage = 'url(' + newImgSrc + ')';
            }
            newImg.src = newImgSrc;
        }

        // Update eye images and visibility
        updateEyeImages(currentPuppetStateInfo);
    };

    function updateEyeImages(puppetStateInfo) {
        const eyeState = puppetStateInfo.eyeState;
        if (!eyeState) {
            leftEyeElement.style.display = 'none';
            rightEyeElement.style.display = 'none';
            return;
        }

        leftEyeElement.style.display = 'block';
        rightEyeElement.style.display = 'block';

        const isBlinking = puppetStateInfo.imageName === puppetStateInfo.blinkImageName;
        const leftEye = eyeState.eyes.left;
        const rightEye = eyeState.eyes.right;

        const updateImage = (element, pupilElement, eyeData, imageCache, pupilCache, imagePath) => {
            const newSrc = 'http://127.0.0.1:8080/uploads/' + imagePath;
            if (element.style.backgroundImage !== 'url("' + newSrc + '")') {
                 const img = new Image();
                 img.onload = () => {
                    imageCache.width = img.width;
                    imageCache.height = img.height;
                    element.style.backgroundImage = 'url(' + newSrc + ')';
                 }
                 img.src = newSrc;
            }
            if(pupilElement) pupilElement.style.display = eyeData.pupil ? 'block' : 'none';
        };

        if (isBlinking && leftEye.closedState && rightEye.closedState) {
            updateImage(leftEyeElement, null, leftEye, leftEyeImage, null, leftEye.closedState);
            updateImage(rightEyeElement, null, rightEye, rightEyeImage, null, rightEye.closedState);
            leftPupilElement.style.display = 'none';
            rightPupilElement.style.display = 'none';
        } else {
            updateImage(leftEyeElement, leftPupilElement, leftEye, leftEyeImage, leftPupilImage, leftEye.openState);
            updateImage(rightEyeElement, rightPupilElement, rightEye, rightEyeImage, rightPupilImage, rightEye.openState);
            if(leftEye.pupil) updateImage(leftPupilElement, null, leftEye, leftPupilImage, null, leftEye.pupil);
            if(rightEye.pupil) updateImage(rightPupilElement, null, rightEye, rightPupilImage, null, rightEye.pupil);
        }
    }

    function updatePupilPositions(rotation = 0, imageScaleFactor, imageTopLeftX, imageTopLeftY, scaledWidth, scaledHeight) {
        if (!currentPuppetStateInfo?.eyeState || !baseImage.width) return;

        const eyeState = currentPuppetStateInfo.eyeState.eyes;
        let focusPointOnScreen = null;

        if (!isCheckingAudience) {
            if (eyeState.focusOnGame) {
                 focusPointOnScreen = {
                    x: imageTopLeftX + (eyeState.gameScreenLocation.x * scaledWidth),
                    y: imageTopLeftY + (eyeState.gameScreenLocation.y * scaledHeight)
                };
            } else if (eyeState.followCursor && pointerPosition) {
                focusPointOnScreen = pointerPosition;
            }
        }

        let finalFocusPointInImage = null;
        if (focusPointOnScreen) {
            const pointInImageXUnrotated = (focusPointOnScreen.x - imageTopLeftX) / imageScaleFactor;
            const pointInImageYUnrotated = (focusPointOnScreen.y - imageTopLeftY) / imageScaleFactor;

            const rotationInRadians = -rotation * (Math.PI / 180);
            const imageCenterX = baseImage.width / 2;
            const imageCenterY = baseImage.height / 2;
            const pointRelToCenterX = pointInImageXUnrotated - imageCenterX;
            const pointRelToCenterY = pointInImageYUnrotated - imageCenterY;

            const cosAngle = Math.cos(rotationInRadians);
            const sinAngle = Math.sin(rotationInRadians);

            const rotatedPointRelToCenterX = pointRelToCenterX * cosAngle - pointRelToCenterY * sinAngle;
            const rotatedPointRelToCenterY = pointRelToCenterX * sinAngle + pointRelToCenterY * cosAngle;

            finalFocusPointInImage = {
                x: rotatedPointRelToCenterX + imageCenterX,
                y: rotatedPointRelToCenterY + imageCenterY
            };
        }

        const positionPupil = (pupilEl, eyeData) => {
             let targetX = 0;
             let targetY = 0;
             if(finalFocusPointInImage){
                const angle = Math.atan2(
                    (finalFocusPointInImage.y + jitter.y) - eyeData.position.y,
                    (finalFocusPointInImage.x + jitter.x) - eyeData.position.x
                );
                targetX = Math.cos(angle) * (eyeData.maxPupilRadiusX * eyeData.scaleX);
                targetY = Math.sin(angle) * (eyeData.maxPupilRadiusY * eyeData.scaleY);
             }
             pupilEl.style.transform = 'translate(' + (targetX * imageScaleFactor) + 'px, ' + (targetY * imageScaleFactor) + 'px)';
        };

        positionPupil(leftPupilElement, eyeState.left);
        positionPupil(rightPupilElement, eyeState.right);
    }

    function setElementBase(element, eyeData, imageCache, pupilCache, imageScaleFactor, imageTopLeftX, imageTopLeftY) {
        if (!eyeData) return;
        element.style.left = (imageTopLeftX + (eyeData.position.x * imageScaleFactor)) + 'px';
        element.style.top = (imageTopLeftY + (eyeData.position.y * imageScaleFactor)) + 'px';
        element.style.width = (imageCache.width * eyeData.scaleX * imageScaleFactor) + 'px';
        element.style.height = (imageCache.height * eyeData.scaleY * imageScaleFactor) + 'px';

        if (eyeData.pupil) {
             const pupilEl = element.querySelector('.pupil');
             if (pupilEl) {
                pupilEl.style.width = (pupilCache.width * eyeData.scaleX * imageScaleFactor) + 'px';
                pupilEl.style.height = (pupilCache.height * eyeData.scaleY * imageScaleFactor) + 'px';
             }
        }
    }

    function animate() {
        requestAnimationFrame(animate);
        const now = Date.now();
        const deltaTime = now - lastTime;
        lastTime = now;

        let rotation = 0;
        const eyeState = currentPuppetStateInfo?.eyeState;
        let imageScaleFactor = 1, imageTopLeftX = 0, imageTopLeftY = 0, scaledWidth = 0, scaledHeight = 0;

        if (currentPuppetStateInfo && eyeState && baseImage.width > 0) {
            const containerWidth = puppetElement.clientWidth;
            const containerHeight = puppetElement.clientHeight;

            imageScaleFactor = Math.min(containerWidth / baseImage.width, containerHeight / baseImage.height);
            scaledWidth = baseImage.width * imageScaleFactor;
            scaledHeight = baseImage.height * imageScaleFactor;

            imageTopLeftX = (containerWidth - scaledWidth) / 2;
            imageTopLeftY = (containerHeight - scaledHeight) / 2;

            setElementBase(leftEyeElement, eyeState.eyes.left, leftEyeImage, leftPupilImage, imageScaleFactor, imageTopLeftX, imageTopLeftY);
            setElementBase(rightEyeElement, eyeState.eyes.right, rightEyeImage, rightPupilImage, imageScaleFactor, imageTopLeftX, imageTopLeftY);
        }

        if (eyeState?.eyes) {
            // Audience Check Logic
            if (eyeState.eyes.checkOnAudience) {
                if (isCheckingAudience) {
                    if (now >= audienceCheckEndTime) {
                        isCheckingAudience = false;
                        audienceCheckTimer = 0;
                    }
                } else {
                    audienceCheckTimer += deltaTime;
                    if (audienceCheckTimer >= eyeState.eyes.audienceCheckRate) {
                        isCheckingAudience = true;
                        audienceCheckEndTime = now + eyeState.eyes.audienceCheckDuration;
                    }
                }
            }

            // Game Focus Jitter Logic
            if (eyeState.eyes.focusOnGame && !isCheckingAudience) {
                jitterTimer += deltaTime;
                if (jitterTimer > 100) {
                    jitterTimer = 0;
                    const randomAngle = Math.random() * 2 * Math.PI;
                    const randomRadius = Math.random() * 10;
                    jitter.x = Math.cos(randomAngle) * randomRadius;
                    jitter.y = Math.sin(randomAngle) * randomRadius;
                }
            } else {
                jitter.x = 0;
                jitter.y = 0;
            }
        }


        if (currentAnimationState) {
            const scaleX = currentAnimationState.scaleX;
            const scaleY = currentAnimationState.scaleY;
            rotation = currentAnimationState.rotation;
            const translationX = currentAnimationState.translationX;
            const translationY = currentAnimationState.translationY;

            puppetElement.style.transform = 'scale(' + scaleX + ', ' + scaleY + ') rotate(' + rotation + 'deg) translate(' + translationX + 'px, ' + translationY + 'px)';

            const glowIntensity = currentAnimationState.glowIntensity;
            if (glowIntensity > 0) {
                const glowColor = currentAnimationState.glowColor;
                const colorMatrixElement = document.getElementById('color-matrix');
                const r = ((glowColor >> 16) & 0xFF) / 255;
                const g = ((glowColor >> 8) & 0xFF) / 255;
                const b = (glowColor & 0xFF) / 255;

                const matrix = [
                    glowIntensity * r, 0, 0, 0, r * 0.2,
                    0, glowIntensity * g, 0, 0, g * 0.2,
                    0, 0, glowIntensity * b, 0, b * 0.2,
                    0, 0, 0, 1, 0
                ].join(' ');

                colorMatrixElement.setAttribute('values', matrix);
                puppetElement.style.filter = 'url(#color-tint-filter)';
            } else {
                puppetElement.style.filter = 'none';
            }
        } else {
            puppetElement.style.transform = 'scale(1) rotate(0deg) translate(0px, 0px)';
            puppetElement.style.filter = 'none';
        }

        updatePupilPositions(rotation, imageScaleFactor, imageTopLeftX, imageTopLeftY, scaledWidth, scaledHeight);
    }

    animate();
