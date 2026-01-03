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

The viseme detection system uses the microphone's frequency data to determine the most likely facial shape (viseme) for a given sound. The core of this system is a powerful rule editor that allows you to visually define the acoustic properties of each phoneme, moving beyond simple volume detection.
 
1. **The Rule Editor**
   * For each PhonemeMatchNode in your puppet's state machine, you can define a VisemeRule. This rule is a collection of conditions, which are represented by boxes drawn on the frequency graph.
  
   * AND Boxes (White): These define the frequencies and volumes that must contain audio energy (a "peak") for the rule to be considered a match. You can have multiple AND boxes to define complex sounds with multiple formants (e.g., the 'Ee' sound). By default, if a rule has one or fewer conditions, drawing a new box will replace the old one.
   
   * Adding to a Rule: By selecting the + (Add) tool, you can draw additional AND boxes to an existing rule. If a rule already has multiple conditions, the tool will smartly default to ADD mode to prevent you from accidentally deleting your work.
   
   * NOT Boxes (Red): These define areas that must not contain any audio energy. They act as a veto or a "cutout" tool, allowing you to refine your rules and prevent similar-sounding phonemes from being incorrectly triggered. You can create these by selecting the - (Subtract) tool.
   
   * Editing Conditions: Click on any existing AND or NOT box to select it. This will allow you to modify its Required Hits count or delete it entirely.

2. **Intersection Logic: "The Smaller Box Wins"**
   * When AND and NOT boxes overlap, the system uses an intuitive "smaller box wins" logic to resolve conflicts. This allows for incredibly detailed rule creation, similar to boolean operations in 3D modeling or vector art software.
   
   * Cutouts: If you draw a small NOT box inside a larger AND box, the NOT box takes precedence in that intersection. This is perfect for creating "dead zones" in your rule, like for the silence between phonemes in a sound like "LAH".
   
   * Refinements: If you draw a small AND box inside a larger NOT box, the AND box wins. This allows you to exclude a broad range of frequencies while specifically targeting a very narrow, precise sound within that range.
   
3. **Real-time Visualization**

   * The frequency graph provides rich, real-time feedback to help you tune your rules:
   
   * Gray Dots: Background audio peaks that are not currently matching any part of your rule.
   
   * Green Path: When peaks fall inside an AND box, they are connected by a solid green line. This instantly shows you the shape of the sound your rule is capturing.
   
   * Magenta Dots: When a peak falls inside a NOT box, it is drawn as a prominent magenta dot. This clearly indicates that a "veto" condition has been met.
   
4. **Execution Flow**
   1. Volume Gate: If the total volume is below the SILENCE_THRESHOLD, the viseme is immediately set to Neutral / Closed.
   
   2. Peak Detection: The system analyzes the frequency data to find all significant peaks of audio energy.
   3. Rule Evaluation: A rule is considered a match if, and only if:
         * For every AND box in the rule, at least the Required Hits number of peaks are found inside it.
         * For every NOT box in the rule, zero peaks are found inside it.
         * The "Smaller Box Wins" logic is used to resolve any overlaps between AND and NOT boxes before the final count.

5. **Priority & Fallback:** 
   * If multiple rules match simultaneously, the one with the higher "Branch Priority" (set in the state editor) is chosen.

6. **Viseme Analysis (Encouraged Priority)**
   * A. Hissing Fricatives ('S', 'Sh')
       * 'S': Use a single, large AND box in the high-frequency range **(4000-8000 Hz)**.
       * 'Sh': Use a single AND box in the mid-high range **(2000-4000 Hz)**. To prevent 'S' from triggering it, add a NOT box in the 'S' range above **(4000 Hz)**.

   * B. Lip Bite ('F', 'V')
       * This is a great use of AND and NOT. Start by creating a rule for 'F': draw one large AND box for the soft, noisy fricative sound from **(1500-7000 Hz)**.
       * Now create a separate rule for 'V'. Use the same large AND box **(1500-7000 Hz)**, but use the + tool to add a second, small AND box to catch the low-frequency voicing hum around ~150 Hz. The 'V' rule will only trigger if both the hiss and the hum are present.

   * C. Closed Mouth / Murmur ('M')
       * Draw a tight AND box in the very low "hum" frequencies **(100-250 Hz)**.
       * To ensure it's a closed-mouth sound, draw a large NOT box covering all the mid-to-high frequencies **(e.g., 500-8000 Hz)**. This prevents open-mouth vowels from triggering the 'M' rule.
       * *Note: The 'M' sound is often sustained. Connect the output of this Node to a DelayTimerNode with a short delay **(e.g., 100-200ms)**. This creates a "sustained M" detector that won't fire on quick, non-M sounds that happen to have low-frequency energy.*
   
   * D. Wide / Smile ('Ee')
      * This is the classic example for multiple AND boxes. Draw one AND box for the low-frequency peak **(~300 Hz)** and a second AND box for the high-frequency peak **(~2200 Hz)**. The rule will only match if peaks are found in both boxes simultaneously.

   * E. Pucker / Narrow ('Oo')
     * Start with an AND box in the low frequencies **(250-400 Hz)**.
     * To refine it and prevent confusion with 'W'/'R', add a NOT box in the mid-range **(800-1300 Hz)**. This ensures the sound is a pure, single low-frequency peak.

   * F. Rounded Lips ('W', 'R')
     * Create a rule with two AND boxes: one in the low range **(250-400 Hz)** and another in the mid-range (800-1300 Hz). This explicitly looks for the two-peak signature that separates it from 'Oo'.

   * G. Open Vowels ('Ah', 'O')
     * 'Ah': Create a rule with one large AND box targeting the strong mid-range peak between **(~700-1200)** Hz.
     * 'O': Create a separate rule with an AND box targeting the lower-mid range peak between **(~400-800)** Hz.
     * *You can use NOT boxes in each rule to exclude the other's primary range and increase accuracy.*
   
   * H. Tongue-to-Teeth ('L', 'Th') - Detecting Sustained Tones
     * The 'L' and 'Th' sounds are defined by being sustained.
     * Create a PhonemeMatchNode with a single AND box in the low-mid range **(300-800 Hz)**. 
     * *Add a Delay. Connect the output of the PhonemeMatchNode to the input of a DelayTimerNode. Set a short delay, like 200ms.*

