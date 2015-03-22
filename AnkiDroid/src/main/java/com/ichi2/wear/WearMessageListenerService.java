package com.ichi2.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
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
import com.google.android.gms.wearable.WearableListenerService;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.ichi2.anki.AbstractFlashcardViewer;
import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.DeckPicker;
import com.ichi2.anki.R;
import com.ichi2.async.CollectionLoader;
import com.ichi2.async.DeckTask;
import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Sched;

import java.util.ArrayList;
import java.util.TreeSet;

import timber.log.Timber;

/**
 * Created by Yannik on 12.03.2015.
 */
public class WearMessageListenerService extends WearableListenerService implements LoaderManager.LoaderCallbacks<Collection> {
    private static final String TAG = "Phone_MessageListener";
    private static final String W2P_REQUEST_CARD = "/com.ichi2.wear/requestCard";
    private static final String P2W_RESPOND_CARD = "/com.ichi2.wear/respondWithCard";
    private static final String W2P_RESPOND_CARD_EASE = "/com.ichi2.wear/cardEase";
    private GoogleApiClient googleApiClient;
    protected Sched mSched;
    protected Card mCurrentCard;
    private ArrayList<String> deckNames = new ArrayList<String>();

    protected DeckTask.TaskListener mAnswerCardHandler = new DeckTask.TaskListener() {
        private boolean mNoMoreCards;


        @Override
        public void onPreExecute() {
//            mProgressBar.setVisibility(View.VISIBLE);
//            mCardTimer.stop();
//            blockControls();
        }


        @Override
        public void onProgressUpdate(DeckTask.TaskData... values) {
            Resources res = getResources();

            if (mSched == null) {
                // TODO: proper testing for restored activity
                return;
            }

            mCurrentCard = values[0].getCard();
            if (mCurrentCard == null) {
                // If the card is null means that there are no more cards scheduled for review.
                mNoMoreCards = true;
            } else {
                // Start reviewing next card
                //updateTypeAnswerInfo();
                if (sendNewCardToWear) {
                    fireMessage((Html.fromHtml(mCurrentCard._getQA().get("q")).toString() + "<-!SEP!->" + Html.fromHtml(mCurrentCard.getPureAnswerForReading())).getBytes());
                    sendNewCardToWear = false;
                }
            }

            // Since reps are incremented on fetch of next card, we will miss counting the
            // last rep since there isn't a next card. We manually account for it here.
            if (mNoMoreCards) {
                mSched.setReps(mSched.getReps() + 1);
            }

            // if (mChosenAnswer.getText().equals("")) {
            // setDueMessage();
            // }
        }


        @Override
        public void onPostExecute(DeckTask.TaskData result) {
            if (!result.getBoolean()) {
                // RuntimeException occured on answering cards
                //closeReviewer(DeckPicker.RESULT_DB_ERROR, false);
                return;
            }
            // Check for no more cards before session complete. If they are both true, no more cards will take
            // precedence when returning to study options.
            //if (mNoMoreCards) {
                //closeReviewer(RESULT_NO_MORE_CARDS, true);
            //}
        }


        @Override
        public void onCancelled() {
        }
    };

    DeckTask.TaskListener mLoadCountsHandler = new DeckTask.TaskListener() {

        @SuppressWarnings("unchecked")
        @Override
        public void onPostExecute(DeckTask.TaskData result) {
            if (result == null) {
                Timber.w("loadCounts() onPostExecute :: result = null");
                return;
            }
            Object[] res = result.getObjArray();
            TreeSet<Object[]> countList = (TreeSet<Object[]>) res[0];
            Timber.d("loadCounts() onPostExecute :: result = (length %d TreeSet, %d, %d)", countList.size(), res[1], res[2]);
            updateDeckNames(countList, (Integer) res[1], (Integer) res[2]);
            onCollectionLoaded(getCol());
        }


        @Override
        public void onPreExecute() {
        }


        @Override
        public void onProgressUpdate(DeckTask.TaskData... values) {
        }


        @Override
        public void onCancelled() {
            Timber.d("loadCounts onCancelled()");
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        String path = AnkiDroidApp.getCollectionPath();
        AnkiDroidApp.openCollection(path);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();


        if (colOpen()) {
            DeckTask.launchDeckTask(DeckTask.TASK_TYPE_LOAD_DECK_COUNTS, mLoadCountsHandler, new DeckTask.TaskData(getCol()));
        } else {
            Log.v(TAG, "collection is not open!");
        }

        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    boolean sendNewCardToWear = false;
    private static final int TASK_TYPE_ANSWER_CARD_NULL = 12;
    private static final int TASK_TYPE_ANSWER_CARD = 13;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(W2P_REQUEST_CARD)) {
            final String message = new String(messageEvent.getData());
            Log.v("myTag", "Message path received on phone is: " + messageEvent.getPath());
            Log.v("myTag", "Message received on phone is: " + message);
            if (mCurrentCard != null) {
                Log.v("TEST", mCurrentCard.getPureAnswerForReading());
                Log.v("TEST2", mCurrentCard.aSimple());
                fireMessage((Html.fromHtml(mCurrentCard._getQA().get("q")).toString() + "<-!SEP!->" + Html.fromHtml(mCurrentCard.getPureAnswerForReading()).toString()).getBytes());
            } else {
                Log.v(TAG, "mCurrentCard is null");
                sendNewCardToWear = true;

                Intent messageIntent = new Intent();
                messageIntent.setAction(Intent.ACTION_SEND);
                messageIntent.putExtra("task", TASK_TYPE_ANSWER_CARD_NULL);
                LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            }


        } else if (messageEvent.getPath().equals(W2P_RESPOND_CARD_EASE)) {
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("task", TASK_TYPE_ANSWER_CARD);

            int ease = 0;
            String easeString = new String(messageEvent.getData());
            if (easeString.equals("failed")) {
                Timber.i("WearMessageListenerService:: EASE_FAILED received");
                ease = AbstractFlashcardViewer.EASE_FAILED;
            } else if (easeString.equals("hard")) {
                Timber.i("WearMessageListenerService:: EASE_HARD received");
                ease = AbstractFlashcardViewer.EASE_HARD;
            } else if (easeString.equals("mid")) {
                Timber.i("WearMessageListenerService:: EASE_MID received");
                ease = AbstractFlashcardViewer.EASE_MID;
            } else if (easeString.equals("easy")) {
                Timber.i("WearMessageListenerService:: EASE_EASY received");
                ease = AbstractFlashcardViewer.EASE_EASY;
            } else {
                ease = 0;
                return;
            }
            messageIntent.putExtra("ease", ease);

            if (ease != 0) {
                sendNewCardToWear = true;
                LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            }

        } else {
            super.onMessageReceived(messageEvent);
        }
    }

    private void fireMessage(final byte[] data) {

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
                            P2W_RESPOND_CARD, data);
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


    @Override
    public Loader<Collection> onCreateLoader(int id, Bundle args) {
        // Currently only using one loader, so ignore id
        return new CollectionLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection col) {
        if (col != null && colOpen()) {

            // Load the first card and start reviewing. Uses the answer card
            // task to load a card, but since we send null
            // as the card to answer, no card will be answered.
            onCollectionLoaded(col);

            // Since we aren't actually answering a card, decrement the rep count
            mSched.setReps(mSched.getReps() - 1);
            // Add a weak reference to current activity so that scheduler can talk to to Activity
//            mSched.setContext(new WeakReference<Context>(this));

        } else {
//            onCollectionLoadError();
        }
    }


    public void onCollectionLoaded(Collection col) {
        queryForCurrentCard();
    }

    @Override
    public void onLoaderReset(Loader<Collection> loader) {
        // We don't currently retain any references, so no need to free any data here
    }


    private void updateDeckNames(TreeSet<Object[]> decks, int eta, int count) {
        if (decks == null) {
            Timber.e("updateDecksList: empty decks list");
            return;
        }
        deckNames.clear();
        for (Object[] d : decks) {
            String[] name = ((String[]) d[0]);
            deckNames.add(DeckPicker.readableDeckName(name));
        }
    }

    public boolean colOpen() {
        return AnkiDroidApp.colIsOpen();
    }

    public Collection getCol() {
        return AnkiDroidApp.getCol();
    }

    private void queryForCurrentCard() {
        mSched = getCol().getSched();
        mSched.reset();     // Reset schedule incase card had previous been loaded
        DeckTask.launchDeckTask(DeckTask.TASK_TYPE_ANSWER_CARD, mAnswerCardHandler, new DeckTask.TaskData(mSched, null, 0));
        // Since we aren't actually answering a card, decrement the rep count
        mSched.setReps(mSched.getReps() - 1);
    }


    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int task = intent.getIntExtra("task", 1337);
            mSched = getCol().getSched();
            switch (task) {
                case TASK_TYPE_ANSWER_CARD_NULL:
                    queryForCurrentCard();
                    break;
                case TASK_TYPE_ANSWER_CARD:
                    DeckTask.launchDeckTask(DeckTask.TASK_TYPE_ANSWER_CARD, mAnswerCardHandler, new DeckTask.TaskData(mSched,
                            mCurrentCard, intent.getIntExtra("ease", AbstractFlashcardViewer.EASE_MID)));
                    break;
            }
        }
    }


}
