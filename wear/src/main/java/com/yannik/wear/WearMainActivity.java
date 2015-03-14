package com.yannik.wear;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    private static final String PATH = "/com.anki";
    private TextView mTextView;
    private Button sendButton;
    private GoogleApiClient googleApiClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wear_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();


        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                sendButton = (Button) stub.findViewById(R.id.sendButton);
                sendButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        fireMessage();
                    }
                });
            }
        });
    }

    private void fireMessage() {
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
                            PATH, "gimme the next 10".getBytes());
                    messageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Status status = sendMessageResult.getStatus();
                            Log.d(TAG, "Status: " + status.toString());
                            if (status.getStatusCode() != WearableStatusCodes.SUCCESS) {
//                                alertButton.setProgress(-1);
//                                label.setText("Tap to retry. Alert not sent :(");
                            }
                        }
                    });
                }
            }
        });
    }
}
