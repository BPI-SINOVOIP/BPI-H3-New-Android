include device/softwinner/dolphin-common/dolphin-common.mk
#include vendor/tvd/tvd.mk
include vendor/fvd/fvd.mk
include vendor/google/products/gms.mk

#widevine
BOARD_WIDEVINE_OEMCRYPTO_LEVEL := 1

#marlin, depend on widevine level
BOARD_MARLIN_USE_SECUREOS := 1

PRODUCT_PROPERTY_OVERRIDES += \
	drm.service.enabled=true

PRODUCT_PACKAGES += \
	com.google.widevine.software.drm.xml \
	com.google.widevine.software.drm \
	libdrmwvmplugin \
	libwvm \
	libWVStreamControlAPI_L${BOARD_WIDEVINE_OEMCRYPTO_LEVEL} \
	libwvdrm_L${BOARD_WIDEVINE_OEMCRYPTO_LEVEL} \
	libdrmdecrypt \
	libwvdrmengine

ifeq ($(BOARD_WIDEVINE_OEMCRYPTO_LEVEL), 1)
PRODUCT_PACKAGES += \
	liboemcrypto \
	libtee_client
endif

# drm (marlin)
PRODUCT_PACKAGES += \
	wasabi-drm
DEVICE_PACKAGE_OVERLAYS := \
    device/softwinner/dolphin-fvd-p1-secure/overlay \
    $(DEVICE_PACKAGE_OVERLAYS)

# for recovery
PRODUCT_COPY_FILES += \
	device/softwinner/dolphin-fvd-p1-secure/recovery.fstab:recovery.fstab

PRODUCT_COPY_FILES += \
    device/softwinner/dolphin-fvd-p1-secure/kernel:kernel \
    device/softwinner/dolphin-fvd-p1-secure/fstab.sun8i:root/fstab.sun8i \
    device/softwinner/dolphin-fvd-p1-secure/init.rc:root/init.rc \
    device/softwinner/dolphin-fvd-p1-secure/verity/rsa_key/verity_key:root/verity_key \
    device/softwinner/dolphin-fvd-p1-secure/init.recovery.sun8i.rc:root/init.recovery.sun8i.rc \
    device/softwinner/dolphin-fvd-p1-secure/ueventd.sun8i.rc:root/ueventd.sun8i.rc \
    device/softwinner/dolphin-fvd-p1-secure/modules/modules/fivm.ko:root/fivm.ko\
    device/softwinner/dolphin-fvd-p1-secure/modules/modules/nand.ko:root/nand.ko

PRODUCT_COPY_FILES += \
    device/softwinner/dolphin-fvd-p1-secure/configs/tablet_core_hardware.xml:system/etc/permissions/tablet_core_hardware.xml

PRODUCT_COPY_FILES += \
    device/softwinner/dolphin-fvd-p1-secure/configs/camera.cfg:system/etc/camera.cfg \
    device/softwinner/dolphin-fvd-p1-secure/configs/cameralist.cfg:system/etc/cameralist.cfg \
    device/softwinner/dolphin-fvd-p1-secure/configs/sunxi-keyboard.kl:system/usr/keylayout/sunxi-keyboard.kl \
    device/softwinner/dolphin-fvd-p1-secure/configs/media_profiles.xml:system/etc/media_profiles.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml

PRODUCT_COPY_FILES += \
    device/softwinner/dolphin-fvd-p1-secure/initlogo.rle:root/initlogo.rle \
    device/softwinner/dolphin-fvd-p1-secure/needfix.rle:root/needfix.rle \
    device/softwinner/dolphin-fvd-p1-secure/media/bootanimation.zip:system/media/bootanimation.zip

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
    
# build Miracast HDCP2.2 Lib
PRODUCT_PACKAGES += \
	miracast_hdcp_lib

# build OTA Update.apk
PRODUCT_PACKAGES += \
    Update


# ########## DISPLAY CONFIGS BEGIN #############

PRODUCT_PROPERTY_OVERRIDES += \
	persist.sys.disp_density=160 \
	ro.hwc.sysrsl=5

#DISPLAY_INIT_POLICY is used in init_disp.c to choose display policy.
DISPLAY_INIT_POLICY := 1
HDMI_CHANNEL := 0
HDMI_DEFAULT_MODE := 4
CVBS_CHANNEL := 1
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
    ro.udisk.lable=dolphin \
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
	ro.hwui.r_buffer_cache_size=17

#version and ota update
PRODUCT_PROPERTY_OVERRIDES += \
    ro.product.firmware=homlet4.4.2-h3-v1.0-secure \
    ro.product.rom.type=YB \
    ro.product.rom.name=BoxRom

PRODUCT_PROPERTY_OVERRIDES += \
        ro.carrier=wifi-only

# default language setting
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.timezone=Asia/Shanghai \
    persist.sys.country=CN \
    persist.sys.language=zh


$(call inherit-product-if-exists, device/softwinner/dolphin-fvd-p1-secure/modules/modules.mk)

# Overrides
PRODUCT_CHARACTERISTICS := homlet
PRODUCT_BRAND := Allwinner
PRODUCT_NAME := dolphin_fvd_p1_secure
PRODUCT_DEVICE := dolphin-fvd-p1-secure
PRODUCT_MODEL := dolphin
PRODUCT_MANUFACTURER := Allwinner
SECURE_PRODUCT := true

#secure parameter
SECURE_BOOT:= true

#pack parameter
PACK_BOARD=dolphin-p1-secure
