ifeq ($(TARGET_ARCH),arm)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= \
	src/pppoe.c \
	src/if.c \
	src/debug.c \
	src/common.c \
	src/ppp.c \
	src/discovery.c
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/src
LOCAL_CFLAGS := -DANDROID_CHANGES -DVERSION="3.10"
LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE:= pppoe
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= \
        src/plugin.c \
        src/if.c \
        src/debug.c \
        src/common.c \
        src/ppp.c \
        src/discovery.c
LOCAL_C_INCLUDES := \
        external/ppp \
        external/ppp/pppd/include
LOCAL_CFLAGS := -DANDROID_CHANGES -DRP_VERSION="3.10"
LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE:= librp-pppoe
include $(BUILD_STATIC_LIBRARY)

endif
