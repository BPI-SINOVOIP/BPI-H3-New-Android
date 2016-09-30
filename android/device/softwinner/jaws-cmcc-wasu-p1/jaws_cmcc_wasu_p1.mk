include device/softwinner/jaws-common/jaws-common.mk
include vendor/cmccwasu/cmcc-wasu-common.mk

# override BUILD_NUMBER, it default defined in jaws-common.mk
BUILD_NUMBER := 1.0.1

DEVICE_PACKAGE_OVERLAYS := \
    device/softwinner/jaws-cmcc-wasu-p1/overlay \
    $(DEVICE_PACKAGE_OVERLAYS)

# for recovery
PRODUCT_COPY_FILES += \
	device/softwinner/jaws-cmcc-wasu-p1/recovery.fstab:recovery.fstab

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-cmcc-wasu-p1/kernel:kernel \
    device/softwinner/jaws-cmcc-wasu-p1/fstab.sun9i:root/fstab.sun9i \
    device/softwinner/jaws-cmcc-wasu-p1/init.rc:root/init.rc \
    device/softwinner/jaws-cmcc-wasu-p1/init.recovery.sun9i.rc:root/init.recovery.sun9i.rc \
    device/softwinner/jaws-cmcc-wasu-p1/ueventd.sun9i.rc:root/ueventd.sun9i.rc \
    device/softwinner/jaws-cmcc-wasu-p1/modules/modules/nand.ko:root/nand.ko

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-cmcc-wasu-p1/configs/tablet_core_hardware.xml:system/etc/permissions/tablet_core_hardware.xml

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-cmcc-wasu-p1/configs/camera.cfg:system/etc/camera.cfg \
    device/softwinner/jaws-cmcc-wasu-p1/configs/cameralist.cfg:system/etc/cameralist.cfg \
    device/softwinner/jaws-cmcc-wasu-p1/configs/sunxi-keyboard.kl:system/usr/keylayout/sunxi-keyboard.kl \
    device/softwinner/jaws-cmcc-wasu-p1/configs/media_profiles.xml:system/etc/media_profiles.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-cmcc-wasu-p1/initlogo.rle:root/initlogo.rle \
    device/softwinner/jaws-cmcc-wasu-p1/needfix.rle:root/needfix.rle \
    device/softwinner/jaws-cmcc-wasu-p1/media/bootanimation.zip:system/media/bootanimation.zip

# wifi & bt config file
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.bluetooth.xml:system/etc/permissions/android.hardware.bluetooth.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:system/etc/permissions/android.hardware.bluetooth_le.xml

# build Bluetooth.apk
PRODUCT_PACKAGES += \
	Bluetooth

PRODUCT_PACKAGES += \
    USBBT
PRODUCT_PACKAGES += \
    libusb

# build OTA Update.apk
PRODUCT_PACKAGES += \
    Update


# ########## DISPLAY CONFIGS BEGIN #############

PRODUCT_PROPERTY_OVERRIDES += \
	persist.sys.hwc_p2p=0 \
	persist.sys.disp_density=160 \
	persist.sys.disp_policy=1 \
	persist.sys.hdmi_hpd=1 \
	persist.sys.hdmi_rvthpd=2 \
	persist.sys.cvbs_hpd=0 \
	persist.sys.cvbs_rvthpd=0

#DISPLAY_INIT_POLICY is used in init_disp.c to choose display policy.
DISPLAY_INIT_POLICY := 1
HDMI_CHANNEL := 1
HDMI_DEFAULT_MODE := 10
CVBS_CHANNEL := 0
CVBS_DEFAULT_MODE := 11
#SHOW_INITLOGO := true

# ########## DISPLAY CONFIGS END ##############


# dalvik vm parameters set in the init/property_service.c
PRODUCT_PROPERTY_OVERRIDES += \
    ro.zygote.disable_gl_preload=true

# drm
PRODUCT_PROPERTY_OVERRIDES += \
    drm.service.enabled=false

# usb
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.usb.config=mtp,adb \
    ro.udisk.lable=jaws \
    ro.adb.secure=0

# ui
PRODUCT_PROPERTY_OVERRIDES += \
    ro.property.tabletUI=false \
    ro.property.fontScale=1.0 \
    ro.sf.hwrotation=0 \
    debug.hwui.render_dirty_regions=false \
    ro.property.max_video_height=2160

#for evb hdmi density setting
PRODUCT_PROPERTY_OVERRIDES += \
    persist.evb_flag=1

PRODUCT_PROPERTY_OVERRIDES += \
	ro.hwui.texture_cache_size=170 \
	ro.hwui.layer_cache_size=135 \
	ro.hwui.path_cache_size=34 \
	ro.hwui.shap_cache_size=9 \
	ro.hwui.drop_shadow_cache_size=17 \
	ro.hwui.r_buffer_cache_size=17 \
    ro.sf.lcd_density=160

#version and ota update
PRODUCT_PROPERTY_OVERRIDES += \
    ro.product.rom.type=YB \
    ro.product.rom.name=BoxRom

PRODUCT_PROPERTY_OVERRIDES += \
        ro.carrier=wifi-only

# default language setting
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.timezone=Asia/Shanghai \
    persist.sys.country=CN \
    persist.sys.language=zh


$(call inherit-product-if-exists, device/softwinner/jaws-cmcc-wasu-p1/modules/modules.mk)

# Overrides
PRODUCT_CHARACTERISTICS := homlet
PRODUCT_BRAND := Allwinner
PRODUCT_NAME := jaws_cmcc_wasu_p1
PRODUCT_DEVICE := jaws-cmcc-wasu-p1
PRODUCT_MODEL := jaws
PRODUCT_MANUFACTURER := Allwinner

#pack parameter
PACK_BOARD = jaws-cmcc-wasu-p1
