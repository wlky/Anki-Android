package com.yannik.wear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WatchViewStub;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableStatusCodes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;


public class WearMainActivity extends Activity {
    private static final String TAG = "WearMain";
    private static final String W2P_REQUEST_CARD = "/com.ichi2.wear/requestCard";
    private static final String W2P_RESPOND_CARD_EASE = "/com.ichi2.wear/cardEase";
    public static TextView mTextView;
    private GoogleApiClient googleApiClient;
    private PullButton failed, hard, mid, easy;
    private boolean showingAnswer = false;
    private Timer easeButtonShowTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wear_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();
        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.d(TAG, "Wear connected to Google Api");
                fireMessage(W2P_REQUEST_CARD, null);
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "Wear connection to Google Api suspended");
            }
        });
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!showingAnswer) {
                            mTextView.setText(a);
                            showingAnswer = true;
                            showButtons();
                        }
                    }
                });
                mTextView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                if (showingAnswer){
                                    mTextView.setText(q);
                                }
                                break;
                            case MotionEvent.ACTION_CANCEL:
                                if (showingAnswer){
                                    mTextView.setText(a);
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                                if (showingAnswer){
                                    mTextView.setText(a);
                                }
                                break;
                        }
//                        Log.v("test", "ontouchevent " + event.getAction());
                        return false;

                    }
                });

                easy = (PullButton) stub.findViewById(R.id.easyButton);
                mid = (PullButton) stub.findViewById(R.id.midButton);
                hard = (PullButton) stub.findViewById(R.id.hardButton);
                failed = (PullButton) stub.findViewById(R.id.failedButton);


                View.OnClickListener easeButtonListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String ease = "";
                        switch (v.getId()) {
                            case R.id.failedButton:
                                ease = "failed";
                                break;
                            case R.id.hardButton:
                                ease = "hard";
                                break;
                            case R.id.midButton:
                                ease = "mid";
                                break;
                            case R.id.easyButton:
                                ease = "easy";
                                break;
                        }

                        fireMessage(W2P_RESPOND_CARD_EASE, ease);
                        hideButtons();
                        showingAnswer = false;
                    }
                };


                failed.setOnSwipeListener(easeButtonListener);
                easy.setOnSwipeListener(easeButtonListener);
                hard.setOnSwipeListener(easeButtonListener);
                mid.setOnSwipeListener(easeButtonListener);
//                hideButtons();
            }
        });


        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }


    private void hideButtons() {
        easy.setVisibility(View.GONE);
        mid.setVisibility(View.GONE);
        hard.setVisibility(View.GONE);
        failed.setVisibility(View.GONE);
    }

    private void showButtons() {
        switch (numButtons){
            case 2:
                mid.slideIn(100);
                failed.slideIn(300);
                try {
                    failed.setText(nextReviewTimes.getString(0));
                    mid.setText(nextReviewTimes.getString(1));
                } catch (JSONException e) {}
                break;
            case 3:
                easy.slideIn(100);
                mid.slideIn(300);
                failed.slideIn(500);
                try {
                    failed.setText(nextReviewTimes.getString(0));
                    mid.setText(nextReviewTimes.getString(1));
                    easy.setText(nextReviewTimes.getString(2));
                } catch (JSONException e) {}
                break;
            case 4:
                easy.slideIn(100);
                mid.slideIn(300);
                hard.slideIn(500);
                failed.slideIn(700);
                try {
                    failed.setText(nextReviewTimes.getString(0));
                    hard.setText(nextReviewTimes.getString(1));
                    mid.setText(nextReviewTimes.getString(2));
                    easy.setText(nextReviewTimes.getString(3));
                } catch (JSONException e) {}
                break;
        }
    }

    private void fireMessage(final String path, final String ease) {
        Log.d(TAG, "Firing Request for card");
        // Send the RPC
        PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                for (int i = 0; i < result.getNodes().size(); i++) {
                    Node node = result.getNodes().get(i);
                    String nName = node.getDisplayName();
                    String nId = node.getId();
                    Log.d(TAG, "Node name and ID: " + nName + " | " + nId);

                    Wearable.MessageApi.addListener(googleApiClient, new MessageApi.MessageListener() {
                        @Override
                        public void onMessageReceived(MessageEvent messageEvent) {
                            Log.d(TAG, "Message received: " + messageEvent);
                        }
                    });

                    PendingResult<MessageApi.SendMessageResult> messageResult = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(),
                            path, (ease == null ? "nextCardPlease" : ease).getBytes());
                    messageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Status status = sendMessageResult.getStatus();
                            Log.d(TAG, "Status: " + status.toString());
                            //                         if (status.getStatusCode() != WearableStatusCodes.SUCCESS) {
//                                alertButton.setProgress(-1);
//                                label.setText("Tap to retry. Alert not sent :(");
                            //                           }
                        }
                    });
                }
            }
        });
    }

    int numButtons = 4;
    JSONArray nextReviewTimes;
    Spanned q, a;

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                JSONObject js = new JSONObject(intent.getStringExtra("message"));
                q = new SpannedString(js.getString("q"));
                a = new SpannedString(js.getString("a"));
                nextReviewTimes = js.getJSONArray("b");
                numButtons = nextReviewTimes.length();
                mTextView.setText(q);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

}
