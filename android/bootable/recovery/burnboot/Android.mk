LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	BurnBoot.c \
	Utils.c 

LOCAL_C_INCLUDES += \
	external/zlib \
	external/safe-iop/include



LOCAL_MODULE := libburnboot

LOCAL_CFLAGS += -Wall

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    main_burnboot.c \
	BurnBoot.c \
	Utils.c

LOCAL_C_INCLUDES += \
	external/zlib \
	external/safe-iop/include

LOCAL_MODULE := burnboot

LOCAL_CFLAGS += -Wall

include $(BUILD_EXECUTABLE)
