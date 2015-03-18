[OpenImgur](https://kennyc1012.github.io/OpenImgur/)
=========

Open source Imgur Android App

![screenshot](https://github.com/Kennyc1012/OpenImgur/raw/master/assets/ss1.png)

#Features
- Built in native java
- Native support for both Gifs and GifV image formats
- Tablet Support
- Material Design
- Dark Theme
- Meme Generator
- Support for Api 15+

#What's New
Check the [Changelog](https://github.com/Kennyc1012/OpenImgur/blob/master/CHANGELOG.MD) for the changes of each version

#Building
When forking and building OpenImgur, you must use your own API keys. You can obtain them for free from Imgur. To use them, either create a gradle.properties file and add them with the keys API_CLIENT_ID and API_CLIENT_SECRET
```groovy
API_CLIENT_ID = "YOUR CLIENT ID"
API_CLIENT_SECRET ="YOUR CLIENT SECRET"
```
Or you can hard code them into the ApiClient.java class 
```java
public static final String CLIENT_ID = "YOUR CLIENT ID";
public static final String CLIENT_SECRET = "YOUR CLIENT SECRET";
```

When registering for an API key from Imgur, you need to specify a redirect url to handle logins. The url that the app currently uses is https://com.kenny.openimgur/ . You can use that or provide your own and update the url in ProfileActivity.java 

#Contribution
Pull requests are welcomed and encouraged. If you experience any bugs, please [file an issue](https://github.com/Kennyc1012/OpenImgur/issues/new)

License
=======

    Copyright 2014 Kenny Campagna

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
