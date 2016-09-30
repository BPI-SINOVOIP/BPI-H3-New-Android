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

package android.hardware.display;

import android.content.Context;
import android.os.Handler;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;

import java.util.ArrayList;

import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import android.util.Log;

/**
 * Manages the properties of attached displays.
 * <p>
 * Get an instance of this class by calling
 * {@link android.content.Context#getSystemService(java.lang.String)
 * Context.getSystemService()} with the argument
 * {@link android.content.Context#DISPLAY_SERVICE}.
 * </p>
 */
public final class DisplayManager {
    private static final String TAG = "DisplayManager";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final DisplayManagerGlobal mGlobal;

    private final Object mLock = new Object();
    private final SparseArray<Display> mDisplays = new SparseArray<Display>();

    private final ArrayList<Display> mTempDisplays = new ArrayList<Display>();

    /**
     * Broadcast receiver that indicates when the Wifi display status changes.
     * <p>
     * The status is provided as a {@link WifiDisplayStatus} object in the
     * {@link #EXTRA_WIFI_DISPLAY_STATUS} extra.
     * </p><p>
     * This broadcast is only sent to registered receivers and can only be sent by the system.
     * </p>
     * @hide
     */
    public static final String ACTION_WIFI_DISPLAY_STATUS_CHANGED =
            "android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED";

    /**
     * Contains a {@link WifiDisplayStatus} object.
     * @hide
     */
    public static final String EXTRA_WIFI_DISPLAY_STATUS =
            "android.hardware.display.extra.WIFI_DISPLAY_STATUS";

    /**
     * Display category: Presentation displays.
     * <p>
     * This category can be used to identify secondary displays that are suitable for
     * use as presentation displays such as HDMI or Wireless displays.  Applications
     * may automatically project their content to presentation displays to provide
     * richer second screen experiences.
     * </p>
     *
     * @see android.app.Presentation
     * @see Display#FLAG_PRESENTATION
     * @see #getDisplays(String)
     */
    public static final String DISPLAY_CATEGORY_PRESENTATION =
            "android.hardware.display.category.PRESENTATION";

    /**
     * Virtual display flag: Create a public display.
     *
     * <h3>Public virtual displays</h3>
     * <p>
     * When this flag is set, the virtual display is public.
     * </p><p>
     * A public virtual display behaves just like most any other display that is connected
     * to the system such as an HDMI or Wireless display.  Applications can open
     * windows on the display and the system may mirror the contents of other displays
     * onto it.
     * </p><p>
     * Creating a public virtual display requires the
     * {@link android.Manifest.permission#CAPTURE_VIDEO_OUTPUT}
     * or {@link android.Manifest.permission#CAPTURE_SECURE_VIDEO_OUTPUT} permission.
     * These permissions are reserved for use by system components and are not available to
     * third-party applications.
     * </p>
     *
     * <h3>Private virtual displays</h3>
     * <p>
     * When this flag is not set, the virtual display is private as defined by the
     * {@link Display#FLAG_PRIVATE} display flag.
     * </p>
     * A private virtual display belongs to the application that created it.
     * Only the a owner of a private virtual display is allowed to place windows upon it.
     * The private virtual display also does not participate in display mirroring: it will
     * neither receive mirrored content from another display nor allow its own content to
     * be mirrored elsewhere.  More precisely, the only processes that are allowed to
     * enumerate or interact with the private display are those that have the same UID as the
     * application that originally created the private virtual display.
      * </p>
     *
     * @see #createVirtualDisplay
     */
    public static final int VIRTUAL_DISPLAY_FLAG_PUBLIC = 1 << 0;

    /**
     * Virtual display flag: Create a presentation display.
     *
     * <h3>Presentation virtual displays</h3>
     * <p>
     * When this flag is set, the virtual display is registered as a presentation
     * display in the {@link #DISPLAY_CATEGORY_PRESENTATION presentation display category}.
     * Applications may automatically project their content to presentation displays
     * to provide richer second screen experiences.
     * </p>
     *
     * <h3>Non-presentation virtual displays</h3>
     * <p>
     * When this flag is not set, the virtual display is not registered as a presentation
     * display.  Applications can still project their content on the display but they
     * will typically not do so automatically.  This option is appropriate for
     * more special-purpose displays.
     * </p>
     *
     * @see android.app.Presentation
     * @see #createVirtualDisplay
     * @see #DISPLAY_CATEGORY_PRESENTATION
     * @see Display#FLAG_PRESENTATION
     */
    public static final int VIRTUAL_DISPLAY_FLAG_PRESENTATION = 1 << 1;

    /**
     * Virtual display flag: Create a secure display.
     *
     * <h3>Secure virtual displays</h3>
     * <p>
     * When this flag is set, the virtual display is considered secure as defined
     * by the {@link Display#FLAG_SECURE} display flag.  The caller promises to take
     * reasonable measures, such as over-the-air encryption, to prevent the contents
     * of the display from being intercepted or recorded on a persistent medium.
     * </p><p>
     * Creating a secure virtual display requires the
     * {@link android.Manifest.permission#CAPTURE_SECURE_VIDEO_OUTPUT} permission.
     * This permission is reserved for use by system components and is not available to
     * third-party applications.
     * </p>
     *
     * <h3>Non-secure virtual displays</h3>
     * <p>
     * When this flag is not set, the virtual display is considered unsecure.
     * The content of secure windows will be blanked if shown on this display.
     * </p>
     *
     * @see Display#FLAG_SECURE
     * @see #createVirtualDisplay
     */
    public static final int VIRTUAL_DISPLAY_FLAG_SECURE = 1 << 2;

    public static final String EXTRA_HDMISTATUS = "hdmistatus";

    public static final int DISPLAY_2D_ORIGINAL                  = 0;
    public static final int DISPLAY_2D_LEFT                      = 1;
    public static final int DISPLAY_2D_TOP                       = 2;
    public static final int DISPLAY_3D_LEFT_RIGHT_HDMI           = 3;
    public static final int DISPLAY_3D_TOP_BOTTOM_HDMI           = 4;
    public static final int DISPLAY_2D_DUAL_STREAM               = 5;
    public static final int DISPLAY_3D_DUAL_STREAM               = 6;

    public static final int DISPLAY_OUTPUT_TYPE_NONE             = 0;
    public static final int DISPLAY_OUTPUT_TYPE_LCD              = 1;
    public static final int DISPLAY_OUTPUT_TYPE_TV               = 2;
    public static final int DISPLAY_OUTPUT_TYPE_HDMI             = 4;
    public static final int DISPLAY_OUTPUT_TYPE_VGA              = 8;

    public static final int DISPLAY_TVFORMAT_480I                = 0;
    public static final int DISPLAY_TVFORMAT_576I                = 1;
    public static final int DISPLAY_TVFORMAT_480P                = 2;
    public static final int DISPLAY_TVFORMAT_576P                = 3;
    public static final int DISPLAY_TVFORMAT_720P_50HZ           = 4;
    public static final int DISPLAY_TVFORMAT_720P_60HZ           = 5;
    public static final int DISPLAY_TVFORMAT_1080I_50HZ          = 6;
    public static final int DISPLAY_TVFORMAT_1080I_60HZ          = 7;
    public static final int DISPLAY_TVFORMAT_1080P_24HZ          = 8;
    public static final int DISPLAY_TVFORMAT_1080P_50HZ          = 9;
    public static final int DISPLAY_TVFORMAT_1080P_60HZ          = 0xa;
    public static final int DISPLAY_TVFORMAT_3840_2160P_30HZ     = 0x1c;
    public static final int DISPLAY_TVFORMAT_3840_2160P_25HZ     = 0x1d;
    public static final int DISPLAY_TVFORMAT_3840_2160P_24HZ     = 0x1e;
    public static final int DISPLAY_TVFORMAT_PAL                 = 0xb;
    public static final int DISPLAY_TVFORMAT_PAL_SVIDEO          = 0xc;
    public static final int DISPLAY_TVFORMAT_PAL_CVBS_SVIDEO     = 0xd;
    public static final int DISPLAY_TVFORMAT_NTSC                = 0xe;
    public static final int DISPLAY_TVFORMAT_NTSC_SVIDEO         = 0xf;
    public static final int DISPLAY_TVFORMAT_NTSC_CVBS_SVIDEO    = 0x10;
    public static final int DISPLAY_TVFORMAT_PAL_M               = 0x11;
    public static final int DISPLAY_TVFORMAT_PAL_M_SVIDEO        = 0x12;
    public static final int DISPLAY_TVFORMAT_PAL_M_CVBS_SVIDEO   = 0x13;
    public static final int DISPLAY_TVFORMAT_PAL_NC              = 0x14;
    public static final int DISPLAY_TVFORMAT_PAL_NC_SVIDEO       = 0x15;
    public static final int DISPLAY_TVFORMAT_PAL_NC_CVBS_SVIDEO  = 0x16;

    public static final int DISPLAY_VGA_FORMAT_640x480P_60HZ = 0x0;
    public static final int DISPLAY_VGA_FORMAT_800x600P_60HZ = 0x1;
    public static final int DISPLAY_VGA_FORMAT_1024x768P_60HZ = 0x2;
    public static final int DISPLAY_VGA_FORMAT_1280x768P_60HZ = 0x3;
    public static final int DISPLAY_VGA_FORMAT_1280x800P_60HZ = 0x4;
    public static final int DISPLAY_VGA_FORMAT_1366x768P_60HZ = 0x5;
    public static final int DISPLAY_VGA_FORMAT_1440x900P_60HZ = 0x6;
    public static final int DISPLAY_VGA_FORMAT_1920x1080P_60HZ = 0x7;
    public static final int DISPLAY_VGA_FORMAT_1920x1200P_60HZ = 0x8;

    /*@hide*/
    public static final int DISPLAY_OUTPUT_TYPE_MASK = 0xff00;
    /*@hide*/
    public static final int DISPLAY_OUTPUT_MODE_MASK = 0xff;

    /* keep the same as hardware/libhardware/include/hardware/Hwcomposer.h */
    private static final int DISPLAY_CMD_SET3DMODE               = 0x01;
    private static final int DISPLAY_CMD_SETOUTPUTMODE           = 0x06;
    private static final int DISPLAY_CMD_SETMARGIN              = 0x07;
    private static final int DISPLAY_CMD_SETSATURATION           = 0x08;
    private static final int DISPLAY_CMD_SETHUE                  = 0x09;
    private static final int DISPLAY_CMD_SETCONTRAST             = 0x0a;
    private static final int DISPLAY_CMD_SETBRIGHT               = 0x0b;
    private static final int DISPLAY_CMD_SET3DLAYEROFFSET        = 0x0c;
    private static final int DISPLAY_CMD_IS_SUPPORT_HDMI_MODE    = 0x19;
    private static final int DISPLAY_CMD_GETSUPPORT3DMODE        = 0x20;
    private static final int DISPLAY_CMD_GETOUTPUTTYPE           = 0x21;
    private static final int DISPLAY_CMD_GETOUTPUTMODE           = 0x22;
    private static final int DISPLAY_CMD_GETSATURATION           = 0x23;
    private static final int DISPLAY_CMD_GETHUE                  = 0x24;
    private static final int DISPLAY_CMD_GETCONTRAST             = 0x25;
    private static final int DISPLAY_CMD_GETBRIGHT               = 0x26;
    private static final int DISPLAY_CMD_GETMARGIN_W               = 0x27;
    private static final int DISPLAY_CMD_GETMARGIN_H               = 0x28;

    private final String RSL_FILENAME = "/mnt/Reserve0/disp_rsl.fex";
    private final String MARGIN_FILENAME = "/mnt/Reserve0/disp_margin.fex";

    /** @hide */
    public DisplayManager(Context context) {
        mContext = context;
        mGlobal = DisplayManagerGlobal.getInstance();
    }

    /**
     * Gets information about a logical display.
     *
     * The display metrics may be adjusted to provide compatibility
     * for legacy applications.
     *
     * @param displayId The logical display id.
     * @return The display object, or null if there is no valid display with the given id.
     */
    public Display getDisplay(int displayId) {
        synchronized (mLock) {
            return getOrCreateDisplayLocked(displayId, false /*assumeValid*/);
        }
    }

    /**
     * Gets all currently valid logical displays.
     *
     * @return An array containing all displays.
     */
    public Display[] getDisplays() {
        return getDisplays(null);
    }

    /**
     * Gets all currently valid logical displays of the specified category.
     * <p>
     * When there are multiple displays in a category the returned displays are sorted
     * of preference.  For example, if the requested category is
     * {@link #DISPLAY_CATEGORY_PRESENTATION} and there are multiple presentation displays
     * then the displays are sorted so that the first display in the returned array
     * is the most preferred presentation display.  The application may simply
     * use the first display or allow the user to choose.
     * </p>
     *
     * @param category The requested display category or null to return all displays.
     * @return An array containing all displays sorted by order of preference.
     *
     * @see #DISPLAY_CATEGORY_PRESENTATION
     */
    public Display[] getDisplays(String category) {
        final int[] displayIds = mGlobal.getDisplayIds();
        synchronized (mLock) {
            try {
                if (category == null) {
                    addAllDisplaysLocked(mTempDisplays, displayIds);
                } else if (category.equals(DISPLAY_CATEGORY_PRESENTATION)) {
                    addPresentationDisplaysLocked(mTempDisplays, displayIds, Display.TYPE_WIFI);
                    addPresentationDisplaysLocked(mTempDisplays, displayIds, Display.TYPE_HDMI);
                    addPresentationDisplaysLocked(mTempDisplays, displayIds, Display.TYPE_OVERLAY);
                    addPresentationDisplaysLocked(mTempDisplays, displayIds, Display.TYPE_VIRTUAL);
                }
                return mTempDisplays.toArray(new Display[mTempDisplays.size()]);
            } finally {
                mTempDisplays.clear();
            }
        }
    }

    private void addAllDisplaysLocked(ArrayList<Display> displays, int[] displayIds) {
        for (int i = 0; i < displayIds.length; i++) {
            Display display = getOrCreateDisplayLocked(displayIds[i], true /*assumeValid*/);
            if (display != null) {
                displays.add(display);
            }
        }
    }

    private void addPresentationDisplaysLocked(
            ArrayList<Display> displays, int[] displayIds, int matchType) {
        for (int i = 0; i < displayIds.length; i++) {
            Display display = getOrCreateDisplayLocked(displayIds[i], true /*assumeValid*/);
            if (display != null
                    && (display.getFlags() & Display.FLAG_PRESENTATION) != 0
                    && display.getType() == matchType) {
                displays.add(display);
            }
        }
    }

    private Display getOrCreateDisplayLocked(int displayId, boolean assumeValid) {
        Display display = mDisplays.get(displayId);
        if (display == null) {
            display = mGlobal.getCompatibleDisplay(displayId,
                    mContext.getDisplayAdjustments(displayId));
            if (display != null) {
                mDisplays.put(displayId, display);
            }
        } else if (!assumeValid && !display.isValid()) {
            display = null;
        }
        return display;
    }

    /**
     * Registers an display listener to receive notifications about when
     * displays are added, removed or changed.
     *
     * @param listener The listener to register.
     * @param handler The handler on which the listener should be invoked, or null
     * if the listener should be invoked on the calling thread's looper.
     *
     * @see #unregisterDisplayListener
     */
    public void registerDisplayListener(DisplayListener listener, Handler handler) {
        mGlobal.registerDisplayListener(listener, handler);
    }

    /**
     * Unregisters an input device listener.
     *
     * @param listener The listener to unregister.
     *
     * @see #registerDisplayListener
     */
    public void unregisterDisplayListener(DisplayListener listener) {
        mGlobal.unregisterDisplayListener(listener);
    }

    /**
     * Starts scanning for available Wifi displays.
     * The results are sent as a {@link #ACTION_WIFI_DISPLAY_STATUS_CHANGED} broadcast.
     * <p>
     * Calls to this method nest and must be matched by an equal number of calls to
     * {@link #stopWifiDisplayScan()}.
     * </p><p>
     * Requires {@link android.Manifest.permission#CONFIGURE_WIFI_DISPLAY}.
     * </p>
     *
     * @hide
     */
    public void startWifiDisplayScan() {
        mGlobal.startWifiDisplayScan();
    }

    /**
     * Stops scanning for available Wifi displays.
     * <p>
     * Requires {@link android.Manifest.permission#CONFIGURE_WIFI_DISPLAY}.
     * </p>
     *
     * @hide
     */
    public void stopWifiDisplayScan() {
        mGlobal.stopWifiDisplayScan();
    }

    /**
     * Connects to a Wifi display.
     * The results are sent as a {@link #ACTION_WIFI_DISPLAY_STATUS_CHANGED} broadcast.
     * <p>
     * Automatically remembers the display after a successful connection, if not
     * already remembered.
     * </p><p>
     * Requires {@link android.Manifest.permission#CONFIGURE_WIFI_DISPLAY}.
     * </p>
     *
     * @param deviceAddress The MAC address of the device to which we should connect.
     * @hide
     */
    public void connectWifiDisplay(String deviceAddress) {
        mGlobal.connectWifiDisplay(deviceAddress);
    }

    /** @hide */
    public void pauseWifiDisplay() {
        mGlobal.pauseWifiDisplay();
    }

    /** @hide */
    public void resumeWifiDisplay() {
        mGlobal.resumeWifiDisplay();
    }

    /**
     * Disconnects from the current Wifi display.
     * The results are sent as a {@link #ACTION_WIFI_DISPLAY_STATUS_CHANGED} broadcast.
     * @hide
     */
    public void disconnectWifiDisplay() {
        mGlobal.disconnectWifiDisplay();
    }

    /**
     * Renames a Wifi display.
     * <p>
     * The display must already be remembered for this call to succeed.  In other words,
     * we must already have successfully connected to the display at least once and then
     * not forgotten it.
     * </p><p>
     * Requires {@link android.Manifest.permission#CONFIGURE_WIFI_DISPLAY}.
     * </p>
     *
     * @param deviceAddress The MAC address of the device to rename.
     * @param alias The alias name by which to remember the device, or null
     * or empty if no alias should be used.
     * @hide
     */
    public void renameWifiDisplay(String deviceAddress, String alias) {
        mGlobal.renameWifiDisplay(deviceAddress, alias);
    }

    /**
     * Forgets a previously remembered Wifi display.
     * <p>
     * Automatically disconnects from the display if currently connected to it.
     * </p><p>
     * Requires {@link android.Manifest.permission#CONFIGURE_WIFI_DISPLAY}.
     * </p>
     *
     * @param deviceAddress The MAC address of the device to forget.
     * @hide
     */
    public void forgetWifiDisplay(String deviceAddress) {
        mGlobal.forgetWifiDisplay(deviceAddress);
    }

    /**
     * Gets the current Wifi display status.
     * Watch for changes in the status by registering a broadcast receiver for
     * {@link #ACTION_WIFI_DISPLAY_STATUS_CHANGED}.
     *
     * @return The current Wifi display status.
     * @hide
     */
    public WifiDisplayStatus getWifiDisplayStatus() {
        return mGlobal.getWifiDisplayStatus();
    }

    /**
     * Creates a virtual display.
     * <p>
     * The content of a virtual display is rendered to a {@link Surface} provided
     * by the application.
     * </p><p>
     * The virtual display should be {@link VirtualDisplay#release released}
     * when no longer needed.  Because a virtual display renders to a surface
     * provided by the application, it will be released automatically when the
     * process terminates and all remaining windows on it will be forcibly removed.
     * </p><p>
     * The behavior of the virtual display depends on the flags that are provided
     * to this method.  By default, virtual displays are created to be private,
     * non-presentation and unsecure.  Permissions may be required to use certain flags.
     * </p>
     *
     * @param name The name of the virtual display, must be non-empty.
     * @param width The width of the virtual display in pixels, must be greater than 0.
     * @param height The height of the virtual display in pixels, must be greater than 0.
     * @param densityDpi The density of the virtual display in dpi, must be greater than 0.
     * @param surface The surface to which the content of the virtual display should
     * be rendered, must be non-null.
     * @param flags A combination of virtual display flags:
     * {@link #VIRTUAL_DISPLAY_FLAG_PUBLIC}, {@link #VIRTUAL_DISPLAY_FLAG_PRESENTATION}
     * or {@link #VIRTUAL_DISPLAY_FLAG_SECURE}.
     * @return The newly created virtual display, or null if the application could
     * not create the virtual display.
     *
     * @throws SecurityException if the caller does not have permission to create
     * a virtual display with the specified flags.
     */
    public VirtualDisplay createVirtualDisplay(String name,
            int width, int height, int densityDpi, Surface surface, int flags) {
        return mGlobal.createVirtualDisplay(mContext,
                name, width, height, densityDpi, surface, flags);
    }

    /**
     * Listens for changes in available display devices.
     */
    public interface DisplayListener {
        /**
         * Called whenever a logical display has been added to the system.
         * Use {@link DisplayManager#getDisplay} to get more information about
         * the display.
         *
         * @param displayId The id of the logical display that was added.
         */
        void onDisplayAdded(int displayId);

        /**
         * Called whenever a logical display has been removed from the system.
         *
         * @param displayId The id of the logical display that was removed.
         */
        void onDisplayRemoved(int displayId);

        /**
         * Called whenever the properties of a logical display have changed.
         *
         * @param displayId The id of the logical display that changed.
         */
        void onDisplayChanged(int displayId);
    }

    public int getDisplaySupport3DMode(int displaytype) {
        return mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETSUPPORT3DMODE,
                0, 0);
    }

    public int getDisplayOutputType(int displaytype) {
        return mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETOUTPUTTYPE,
                0, 0);
    }

    public int getDisplayOutputMode(int displaytype) {
        return mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETOUTPUTMODE,
                0, 0);
    }

    public int getDisplaySaturation(int displaytype) {
        return mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETSATURATION,
                0, 0);
    }

    public int getDisplayhue(int displaytype) {
        return mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETHUE,
                0, 0);
    }
    public int getDisplayContrast(int displaytype) {
        return mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETCONTRAST,
                0, 0);
    }

    public int getDisplayBright(int displaytype) {
        return mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETBRIGHT,
                0, 0);
    }

    public int getDisplayPercent(int displaytype) {
        int hpercent = mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETMARGIN_W,
                0, 0);
        int vpercent = mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETMARGIN_H,
                0, 0);
		return (hpercent > vpercent) ? hpercent : vpercent;

    }

    public int[] getDisplayMargin(int displaytype) {
        int[] percent = new int[2];
        percent[0] = mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETMARGIN_W,
                0, 0);
        percent[1] = mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_GETMARGIN_H,
                0, 0);
		return percent;
    }

    private int saveDisplayMargin(int displaytype, int percentWidth, int percentHeight) {
        final File FILE = new File(MARGIN_FILENAME);
        if (!FILE.exists()) {
            Log.w(TAG, "file: " + MARGIN_FILENAME + " is not exists");
            try {
                FILE.createNewFile();
            } catch (IOException e) {
                Log.d(TAG, "file: creat file failed");
            }
        }

        final String values = new String(Integer.toHexString(percentWidth) + "\n" + Integer.toHexString(percentHeight) + "\n");
        try {
            FileOutputStream fos = new FileOutputStream(FILE);
            fos.write(values.getBytes());
            fos.flush();
            fos.getFD().sync();
            fos.close();
        } catch (IOException e) {
        }
        return 0;
    }

    public int saveDisplayResolution(int displaytype, int type, int mode) {
        final File FILE = new File(RSL_FILENAME);
        String [] tempString = new String[3];
        int [] value = new int[3];
        if (!FILE.exists()) {
			Log.w(TAG, "file: " + RSL_FILENAME + " is not exists");
			try {
				FILE.createNewFile();
			} catch (IOException e) {
				Log.d(TAG, "file: creat file failed");
			}
        }
        else {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(FILE));
				for (int i = 0; i < 3; i++) {
					tempString[i] = reader.readLine();
					if (tempString[i] != null)
						value[i] = Integer.parseInt(tempString[i], 16);
				}

				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
        }

        if (type == DISPLAY_OUTPUT_TYPE_TV) {
			value[0] = ((2 & 0xff) << 8) | ((mode & 0xff) << 0);
        } else if (type == DISPLAY_OUTPUT_TYPE_HDMI) {
			value[1] = ((4 & 0xff) << 8) | ((mode & 0xff) << 0);
        } else if (type == DISPLAY_OUTPUT_TYPE_VGA) {
			value[2] = ((8 & 0xff) << 8) | (((mode - 0x17) & 0xff) << 0);
        }
        final String values = new String(Integer.toHexString(value[0]) + "\n" + Integer.toHexString(value[1]) + "\n" + Integer.toHexString(value[2]) + "\n");
        try {
			FileOutputStream fos = new FileOutputStream(FILE);
			fos.write(values.getBytes());
			fos.flush();
			fos.getFD().sync();
			fos.close();
        } catch (IOException e) {
        }
        return 0;

    }

    public int getDisplayResolution(int displaytype) {
        final File FILE = new File(RSL_FILENAME);
        String [] tempString = new String[3];
        int value = 0;
        if (!FILE.exists()) {
			Log.w(TAG, "file: " + RSL_FILENAME + " is not exists");
			try {
				FILE.createNewFile();
			} catch (IOException e) {
				Log.d(TAG, "file: creat file failed");
			}
        }
        else {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(FILE));
				for (int i = 0; i < 3; i++) {
					tempString[i] = reader.readLine();
					if (tempString[i] != null){
						int tempValue = Integer.parseInt(tempString[i], 16);
						if(getDisplayTypeFromFormat(tempValue) == displaytype) {
							value = tempValue;
							break;
						}
					}
				}

				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e1) {
					}
				}
			}
        }

		return value;
    }

    public int setDisplay3DMode(int displaytype, int display3dMode) {
        return mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SET3DMODE,
                display3dMode, 0, 0);
    }

    static public int getDisplayModeFromFormat(int format) {
        return format & DISPLAY_OUTPUT_MODE_MASK;
    }

    static public int getDisplayTypeFromFormat(int format) {
        return (format & DISPLAY_OUTPUT_TYPE_MASK) >> 8;
    }

    public int makeDisplayFormat(int type, int mode) {
        return ((type << 8) & DISPLAY_OUTPUT_TYPE_MASK) | (mode & DISPLAY_OUTPUT_MODE_MASK);
    }

    public int getDisplayOutput(int displaytype){
        int type = getDisplayOutputType(displaytype);
        int mode = getDisplayOutputMode(displaytype);
        return makeDisplayFormat(type, mode);
    }

    public int setDisplayOutput(int displaytype, int format) {
        int type = (format & DISPLAY_OUTPUT_TYPE_MASK) >> 8;
        int mode = format & DISPLAY_OUTPUT_MODE_MASK;
        return setDisplayOutputMode(displaytype, type, mode);
    }

    public int setDisplayOutputMode(int displaytype, int outtype, int outmode) {
        int ret = mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SETOUTPUTMODE,
                outtype, outmode, 0);
        return ret;
    }

    public boolean isSupportHdmiMode(int displaytype, int mode){
        return mGlobal.getDisplayParameter(displaytype, DISPLAY_CMD_IS_SUPPORT_HDMI_MODE,
                mode, 0) == 1;
    }

    public int setDisplayPercent(int displaytype, int percent) {
        int ret = mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SETMARGIN,
                percent, percent, 0);
        saveDisplayMargin(displaytype, percent, percent);
        return ret;
    }

    public int setDisplayMargin(int displaytype, int hpercent, int vpercent) {
        int ret = mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SETMARGIN,
                hpercent, vpercent, 0);
        saveDisplayMargin(displaytype, hpercent, vpercent);
        return ret;
    }

    public int setDisplaySaturation(int displaytype, int saturation) {
        return mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SETSATURATION,
                saturation, 0, 0);
    }

    public int setDisplayHue(int displaytype, int hue) {
        return mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SETHUE,
                hue, 0, 0);
    }
    public int setDisplayContrast(int displaytype, int contrast) {
        return mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SETCONTRAST,
                contrast, 0, 0);
    }

    public int setDisplayBright(int displaytype, int bright) {
        return mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SETBRIGHT,
                bright, 0, 0);
    }

    public int setDisplay3DLayerOffset(int displaytype, int offset) {
        return mGlobal.setDisplayParameter(displaytype, DISPLAY_CMD_SET3DLAYEROFFSET,
                offset, 0, 0);
    }
}
