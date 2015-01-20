#Open Imgur Changelog

<b>1.6.1</b>
- Fixed rare chance of not having items from savedInstance in
ViewActivity

- Upload to gallery bug fix

- Settings Fragment fix

<b>1.6</b>
- All uploads done within the app will now appear in the Uploads tab
which can be accessed via the Navigation Drawer.

- Fix Issues with parceling.

- Fork the byakugallery library and fix the issue of it throwing an
exception if decoding fails. If failure occurs, photos will use the
PhotoViewAttacher for zooming the image.

- Turn off crashlytics logging for caught exceptions, most were just
network issues.

- Allows users to not get prompted when uploading an image when they are
not logged in.

- Allow the DownloadService to accept URLs for downloads.

- Removed stylized dialogs in favor of the system dialogs.

<b>1.5.1</b>
- Bug fixes with parceling objects
- Previous comments layout updated

<b>1.5</b>
- Application id change due to being removed from Google Play
- Crashlytics added
- NSFW thumbnails can now be blocked 

<b>1.4</b>
- Better Image Zooming. Full screen images will now render with Tile Bitmap to support zooming in on sections of the images
to better render large(tall) images
- Comments crash fix
- Fixed bug in cache clearing that would cause the video cache to fail until app is restarted
- Share Intent added
- Grey Theme added

<b>1.3.1</b>
- Fixed a crash when posting comments
- Organized strings.xml file

<b>1.3</b>
- Up/Down vote made apparent
- ADB Logging can now be enabled from settings
- Fix comments loading on wrong image
- Ability to select partition for cache (internal/external)
- Code cleanup

<b>1.2</b>
- Fixed bug in Subreddit searching
- Comments have a new layout that follow material guidelines
- Updated libraries 

<b>1.1</b>
- Change applicationId as I deleted keystone file -_-
- Color themes added

<b>1.0</b>
- Initial Release
