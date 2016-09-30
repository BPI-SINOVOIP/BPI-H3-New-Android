/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.view.View.OnFocusChangeListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.os.SystemClock;
import android.util.Log;

public class NotificationFocusListener implements OnFocusChangeListener {

    static final float SCALE_MIN=0.9f;
    static final float SCALE_MAX=1.0f;
    static final float ALPHA_MIN=0.6f;
    static final float ALPHA_MAX=1f;
    static final long ANIMATION_DURATION=170l;

    private ScaleAnimation zoomOutAnimation=new ScaleAnimation(SCALE_MAX, SCALE_MIN, SCALE_MAX, SCALE_MIN);
    private ScaleAnimation zoomInAnimation=new ScaleAnimation(SCALE_MIN, SCALE_MAX, SCALE_MIN, SCALE_MAX);
    private AlphaAnimation fadeOutAnimation=new AlphaAnimation(ALPHA_MAX, ALPHA_MIN);
    private AlphaAnimation fadeInAnimation=new AlphaAnimation(ALPHA_MIN, ALPHA_MAX);
    private AnimationSet loseFocusAnimationSet=new AnimationSet(true);
    private AnimationSet getFocusAnimationSet=new AnimationSet(true);

    private boolean lastState;
    
    public NotificationFocusListener() {
        loseFocusAnimationSet.addAnimation(zoomOutAnimation);
        loseFocusAnimationSet.addAnimation(fadeOutAnimation);
        getFocusAnimationSet.addAnimation(zoomInAnimation);
        getFocusAnimationSet.addAnimation(fadeInAnimation);
        loseFocusAnimationSet.setDuration(ANIMATION_DURATION);
        getFocusAnimationSet.setDuration(ANIMATION_DURATION);
        loseFocusAnimationSet.setFillAfter(true);
        getFocusAnimationSet.setFillAfter(true);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {

        long lastAnimationStart = -1;
        long startOffset = 0;
        if (v.getAnimation()!=null)
        {
            if (v.getAnimation().getStartTime()!=-1);
                lastAnimationStart = SystemClock.elapsedRealtime()-v.getAnimation().getStartTime();
        }
        if (ANIMATION_DURATION > lastAnimationStart)
            startOffset = ANIMATION_DURATION - lastAnimationStart;

        Animation anmt;
        if (hasFocus)
            anmt =getFocusAnimationSet;
        else
            anmt =loseFocusAnimationSet;

        //anmt.setStartOffset(startOffset);
        
        v.clearAnimation();
        v.startAnimation(anmt);
    }

}
