#Open Imgur Changelog
<b>4.7.1</b> February 25, 2017
- Added Privacy Policy
- Update gradle

<b>4.7.0</b> December 26,2016
- Update several libraries
- Update gradle plugin
- Fix several bugs and crashes
- EOL for app, no longer under active development 

<b>4.6.4</b> August 26, 2016
- Catch NPE

<b>4.6.3</b> August 25, 2016
- Fix TransactionToLargeException on N
- Use CommonAdapters
- Update Gradle Wrapper V2.2.0-beta3

<b>4.6.2</b> August 16, 2016
- Fix NSFW 
- Crash Fix

<b>4.6.1</b> August 12, 2016
- Link Crash Fix

<b>4.6.0</b> August 11, 2016
- Support for Android Nougat (SDK 24)
- Direct Reply for Android N
- Gallery View for Full Screen Albums
- Add data saver for fetching notifications and updating Muzei
- Add Loading status to full screen view
- Handle more links and fix some regex patterns
- Remove AndroidSlidingPanel
- Restructure files. Thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Several crash/bug fixes and performance gains
- Update Support Library V24.1.1
- Update Build tools V24.0.1
- Update Gradle V2.14.1
- Update Gradle Plugin V2.2.0-beta1

<b>4.5.3</b> June 2, 2016
- Crash Fix

<b>4.5.2</b> May 30, 2016
- Crash Fix

<b>4.5.1</b> May 29, 2016
- Convert links to HTTPS
- Fix SQL statements
- DialogFragment crash fix
- Better handling of video links
- Add Delete option for single image downloads
- Check for "in_gallery" flag in JSON response
- Code Cleanup
- Update BottomSheet V2.1.2
- Update Retrofit V2.0.2
- Update Butterknife V8.0.1
- Update ColorChooser V0.3.0
- Update MultiStateView V1.2.0
- Update Support Library V23.4.0
- Update Gradle Plugin V2.2.0-alpha2
- Update Gradle V2.13
- Create F-Droid flavor

<b>4.5.0</b> April 16, 2016
- Use FileProvider for sharing files
- Change download folder name to Opengur
- Use native share menu for Lollipop+ devices
- Calculate cache directory correctly
- Numerous bug fixes
- Update gif drawable V1.1.15
- Update subsampling image view V3.5.0
- Update support library V23.3.0
- Update Retrofit V2.0.1
- Update gradle plugin V2.1.0-beta1

<b>4.4.0</b> March 12, 2016
- Update Retrofit V2.0.0 
- Update Okhttp V3.2.0
- Fix NSFW
- Properly detect for gifs
- Returned to last viewed item when returning to grid
- Resource/Code cleanup
- Add error drawables for grids
- Add date to gallery item header
- Update Gif Drawable V1.1.14
- Update Ink Page Indicator V1.1.1
- Remove collections jar and import only the used classes
- Update Gradle Plugin V2.1.0-alpha1
- Various minor bug fixes

<b>4.3.4</b> February 15, 2016
- Open posters profile on callout of OP
- Use [ColorChooser](https://github.com/Kennyc1012/ColorChooser) for selecting app theme
- Better saving of filters
- Don't display album count on single image albums
- Several bug fixes
- Fix incorrect join date on a cached user
- Update BottomSheet V2.1.1
- Update Gradle Plugin V2.0.0-beta4

<b>4.3.3</b> February 7, 2016
- Fix a bug from the Api respnding incorrectly
- Fix potential crash when uploading
- Show toast when unable to open link
- Update TextDrawable V1.1 (fork)
- Update SlidingPanel V3.2.1 (fork)
- Update gradle plugin V2.0.0-beta2

<b>4.3.2</b> January 26, 2016
- Fix another profile info crash
- Handle all Imgur Links
- Update Gif Drawable V 1.1.13

<b>4.3.1</b> January 23, 2016
- Fix profile info crash
- Revert link clicking on photo titles/descriptions. 

<b>4.3.0</b> January 23, 2016
- Revamp of uploading
- Display trophies on profiles
- Add shared element transitions for Meme creation
- Allow multiple subreddits in Muzei (comma separate values)
- Better link handling
- Rework caching directories
- Switch to design library Snackbar
- Use Singleton for database calls
- Fix Muzei icon
- Fix endless Alert dialog error
- Several crash fixes
- Update Okhttp V2.7.1
- Update gradle V2.10
- Update gradle plugin V2.0.0-alpha7

<b>4.2.2</b> December 21, 2015
- Make Immersive mode a setting
- Update Share Bottom sheets to be grid style

<b>4.2.1</b> December 19, 2015
- Add "Worst" comment sorting 
- Add Immersive mode to fullscreen photos
- Add vote status to comments
- Tablet will use a grid style for share intents
- Add External Searching for Marshmallow devices
- Refactor styles
- Update OKHTTP V2.7.0
- Update GSON V2.5
- Update GifDrawable V1.1.11
- Update Universal Image Loader V1.9.5
- Update Gradle V2.8
- Update Gradle plugin V2.0.0-alpha3

<b>4.2.0</b> November 14, 2015
- Add album count to thumbnails
- Fully convert over to RecyclerViews
- Add Loading footer to grids
- Update Share Intent
- Remove OAuth Crashlytics Logging
- Tweak the wait time before trying to request a new token
- Update BottomSheet to V2.0.4
- Update Support Library to V23.1.1

<b>4.1.0</b> November 2, 2015
- Simplify filter menus
- Add GIF indicator in grid view
- Fix GSON parsing
- Fix Read Storage permission
- Convert drawables to vectors
- Crash fixes
- More user logout fixes
- Update build tools to V1.4.0-beta6
- Update OKHTTP to V2.5.0
- Update Retrofit to V2.0.0-beta2
- Update GSON to V2.4
- Update Bottomsheet to V2.0.3

<b>4.0.1</b> October 4, 2015
- Handle retrofit crashes when fragment has been destroyed 
- Catch any errors when decoding images for upload
- Don't fetch comments/tags when unlisted 
- Hide message button on profiles when not logged in
- Fix Holo theme on start up on pre L devices. Thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Use plurals for point(s) text
- Resource clean up
- Update Gradle to v2.7
- Update Gradle build tools to v1.4.0-beta3
- Update Gif Drawable to v1.1.10
- Update Subsampling Scale Image View to v3.4.0

<b>4.0</b> September 26, 2015
- Marshmallow support
- Album Downloads
- Subreddit pinning
- Remove internal photo picker
- Wake lock crash catch 
- More token refresh logic
- Update Bottom sheet to 2.0.1
- Remove custom MultiStateView

<b>3.4.2</b> August 31, 2015
- Fix crash when sorting notifications
- Update BottomSheet to version 1.2.4
- Use BottomSheet for shate dialogs
- Don't crash on notifications when no user is present
- Update Token refresh logic

<b>3.4.1</b> August 30, 2015
- Fix a crash in a SQL statement regarding notifications 

<b>3.4.0</b> August 29, 2015
- Notifications addd
- Added reporting posts
- Allow hiding of comment bar in gallery view
- Update Gif Drawable to v1.1.9
- Update [TextDrawable](https://github.com/Kennyc1012/TextDrawable) to use own fork
- Switch to new [BottomSheet](https://github.com/Kennyc1012/BottomSheet) library
- Convert to [MultiStateView](https://github.com/Kennyc1012/MultiStateView) library 
- Multiple crash fixes

<b>3.3.0</b> August 6, 2015
- Convert ProfileActivity to use design lib scrolling components
- Add ability to use own photos for memes
- Fix chat margins
- Several bug/crash fixes 

<b>3.2.2</b> August 1, 2015
- Fix incorrect dates on self profile
- Fix Highest Scoring filter
- Fix comment posting
- Update GridItemDecoration
- Convert Convo adapter to RecyclerView
- Update Gif drawable to 1.1.8
- Profile bug fixes

<b>3.2.1</b> July 25,2015
- Entire API rewrite. Now using Retrofit
- Link handling in Convos
- Removal of favorites from profile
- Update butterknife to 7.0.1
- Update support libraries to 22.2.1
- Miscellaneous bug fixes 

<b>3.2.0</b> July 10,2015
- Album upload support
- Albums tab added to Profile 
- More improvements to full screen photo viewing

<b>3.1.1</b> June 30,2015
- Display subreddit in Muzei settings
- Reduce duplicate images in Muzei
- Don't redraw gallery list when voting
- Fix several bugs

<b>3.1.0</b> June 26, 2015
- Muzei Support
- Reneable scroll bars for galleries
- Fix full screen zooming crashes
- Fix fragment transaction crashes 
- Ensure cache gets cleared after 3 days
- Add Travis CI

<b>3.0</b> June 19, 2015
- Revamp of comments
- Up Min SDK to 17 (4.2)
- Change Share Text to Opengur
- Switch to TabLayout from Design Lib and remove Material Tab library
- Default to Dark Theme
- Add xxxhdpi icons
- Start RecyclerView conversions for lists
- Update Subsampling Image View to 3.1.4

<b>2.9.0</b> June 9, 2015
- Better Themeing
- Better token refresh
- Remove Material Dialogs library
- Switch drawer to NavigationView
- Added text suggestion to comment dialog
- Fix dialog styles
- Reduce memory in Full screen
- Turn of translucent navigation bar temporarily 
- Fix Topics crash
- Add Thumbnail quality settings
- Allow direct album link copying 
- Fix UserManager memory leak

<b>2.8.2</b> June 6, 2015
- Better gif support while viewing in full screen
- Added Beta community nav item
- Added product flavors for beta and full release

<b>2.8.1</b> June 3, 2015
- Fix crash while in full screen image viewing

<b>2.8</b> June 3, 2015
- Update account favorites end point for self profiles
- Full photo swiping added. When viewing an album, swiping between photos is enabled in full screen 
- Long pressing an image will now show the download and share options
- Handle user mentions from comments
- Comment Copying
- Fix some theme issues
- Bug fixes
- Add subsampling-scale-image-view for better zoom support
- Update Universal Image Loader to 1.9.4
- Update Fabric
- Update OKHTTP to 2.4.0
- Update SnackBar to 2.5
- Update Material Dialogs to 0.7.5.1
- Update support libraries to 22.2.0
- Update Gif Drawable to 1.1.7
- Remove PhotoView library
- Remove ByakuGallery library

<b>2.7.1</b> May 14, 2015
- Changed app name to Opengur as per Imgur's request
- Copy correct links
- Updated Material Dialogs to 0.7.4.1
- Updated SnackBar to 2.3
- Update gradle to 2.4
- Update android build tools to 1.2.3 

<b>2.7</b> May 3, 2015
- Tags added
- Titles won't display on single image galleries
- Update icons 
- Update SnackBar to 2.0.2
- Update AppCompat to 22.1.1
- Update Material Dialogs to 0.7.3.1
- Update Build Tools to 1.2.2

<b>2.6.2</b> April 24, 2015
- Experimental Settings section added
- Several miscellaneous bug fixes 
- Fix Floating Action Button visibility toggling when scrolling
- Update appcompat to 22.1.0
- Update Material Dialogs to 0.7.2.7
- Update SnackBar to 2.0
- Update Gif Drawable to 1.1.6
- Update Floating Action Button to 1.4.0

<b>2.6.1</b> April 13, 2015
- Fix crashes in GalleryFragment relating to null activity in onCreateOptionsMenu
- Don't animate drawer hamburger menu 

<b>2.6</b> April 12, 2015
- Gallery search added
- Switch Floating Action Button [library](https://github.com/Clans/FloatingActionButton)
- Various crash fixes IllegalStateException regarding fragment transactions 
- Fix for when a user pressed the "deny" button on the login page
- Update Photoview to 1.2.3
- Update Material Dialogs to 0.7.1.2
- Update bottom sheet to a separate fork for custom callbacks

<b>2.5.1</b>April 1, 2015
- Crash fix for P2R [#68](https://github.com/Kennyc1012/OpenImgur/issues/68), thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Set defeaults to the same as Imgur [#70](https://github.com/Kennyc1012/OpenImgur/issues/70)
- Remove the options from the share Intent 
- Remove default loading image
- Use AppCompat theme for comment dialog, thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Update fabric to 2.2.2
- Update Gif Drawable to 1.1.3
- Update Material Dialogs to 0.6.7.0
- Update Bottom Sheet to 1.0

<b>2.5</b> March 29, 2015
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

<b>2.4</b> March 19, 2015
- Ability to open direct photo links from imgur 
- Confirmation when exiting the app
- Enable Javascript for Login Webview
- Ability to refresh a gallery item added
- Collapse search view when submitting a search for subreddits, thanks [Jawnnypoo](https://github.com/Jawnnypoo)
- Handle video download links better 
- Various bug fixes
- Update Material Dialogs to version 0.6.4.6

<b>2.3</b> March 14, 2015
- Meme Generator Activitiy added
- Fixed album link handling 
- Various bug fixes relating to the ViewActivity
- Updated SDK to version 22
- Updated Appcompat to version 22
- Update Material Dialogs to version 0.6.4.0

<b>2.2</b> March 7, 2015
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

<b>2.0</b> February 24, 2015
- New Profile Layout. Users can now view submissions, favorites, and comments of other users as well as messages and uploads for
their own account.
- Random mode added
- Highest Scoring Filter added
- Task Descriptions updated for Lollipop devices
- Better scroll detection
- Various bug fixes 

<b>1.6.3</b> February 1, 2015
- Fix trying to use a recycled bitmap crash when viewing an image full screen.
- Added null and empty check for adding items in an adapter
- Made sure the device can handle the photo/gallery intent before
launching

<b>1.6.2</b> January 28, 2015
- Fix third party login. To login with a a third party, click the logo of desired party, and click allow with the fields
empty, you will then go through the login flow on that website. 

<b>1.6.1</b> January 20, 2015
- Fixed rare chance of not having items from savedInstance in
ViewActivity
- Upload to gallery bug fix
- Settings Fragment fix

<b>1.6</b> January 17, 2015
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

<b>1.5.1</b>January 12, 2015
- Bug fixes with parceling objects
- Previous comments layout updated

<b>1.5</b> January 11, 2015
- Application id change due to being removed from Google Play
- Crashlytics added
- NSFW thumbnails can now be blocked 

<b>1.4</b> January 6, 2015
- Better Image Zooming. Full screen images will now render with Tile Bitmap to support zooming in on sections of the images
to better render large(tall) images
- Comments crash fix
- Fixed bug in cache clearing that would cause the video cache to fail until app is restarted
- Share Intent added
- Grey Theme added

<b>1.3.1</b> Januaru 4, 2015
- Fixed a crash when posting comments
- Organized strings.xml file

<b>1.3</b> January 2, 2015
- Up/Down vote made apparent
- ADB Logging can now be enabled from settings
- Fix comments loading on wrong image
- Ability to select partition for cache (internal/external)
- Code cleanup

<b>1.2</b> December 21, 2014
- Fixed bug in Subreddit searching
- Comments have a new layout that follow material guidelines
- Updated libraries 

<b>1.1</b> December 20, 2014
- Change applicationId as I deleted keystone file -_-
- Color themes added

<b>1.0</b> December 7, 2014
- Initial Release
