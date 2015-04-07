package com.yannik.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannedString;
import android.util.Log;

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

import java.util.ArrayList;
import java.util.List;

public class WearMainActivity extends FragmentActivity {
    private static final String TAG = "WearMain";

    private static GoogleApiClient googleApiClient;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wear_main);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());

        ReviewFragment reviewFragment = ReviewFragment.newInstance();
        CollectionFragment decksFragment = CollectionFragment.newInstance(null);

        decksFragment.setChooseDeckListener(new CollectionFragment.OnFragmentInteractionListener() {
            @Override
            public void onFragmentInteraction(long id) {
                fireMessage(ReviewFragment.W2P_CHOOSE_COLLECTION,""+id);
                viewPager.setCurrentItem(0);
            }
        });

        jsonReceivers.add(reviewFragment);
        jsonReceivers.add(decksFragment);
        adapter.addFragment(reviewFragment);
        adapter.addFragment(decksFragment);
        viewPager.setAdapter(adapter);

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);


        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();
        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.d(TAG, "Wear connected to Google Api");
                fireMessage(ReviewFragment.W2P_REQUEST_CARD, null);
                fireMessage(ReviewFragment.W2P_REQUEST_DECKS, null);
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "Wear connection to Google Api suspended");
            }
        });




    }




    public static void fireMessage(final String path, final String ease) {
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
                    Log.d(TAG, "firing Message with path: " + path);

                    PendingResult<MessageApi.SendMessageResult> messageResult = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(),
                            path, (ease == null ? "nextCardPlease" : ease).getBytes());
                    messageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Status status = sendMessageResult.getStatus();
                            Log.d(TAG, "Status: " + status.toString());
//                           if (status.getStatusCode() != WearableStatusCodes.SUCCESS) {
//                                alertButton.setProgress(-1);
//                                label.setText("Tap to retry. Alert not sent :(");
//                           }
                        }
                    });
                }
            }
        });
    }

    ArrayList<JsonReceiver> jsonReceivers = new ArrayList<JsonReceiver>();

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            JSONObject js = null;
            String message = intent.getStringExtra("message");
            String path = intent.getStringExtra("path");

            try {
                    js = new JSONObject(message);
            } catch (JSONException e) {}

            for(JsonReceiver jsr : jsonReceivers){
                jsr.onJsonReceive(path, js);
            }

        }
    }

    interface JsonReceiver{
        public void onJsonReceive(String path, JSONObject json);
    }

    private class PagerAdapter extends FragmentPagerAdapter {
        List<Fragment> fragmentList = null;

        public PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            fragmentList = new ArrayList<Fragment>();
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        public void addFragment(Fragment fragment) {
            fragmentList.add(fragment);
            notifyDataSetChanged();
        }
    }
}
