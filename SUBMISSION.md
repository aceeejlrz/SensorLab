# COMP3074 – Sensors Lab (SensorLab)

## Implementation summary

- `MainActivity` implements `SensorEventListener` and gets `SensorManager` in `onCreate()`.
- `TYPE_AMBIENT_TEMPERATURE` and `TYPE_GYROSCOPE` are requested with `getDefaultSensor()` and stored as nullable `Sensor?`.
- Listeners are registered in `onResume()` (only for sensors that are present) and unregistered in `onPause()`.
- Readings are stored in `mutableStateOf` fields, so the Compose UI recomposes whenever a `SensorEvent` arrives.
- If a sensor is missing, the UI shows "Temperature sensor is not available." / "Gyroscope is not available." instead of crashing.
- Temperature readings outside the sensor's own `maximumRange` are ignored. The emulator sends a placeholder of about -1e30 °C before it has a real value.

### Part 18 – Independent practice (extensions completed)

1. **Visual threshold indicator:** the gyroscope card shows "Still" or "ROTATING" with the angular speed `sqrt(x² + y² + z²)`. It turns red above 0.5 rad/s (`GYRO_MOVEMENT_THRESHOLD`).
2. **Sensor properties:** each available sensor's card shows its name, vendor, maximum range and resolution.

## Sensor availability on test device

| Device | Ambient temperature | Gyroscope |
|---|---|---|
| Android Emulator (Medium_Phone) | Available (Goldfish Ambient Temperature sensor) | Available (Goldfish 3-axis Gyroscope) |
| Physical device: _fill in_ | _Available / Not available_ | _Available / Not available_ |

Add the screenshot from your physical device here.

## Reflection questions

**1. Why must `getDefaultSensor()` be treated as potentially returning null?**
Android does not require a device to have every sensor type. When the requested hardware (or software sensor) is not on the device, `getDefaultSensor()` returns `null`. Using that result without a null check would throw a `NullPointerException`, for example when registering a listener. Storing the result as `Sensor?` and using `?.let { }` lets the app run normally and show an "unavailable" message instead.

**2. Why should sensor listeners be unregistered when the activity is paused?**
The system keeps sending events to a registered listener even when the app is not visible. That keeps the sensor hardware powered and wakes the CPU for callbacks nobody sees, which drains the battery. Unregistering in `onPause()` and registering again in `onResume()` means the app only listens while its data is on screen. It also avoids leaking the activity through the listener reference.

**3. What is the difference between gyroscope angular velocity and device orientation?**
The gyroscope measures how fast the device is rotating around each axis right now. Orientation is the angle the device is currently at (for example, tilted 30°). A device held still at any tilt gives gyroscope values near 0. Orientation would come from integrating angular velocity over time (which drifts), or from combining the accelerometer and magnetometer (e.g. `TYPE_ROTATION_VECTOR`).

**4. What units are used for the gyroscope values in this lab?**
Radians per second (rad/s) around the X, Y and Z axes (`values[0]`, `values[1]`, `values[2]`).

**5. Why is battery temperature not a valid replacement for `TYPE_AMBIENT_TEMPERATURE`?**
Battery temperature is the temperature of the battery inside the phone. Charging, CPU/GPU load, the screen and the user's hand all heat it, so it is usually well above room temperature and changes with how the phone is used. `TYPE_AMBIENT_TEMPERATURE` measures the surrounding air in °C. Using battery temperature instead would give wrong data presented as an ambient reading.

**6. How does `mutableStateOf` allow a `SensorEvent` callback to update the Compose UI?**
`mutableStateOf` creates an observable state holder. When a composable reads it (here, inside `setContent`), Compose records that read. When `onSensorChanged()` assigns a new value, the snapshot system marks that state as changed and schedules recomposition for every composable that read it. `SensorScreen` then redraws with the new values. A plain `var` would update the value, but Compose would not know to redraw.
