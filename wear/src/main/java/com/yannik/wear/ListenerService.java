package com.yannik.wear;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Yannik on 12.03.2015.
 */
public class ListenerService extends WearableListenerService {

    private static final String P2W_RESPOND_CARD = "/com.ichi2.wear/respondWithCard";
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(P2W_RESPOND_CARD)) {
            final String message = new String(messageEvent.getData());
            Log.v("myTag", "Message path received on watch is: " + messageEvent.getPath());
            Log.v("myTag", "Message received on watch is: " + message);

            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", new String(messageEvent.getData()));
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);

        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }
}