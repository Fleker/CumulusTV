# CumulusTV
Android TV Live Channel Plugin with user-entered stream files

<img src='https://cloud.githubusercontent.com/assets/3291635/9031614/2a2534ba-3983-11e5-900f-a8cb99f3bf40.png' width='640px'/>

CumulusTV is a service that allows users to add `HLS` (HTTP Live Streaming) files or any website and play them through the Live Channels app on Android TV. Channels can be added through Android TV, or sync your data with Google Drive and edit the data on any computer.

## Contributing
This is a community project. I welcome any other developers to contribute to this project as well, whether in the form of a web app, a phone app, or on the Android TV app. Please submit a pull request.

If you can't develop, you can create an issue here, or send inquires @HandNF on Twitter.

## Download it Now
The app is available for free on <a href="https://play.google.com/apps/testing/com.felkertech.n.cumulustv">Google Play</a> as a beta. Anyone who downloads and uses the beta will be using the most recent <a href="https://github.com/Fleker/CumulusTV/releases">release</a>.

## History
This originally started from a <a href="https://www.reddit.com/r/AndroidTV/comments/3cslyd/app_that_adds_m3u_iptv_streams_to_the_live/">thread on Reddit</a> 
where a user wanted to add user-defined channels using m3u8 files.

## To-Do
* [ ] Find m3u streams to provide as samples
* [ ] Get program guide data to supply streams if data exists


## Suggested Streams
To help users quickly setup, I want a set of suggested streams (m3u8 files) that users can add. If you have a suggestion, add it as an issue for this project so I can add it to the list.

## JSON Format
Google Drive syncs a JSON file between all your devices that can be easily edited. Here is the format:

    { 
      "channels":[      //JSON Array
            {
                "number": "1337", //Channel Number
                "name": "Aquarium", //Channel Title
                "logo": "logo.jpg", //Channel Logo
                "url":"stream.m3u8", //Channel stream or website url
                "splashscreen":"" //Optional splashscreen instead of default one
                "genres":"NEWS,MUSIC" //Comma separated array of channel genres  
            }
        ],
        "modified":"1234", //Timestamp in ms of last edit 
        "possibleGenres":["NEWS", ...] //JSON array of genres which you can use, that are supposed by AndroidTV
    }

## Plugin
Want to make it easy for users to add a Twitch stream without entering a complicated URL? Then you want to create a plugin. This is an app that works easily with CumulusTV, simplifying the amount of work you need to do for a live channel to appear.

To learn how to do this, read <a href="https://github.com/Fleker/CumulusTV/wiki/Create-a-3rd-Party-Plugin">this wiki page</a>.

## Photos
<img src='https://cloud.githubusercontent.com/assets/3291635/9021048/00a04364-37fd-11e5-85be-1e550796d922.png' width='640px'/>

<img src='https://raw.githubusercontent.com/Fleker/CumulusTV/master/device-2016-01-03-194333.png' width='640px'/>
Selecting <a href='http://time.is'>http://time.is</a> as a channel source
