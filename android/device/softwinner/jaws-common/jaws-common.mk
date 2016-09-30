include device/softwinner/common/common.mk

DEVICE_PACKAGE_OVERLAYS := \
    device/softwinner/jaws-common/overlay

PRODUCT_PACKAGES += \
    libion \
    sensors.jaws \
    lights.jaws \
    keystore.jaws \
    hwcomposer.jaws

# --------------------- audio ---------------------
PRODUCT_PACKAGES += \
	audio.primary.jaws \
	audio.a2dp.default \
	audio.usb.default  \
	audio.r_submix.default

PRODUCT_COPY_FILES += \
	device/softwinner/jaws-common/hardware/audio/audio_policy.conf:system/etc/audio_policy.conf \
	device/softwinner/jaws-common/hardware/audio/phone_volume.conf:system/etc/phone_volume.conf \
	device/softwinner/jaws-common/overlay/frameworks/native/data/etc/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml

# -------------------------------------------------

# -------------------- camera --------------------
PRODUCT_PACKAGES += \
	camera.jaws

# ------------------------------------------------

# -------------------- video ----------------------
PRODUCT_PACKAGES += \
	libcedarxbase \
	libcedarxosal \
	libcedarv \
	libcedarv_base \
	libcedarv_adapter \
	libve \
	libaw_audio \
	libaw_audioa \
	libswdrm \
	libfacedetection \
	libsmileeyeblink \
	libthirdpartstream \
	libcedarxsftstream \
	libion_alloc \
	libsrec_jni \
	libcnr \
	libjpgenc \
	libaw_venc \
	libaw_h264enc \
	libI420colorconvert \
	libstagefrighthw \
	libOmxCore \
	libOmxVenc \
	libOmxVdec \
	libvdecoder \
	libadecoder \
	libsdecoder \
	libnormal_audio \
	libad_audio \
	libVE \
	libMemAdapter \
	libplayer \
	libcdx_parser \
	libcdx_base \
	libcdx_stream \
	libawplayer \
	libawmetadataretriever \
	libaw_wvm \
	libOmxAdec

PRODUCT_COPY_FILES += \
	device/softwinner/jaws-common/media_codecs.xml:system/etc/media_codecs.xml

# ------------------------------------------------

# face detection
PRODUCT_COPY_FILES += \
  device/softwinner/jaws-common/facedetection/awfaceftr.aw:system/usr/share/bmd/awfaceftr.aw \
  device/softwinner/jaws-common/facedetection/awfaceftr.ftr:system/usr/share/bmd/awfaceftr.ftr \
  device/softwinner/jaws-common/facedetection/sm.awl:system/usr/share/bmd/sm.awl \
  device/softwinner/jaws-common/facedetection/ey.awl:system/usr/share/bmd/ey.awl \
  device/softwinner/jaws-common/facedetection/eb.awl:system/usr/share/bmd/eb.awl


# sensor
PRODUCT_COPY_FILES += \
    device/softwinner/jaws-common/sensors.sh:system/bin/sensors.sh

# init.rc, init.sun9i.usb.rc
PRODUCT_COPY_FILES += \
    device/softwinner/jaws-common/init.sun9i.rc:root/init.sun9i.rc \
    device/softwinner/jaws-common/init.sun9i.usb.rc:root/init.sun9i.usb.rc

# ---------------------- egl ---------------------
PRODUCT_COPY_FILES += \
    device/softwinner/jaws-common/egl/pvrsrvctl:system/vendor/bin/pvrsrvctl \
    device/softwinner/jaws-common/egl/libusc.so:system/vendor/lib/libusc.so \
    device/softwinner/jaws-common/egl/libglslcompiler.so:system/vendor/lib/libglslcompiler.so \
    device/softwinner/jaws-common/egl/libIMGegl.so:system/vendor/lib/libIMGegl.so \
    device/softwinner/jaws-common/egl/libpvrANDROID_WSEGL.so:system/vendor/lib/libpvrANDROID_WSEGL.so \
    device/softwinner/jaws-common/egl/libPVRScopeServices.so:system/vendor/lib/libPVRScopeServices.so \
    device/softwinner/jaws-common/egl/libsrv_init.so:system/vendor/lib/libsrv_init.so \
    device/softwinner/jaws-common/egl/libsrv_um.so:system/vendor/lib/libsrv_um.so \
    device/softwinner/jaws-common/egl/libEGL_POWERVR_ROGUE.so:system/vendor/lib/egl/libEGL_POWERVR_ROGUE.so \
    device/softwinner/jaws-common/egl/libGLESv1_CM_POWERVR_ROGUE.so:system/vendor/lib/egl/libGLESv1_CM_POWERVR_ROGUE.so \
    device/softwinner/jaws-common/egl/libGLESv2_POWERVR_ROGUE.so:system/vendor/lib/egl/libGLESv2_POWERVR_ROGUE.so \
    device/softwinner/jaws-common/egl/gralloc.sunxi.so:system/vendor/lib/hw/gralloc.sun9i.so \
    device/softwinner/jaws-common/egl/liboclcompiler.so:/system/vendor/lib/liboclcompiler.so \
    device/softwinner/jaws-common/egl/libPVROCL.so:/system/vendor/lib/libPVROCL.so \
    device/softwinner/jaws-common/egl/libufwriter.so:/system/vendor/lib/libufwriter.so \
    device/softwinner/jaws-common/egl/powervr.ini:system/etc/powervr.ini

# ------------------------------------------------

