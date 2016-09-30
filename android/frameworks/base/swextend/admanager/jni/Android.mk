LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    com_softwinner_AdManager.cpp

LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libnativehelper \
    libutils \
    libbinder \
    libui \
    libcutils \
    libsystemmixservice

LOCAL_STATIC_LIBRARIES :=

LOCAL_C_INCLUDES += \
    frameworks/base/core/jni \
    frameworks/base/swextend/systemmix/libsystemmix \
    $(JNI_H_INCLUDE) \
    system/core/include/cutils \

LOCAL_CFLAGS +=

LOCAL_MODULE_TAGS := optional

LOCAL_LDLIBS := -lpthread

LOCAL_MODULE:= libadmanager_jni

LOCAL_PRELINK_MODULE:= false

include $(BUILD_SHARED_LIBRARY)

