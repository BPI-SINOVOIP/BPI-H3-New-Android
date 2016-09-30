LOCAL_PATH:= $(call my-dir)

#####################################################################
# libvmclient.so
#####################################################################
include $(CLEAR_VARS)
LOCAL_MODULE := libvmclient
LOCAL_SRC_FILES := libvmclient.so
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := .so
include $(BUILD_PREBUILT)

#####################################################################
# libvmlogger.so
#####################################################################
include $(CLEAR_VARS)
LOCAL_MODULE := libvmlogger
LOCAL_SRC_FILES := logging/VMLog.cpp
LOCAL_SHARED_LIBRARIES := liblog libcutils libutils
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)
