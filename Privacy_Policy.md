# Privacy Policy for Opengur (February 25, 2017)

Welcome to the privacy policy for Opengur. This will explain the type of data we collect and what we do with it and the permissions
we request within the app and why we need them.


###Information we collect
The information collected in Opengur is minimal. 
- <b>Crash data using Crashlytics(Fabric)</b>. When using Opengur, if the app were to crash, data is collected from the device and sent to
Crashlytics for review. The data collected in this process is where within the app crashed, what type of device you were using, including
Make, model, Android version, orientation of the device, available RAM and Disk Space on the device.
- <b>Search History.</b> When searching within the app either for a subreddit or the Imgur Gallery, your search queries are saved locally to
The device to provide a search history.
- <b>Login Information.</b> When logging into Opengur using an "Imgur" account, a unique token is returned to identify your account.


### How we use data we collect
The data we collect within Opengur is to provide the best experience possible. 

Crash data collected helps us identify issues within the app not found before release and allows us to fix them and provide a better
Crash free experience. This can be turned off whenever via the settings page of the app.

Saving your search history locally to your devices allows faster access to data that you have previously searched for before. This enables
You to quickly and easily return to previous searches. This data can be cleared whenever via the settings page of the app.

Your unique account token is stored locally in order to allow your account access to persist. This enables Opengur to keep your account
Logged in indefinitely for quick access to your "Imgur" account and the content you care about. Your login information (username/password)
is not stored anywhere within the app or the device.

### Permissions Requested
- <b>READ_EXTERNAL_STORAGE.</b> Allows an application to read from external storage. Protection level: dangerous
- <b>WRITE_EXTERNAL_STORAGE.</b> Allows an application to write to external storage. Protection level: dangerous
- <b>ACCESS_NETWORK_STATE.</b> Allows applications to access information about networks.
- <b>INTERNET.</b> Allows applications to open network sockets. Protection level: normal
- <b>WAKE_LOCK.</b> Allows using PowerManager WakeLocks to keep processor from sleeping or screen from dimming. Protection level: normal
- <b>RECEIVE_BOOT_COMPLETED.</b> Allows an application to receive the ACTION_BOOT_COMPLETED that is broadcast after the system finishes booting. Protection level: normal


### Why we need these Permissions
<b>READ_EXTERNAL_STORAGE</b> and <b>WRITE_EXTERNAL_STORAGE</b> are used for accessing and writing files on your devices external storage. When uploading an image or selecting the external storage as your cache directory, Opengur will need to be able to access these files in order to accomplish these tasks. When creating a Meme, or downloading an image, Opengur will need to have access to your external storage in order to save these files.

<b>ACCESS_NETWORK_STATE and <b>INTERNET</b> are required to connect to the "Imgur" API and for the app to function. Without these permissions, the
app would not work.

<b>WAKE_LOCK</b> is used when running a long operation such as an upload or download of an image(s). This allows the device to stay awake so the process of the upload/download is not interrupted by the system attempting to sleep.

<b>RECEIVE_BOOT_COMPLETED</b> is used when notifications are enabled. Opengur will check every N minutes (this is set via settings) for new notifications on Imgur. When your device reboots, this check needs to be restarted again in order for notifications to be checked. This allows Opengur to be notified when the device has successfully reboot so it can restart any notification checks if enabled. 
