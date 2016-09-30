LOCAL_PATH:= $(call my-dir)

########################
# wasabi-drm library

include $(CLEAR_VARS)
LOCAL_MODULE := wasabi-drm
LOCAL_MODULE_SUFFIX := .jar
LOCAL_SRC_FILES := $(LOCAL_MODULE)$(LOCAL_MODULE_SUFFIX)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
include $(BUILD_PREBUILT)

#####################################################################
# static lib
include $(CLEAR_VARS)
LOCAL_PREBUILT_LIBS := \
    libstagefright_aosputil_wasabi.a \
    libstagefright_nuplayer_wasabi.a \
    libWasabi.a

LOCAL_MODULE_TAGS := optional
include $(BUILD_MULTI_PREBUILT)

