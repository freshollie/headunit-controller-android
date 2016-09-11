# AndroidHeadunitController
This is an app I have written to turn my tablet into an automated android head unit, IE the tablet acts exactly the same way an headunit would.

This app will only work on my setup and is very proprietary.

What it does
------------

When the power is connected the tablet:

* Plays a blank music track (to stop noise on my setup)
* Starts a service which makes the screen the correct brightness for the current time
* Starts a USB GPS service (in built GPS does not work well)
* Starts the last playing media app

It does all of this after making sure all of the USB devices have been connected properly

Apps compatible
---------

* AutoBright
* My own build of USB GPS (https://github.com/HvB/UsbGps4Droid)
* Apple Music
* Pocket Casts
* DAB Radio (https://github.com/freshollie/MonkeyboardAndroidRadioApp)
