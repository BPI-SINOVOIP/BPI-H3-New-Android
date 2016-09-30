/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.tv;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;

import com.android.systemui.ExpandHelper;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NotificationRowLayout;

public class NotificationPanel extends RelativeLayout {
    private ExpandHelper mExpandHelper;
    private NotificationRowLayout latestItems;

    static final String TAG = "NotificationPanel";
    static final boolean DEBUG = false;

    boolean mShowing;
    boolean mHasClearableNotifications = false;
    int mNotificationCount = 0;
    View mNotificationScroller;
    ViewGroup mContentFrame;
    ViewGroup mContentParent;
    TvStatusBar mBar;
    View mClearButton;
    
    //animation for NotificationPanel in/out 
    private boolean initAnim = false;
    static final int POS_ANIMATION_OFFSET = 500;
    static final int ANIMATION_DURATION = 300;
    Animator inPosAnim,outPosAnim;
    Animator inAlphaAnim,outAlphaAnim;
    AnimatorSet inAnim,outAnim;
    AnimatorListener outAnimListener;

    static Interpolator sAccelerateInterpolator = new AccelerateInterpolator();
    static Interpolator sDecelerateInterpolator = new DecelerateInterpolator();

    public NotificationPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationPanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void animationInit() {
        if (!initAnim)
        {
            inPosAnim = ObjectAnimator.ofFloat(mContentParent, "translationX",-POS_ANIMATION_OFFSET,0);
            outPosAnim = ObjectAnimator.ofFloat(mContentParent, "translationX",0, -POS_ANIMATION_OFFSET);
            inAlphaAnim = ObjectAnimator.ofFloat(mContentParent, "alpha",1.0f);
            outAlphaAnim = ObjectAnimator.ofFloat(mContentParent, "alpha",0f);
            outAnimListener = new AnimatorListener(){
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.GONE);
                }
                @Override
                public void onAnimationRepeat(Animator animation) {}
                @Override
                public void onAnimationCancel(Animator animation) {}
                @Override
                public void onAnimationStart(Animator animation) {}
            };
            inAnim = new AnimatorSet();
            inAnim.setDuration(ANIMATION_DURATION);
            outAnim = new AnimatorSet();
            outAnim.setDuration(ANIMATION_DURATION);
            inAnim.play(inPosAnim).with(inAlphaAnim);
            inAnim.setInterpolator(sDecelerateInterpolator);
            outAnim.play(outPosAnim).with(outAlphaAnim);
            outAnim.setInterpolator(sAccelerateInterpolator);
            outAnim.addListener(outAnimListener);
            initAnim = true;
        }
    }


    public void setBar(TvStatusBar b) {
        mBar = b;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        setWillNotDraw(false);

        mContentParent = (ViewGroup)findViewById(R.id.content_parent);
        mContentParent.bringToFront();
        mNotificationScroller = findViewById(R.id.notification_scroller);
        mContentFrame = (ViewGroup)findViewById(R.id.content_frame);

        mClearButton = findViewById(R.id.clear_all_button);
        mClearButton.setOnClickListener(mClearButtonListener);
        mShowing = false;
    }

    @Override
    protected void onAttachedToWindow () {
        super.onAttachedToWindow();
        latestItems = (NotificationRowLayout) findViewById(R.id.content);
        int minHeight = getResources().getDimensionPixelSize(R.dimen.notification_row_min_height);
        int maxHeight = getResources().getDimensionPixelSize(R.dimen.notification_row_max_height);
        mExpandHelper = new ExpandHelper(mContext, latestItems, minHeight, maxHeight);
        mExpandHelper.setEventSource(this);
        mExpandHelper.setGravity(Gravity.BOTTOM);
    }

    private View.OnClickListener mClearButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            mBar.clearAll();
        }
    };

    private View.OnClickListener mBackListener = new View.OnClickListener() {
        public void onClick(View v) {
        	show(false);
        }
    };

    public View getClearButton() {
        return mClearButton;
    }
	
    public void show(boolean show) {
        animationInit();
        if (mShowing != show) {
           mShowing = show;
            if (show) {
                setVisibility(View.VISIBLE);
                inAnim.start();
            } else {
                outAnim.start();
                
            }
        }
    }

    public boolean isShowing() {
        return mShowing;
    }

    @Override
    public void onVisibilityChanged(View v, int vis) {
        super.onVisibilityChanged(v, vis);
        if (vis == View.VISIBLE)
            updateClearButton();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    final int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    mBar.animateCollapsePanels();
                }
                return true;
            }

        }
        return super.dispatchKeyEvent(event);
    }

    public void setNotificationCount(int n) {
        mNotificationCount = n;
    }

    public void updateClearButton() {
		if (mBar != null) {
			final boolean showClearButton = (isShowing() && mHasClearableNotifications && mNotificationScroller
					.getVisibility() == View.VISIBLE);
			getClearButton().setVisibility(
					showClearButton? View.VISIBLE : View.INVISIBLE);
		}
    }

    public void setClearable(boolean clearable) {
        mHasClearableNotifications = clearable;
    }

}

