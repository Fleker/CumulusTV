# How to build Cumulus TV

1. Import project into Android Studio
1. Make sure everything builds
1. Run application on an Android TV or other Android device

## Using Google Drive integration
This section describes how to link to Google Drive

1. Go to the [Google Cloud Console](http://console.cloud.google.com)
1. Create a new project (or use an existing project)
1. From the navigation drawer, select **APIs & services**
1. Select **Enable APIs & services**
1. Find the **Google Drive API**
1. Enable this API
1. Return to the **APIs & services** page
1. Navigate to the **Credentials** section
1. Create a new **OAuth client ID**
1. Select **Android** as the Application type
1. Enter the certificate fingerprint
    * Note: You will need to do this for your production and debug keys for it to work in both modes
1. Create the client

**Note**: The app will run fine without Google Drive being enabled. However, the Google Drive feature will not be enabled.

## Using Tv Input Framework
This section describes how to use the Live Channels feature

**Note**: This feature only works on official Android TVs

1. Add a channel. It can be any channel.
1. In the app, select the **Open Live Channels** card
1. You may see a card appear immediately to add sources
    * If you don't, go to the Live Channels settings and select **Channel Sources**
    * Select **Cumulus TV** to start the sync process
1. You should now be able to see all the channels in the channel guide

## Comments

1. The app is currently built using `targetSdkVersion` 25
1. Similarly, the app is also using an outdated version of Google Play Services