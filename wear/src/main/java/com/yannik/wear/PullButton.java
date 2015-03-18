package com.yannik.wear;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Yannik on 06.02.2015.
 */
public class PullButton extends RelativeLayout {

    private float homePosition, extendedPosition;
    private int minMovementDistance = 50;
    private Point displaySize = new Point();
    private int exitY = 0;
    ImageButton icon;
    TextView textView;
    int imageResId = -1;
    boolean upsideDown;
    public PullButton(Context context) {
        this(context, null);
    }

    public PullButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PullButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
       // super(context, attrs, defStyleAttr, defStyleRes);
        super(context, attrs, defStyleAttr);
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.pull_button, this);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        display.getSize(displaySize);


        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullButton);

        final int N = a.getIndexCount();



        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                icon = (ImageButton)findViewById(R.id.icon);
                textView = (TextView) findViewById(R.id.textView);

                for (int i = 0; i < N; ++i)
                {
                    int attr = a.getIndex(i);
                    switch (attr)
                    {
                        case R.styleable.PullButton_icon:
                            imageResId = a.getResourceId(attr, R.drawable.close_button);
                            break;
                        case R.styleable.PullButton_text:
                            String text = a.getString(attr);
                            textView.setText(text);
                            break;
                        case R.styleable.PullButton_upsideDown:
                            upsideDown = a.getBoolean(attr, false);
                            if(upsideDown){
                                textView.setY(0);
                                icon.setY(textView.getHeight());
                            }
                            break;
                    }
                }
                a.recycle();



                if(imageResId != -1){
                    icon.setImageResource(imageResId);
                }

                if(upsideDown){
                    homePosition = getY() - textView.getHeight();
                    exitY = displaySize.y + 10;
                }else {
                    homePosition = getY() + textView.getHeight();
                    exitY = -getHeight() - 10;
                }
                extendedPosition = getY();
                minMovementDistance = displaySize.y / 2;

                setY(homePosition);

            }
        });



        this.animate().setInterpolator(new LinearInterpolator());
        this.setOnTouchListener(new SwipeTouchListener());

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    class SwipeTouchListener implements OnTouchListener {
        private float yDiff;

        private VelocityTracker mVelocityTracker;

        @Override
        public boolean onTouch(final View v, MotionEvent event) {
/*            if(exitY == Integer.MAX_VALUE){
                exitY = -v.getHeight() - 10;
            }*/
            float viewPositionY = v.getY();
            float eventPositionY = event.getRawY();
            if(upsideDown){
                viewPositionY = displaySize.y - viewPositionY - v.getHeight();
                eventPositionY = displaySize.y - eventPositionY;
            }


            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    yDiff = eventPositionY - viewPositionY;

                    mVelocityTracker = VelocityTracker.obtain();
                    event.offsetLocation(0, homePosition - viewPositionY);
                    mVelocityTracker.addMovement(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(!upsideDown) {
                        v.setY(eventPositionY - yDiff);
                    }else{
                        v.setY(displaySize.y - (eventPositionY - yDiff) - v.getHeight());
                    }
                    event.offsetLocation(0, homePosition - viewPositionY);
                    mVelocityTracker.addMovement(event);

                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:

                    mVelocityTracker.computeCurrentVelocity(1);
                    float yVelocity = mVelocityTracker.getYVelocity();

                    if(viewPositionY < minMovementDistance && yVelocity >= 0) {

                        System.out.println("Velocity is: " + yVelocity);
                        System.out.println("Distance is: " + (viewPositionY - exitY));
                        v.animate()
                                .y(exitY)
                                .setDuration(Math.min((long) ((Math.abs(viewPositionY - exitY))/Math.abs(mVelocityTracker.getYVelocity())),500))
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if(ocl!=null)ocl.onClick(PullButton.this);
                                        v.setY(displaySize.y + 10);
                                        v.animate().y(homePosition).setListener(null).setDuration(250);
                                    }
                                });
                    }else if(viewPositionY + v.getHeight() < displaySize.y){
                        v.animate().y(extendedPosition).setListener(null);
                    }else{
                        v.animate().y(homePosition).setListener(null);
                    }

                    mVelocityTracker.recycle();
                    break;
            }


            return true;
        }
    }

    OnClickListener ocl;
    public void setOnSwipeListener(OnClickListener ocl){
        this.ocl = ocl;
    }

    public void setImageRessource(int res){
        imageResId = res;
        if(icon != null) {
            icon.setImageResource(res);
        }
    }

}
