# Android Headunit Controller
Android application designed to be used along side Timurs Kernel to control an android tablet as a headunit.

[Latest Build](/app/build/outputs/apk) (Minimum android version 5.0)

## Functionality

This application is designed to replace Tasker in the use of an in car tablet. One of the key functionalities being a wakelock and continue playing the last playing media, which were not possible with tasker. The app also has a much smaller overhead than tasker.

It has several customisable features which can be turned on and off at the users will.

### Features 

- Set volume to specific level on wake
- Continue playing the last media app on wake
- Start Shuttle Xpress driver on wake
- Launch USB Gps driver on wake
- Run shell script on wake or suspend
- Keep the tablet screen bright while power is applied
- Keep speakers active to avoid ground loop interferance
- Keep autobright alive in the background
- Kill navigation 5 minutes after suspend (Stops wakelocks)

### Shuttle Xpress Input

The app uses my [Shuttle Xpress Driver library](https://github.com/freshollie/ShuttleXpressDriver-Android) providing an interface for the user to map all the inputs of the device to specific actions on the tablet.

<p align="center">
    <img src="https://github.com/freshollie/AndroidHeadunitController/raw/master/screenshots/input_screen.png" alt="input interface" width="800"/>
</p>

<p align="center">
    <img src="https://github.com/freshollie/AndroidHeadunitController/raw/master/screenshots/mapping.png" alt="mapping interface" width="800"/>
</p>

## Contributing

Feel free to contribute by forking the project and submitting pull requests of changes.

Any bugs please submit an issue in the "Issues" section.

## Screenshots

<p align="center">
    <img src="https://github.com/freshollie/AndroidHeadunitController/raw/master/screenshots/wake_settings.png" alt="wake up interface" width="800"/>
</p>

<p align="center">
    <img src="https://github.com/freshollie/AndroidHeadunitController/raw/master/screenshots/info_screen.png" alt="info interface" width="800"/>
</p>

<p align="center">
    <img src="https://github.com/freshollie/AndroidHeadunitController/raw/master/screenshots/notifications.png" alt="Running" width="800"/>
</p>