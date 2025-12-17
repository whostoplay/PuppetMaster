import asyncio
import json
import websockets
from pynput import mouse, keyboard

# --- Configuration ---
WEBSOCKET_URI = "ws://127.0.0.1:8080/mouse"
CALIBRATION_HOTKEY = {keyboard.Key.shift, keyboard.Key.f1}
# -------------------

class CalibrationState:
    AWAITING_TOP_LEFT = 1
    AWAITING_BOTTOM_RIGHT = 2

class MouseTracker:
    def __init__(self, loop):
        self.loop = loop
        self.calibration_step = None
        self.top_left = None
        self.bottom_right = None
        self.websocket = None
        self.current_pressed_keys = set()

    async def connect(self):
        try:
            self.websocket = await websockets.connect(WEBSOCKET_URI)
            print("Connected to server for mouse tracking.")
            await self.websocket.wait_closed()
        except (ConnectionRefusedError, websockets.exceptions.InvalidURI):
            print("Connection refused. Is the server running?")
        except websockets.exceptions.InvalidStatus as e:
            print(f"Connection failed: {e}. The server rejected the connection.")
            print("Please ensure you have RESTARTED the server after the recent code changes.")
        finally:
            self.websocket = None

    def send_message_threadsafe(self, message):
        if self.websocket:
            asyncio.run_coroutine_threadsafe(self.websocket.send(json.dumps(message)), self.loop)

    def on_move(self, x, y):
        final_x, final_y = x, y
        if self.top_left and self.bottom_right:
            tl_x, tl_y = self.top_left
            final_x = x - tl_x
            final_y = y - tl_y

        if self.calibration_step is None:
            self.send_message_threadsafe({"type": "pointer", "x": final_x, "y": final_y})

    def on_click(self, x, y, button, pressed):
        if pressed and self.calibration_step is not None:
            if self.calibration_step == CalibrationState.AWAITING_TOP_LEFT:
                self.top_left = (x, y)
                print(f"Top-left corner set to {self.top_left}. Please click the BOTTOM-RIGHT corner.")
                self.calibration_step = CalibrationState.AWAITING_BOTTOM_RIGHT
            elif self.calibration_step == CalibrationState.AWAITING_BOTTOM_RIGHT:
                tl_x, tl_y = self.top_left
                br_x, br_y = x, y

                final_tl_x = min(tl_x, br_x)
                final_br_x = max(tl_x, br_x)
                final_tl_y = min(tl_y, br_y)
                final_br_y = max(tl_y, br_y)

                self.top_left = (final_tl_x, final_tl_y)
                self.bottom_right = (final_br_x, final_br_y)

                print(f"Bottom-right corner set to {(x, y)}.")
                calibration_data = {
                    "type": "calibration",
                    "topLeft": {"x": final_tl_x, "y": final_tl_y},
                    "bottomRight": {"x": final_br_x, "y": final_br_y}
                }
                self.send_message_threadsafe(calibration_data)
                print("Calibration complete. Sent normalized data to server.")
                self.calibration_step = None

    def on_press(self, key):
        if key in CALIBRATION_HOTKEY:
            self.current_pressed_keys.add(key)
            if all(k in self.current_pressed_keys for k in CALIBRATION_HOTKEY):
                if self.calibration_step is None:
                    print("--- Starting Calibration ---")
                    print("Please click the TOP-LEFT corner where you would like your puppet relative to your game screen.")
                    self.calibration_step = CalibrationState.AWAITING_TOP_LEFT
                else:
                    print("Calibration already in progress. Please click the corners.")

    def on_release(self, key):
        try:
            self.current_pressed_keys.remove(key)
        except KeyError:
            pass

async def main():
    loop = asyncio.get_running_loop()
    tracker = MouseTracker(loop)
    
    websocket_task = asyncio.create_task(tracker.connect())

    with mouse.Listener(on_move=tracker.on_move, on_click=tracker.on_click) as m_listener,\
         keyboard.Listener(on_press=tracker.on_press, on_release=tracker.on_release) as k_listener:
        try:
            await websocket_task
        finally:
            m_listener.stop()
            k_listener.stop()

if __name__ == "__main__":
    print("Starting mouse tracker...")
    print(f"Press {' + '.join(str(k) for k in CALIBRATION_HOTKEY)} to start calibration.")
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nMouse tracker stopped.")
