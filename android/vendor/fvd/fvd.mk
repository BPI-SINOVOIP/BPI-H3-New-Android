	
	
# miracast
#common System APK
# BPI-M2-Plus
PRODUCT_PACKAGES += \
	TvdLauncher \
	TvdSettings2 \
	TvdFileManager \
	TvdSettings \
	Camera2 \
	TvdVideo \
        WebScreensaver \
	SettingsAssist2 \
	MiracastReceiver \
	AllCast


#------------------ BPI-M2-Plus 3rd-party apk --------------------------
PRODUCT_PACKAGES += \
	CameraFi    \
	kodi-16     \
        Pinyin      \
        Zhuyin        

	
#install apk's lib to system/lib
PRODUCT_PACKAGES += \
	libjni_mosaic.so \
	libjni_WFDisplay.so \
	libwfdrtsp.so \
	libwfdplayer.so \
	libwfdmanager.so \
	libwfdutils.so \
	libwfduibc.so \
#   libjni_eglfence_awgallery.so \

#mango tv and FireLauncher
PRODUCT_PACKAGES += \
	libbspatch.so \
	lib_All_imgoTV_bitmaps.so \
	lib_All_imgoTV_nn_tv_air_control.so \
	lib_All_imgoTV_nn_tv_client.so

# init.rc, init.sun9i.usb.rc
#PRODUCT_COPY_FILES += \
	device/softwinner/jaws-common/init.rc:root/init.rc \
