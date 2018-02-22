# How to make a native Android app that can block phone calls

## TL;DR
In this post, I'll show you step by step how to make a native Android app that can block certain numbers from calling you.

I hope that my step by step guide that I'm going to show you here will help you and save you from doing additional research.

_Of course, since I'm not a native Android developer in my day to day job, I'm doing it also for the fact that it will serve me as a good reminder for when I need to deal with a similar situation again._ Shout out to the rest of you #jackOfAllTrades out there 💪

Also, given the statement above; I would appreciate any feedback regarding this code. 🙏

## !TL;DR

I've spent a lot of time going through StackOverflow and blog posts in search of this solution. Of all of those, these were helpful:

+ [How to detect incoming calls on an Android device?
](https://stackoverflow.com/questions/15563921/how-to-detect-incoming-calls-in-an-android-device/15564021)
+ [Can't answer incoming call in android marshmallow 6.0
](https://stackoverflow.com/questions/42339534/cant-answer-incoming-call-in-android-marshmallow-6-0)
+ [Android permission doesn't work even if I have declared it
](https://stackoverflow.com/questions/32635704/android-permission-doesnt-work-even-if-i-have-declared-it)
+ [End incoming call programmatically
](https://stackoverflow.com/questions/20965702/end-incoming-call-programmatically)
+ [Is the phone ringing](http://gabesechansoftware.com/is-the-phone-ringing/)

But sadly, none of them was straightforward, beginner kind of tutorial. So, after a lot of additional research, I made it work, and here's my best attempt at explaining how.

> As a sidenote: while testing this, the discovery of [how to simulate an incoming call or SMS to an emulator in Android Studio](http://www.nikola-breznjak.com/blog/android/simulate-incoming-call-sms-emulator-android-studio/) was also very helpful.

### Starting a new project
In Android Studio go to `File->New->New Project`, give it a name and a location and click `Next`:

![](https://i.imgur.com/4iyIDSJ.png)

Leave the default option for minimum API level:

![](https://i.imgur.com/wBim1w2.png)

Select an `Empty Activity` template:

![](https://i.imgur.com/eLo7Sia.png)

Leave the name of the activity as is:

![](https://i.imgur.com/8M3pwcQ.png)

### AndroidManifest.xml
Set the permissions (two `uses-permission` tags) and the `receiver` tags in `AndroidManifest.xml` file:

```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.nikola.callblockingtestdemo">

    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.CALL_PHONE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver  android:name=".IncomingCallReceiver" android:enabled="true" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.PHONE_STATE" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

With the `READ_PHONE_STATE` permission we get this (as defined in [official docs](https://developer.android.com/reference/android/Manifest.permission.html#READ_PHONE_STATE)):

> Allows read-only access to phone state, including the phone number of the device, current cellular network information, the status of any ongoing calls, and a list of any PhoneAccounts registered on the device.

With the `CALL_PHONE` permission we get this (as defined in [official docs](https://developer.android.com/reference/android/Manifest.permission.html#CALL_PHONE)):

> Allows an application to initiate a phone call without going through the Dialer user interface for the user to confirm the call.

⚠️ I found that even though not stated here, I need this permission so that I can end the call programmatically.

The `receiver` tag is used to define a class that will handle the broadcast action of `android.intent.action.PHONE_STATE`. Android OS will broadcast this action when, as the name implies, the state of the phone call changes (we get a call, decline a call, are on the call, etc.).

### IncomingCallReceiver.java
Create a new class (`File->New->Java Class`), call it `IncomingCallReceiver` and paste this code in (_note: your `package` name will be different than mine!_):

```
package com.example.nikola.callblockingtestdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import java.lang.reflect.Method;
import com.android.internal.telephony.ITelephony;

public class IncomingCallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        ITelephony telephonyService;
        try {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)){
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                try {
                    Method m = tm.getClass().getDeclaredMethod("getITelephony");

                    m.setAccessible(true);
                    telephonyService = (ITelephony) m.invoke(tm);

                    if ((number != null)) {
                        telephonyService.endCall();
                        Toast.makeText(context, "Ending the call from: " + number, Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                Toast.makeText(context, "Ring " + number, Toast.LENGTH_SHORT).show();

            }
            if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_OFFHOOK)){
                Toast.makeText(context, "Answered " + number, Toast.LENGTH_SHORT).show();
            }
            if(state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE)){
                Toast.makeText(context, "Idle "+ number, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

In Android, if we want to 'get' the data from the `BroadcastReceiver`, we need to inherit the `BroadcastReceiver` class, and we need to override the `onReceive` method. In this method, we're using the `TelephonyManager` to get the state of the call, and we're using the `ITelephony` interface to end the call.

To be honest, this is where it gets a bit 'weird', as to get this `ITelephony` interface, you need to create the `ITelephony` interface.

### ITelephony.java
To do that, create a new class (`File->New->Java Class`), call it `ITelephony` and paste this code in (_note: overwrite everything with the below content; yes, even the weird package name_):

```
package com.android.internal.telephony;

public interface ITelephony {
    boolean endCall();
    void answerRingingCall();
    void silenceRinger();
}
```

Android Studio will complain about `package com.android.internal.telephony;` (red squiggly dots under this package name), but that's how it has to be set for this to work. I didn't find the exact explanation why this has to be included, so if you know, please share it in the comments.

### Requesting permissions at runtime
This was one thing that was hindering my success in getting this to work!

Namely, after Android 6.0+, even if you have permissions set in the `AndroidManifest.xml` file, you still have to explicitly ask the user for them if they fall under the category of **dangerous** permissions. This is the list of such permissions:

+ ACCESS_COARSE_LOCATION
+ ACCESS_FINE_LOCATION
+ ADD_VOICEMAIL
+ BODY_SENSORS
+ CALL_PHONE
+ CAMERA
+ GET_ACCOUNTS
+ PROCESS_OUTGOING_CALLS
+ READ_CALENDAR
+ READ_CALL_LOG
+ READ_CELL_BROADCASTS
+ READ_CONTACTS
+ READ_EXTERNAL_STORAGE
+ READ_PHONE_STATE
+ READ_SMS
+ RECEIVE_MMS
+ RECEIVE_SMS
+ RECEIVE_WAP_PUSH
+ RECORD_AUDIO
+ SEND_SMS
+ USE_SIP
+ WRITE_CALENDAR
+ WRITE_CALL_LOG
+ WRITE_CONTACTS
+ WRITE_EXTERNAL_STORAGE

To ask for such permissions here's the code you can use (I used it in `MainActivity.java` in the `onCreate` method):

```
if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
    if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
        String[] permissions = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE};
        requestPermissions(permissions, PERMISSION_REQUEST_READ_PHONE_STATE);
    }
}
```

The `PERMISSION_REQUEST_READ_PHONE_STATE` variable is used to determine which permission was asked for in the `onRequestPermissionsResult` method. Of course, if you don't need to execute any logic depending on whether or not the user approved the permission, you can leave out this method:

```
@Override
public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
        case PERMISSION_REQUEST_READ_PHONE_STATE: {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted: " + PERMISSION_REQUEST_READ_PHONE_STATE, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission NOT granted: " + PERMISSION_REQUEST_READ_PHONE_STATE, Toast.LENGTH_SHORT).show();
            }

            return;
        }
    }
}
```

### App in action
This is how the app looks like in action, tested on the emulator and call triggered by using [Android Device Monitor](http://www.nikola-breznjak.com/blog/android/simulate-incoming-call-sms-emulator-android-studio/) in Android Studio:

![](https://i.imgur.com/CjU9D4J.gif)

## Conclusion
In this post, I showed you how to make a native Android app that can block certain numbers from calling you. I pointed out the blocker that I was facing, and I'm still searching a solution to hide a native incoming call popup that still sometimes shows up for a brief second before the call gets rejected.

So, if you have any ideas, I'm open to suggestions 💪