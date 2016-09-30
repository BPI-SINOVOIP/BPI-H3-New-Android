LOCAL_PATH:= $(call my-dir)

#####################################################################
# basic vmclient
#####################################################################
include $(CLEAR_VARS)
LOCAL_SRC_FILES:= basic/io.c basic/main.c
LOCAL_C_INCLUDES:=$(LOCAL_PATH)/../include $(LOCAL_PATH)/../libs/logging
LOCAL_SHARED_LIBRARIES:= libcutils libutils libvmclient libvmlogger
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= vmclient
#include $(BUILD_EXECUTABLE)

#####################################################################
# basicSw vmclientSw
#####################################################################
include $(CLEAR_VARS)
LOCAL_SRC_FILES:= basicSw/io.c basicSw/main.c
LOCAL_C_INCLUDES:=$(LOCAL_PATH)/../include
LOCAL_SHARED_LIBRARIES:= libcutils libutils libvmclient libvmlogger
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= vmclientSw
include $(BUILD_EXECUTABLE)

