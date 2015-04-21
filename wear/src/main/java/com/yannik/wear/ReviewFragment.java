package com.yannik.wear;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.wearable.view.WatchViewStub;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReviewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReviewFragment extends Fragment implements WearMainActivity.JsonReceiver{
    static final String W2P_REQUEST_CARD = "/com.ichi2.wear/requestCard";
    static final String W2P_RESPOND_CARD_EASE = "/com.ichi2.wear/cardEase";
    static final String W2P_CHOOSE_COLLECTION = "/com.ichi2.wear/chooseCollection";
    static final String P2W_NO_MORE_CARDS = "/com.ichi2.wear/noMoreCards";
    static final String W2P_REQUEST_DECKS = "/com.ichi2.wear/requestDecks";
    public static TextView mTextView;
    private RelativeLayout qaOverlay;
    private PullButton failed, hard, mid, easy;
    private boolean showingEaseButtons = false, showingAnswer = false;
    private Timer easeButtonShowTimer = new Timer();

    private ScrollView qaScrollView;
    private boolean scrollViewMoved;

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment ReviewFragment.
     */
    public static ReviewFragment newInstance() {
        ReviewFragment fragment = new ReviewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public ReviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void hideButtons() {
        showingEaseButtons = false;
        easy.setVisibility(View.GONE);
        mid.setVisibility(View.GONE);
        hard.setVisibility(View.GONE);
        failed.setVisibility(View.GONE);
    }

    private void showButtons() {
        if(nextReviewTimes == null)return;
        switch (numButtons){
            case 2:
                mid.centerX();
                failed.centerX();

                mid.slideIn(100);
                failed.slideIn(300);
                try {
                    failed.setText(nextReviewTimes.getString(0));
                    mid.setText(nextReviewTimes.getString(1));
                } catch (JSONException e) {}
                break;
            case 3:
                failed.centerX();
                easy.left();
                mid.right();

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
                easy.left();
                mid.right();
                hard.left();
                failed.right();

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


    private void showAnswer(){
        mTextView.setText(a);
        showingEaseButtons = true;
        showingAnswer = true;
        showButtons();
    }

    private void blockControls(){
        hideButtons();
        qaOverlay.setOnClickListener(null);
    }

    private void unblockControls(){
        qaOverlay.setOnClickListener(textClickListener);
    }


    private View.OnClickListener textClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!scrollViewMoved && !showingEaseButtons) {
                showAnswer();
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_review, container, false);

        final WatchViewStub stub = (WatchViewStub) view.findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                qaOverlay = (RelativeLayout) stub.findViewById(R.id.questionAnswerOverlay);
                qaScrollView = (ScrollView) stub.findViewById(R.id.questionAnswerScrollView);
                final GestureDetector gestureDetector = new GestureDetector(getActivity().getBaseContext(), new GestureDetector.SimpleOnGestureListener(){
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (showingEaseButtons){
                            if(showingAnswer) {
                                mTextView.setText(q);
                            }else{
                                mTextView.setText(a);
                            }
                            showingAnswer = !showingAnswer;
                        }
                        return false;
                    }
                });


                qaOverlay.setOnClickListener(textClickListener);


                qaOverlay.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
//                        Log.v("test", "ontouchevent " + event.getAction());
                        if(event.getActionMasked() == MotionEvent.ACTION_DOWN){
                            scrollViewMoved = false;
                        }
                        gestureDetector.onTouchEvent(event);
                        qaScrollView.onTouchEvent(event);
                        return false;

                    }
                });

                qaScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        scrollViewMoved = true;
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

                        WearMainActivity.fireMessage(W2P_RESPOND_CARD_EASE, ease);
                        hideButtons();

                    }
                };


                failed.setOnSwipeListener(easeButtonListener);
                easy.setOnSwipeListener(easeButtonListener);
                hard.setOnSwipeListener(easeButtonListener);
                mid.setOnSwipeListener(easeButtonListener);
//                hideButtons();
            }
        });

        return view;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onJsonReceive(String path, JSONObject js) {
        if(path.equals(ListenerService.P2W_RESPOND_CARD)) {
            try {
                hideButtons();
                unblockControls();
                q = new SpannedString(js.getString("q"));
                a = new SpannedString(js.getString("a"));
                nextReviewTimes = js.getJSONArray("b");
                numButtons = nextReviewTimes.length();
                mTextView.setText(q);
                qaScrollView.scrollTo(0,0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else if(path.equals(P2W_NO_MORE_CARDS)){
            blockControls();
            mTextView.setText("No more Cards");
        }
    }



    int numButtons = 4;
    JSONArray nextReviewTimes;
    Spanned q, a;
}
