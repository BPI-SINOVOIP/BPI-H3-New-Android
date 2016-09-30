include device/softwinner/jaws-common/jaws-common.mk
#include vendor/tvd/tvd.mk
include vendor/fvd/fvd.mk

DEVICE_PACKAGE_OVERLAYS := \
    device/softwinner/jaws-optimus/overlay \
    $(DEVICE_PACKAGE_OVERLAYS)

# for recovery
PRODUCT_COPY_FILES += \
	device/softwinner/jaws-optimus/recovery.fstab:recovery.fstab 

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-optimus/kernel:kernel \
    device/softwinner/jaws-optimus/fstab.sun9i:root/fstab.sun9i \
    device/softwinner/jaws-optimus/init.rc:root/init.rc \
    device/softwinner/jaws-optimus/init.recovery.sun9i.rc:root/init.recovery.sun9i.rc \
    device/softwinner/jaws-optimus/ueventd.sun9i.rc:root/ueventd.sun9i.rc \
    device/softwinner/jaws-optimus/modules/modules/nand.ko:root/nand.ko

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-optimus/configs/tablet_core_hardware.xml:system/etc/permissions/tablet_core_hardware.xml

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-optimus/configs/camera.cfg:system/etc/camera.cfg \
    device/softwinner/jaws-optimus/configs/cameralist.cfg:system/etc/cameralist.cfg \
    device/softwinner/jaws-optimus/configs/sunxi-keyboard.kl:system/usr/keylayout/sunxi-keyboard.kl \
    device/softwinner/jaws-optimus/configs/media_profiles.xml:system/etc/media_profiles.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-optimus/initlogo.rle:root/initlogo.rle \
    device/softwinner/jaws-optimus/needfix.rle:root/needfix.rle \
    device/softwinner/jaws-optimus/media/bootanimation.zip:system/media/bootanimation.zip \
    device/softwinner/jaws-optimus/media/boot.wav:system/media/boot.wav \
    device/softwinner/jaws-optimus/media/bootlogo.bmp:system/media/bootlogo.bmp \
    device/softwinner/jaws-optimus/media/initlogo.bmp:system/media/initlogo.bmp

# wifi & bt config file
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.bluetooth.xml:system/etc/permissions/android.hardware.bluetooth.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:system/etc/permissions/android.hardware.bluetooth_le.xml

# build Bluetooth.apk
PRODUCT_PACKAGES += \
	Bluetooth

# build OTA Update.apk
PRODUCT_PACKAGES += \
    Update

PRODUCT_PACKAGES += \
    USBBT
PRODUCT_PACKAGES += \
    libusb



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
	ro.hwui.r_buffer_cache_size=17

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


$(call inherit-product-if-exists, device/softwinner/jaws-optimus/modules/modules.mk)

# Overrides
PRODUCT_CHARACTERISTICS := homlet
PRODUCT_BRAND := Allwinner
PRODUCT_NAME := jaws_optimus
PRODUCT_DEVICE := jaws-optimus
PRODUCT_MODEL := jaws
PRODUCT_MANUFACTURER := Allwinner

#pack parameter
PACK_BOARD := jaws-optimus
