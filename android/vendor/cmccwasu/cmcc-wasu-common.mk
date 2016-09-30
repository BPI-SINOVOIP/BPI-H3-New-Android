
DEVICE_PACKAGE_OVERLAYS += \
    vendor/cmccwasu/overlay \

#define BUSINESS PLATFORM
TARGET_BUSINESS_PLATFORM := cmccwasu

#build external.jar, which contains cmcc-wasu api
PRODUCT_PACKAGES += \
    external

PRIVATE_BOOT_JARS := external

# preinstall huasu apk
PRODUCT_PACKAGES += \
	SWIME2 \
	SWNetworkInfo \
	SWOTT \
	SWXmpp \
	SHCMCC_Installer \
	SHCMCC_wimo \
	SHCMCC_Diagnostic \
	SHCMCC_DeviceManger \
	SWLauncher \
	SHCMCC_Downloader \
	SWLogin \
	SWLocalPlayer \
	SWControlServer_wimo \
	SWUpgrade_usb \
	SWControlServer \
	SWGuide \
	SWSettings \
	ChinaMobileOttItv \
	SWUpgrade \
	SWPlayer \
	libtxcore \
	cmccwasu_systemservice \
	cmccwasu_server \
	TvdLauncher \
	TvdSettings \
	TvdFileManager \
	TvdVideo \
	com.yd.appstore.ott-1 \
	AppStore_YD_OTT-1.9_RC1 \
	libtxcore2014-06-05_33

# cmcc use
#PRODUCT_PACKAGES += \
#	libad_audio \
#	libaxx \
#	libdxx \
#	librx \
#	librv
	
PRODUCT_COPY_FILES += \
	vendor/cmccwasu/packages/SHCMSYSTEM_useraccount.jar:/system/framework/SHCMSYSTEM_useraccount.jar \
	vendor/cmccwasu/packages/shcmsystem.jar:/system/framework/shcmsystem.jar \
	vendor/cmccwasu/init.cmccwasu.rc:root/init.cmccwasu.rc
	
PRODUCT_PROPERTY_OVERRIDES += \
	ro.media.timeshift=1 \
	sys.settings.support=1 \
	sys.settings.support.net.flags=7 \
	epg.launcher.packagename=net.sunniwell.app.ott.chinamobile \
	sys.deepdiagnose.support=1 \
	ro.hwc.sysrsl=5
	
# do not modify this property
PRODUCT_PROPERTY_OVERRIDES += \
	ro.business.platform=cmccwasu \
	ro.sw.defaultlauncherpackage=net.sunniwell.launcher.chinamobile \
	ro.sw.defaultlauncherclass=net.sunniwell.launcher.chinamobile.MainActivity

# use cmcc custom recovery ui
TARGET_RECOVERY_UI_LIB := librecovery_ui_cmccwasu
