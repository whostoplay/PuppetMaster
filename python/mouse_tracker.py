"""
Tracks mouse movement and keyboard hotkeys to send data to a WebSocket server.

This script connects to a WebSocket server to send real-time mouse pointer
coordinates. It also provides a calibration mechanism to define a specific
rectangular area on the screen. When calibrated, the mouse coordinates will be
sent relative to the top-left corner of this area.

Features:
- Connects to a WebSocket server at a configurable URI.
- Listens for global mouse movements and clicks.
- Listens for a global keyboard hotkey to initiate calibration.
- Sends mouse coordinates as JSON messages.
- Sends calibration data (top-left and bottom-right corners) to the server.
- Handles connection errors and provides informative messages.
"""
import asyncio
import json
import websockets
from pynput import mouse, keyboard
from enum import Enum

# --- Configuration ---
WEBSOCKET_URI = "ws://127.0.0.1:8080/mouse"
"""The WebSocket URI of the server to connect to."""

CALIBRATION_HOTKEY = {keyboard.Key.shift, keyboard.Key.f1}
"""The set of keys that must be pressed simultaneously to start calibration."""
# -------------------

class CalibrationStep(Enum):
    """Enumerates the steps involved in the screen calibration process."""
    AWAITING_TOP_LEFT = 1
    AWAITING_BOTTOM_RIGHT = 2

class MouseTracker:
    """
    Manages mouse/keyboard listeners, WebSocket connection, and calibration state.
    """
    def __init__(self, loop: asyncio.AbstractEventLoop):
        """
        Initializes the MouseTracker.

        Args:
            loop: The asyncio event loop to run WebSocket operations on.
        """
        self.loop = loop
        self.calibration_step: CalibrationStep | None = None
        self.top_left: tuple[int, int] | None = None
        self.bottom_right: tuple[int, int] | None = None
        self.websocket: websockets.WebSocketClientProtocol | None = None
        self.current_pressed_keys = set()

    async def connect(self):
        """
        Connects to the WebSocket server and keeps the connection open.

        Handles connection errors and retries. The connection runs until the
        server closes it or an unrecoverable error occurs.
        """
        try:
            async for websocket in websockets.connect(WEBSOCKET_URI):
                print("Connected to server for mouse tracking.")
                self.websocket = websocket
                try:
                    await self.websocket.wait_closed()
                finally:
                    self.websocket = None
        except (ConnectionRefusedError, websockets.exceptions.InvalidURI):
            print("Connection refused. Is the server running?")
        except websockets.exceptions.InvalidStatus as e:
            print(f"Connection failed: {e}. The server rejected the connection.")
            print("Please ensure you have RESTARTED the server after the recent code changes.")


    def send_message_threadsafe(self, message: dict):
        """
        Sends a JSON message to the WebSocket server in a thread-safe manner.

        Args:
            message: A dictionary to be serialized into JSON and sent.
        """
        if self.websocket:
            asyncio.run_coroutine_threadsafe(
                self.websocket.send(json.dumps(message)), self.loop
            )

    def on_move(self, x: int, y: int):
        """
        Callback for mouse movement events from pynput.

        If calibration is complete, it sends coordinates relative to the calibrated
        top-left corner. Otherwise, it sends absolute coordinates.

        Args:
            x: The absolute x-coordinate of the mouse.
            y: The absolute y-coordinate of the mouse.
        """
        final_x, final_y = x, y
        if self.top_left and self.bottom_right:
            tl_x, tl_y = self.top_left
            # Adjust coordinates based on calibration
            final_x = x - tl_x
            final_y = y - tl_y

        # Only send pointer events when not in the middle of calibration
        if self.calibration_step is None:
            self.send_message_threadsafe({"type": "pointer", "x": final_x, "y": final_y})

    def on_click(self, x: int, y: int, button, pressed: bool):
        """
        Callback for mouse click events from pynput.

        Handles the multi-step calibration process on clicks.

        Args:
            x: The x-coordinate of the click.
            y: The y-coordinate of the click.
            button: The mouse button that was clicked.
            pressed: True if the button was pressed, False if released.
        """
        if not pressed or self.calibration_step is None:
            return

        if self.calibration_step == CalibrationStep.AWAITING_TOP_LEFT:
            self.top_left = (x, y)
            print(f"Top-left corner set to {self.top_left}. Please click the BOTTOM-RIGHT corner.")
            self.calibration_step = CalibrationStep.AWAITING_BOTTOM_RIGHT
        elif self.calibration_step == CalibrationStep.AWAITING_BOTTOM_RIGHT:
            self._finalize_calibration(x, y)

    def _finalize_calibration(self, br_x: int, br_y: int):
        """
        Finalizes the calibration process using the second click coordinate.

        Calculates the true top-left and bottom-right corners regardless of click
        order, sends the data to the server, and resets the calibration state.

        Args:
            br_x: The x-coordinate of the second click.
            br_y: The y-coordinate of the second click.
        """
        tl_x, tl_y = self.top_left

        # Ensure top_left is actually top-left and bottom_right is bottom-right
        final_tl_x = min(tl_x, br_x)
        final_br_x = max(tl_x, br_x)
        final_tl_y = min(tl_y, br_y)
        final_br_y = max(tl_y, br_y)

        self.top_left = (final_tl_x, final_tl_y)
        self.bottom_right = (final_br_x, final_br_y)

        print("Bottom-right corner set. Calibration complete.")
        print(f"  Top-Left: {self.top_left}")
        print(f"  Bottom-Right: {self.bottom_right}")

        calibration_data = {
            "type": "calibration",
            "topLeft": {"x": final_tl_x, "y": final_tl_y},
            "bottomRight": {"x": final_br_x, "y": final_br_y}
        }
        self.send_message_threadsafe(calibration_data)
        print("Sent calibration data to server.")
        self.calibration_step = None


    def on_press(self, key):
        """
        Callback for keyboard key press events from pynput.

        Checks for the calibration hotkey combination.

        Args:
            key: The key that was pressed.
        """
        if key in CALIBRATION_HOTKEY:
            self.current_pressed_keys.add(key)
            if self.current_pressed_keys == CALIBRATION_HOTKEY:
                self._start_calibration()

    def _start_calibration(self):
        """Initiates the calibration process."""
        if self.calibration_step is None:
            print("\n--- Starting Calibration ---")
            print("Please click the TOP-LEFT corner of the desired area.")
            self.calibration_step = CalibrationStep.AWAITING_TOP_LEFT
            # Reset previous calibration
            self.top_left = None
            self.bottom_right = None
        else:
            print("Calibration is already in progress. Please click the corners.")


    def on_release(self, key):
        """
        Callback for keyboard key release events from pynput.

        Args:
            key: The key that was released.
        """
        self.current_pressed_keys.discard(key)


async def main():
    """
    The main entry point for the script.

    Initializes the MouseTracker, starts the WebSocket connection, and sets up
    the mouse and keyboard listeners.
    """
    loop = asyncio.get_running_loop()
    tracker = MouseTracker(loop)

    # Start the websocket connection task
    websocket_task = asyncio.create_task(tracker.connect())

    # Use pynput listeners for mouse and keyboard events
    mouse_listener = mouse.Listener(on_move=tracker.on_move, on_click=tracker.on_click)
    keyboard_listener = keyboard.Listener(on_press=tracker.on_press, on_release=tracker.on_release)

    mouse_listener.start()
    keyboard_listener.start()

    try:
        # Keep the main function alive to handle websocket connection
        await websocket_task
    finally:
        print("Stopping listeners...")
        mouse_listener.stop()
        keyboard_listener.stop()


if __name__ == "__main__":
    hotkey_str = ' + '.join(str(k).replace('Key.', '') for k in CALIBRATION_HOTKEY)
    print("Starting mouse tracker...")
    print(f"Press {hotkey_str} to start calibration.")
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nMouse tracker stopped by user.")
