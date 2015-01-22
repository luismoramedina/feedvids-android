feedvids
========

Instructions
------------

You must create a controller/src/main/res/values/secret.xml file with this content:

'<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="consumer_key">$YOUR_POCKET_CONSUMER_KEY</string>
</resources>'

if not... this error appear on build...

feedvids-android\controller\src\main\java\com\feedvids\controller\PocketListActivity.java:XXX: cannot find symbol
symbol  : variable consumer_key
location: class com.feedvids.controller.R.string
                    getString(R.string.consumer_key),
                                      ^


TODO
----

- NEW search
- NEW Order manually
- NEW filter by tag
- NEW Mark watched when you watch it
- NEW swipe = delete
- NEW Save last watched
- NEW Colors by item

DONE
----

- NEW rev / fwd
- NEW get video info
    - Url for request data to youtube: api key and more than one video, snippet and contentDetails
    https://www.googleapis.com/youtube/v3/videos?id=nvr7T4sfads,7lCDEYXw3mM,AxvPIJj38jI&key=AIzaSyDvmmt8JxytxYgeWCOnmYPIySaG9tGBPNc%20&part=snippet,contentDetails,statistics,status
    DOC: https://developers.google.com/youtube/v3/getting-started
- NEW display current video playing
- BUG order when more than one videos
- NEW share
- NEW open on youtube