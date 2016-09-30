LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    main_systemservice.cpp \
    SystemService.cpp

LOCAL_SHARED_LIBRARIES := \
    libutils \
    liblog \
    libcutils

LOCAL_C_INCLUDES := \
    SystemService.h

LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS += -DLOG_TAG=\"CMCC-WASU\" -DNDEBUG=0

LOCAL_MODULE:= cmccwasu_systemservice

include $(BUILD_EXECUTABLE)