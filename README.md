# Android Vinli Net Demo

This is a simple demo application demonstrating the usage of the [Vinli Net SDK](https://github.com/vinli/android-net). It makes heavy use of RxJava, but deep knowledge the mechanics of rx should not be required. Refer to [NetDemoActivity.java](app/src/main/java/li/vin/netdemo/NetDemoActivity.java) for comments documenting usage.

Refer to [StreamingActivity.java](app/src/main/java/li/vin/netdemo/StreamingActivity.java) for a demonstration on how to use the streaming portions of the [Vinli Net SDK](https://github.com/vinli/android-net).

To build the demo application, you need to create a `secrets.xml` file under `app/src/main/res/values` that looks like the following:

```xml
<resources>
    <string name="google_maps_api_key">Your Google Maps API Key</string>

    <string name="app_client_id">Your Vinli App Client Id</string>
    <string name="app_redirect_uri">Your Redirect URI</string>
</resources>
```

[Link](https://developers.google.com/maps/documentation/android-api/signup) to get a Google Maps API Key.

[Link](https://dev.vin.li/#/home) to the Vinli Developer Portal to get your Vinli App Client Id.
