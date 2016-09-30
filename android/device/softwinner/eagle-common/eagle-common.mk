include device/softwinner/common/common.mk

#marlin
BOARD_MARLIN_USE_SECUREOS := 0

DEVICE_PACKAGE_OVERLAYS := \
    device/softwinner/eagle-common/overlay

PRODUCT_PACKAGES += \
    libion \
    sensors.eagle \
    lights.eagle \
    keystore.eagle \
    hwcomposer.eagle

#------------------ audio -------------------------
PRODUCT_PACKAGES += \
	audio.primary.eagle \
	audio.a2dp.default \
	audio.usb.default  \
	audio.r_submix.default

PRODUCT_COPY_FILES += \
	device/softwinner/eagle-common/hardware/audio/audio_policy.conf:system/etc/audio_policy.conf \
	device/softwinner/eagle-common/hardware/audio/phone_volume.conf:system/etc/phone_volume.conf \
	device/softwinner/eagle-common/hardware/audio/ac100_paths.xml:system/etc/ac100_paths.xml \
	device/softwinner/eagle-common/overlay/frameworks/native/data/etc/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml

#------------------------------------------------

#------------------ camera -------------------------
PRODUCT_PACKAGES += \
	camera.eagle

#------------------------------------------------

#------------------ video -------------------------
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
	libOmxAdec \
	libCTC_MediaProcessor \
	libtvdemux \
	liblive555 \
	librtp     \
	libawavs   \
	libawh264  \
	libawh265  \
	libawh265soft \
	libawmjpeg    \
	libawmjpegplus \
	libawmpeg2    \
	libawmpeg4base \
	libawmpeg4divx311 \
	libawmpeg4h263   \
	libawmpeg4normal \
	libawmpeg4vp6    \
	libawvp6soft     \
	libawmpeg4dx     \
	libawvp8         \
	libawvp9soft     \
	libawwmv3        \
	libawwmv12soft   \
	libaw_env        \
	libtvdemux       \
	libOmxAdec       \
	libawrender      \
	libCTC_MediaProcessor

PRODUCT_COPY_FILES += \
	device/softwinner/eagle-common/media_codecs.xml:system/etc/media_codecs.xml

#------------------------------------------------

# face detection
PRODUCT_COPY_FILES += \
  device/softwinner/eagle-common/facedetection/awfaceftr.aw:system/usr/share/bmd/awfaceftr.aw \
  device/softwinner/eagle-common/facedetection/awfaceftr.ftr:system/usr/share/bmd/awfaceftr.ftr \
  device/softwinner/eagle-common/facedetection/sm.awl:system/usr/share/bmd/sm.awl \
  device/softwinner/eagle-common/facedetection/ey.awl:system/usr/share/bmd/ey.awl \
  device/softwinner/eagle-common/facedetection/eb.awl:system/usr/share/bmd/eb.awl

#-------------------- secure  -------------------------
PRODUCT_PACKAGES += \
	libsec_storage

#-----------------------------------------------------

# sensor
PRODUCT_COPY_FILES += \
    device/softwinner/eagle-common/sensors.sh:system/bin/sensors.sh

# init.rc, init.sun8i.usb.rc
PRODUCT_COPY_FILES += \
    device/softwinner/eagle-common/init.sun8i.rc:root/init.sun8i.rc \
    device/softwinner/eagle-common/init.sun8i.usb.rc:root/init.sun8i.usb.rc

#------------------ egl -------------------------
PRODUCT_COPY_FILES += \
    device/softwinner/eagle-common/egl/pvrsrvctl:system/vendor/bin/pvrsrvctl \
    device/softwinner/eagle-common/egl/libusc.so:system/vendor/lib/libusc.so \
    device/softwinner/eagle-common/egl/libglslcompiler.so:system/vendor/lib/libglslcompiler.so \
    device/softwinner/eagle-common/egl/libIMGegl.so:system/vendor/lib/libIMGegl.so \
    device/softwinner/eagle-common/egl/libpvr2d.so:system/vendor/lib/libpvr2d.so \
    device/softwinner/eagle-common/egl/libpvrANDROID_WSEGL.so:system/vendor/lib/libpvrANDROID_WSEGL.so \
    device/softwinner/eagle-common/egl/libPVRScopeServices.so:system/vendor/lib/libPVRScopeServices.so \
    device/softwinner/eagle-common/egl/libsrv_init.so:system/vendor/lib/libsrv_init.so \
    device/softwinner/eagle-common/egl/libsrv_um.so:system/vendor/lib/libsrv_um.so \
    device/softwinner/eagle-common/egl/libEGL_POWERVR_SGX544_115.so:system/vendor/lib/egl/libEGL_POWERVR_SGX544_115.so \
    device/softwinner/eagle-common/egl/libGLESv1_CM_POWERVR_SGX544_115.so:system/vendor/lib/egl/libGLESv1_CM_POWERVR_SGX544_115.so \
    device/softwinner/eagle-common/egl/libGLESv2_POWERVR_SGX544_115.so:system/vendor/lib/egl/libGLESv2_POWERVR_SGX544_115.so \
    device/softwinner/eagle-common/egl/gralloc.sun8i.so:system/vendor/lib/hw/gralloc.sun8i.so \
    device/softwinner/eagle-common/egl/powervr.ini:system/etc/powervr.ini

#------------------------------------------------

