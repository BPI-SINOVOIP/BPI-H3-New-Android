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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.StatusBarManager;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.view.KeyEvent;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.util.Slog;
import android.util.Log;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.content.res.Resources;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;

import com.android.systemui.R;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationData.Entry;
import com.android.systemui.statusbar.policy.NotificationRowLayout;

/*
 * Status bar implementation for "large screen" products that mostly present no on-screen nav
 */

public class TvStatusBar extends BaseStatusBar {
	public static final boolean DEBUG = true;
	public static final String TAG = "TvStatusBar";

    public static final int MSG_OPEN_NOTIFICATION_PANEL = 1000;
    public static final int MSG_CLOSE_NOTIFICATION_PANEL = 1001;
    NotificationPanel mNotificationPanel;
    WindowManager.LayoutParams mNotificationPanelParams;

    private ScaleAnimation zoomOutAnimation=new ScaleAnimation(NotificationFocusListener.SCALE_MAX,
        0.1f, NotificationFocusListener.SCALE_MAX, 0.1f);
    private AlphaAnimation fadeOutAnimation=new AlphaAnimation(NotificationFocusListener.ALPHA_MAX,
        0f);
    private AnimationSet itemInitAnimationSet=new AnimationSet(true);

    @Override
    public void addIcon(String slot, int index, int viewIndex, StatusBarIcon icon) {
    }

    @Override
    public void updateIcon(String slot, int index, int viewIndex, StatusBarIcon old,
            StatusBarIcon icon) {
    }

    @Override
    public void removeIcon(String slot, int index, int viewIndex) {
    }

    public void addNotification(IBinder key, StatusBarNotification notification) {
    	if (DEBUG) Slog.d(TAG, "add Notification(" + key + " -> " + notification + ")");
		Entry mEntry = createNotificationViews(key, notification);
		if (mEntry == null) {
			return;
		}
        addNotificationViews(mEntry);

        Slog.d(TAG,"Notification size " + mNotificationData.size());

        setAreThereNotifications();
        mNotificationPanel.updateClearButton();
    }

    @Override
    protected BaseStatusBar.H createHandler() {
        return new TvStatusBar.H();
    }
    
    private class H extends BaseStatusBar.H {
        public void handleMessage(Message m) {
            super.handleMessage(m);
            switch (m.what) {
            case MSG_OPEN_NOTIFICATION_PANEL:
                if (DEBUG) Slog.d(TAG, "opening notifications panel");
                if (!mNotificationPanel.isShowing()) {
                    mNotificationPanel.show(true);
                    
                }
                break;
            case MSG_CLOSE_NOTIFICATION_PANEL:
                if (DEBUG) Slog.d(TAG, "closing notifications panel");
                if (mNotificationPanel.isShowing()) {
                    mNotificationPanel.show(false);
                }
                break;
            }
        }
    }

    @Override
    public void removeNotification(IBinder key) {
        if (DEBUG) Slog.d(TAG, "removeNotification(" + key + ")");
        removeNotificationViews(key);
        setAreThereNotifications();
        mNotificationPanel.updateClearButton();
    }

    @Override
    public void disable(int state) {
    }

    @Override
    public void animateExpandNotificationsPanel() {
        mHandler.removeMessages(MSG_OPEN_NOTIFICATION_PANEL);
        mHandler.sendEmptyMessage(MSG_OPEN_NOTIFICATION_PANEL);
        sendBroadcast();
    }

    @Override
    public void animateCollapsePanels(int flags) {
    	 if ((flags & CommandQueue.FLAG_EXCLUDE_NOTIFICATION_PANEL) == 0) {
             mHandler.removeMessages(MSG_CLOSE_NOTIFICATION_PANEL);
             mHandler.sendEmptyMessage(MSG_CLOSE_NOTIFICATION_PANEL);
         }
    }

    @Override
    public void setSystemUiVisibility(int vis, int mask) {
    }

    @Override
    public void topAppWindowChanged(boolean visible) {
        if (DEBUG) {
            Slog.d(TAG, (visible?"showing":"hiding") + " the MENU button");
        }
    }

    @Override
    public void setImeWindowStatus(IBinder token, int vis, int backDisposition) {
    }

    @Override
    public void setHardKeyboardStatus(boolean available, boolean enabled) {
    }

    @Override
    public void toggleRecentApps() {
    }

    @Override // CommandQueue
    public void setWindowState(int window, int state) {
    }

    private int getNotificationPanelHeight() {
    	final Point size = new Point();
    	mWindowManager.getDefaultDisplay().getRealSize(size);

    	if(DEBUG)
    		Slog.d(TAG, "Screen height(size.y): " + size.y);
    	return size.y;
    }
    protected void addPanelWindows() {
        final Context context = mContext;
        final Resources res = mContext.getResources();

        // Notification Panel
        mNotificationPanel = (NotificationPanel)View.inflate(context,
                R.layout.tv_system_bar_notification_panel, null);
        mNotificationPanel.setBar(this);
        mNotificationPanel.setVisibility(View.INVISIBLE);
        Log.e(TAG,"width = " + res.getDimensionPixelSize(R.dimen.notification_panel_width));
        WindowManager.LayoutParams lp = mNotificationPanelParams = new WindowManager.LayoutParams(
                res.getDimensionPixelSize(R.dimen.notification_panel_width),
                getNotificationPanelHeight(),
                WindowManager.LayoutParams.TYPE_NAVIGATION_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.dimAmount = 0.8f;
        		
        mWindowManager.addView(mNotificationPanel, lp);

        mPile = (NotificationRowLayout)mNotificationPanel.findViewById(R.id.content);
        mPile.removeAllViews();
        mPile.setLongPressListener(getNotificationLongClicker());

        ScrollView scroller = (ScrollView)mPile.getParent();
        scroller.setFillViewport(true);

        itemInitAnimationSet.addAnimation(zoomOutAnimation);
        itemInitAnimationSet.addAnimation(fadeOutAnimation);
        itemInitAnimationSet.setDuration(50);
        itemInitAnimationSet.setFillAfter(true);
    }
    @Override
    protected void createAndAddWindows() {
    	addPanelWindows();
    }

    @Override
    protected WindowManager.LayoutParams getSearchLayoutParams(
            LayoutParams layoutParams) {
        return null;
    }

    @Override
    protected void haltTicker() {
    }

    @Override
    protected void setAreThereNotifications() {
    	 if (mNotificationPanel != null) {
             mNotificationPanel.setClearable(isDeviceProvisioned() && mNotificationData.hasClearableItems());
         }
    }

    @Override
    protected void updateNotificationIcons() {
		loadNotificationPanel();
        return;
    }

    private void loadNotificationPanel() {
		if (mPile == null) return;
        int N = mNotificationData.size();

        ArrayList<View> toShow = new ArrayList<View>();

        final boolean provisioned = isDeviceProvisioned();
        Slog.d(TAG,"provisioned: " + provisioned + " size " + N);
        // If the device hasn't been through Setup, we only show system notifications
        for (int i=0; i<N; i++) {
            Entry ent = mNotificationData.get(N-i-1);
            if (provisioned || showNotificationEvenIfUnprovisioned(ent.notification)) {
                toShow.add(ent.row);
                Slog.d(TAG,"provisioned: " + provisioned + " i: " + i);
            }
        }

        ArrayList<View> toRemove = new ArrayList<View>();
        for (int i=0; i<mPile.getChildCount(); i++) {
            View child = mPile.getChildAt(i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }

        for (View remove : toRemove) {
            mPile.removeView(remove);
        }

        for (int i=0; i<toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                // the notification panel has the most important things at the bottom
                mPile.addView(v, Math.min(toShow.size()-1-i, mPile.getChildCount()));
            }
        }
        mNotificationPanel.setNotificationCount(toShow.size());
    }

    @Override
    protected void tick(IBinder key, StatusBarNotification n, boolean firstTime) {
    }

    @Override
    protected void updateExpandedViewPos(int expandedPosition) {
    }

    @Override
    protected int getExpandedViewMaxHeight() {
    	return getNotificationPanelHeight();
    }

    @Override
    protected boolean shouldDisableNavbarGestures() {
        return true;
    }

    public View getStatusBarView() {
        return null;
    }

    @Override
    public void resetHeadsUpDecayTimer() {
    }

    @Override
    public void animateExpandSettingsPanel() {
    }

    @Override
    protected void refreshLayout(int layoutDirection) {
    }

    public void clearAll() {
        try {
            mBarService.onClearAllNotifications();
        } catch (RemoteException ex) {
            // system process is dead if we're here.
        }
        for (int i=0; i<mNotificationData.size(); i++) {
        	Entry ent = mNotificationData.get(i);
        	if(ent.notification.isClearable()){
        		 ent.content.startAnimation(itemInitAnimationSet);
        	} 	
        }
        animateCollapsePanels();

    }

    public void animateCollapsePanels() {
        animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE);
    }
    
    private void sendBroadcast(){
    	Intent intent = new Intent("com.softwinner.systemui.expand");
    	intent.putExtra("expand", true);
    	mContext.sendBroadcast(intent);	
    }

}
