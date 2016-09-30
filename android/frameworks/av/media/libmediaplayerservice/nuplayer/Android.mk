LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=                       \
        GenericSource.cpp               \
        HTTPLiveSource.cpp              \
        NuPlayer.cpp                    \
        NuPlayerDecoder.cpp             \
        NuPlayerDriver.cpp              \
        NuPlayerRenderer.cpp            \
        NuPlayerStreamListener.cpp      \
        RTSPSource.cpp                  \
        StreamingSource.cpp             \
        mp4/MP4Source.cpp               \

LOCAL_C_INCLUDES := \
	$(TOP)/frameworks/av/media/libstagefright/httplive            \
	$(TOP)/frameworks/av/media/libstagefright/include             \
	$(TOP)/frameworks/av/media/libstagefright/mpeg2ts             \
	$(TOP)/frameworks/av/media/libstagefright/rtsp                \
	$(TOP)/frameworks/av/media/libcedarc/include                  \
	$(TOP)/frameworks/native/include/media/openmax

ifeq ($(MARLIN_DEBUG), 1)
LOCAL_C_INCLUDES += \
	$(TOP)/vendor/intertrust/Embedded/AOSP/Source

else
LOCAL_C_INCLUDES += \
	$(TOP)/hardware/aw/marlin/Include

endif

LOCAL_MODULE:= libstagefright_nuplayer

LOCAL_MODULE_TAGS := eng

include $(BUILD_STATIC_LIBRARY)

