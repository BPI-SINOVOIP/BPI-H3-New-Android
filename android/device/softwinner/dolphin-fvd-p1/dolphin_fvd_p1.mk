include device/softwinner/dolphin-common/dolphin-common.mk
include vendor/fvd/fvd.mk

#verimatrix
BOARD_USE_VERIMATRIX := 1
ifeq ($(BOARD_USE_VERIMATRIX), 1)
PRODUCT_PACKAGES += \
    libvmclient \
    libvmlogger \
    vmclientSw
endif

DEVICE_PACKAGE_OVERLAYS := \
    device/softwinner/dolphin-fvd-p1/overlay \
    $(DEVICE_PACKAGE_OVERLAYS)

# for recovery
PRODUCT_COPY_FILES += \
	device/softwinner/dolphin-fvd-p1/recovery.fstab:recovery.fstab

PRODUCT_COPY_FILES += \
    device/softwinner/dolphin-fvd-p1/kernel:kernel \
    device/softwinner/dolphin-fvd-p1/fstab.sun8i:root/fstab.sun8i \
    device/softwinner/dolphin-fvd-p1/init.rc:root/init.rc \
    device/softwinner/dolphin-fvd-p1/verity/rsa_key/verity_key:root/verity_key \
    device/softwinner/dolphin-fvd-p1/init.recovery.sun8i.rc:root/init.recovery.sun8i.rc \
    device/softwinner/dolphin-fvd-p1/ueventd.sun8i.rc:root/ueventd.sun8i.rc \
    device/softwinner/dolphin-fvd-p1/modules/modules/fivm.ko:root/fivm.ko\
    device/softwinner/dolphin-fvd-p1/modules/modules/nand.ko:root/nand.ko

PRODUCT_COPY_FILES += \
    device/softwinner/dolphin-fvd-p1/configs/tablet_core_hardware.xml:system/etc/permissions/tablet_core_hardware.xml

PRODUCT_COPY_FILES += \
    device/softwinner/dolphin-fvd-p1/configs/camera.cfg:system/etc/camera.cfg \
    device/softwinner/dolphin-fvd-p1/configs/cameralist.cfg:system/etc/cameralist.cfg \
    device/softwinner/dolphin-fvd-p1/configs/sunxi-keyboard.kl:system/usr/keylayout/sunxi-keyboard.kl \
    device/softwinner/dolphin-fvd-p1/configs/media_profiles.xml:system/etc/media_profiles.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.usb.accessory.xml:system/etc/permissions/android.hardware.usb.accessory.xml

PRODUCT_COPY_FILES += \
    device/softwinner/dolphin-fvd-p1/initlogo.rle:root/initlogo.rle \
    device/softwinner/dolphin-fvd-p1/needfix.rle:root/needfix.rle \
    device/softwinner/dolphin-fvd-p1/media/bootanimation.zip:system/media/bootanimation.zip

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
	persist.sys.disp_density=160 \
	ro.hwc.sysrsl=5 \
	persist.sys.disp_policy=2 \
	persist.sys.hdmi_hpd=1 \
	persist.sys.hdmi_rvthpd=0 \
	persist.sys.cvbs_hpd=0 \
	persist.sys.cvbs_rvthpd=0

#DISPLAY_INIT_POLICY is used in init_disp.c to choose display policy.
DISPLAY_INIT_POLICY := 2
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
    ro.product.rom.type=YB \
    ro.product.rom.name=BoxRom

PRODUCT_PROPERTY_OVERRIDES += \
        ro.carrier=wifi-only

PRODUCT_PROPERTY_OVERRIDES += \
    media.boost.pref=modec100:160

# default language setting
PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.timezone=Asia/Shanghai \
    persist.sys.country=CN \
    persist.sys.language=zh


$(call inherit-product-if-exists, device/softwinner/dolphin-fvd-p1/modules/modules.mk)

# Overrides
PRODUCT_CHARACTERISTICS := homlet
PRODUCT_BRAND := Allwinner
PRODUCT_NAME := dolphin_fvd_p1
PRODUCT_DEVICE := dolphin-fvd-p1
PRODUCT_MODEL := dolphin
PRODUCT_MANUFACTURER := Allwinner

#pack parameter
PACK_BOARD := dolphin-p1

