LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	main.cpp \
    FdHelper.cpp

LOCAL_C_INCLUDES += system/core/include/utils/

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libutils

LOCAL_MODULE_TAGS := eng

LOCAL_MODULE:= fdhelper

include $(BUILD_EXECUTABLE)
