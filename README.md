This is a Kotlin Multiplatform project targeting Android, Desktop (JVM), Server.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

---

# Puppet Master
Puppet Master an Open Source PNG-Tuber tool. Host a whole troupe of puppets, connect them to custom states and volume thresholds, and more importantly, take them ANYWHERE

## KMP
- Puppet Master is Kotlin Multi-Platform Project targeting Desktop and Android with a server backend. 
- Most Features of Puppet Master are designed to work the same on both android and desktop, but some features, like hotkeys, are obviously platform exclusive.
- Use your phone to quickly launch a streaming session, or sit down at a computer and design a fully animated puppet.

## Key Features

### Puppets
- A puppet is your pngtuber, with all of its states, images, addons, and quirks. A .puppet file is a fully standalone zip archive containing everything you need to take a puppet anywhere.
- Control conditions for switching between images, add special effects, and even custom eyes that follow your cursor!
- Easily export and import .puppet files to share a puppet between devices as a zip archive.

### State Conditions 
- Puppet Master features Hotkey driven and Volume based state conditions, allowing you to tie pngs to specific keys or mic thresholds.

### Blinking
- Add a Blink image to any state, and your puppet will automatically start blinking randomly within a customisable range.
- With custom Eye layers, both your custom eyes and your base image will blink.

### Special Effects
- Rescale your puppet, or have it grow and shrink on a loop.
- Set the vibration intensity, perfect for when you switch to yelling mode!
- Apply a custom tint
- Spinning, that's a neat effect. 
- Use the Preview Display to view the outcome of your effects before saving them.
- Apply a Special Effect to as many states as you want using the State Editor
- Updated effects must be re-applied to each puppet state.
  - This is a known bug, but also a good workaround for lacking a duplicate feature.

### Troupes
- A Troupe is an entire collection of puppets, consisting of the entire /uploads directory and ALL puppet info. This is the main save; be careful with it.
- Troupes can easily be renamed, loaded from files, or start from scratch with Create New Troupe (your last troupe will stay saved as long as you don't name the new one the same!)

### Eye Contact Studio
- Add custom Eye layers to your puppet.
- Eyes can contain an optional pupil layer, which can then follow your cursor or focus on the game screen
- Check on the audience so they know you're paying attention.
- Customise the orbits of pupils.
- Scale, rotate, and place your eyes directly onto your png.

# Server Side
- Puppet Master can be run three ways: As a standalone Desktop/Phone application, publishing to a server, or listening to state updates FROM a server. 
- When "Publishing" is selected, the application in front of you will control its own puppet, and the puppet on the server. 
- When simply "Online", the application will listen to the server and update its puppet based on the server's json info.
- When "Offline", the application will simply control its own puppet. 

## KMP Server Setup
- TODO()

## Offloading Your Puppet
- Puppet Master includes a server module to be ran in a docker container. Included alongside the server is a file, "obs.html", and a python script mouse_tracker.py. 
- OBS.html is a local webpage used to load a copy of the Green Screen Studio space. Update the URL in the file to match your server location, point an OBS browser source at the local file and you're all set! (You'll want "obs.html" to be on the same device as your OBS installation, not the server's device.)
- mouse_tracker.py requires setting up a standard python venv and running the script. Follow the on screen instructions, and you'll be able to pass mouse input directly to the server, allowing eye tracking to work without running Puppet Master itself. (You'll want "mouse_tracker.py" and your venv set up on your streaming box.)
- You can also simply run the desktop/android application, match the server IP in the settings, go online, and hit Publish. This will push your puppet state from the client to the server, and then up to OBS. This can be useful to avoid chroma-keying and to force the rest of the UI to always be hidden.

### My Setup, an Example
- OBS on a small, light streaming box that can only really run OBS
- Puppet Master Server on a debian device downstairs, allowing me to connect any client to my streaming box. 
- Puppet Master Android on my phone. Instead of running anything but OBS on my stream box, I can use my phone for puppet master and set it right beside my Snowball microphone. My phone controls the puppet state while my stream box can focus on just running OBS and displaying the capture card output.
- Alternatively, I run Puppet Master desktop directly on my streaming box, and use my Snowball's input instead to control everything. 
- Puppet Master Desktop on my laptop in case I need to quickly make some changes without risking messing up stream. I can update my puppet locally, then quickly publish to the server and let my phone receive the latest troupe info.

### Viseme Detection Flow

1.  **START → Input Audio Frame**
    *   Get the latest `frequencyData` array from the microphone.

2.  **Volume Gate**
    *   Is the total energy below your `SILENCE_THRESHOLD`?
        *   **YES:** Set viseme to `Neutral / Closed`. → **END**
        *   **NO:** Proceed to analysis.

3.  **Viseme Analysis (In Order of Priority)**
    *   **A. Check for Hissing Fricatives ('S', 'Sh')**
        *   *Look for a strong band of high-frequency noise.*
        *   If found between **4000-8000 Hz**: Set viseme to `Fricative / Hiss` ('S' sound). → **END**
        *   If found between **2000-4000 Hz**: Set viseme to `Fricative / Hiss` ('Sh' sound). → **END**
    *   **B. Check for Wide / Smile ('Ee')**
        *   *Look for a "split peak" signature.*
        *   If you see a peak around **~300 Hz** AND another peak around **~2200 Hz**: Set viseme to `Wide / Smile`. → **END**
    *   **C. Check for Lip Bite ('F', 'V')**
        *   *Look for a soft, wide band of noise from 1500-7000 Hz.*
        *   If found **WITH** a low-frequency voicing hum (~150 Hz): Set viseme to `Lip Bite` ('V' sound). → **END**
        *   If found **WITHOUT** a low-frequency hum: Set viseme to `Lip Bite` ('F' sound). → **END**
    *   **D. Check for Pucker / Narrow ('Oo')**
        *   *Look for a single, clean peak in the low frequencies.*
        *   If found around **250-400 Hz** with little energy elsewhere: Set viseme to `Pucker / Narrow`. → **END**
    *   **E. Check for Open Vowels ('Ah', 'O')**
        *   *Look for strong, clear peaks in the mid-range.*
        *   If the strongest peak is **~700-1200 Hz**: Set viseme to `Open` ('Ah' sound). → **END**
        *   If the strongest peak is **~400-800 Hz**: Set viseme to `Open` ('O' sound). → **END**
    *   **F. Check for Tongue-to-Teeth ('L', 'Th')**
        *   *Look for a sustained, weaker peak in the low-mid range.*
        *   If found: Set viseme to `Tongue to Teeth`. → **END**

4.  **Fallback / Default**
    *   If no other rules have matched, the sound is likely an indistinct vowel or a quick consonant like 'M', 'B', or 'P'.
    *   **Default to:** `Neutral / Closed`. → **END**
