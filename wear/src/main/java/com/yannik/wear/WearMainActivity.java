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


public class WearMainActivity extends Activity {
    private static final String TAG = "WearMain";
    private static final String W2P_REQUEST_CARD = "/com.ichi2.wear/requestCard";
    private static final String W2P_RESPOND_CARD_EASE = "/com.ichi2.wear/cardEase";
    public static TextView mTextView;
    private GoogleApiClient googleApiClient;
    private PullButton failed, hard, mid, easy;


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
                        mTextView.setText(a);
                        showButtons();

                    }
                });

                easy = (PullButton) stub.findViewById(R.id.easyButton);
                mid = (PullButton) stub.findViewById(R.id.midButton);
                hard = (PullButton) stub.findViewById(R.id.hardButton);
                failed = (PullButton) stub.findViewById(R.id.failedButton);



                /*easy.setImageRessource(R.drawable.generic_confirmation_00170);
                mid.setImageRessource(R.drawable.generic_confirmation_00170);
                hard.setImageRessource(R.drawable.close_button);
                failed.setImageRessource(R.drawable.close_button);*/

                failed.setOnSwipeListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fireMessage(W2P_RESPOND_CARD_EASE, "failed");
                        hideButtons();
                    }
                });

                easy.setOnSwipeListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fireMessage(W2P_RESPOND_CARD_EASE, "easy");
                        hideButtons();
                    }
                });
                hard.setOnSwipeListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fireMessage(W2P_RESPOND_CARD_EASE, "hard");
                        hideButtons();
                    }
                });

                mid.setOnSwipeListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fireMessage(W2P_RESPOND_CARD_EASE, "mid");
                        hideButtons();
                    }
                });
            }
        });




        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    private void hideButtons(){
        easy.setVisibility(View.GONE);
        mid.setVisibility(View.GONE);
        hard.setVisibility(View.GONE);
        failed.setVisibility(View.GONE);
    }

    private void showButtons(){
        easy.setVisibility(View.VISIBLE);
        mid.setVisibility(View.VISIBLE);
        hard.setVisibility(View.VISIBLE);
        failed.setVisibility(View.VISIBLE);
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

    Spanned q,a;
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String inc[] = intent.getStringExtra("message").split("<-!SEP!->");
            q = new SpannedString(inc[0]);
            a = new SpannedString(inc[1]);
            mTextView.setText(q);
        }
    }

}
