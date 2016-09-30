LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        src/bt_hci_bdroid.c \
        src/lpm.c \
        src/bt_hw.c \
        src/btsnoop.c \
        src/utils.c
 
LOCAL_SRC_FILES += \
       src/hci_h5.c \
       src/userial.c \
       src/bt_skbuff.c \
       src/bt_list.c

LOCAL_SRC_FILES += \
        src/hci_h4.c \
        src/usb.c

LOCAL_C_INCLUDES += \
        external/libusb

LOCAL_SHARED_LIBRARIES += \
        libusb


LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/include \
        $(LOCAL_PATH)/../utils/include \
        $(LOCAL_PATH)/../libaw/include

LOCAL_SHARED_LIBRARIES += \
        libcutils \
        liblog \
        libdl \
        libbt-utils

LOCAL_STATIC_LIBRARIES := libbt-aw libwifi_hardware_info

LOCAL_MODULE := libbt-hci
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

include $(BUILD_SHARED_LIBRARY)
