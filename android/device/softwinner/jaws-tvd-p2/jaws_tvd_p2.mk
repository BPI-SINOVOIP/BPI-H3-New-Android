include device/softwinner/jaws-common/jaws-common.mk
#include vendor/tvd/tvd.mk
include vendor/fvd/fvd.mk

DEVICE_PACKAGE_OVERLAYS := \
    device/softwinner/jaws-tvd-p2/overlay \
    $(DEVICE_PACKAGE_OVERLAYS)

# for recovery
PRODUCT_COPY_FILES += \
	device/softwinner/jaws-tvd-p2/recovery.fstab:recovery.fstab

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-tvd-p2/kernel:kernel \
    device/softwinner/jaws-tvd-p2/fstab.sun9i:root/fstab.sun9i \
    device/softwinner/jaws-tvd-p2/init.rc:root/init.rc \
    device/softwinner/jaws-tvd-p2/insmod.rc:root/insmod.rc \
    device/softwinner/jaws-tvd-p2/init.recovery.sun9i.rc:root/init.recovery.sun9i.rc \
    device/softwinner/jaws-tvd-p2/ueventd.sun9i.rc:root/ueventd.sun9i.rc \
    device/softwinner/jaws-tvd-p2/modules/modules/nand.ko:root/nand.ko

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-tvd-p2/configs/tablet_core_hardware.xml:system/etc/permissions/tablet_core_hardware.xml

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-tvd-p2/configs/camera.cfg:system/etc/camera.cfg \
    device/softwinner/jaws-tvd-p2/configs/cameralist.cfg:system/etc/cameralist.cfg \
    device/softwinner/jaws-tvd-p2/configs/sunxi-keyboard.kl:system/usr/keylayout/sunxi-keyboard.kl \
    device/softwinner/jaws-tvd-p2/configs/media_profiles.xml:system/etc/media_profiles.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml

PRODUCT_COPY_FILES += \
    device/softwinner/jaws-tvd-p2/initlogo.rle:root/initlogo.rle \
    device/softwinner/jaws-tvd-p2/needfix.rle:root/needfix.rle \
    device/softwinner/jaws-tvd-p2/media/bootanimation.zip:system/media/bootanimation.zip \
    device/softwinner/jaws-tvd-p2/media/boot.wav:system/media/boot.wav \
    device/softwinner/jaws-tvd-p2/media/bootlogo.bmp:system/media/bootlogo.bmp \
    device/softwinner/jaws-tvd-p2/media/initlogo.bmp:system/media/initlogo.bmp

# wifi & bt config file
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml \
    frameworks/native/data/etc/android.hardware.wifi.direct.xml:system/etc/permissions/android.hardware.wifi.direct.xml \
    frameworks/native/data/etc/android.hardware.bluetooth.xml:system/etc/permissions/android.hardware.bluetooth.xml \
    frameworks/native/data/etc/android.hardware.bluetooth_le.xml:system/etc/permissions/android.hardware.bluetooth_le.xml

# camera config for isp
PRODUCT_COPY_FILES += \
    device/softwinner/jaws-common/hawkview/16M/ov16825/isp_3a_param.ini:system/etc/hawkview/ov16825/isp_3a_param.ini \
    device/softwinner/jaws-common/hawkview/16M/ov16825/isp_iso_param.ini:system/etc/hawkview/ov16825/isp_iso_param.ini \
    device/softwinner/jaws-common/hawkview/16M/ov16825/isp_test_param.ini:system/etc/hawkview/ov16825/isp_test_param.ini \
    device/softwinner/jaws-common/hawkview/16M/ov16825/isp_tuning_param.ini:system/etc/hawkview/ov16825/isp_tuning_param.ini \
    device/softwinner/jaws-common/hawkview/16M/ov16825/bin/gamma_tbl.bin:system/etc/hawkview/ov16825/bin/gamma_tbl.bin \
    device/softwinner/jaws-common/hawkview/16M/ov16825/bin/hdr_tbl.bin:system/etc/hawkview/ov16825/bin/hdr_tbl.bin \
    device/softwinner/jaws-common/hawkview/16M/ov16825/bin/lsc_tbl.bin:system/etc/hawkview/ov16825/bin/lsc_tbl.bin

# build Bluetooth.apk
PRODUCT_PACKAGES += \
	Bluetooth

# add insmod process
PRODUCT_PACKAGES += \
    insmod_task

# build OTA Update.apk
PRODUCT_PACKAGES += \
    Update

# build tvremoteserver
PRODUCT_PACKAGES += \
	tvremoteserver

PRODUCT_PROPERTY_OVERRIDES += \
	persist.sf.showhdmisettings=7 \
	persist.sys.hdmimode=10 \
	persist.sys.hwc_p2p=0 \
	persist.sys.disp_density=160

# When set ro.sys.adaptive_memory=1, firmware can adaptive different dram size.
# And dalvik vm parameters configuration will become invalid.
PRODUCT_PROPERTY_OVERRIDES += \
    ro.sys.adaptive_memory=0

# dalvik vm parameters
PRODUCT_PROPERTY_OVERRIDES += \
    dalvik.vm.heapsize=512m \
    dalvik.vm.heapstartsize=16m \
    dalvik.vm.heapgrowthlimit=384m \
    dalvik.vm.heaptargetutilization=0.75 \
    dalvik.vm.heapminfree=2m \
    dalvik.vm.heapmaxfree=8m \
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
#Fire launcher game 
PRODUCT_PROPERTY_OVERRIDES += \
    ro.business.game=1

PRODUCT_PROPERTY_OVERRIDES += \
        ro.carrier=wifi-only

# default language setting
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.timezone=Asia/Shanghai \
    persist.sys.country=CN \
    persist.sys.language=zh


$(call inherit-product-if-exists, device/softwinner/jaws-tvd-p2/modules/modules.mk)

# Overrides
PRODUCT_CHARACTERISTICS := homlet
PRODUCT_BRAND := Allwinner
PRODUCT_NAME := jaws_tvd_p2
PRODUCT_DEVICE := jaws-tvd-p2
PRODUCT_MODEL := jaws
PRODUCT_MANUFACTURER := Allwinner

#pack parameter
PACK_BOARD := jaws-p2
