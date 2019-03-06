# InformedConsentMonitor

This is an android application aimed at monitoring the patient body reaction an stress level during the reading of the informed consent.
The informed consent itself is shown to the user through a webapp (https://github.com/Heartbound201/informed-consent-v2) and displayed on the webview of the android device. An Internet connection is therefore required.
Meanwhile the body response is monitored through a device (Shimmer3 GSR+ Unit) which measures skin conductance and heart rate. The connection between the sensor device and the android one is a bluetooth connection.
Data from the sensor device is paired from data extracted from the webapp itself, that is what paragraph is currently displayed on the monitor and where is the patient looking at, in order to associate the stress level to a specific paragraph.
The Eye tracking is achieved using the webgazer library (https://github.com/brownhci/WebGazer)
All the data is saved locally on a SQLite server and can exported as .csv files.

## Installation

The build output is an .apk file which can directly installed on your android device. The android version required is KitKat or newer, also remember to enable the apk installation from unknown sources from the security settings.
Alternatively, you can import this project on Android Studio and run the Main Activity, doing this will automatically build and install the apk on a usb connected device (you must enable usb debugging in the developer settings of that device) 

Once installed, on the first startup, the application will require permission to access the device camera and storage. Those are in order to use the webgazer library and to save .csv files on your device storage.

At the startup, if bluetooth is active, the application will prompt the user with a dialog to allow the connection with the Shimmer3 sensor device. If the connection fails, a button allows to retry.
