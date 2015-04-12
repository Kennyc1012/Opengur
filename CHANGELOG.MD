#Open Imgur Changelog

<b>2.6</b>
- Gallery search added
- Switch Floating Action Button [library](https://github.com/Clans/FloatingActionButton)
- Various crash fixes IllegalStateException regarding fragment transactions 
- Fix for when a user pressed the "deny" button on the login page
- Update Photoview to 1.2.3
- Update Material Dialogs to 0.7.1.2
- Update bottom sheet to a separate fork for custom callbacks

<b>2.5.1</b>
- Crash fix for P2R [#68](https://github.com/Kennyc1012/OpenImgur/issues/68), thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Set defeaults to the same as Imgur [#70](https://github.com/Kennyc1012/OpenImgur/issues/70)
- Remove the options from the share Intent 
- Remove default loading image
- Use AppCompat theme for comment dialog, thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Update fabric to 2.2.2
- Update Gif Drawable to 1.1.3
- Update Material Dialogs to 0.6.7.0
- Update Bottom Sheet to 1.0

<b>2.5</b>
- Pull to refresh added to grid fragments, thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Background uploads added
- Link protoctols changed to HTTPS thanks, [zamabe](https://github.com/zamabe)
- Black Theme added
- Notification icon fixed for Lollipop devices, thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Crash fix for SQL Exception when querying topics [#52](https://github.com/Kennyc1012/OpenImgur/issues/52)
- Login view fixed thanks, [Jawnnypoo](https://github.com/Jawnnypoo)
- Close cursor for subreddit search adapter when fragment is destroyed, thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Fix wrong text color for share widget
- Cache will be clear every 4 days instead of 7
- Update OKHTTP to 2.3.0 release
- Update Butterknife to 6.1.0 release
- Update Bottomsheet to 0.9 release
- Update Material Dialogs to 0.6.6.1 release

<b>2.4</b>
- Ability to open direct photo links from imgur 
- Confirmation when exiting the app
- Enable Javascript for Login Webview
- Ability to refresh a gallery item added
- Collapse search view when submitting a search for subreddits, thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Handle video download links better 
- Various bug fixes
- Update Material Dialogs to version 0.6.4.6

<b>2.3</b>
- Meme Generator Activitiy added
- Fixed album link handling 
- Various bug fixes relating to the ViewActivity
- Updated SDK to version 22
- Updated Appcompat to version 22
- Update Material Dialogs to version 0.6.4.0

<b>2.2</b>
- Dark Theme added
- Subreddit searchs will be saved and used as future suggestions
- Handle album links
- Bug fixes
- Updated several libraries 

<b>2.1</b>
- Topics added
- New Icon
- Database calls will use a singleton for read and write databases
- Updated OKHttp to 2.2.0 release
- Bug fixes

<b>2.0</b>
- New Profile Layout. Users can now view submissions, favorites, and comments of other users as well as messages and uploads for
their own account.
- Random mode added
- Highest Scoring Filter added
- Task Descriptions updated for Lollipop devices
- Better scroll detection
- Various bug fixes 
<b>1.6.3</b>
- Fix trying to use a recycled bitmap crash when viewing an image full screen.
- Added null and empty check for adding items in an adapter
- Made sure the device can handle the photo/gallery intent before
launching

<b>1.6.2</b>
- Fix third party login. To login with a a third party, click the logo of desired party, and click allow with the fields
empty, you will then go through the login flow on that website. 

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
