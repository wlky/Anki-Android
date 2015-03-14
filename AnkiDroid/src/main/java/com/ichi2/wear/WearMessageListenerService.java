package com.ichi2.wear;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Yannik on 12.03.2015.
 */
public class WearMessageListenerService extends WearableListenerService {
    private static final String PATH = "/com.anki";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(PATH)) {
            final String message = new String(messageEvent.getData());
            Log.v("myTag", "Message path received on phone is: " + messageEvent.getPath());
            Log.v("myTag", "Message received on phone is: " + message);
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }

}
