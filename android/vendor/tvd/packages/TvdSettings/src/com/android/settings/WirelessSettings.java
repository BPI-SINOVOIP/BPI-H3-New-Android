/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.nfc.NfcEnabler;

import java.util.Collection;

public class WirelessSettings extends RestrictedSettingsFragment
        implements OnPreferenceChangeListener {
    private static final String TAG = "WirelessSettings";

    private static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private static final String KEY_WIMAX_SETTINGS = "wimax_settings";
    private static final String KEY_ANDROID_BEAM_SETTINGS = "android_beam_settings";
    private static final String KEY_VPN_SETTINGS = "vpn_settings";
    private static final String KEY_TETHER_SETTINGS = "tether_settings";
    private static final String KEY_PROXY_SETTINGS = "proxy_settings";
    private static final String KEY_MOBILE_NETWORK_SETTINGS = "mobile_network_settings";
    private static final String KEY_MANAGE_MOBILE_PLAN = "manage_mobile_plan";
    private static final String KEY_SMS_APPLICATION = "sms_application";
    private static final String KEY_TOGGLE_NSD = "toggle_nsd"; //network service discovery
    private static final String KEY_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";

    public static final String EXIT_ECM_RESULT = "exit_ecm_result";
    public static final int REQUEST_CODE_EXIT_ECM = 1;

    private CheckBoxPreference mAirplaneModePreference;
    private NfcEnabler mNfcEnabler;
    private NfcAdapter mNfcAdapter;
    private NsdEnabler mNsdEnabler;

    private ConnectivityManager mCm;

    public WirelessSettings() {
        super(null);
    }
    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (ensurePinRestrictedPreference(preference)) {
            return true;
        }

        // Let the intents be launched by the Preference manager
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private static final String CONNECTED_TO_PROVISIONING_NETWORK_ACTION
            = "com.android.server.connectivityservice.CONNECTED_TO_PROVISIONING_NETWORK_ACTION";

    @Override
    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog: dialogId=" + dialogId);
        return super.onCreateDialog(dialogId);
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    public static boolean isRadioAllowed(Context context, String type) {
        return true;
    }

    private boolean isSmsSupported() {
        // Some tablet has sim card but could not do telephony operations. Skip those.
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        addPreferencesFromResource(R.xml.wireless_settings);

        final boolean isSecondaryUser = UserHandle.myUserId() != UserHandle.USER_OWNER;

        final Activity activity = getActivity();
        mAirplaneModePreference = (CheckBoxPreference) findPreference(KEY_TOGGLE_AIRPLANE);
        CheckBoxPreference nfc = (CheckBoxPreference) findPreference(KEY_TOGGLE_NFC);
        PreferenceScreen androidBeam = (PreferenceScreen) findPreference(KEY_ANDROID_BEAM_SETTINGS);
        CheckBoxPreference nsd = (CheckBoxPreference) findPreference(KEY_TOGGLE_NSD);

        mNfcEnabler = new NfcEnabler(activity, nfc, androidBeam);

        // Remove NSD checkbox by default
        getPreferenceScreen().removePreference(nsd);

        //mNsdEnabler = new NsdEnabler(activity, nsd);

        String toggleable = Settings.Global.getString(activity.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);

        //enable/disable wimax depending on the value in config.xml
        boolean isWimaxEnabled = !isSecondaryUser && this.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (!isWimaxEnabled) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
            if (ps != null) root.removePreference(ps);
        } else {
            if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIMAX )
                    && isWimaxEnabled) {
                Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
                ps.setDependency(KEY_TOGGLE_AIRPLANE);
            }
        }
        protectByRestrictions(KEY_WIMAX_SETTINGS);

        // Manually set dependencies for Wifi when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIFI)) {
            findPreference(KEY_VPN_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }
        if (isSecondaryUser) { // Disable VPN
            removePreference(KEY_VPN_SETTINGS);
        }
        protectByRestrictions(KEY_VPN_SETTINGS);
        // Manually set dependencies for Bluetooth when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_BLUETOOTH)) {
            // No bluetooth-dependent items in the list. Code kept in case one is added later.
        }

        // Manually set dependencies for NFC when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_NFC)) {
            findPreference(KEY_TOGGLE_NFC).setDependency(KEY_TOGGLE_AIRPLANE);
            findPreference(KEY_ANDROID_BEAM_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }

        // Remove NFC if its not available
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (mNfcAdapter == null) {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(androidBeam);
            mNfcEnabler = null;
        }


		// Remove Mobile Network Settings and Manage Mobile Plan if it's a wifi-only device.
        if (isSecondaryUser || Utils.isWifiOnly(getActivity())) {
            removePreference(KEY_MOBILE_NETWORK_SETTINGS);
            removePreference(KEY_MANAGE_MOBILE_PLAN);
        }
        protectByRestrictions(KEY_MOBILE_NETWORK_SETTINGS);
        protectByRestrictions(KEY_MANAGE_MOBILE_PLAN);

        // Remove SMS Application if the device does not support SMS
        if (!isSmsSupported()) {
            removePreference(KEY_SMS_APPLICATION);
        }

        // Remove Airplane Mode settings if it's a stationary device such as a TV.
        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
            removePreference(KEY_TOGGLE_AIRPLANE);
        }
		removePreference(KEY_TOGGLE_AIRPLANE);
		removePreference(KEY_SMS_APPLICATION);
		removePreference(KEY_MOBILE_NETWORK_SETTINGS);
		removePreference(KEY_MANAGE_MOBILE_PLAN);

        // Enable Proxy selector settings if allowed.
        Preference mGlobalProxy = findPreference(KEY_PROXY_SETTINGS);
        DevicePolicyManager mDPM = (DevicePolicyManager)
                activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // proxy UI disabled until we have better app support
        getPreferenceScreen().removePreference(mGlobalProxy);
        mGlobalProxy.setEnabled(mDPM.getGlobalProxyAdmin() == null);

        // Disable Tethering if it's not allowed or if it's a wifi-only device
        ConnectivityManager cm =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (isSecondaryUser || !cm.isTetheringSupported()) {
            getPreferenceScreen().removePreference(findPreference(KEY_TETHER_SETTINGS));
        } else {
            Preference p = findPreference(KEY_TETHER_SETTINGS);
            p.setTitle(Utils.getTetheringLabel(cm));
        }
        protectByRestrictions(KEY_TETHER_SETTINGS);

        // Enable link to CMAS app settings depending on the value in config.xml.
        boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        try {
            if (isCellBroadcastAppLinkEnabled) {
                PackageManager pm = getPackageManager();
                if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                }
            }
        } catch (IllegalArgumentException ignored) {
            isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
        }
        if (isSecondaryUser || !isCellBroadcastAppLinkEnabled) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(KEY_CELL_BROADCAST_SETTINGS);
            if (ps != null) root.removePreference(ps);
        }
        protectByRestrictions(KEY_CELL_BROADCAST_SETTINGS);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
        if (mNsdEnabler != null) {
            mNsdEnabler.resume();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
        if (mNsdEnabler != null) {
            mNsdEnabler.pause();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            Boolean isChoiceYes = data.getBooleanExtra(EXIT_ECM_RESULT, false);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_more_networks;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

}
