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


New Architecture: The Hybrid Model
1.
Client as the "Configuration Center": The frontend applications (desktop/Android) are where the user designs their avatar. This includes:
◦
Creating states (e.g., "idle", "talking", "shocked").
◦
Uploading the image for each state to the server.
◦
Defining behavior rules (e.g., "When my microphone volume is over 20%, switch to the 'talking' state").
◦
Saving/loading these complete avatar configurations locally on the client device.
◦
A "Go Live" or "Publish" button that sends the entire configuration (the list of states, their associated images, and the behavior rules) to the server.
2.
Server as the "Autonomous Host": The server's role is now much smarter.
◦
It receives and persists the complete avatar configuration from the client.
◦
It independently listens for real-time input, like audio from OBS.
◦
It runs its own logic (based on the configuration it received) to decide which state should be active. For example, it will analyze the audio stream and switch between "idle" and "talking" states automatically.
◦
It continues to broadcast the URL of the currently active image to OBS via the /obs websocket.