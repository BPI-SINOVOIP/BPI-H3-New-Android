# Copyright 2005 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	builtins.c \
	init.c \
	devices.c \
	property_service.c \
	util.c \
	parser.c \
	init_disp.c \
	logo.c \
	keychords.c \
	signal_handler.c \
	init_parser.c \
	ueventd.c \
	ueventd_parser.c \
	watchdogd.c

#ifeq ($(BLUETOOTH_HCI_USE_USB), true)
    LOCAL_CFLAGS += -DUSE_USB_BT
#endif

ifeq ($(strip $(INIT_BOOTCHART)),true)
LOCAL_SRC_FILES += bootchart.c
LOCAL_CFLAGS    += -DBOOTCHART=1
endif

ifneq (,$(filter userdebug eng,$(TARGET_BUILD_VARIANT)))
LOCAL_CFLAGS += -DALLOW_LOCAL_PROP_OVERRIDE=1
endif

ifeq ($(strip $(TARGET_USE_BOOSTUP_OPZ)), true)
LOCAL_SRC_FILES += boostup.c
LOCAL_CFLAGS    += -DAW_BOOSTUP_ENABLE=1
ifeq ($(strip $(TARGET_BOARD_PLATFORM)), jaws)
LOCAL_CFLAGS	+= -DSUN9IW1P1=1
endif
ifeq ($(strip $(SW_CHIP_PLATFORM)), H8)
LOCAL_CFLAGS	+= -DSUN8IW6P1=1
endif
ifeq ($(strip $(SW_CHIP_PLATFORM)), H3)
LOCAL_CFLAGS    += -DSUN8IW7P1=1
endif
endif

ifneq ($(DISPLAY_INIT_POLICY),)
    LOCAL_CFLAGS += -DDISP_POLICY=$(DISPLAY_INIT_POLICY)
else
    LOCAL_CFLAGS += -DDISP_POLICY=0
endif

ifneq ($(HDMI_CHANNEL),)
    LOCAL_CFLAGS += -DHDMI_CHANNEL=$(HDMI_CHANNEL)
else
    LOCAL_CFLAGS += -DHDMI_CHANNEL=-1
endif
ifneq ($(HDMI_DEFAULT_MODE),)
    LOCAL_CFLAGS += -DDISP_DEFAULT_HDMI_MODE=$(HDMI_DEFAULT_MODE)
else
    LOCAL_CFLAGS += -DDISP_DEFAULT_HDMI_MODE=4
endif

ifneq ($(CVBS_CHANNEL),)
    LOCAL_CFLAGS += -DCVBS_CHANNEL=$(CVBS_CHANNEL)
else
    LOCAL_CFLAGS += -DCVBS_CHANNEL=-1
endif
ifneq ($(CVBS_DEFAULT_MODE),)
    LOCAL_CFLAGS += -DDISP_DEFAULT_CVBS_MODE=$(CVBS_DEFAULT_MODE)
else
    LOCAL_CFLAGS += -DDISP_DEFAULT_CVBS_MODE=11
endif

ifneq ($(SHOW_INITLOGO),true)
    LOCAL_CFLAGS += -DDONT_SHOW_INITLOGO
endif

LOCAL_MODULE:= init

LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_MODULE_PATH := $(TARGET_ROOT_OUT)
LOCAL_UNSTRIPPED_PATH := $(TARGET_ROOT_OUT_UNSTRIPPED)
LOCAL_C_INCLUDES += $(TARGET_HARDWARE_INCLUDE)
LOCAL_STATIC_LIBRARIES := libfs_mgr libcutils libc

LOCAL_STATIC_LIBRARIES := \
	libfs_mgr \
	liblogwrap \
	libcutils \
	liblog \
	libc \
	libselinux \
	libmincrypt \
	libext4_utils_static

#ifeq ($(BLUETOOTH_HCI_USE_USB), true)
    LOCAL_STATIC_LIBRARIES += libusbhost
#endif

include $(BUILD_EXECUTABLE)

# Make a symlink from /sbin/ueventd and /sbin/watchdogd to /init
SYMLINKS := \
	$(TARGET_ROOT_OUT)/sbin/ueventd \
	$(TARGET_ROOT_OUT)/sbin/watchdogd

$(SYMLINKS): INIT_BINARY := $(LOCAL_MODULE)
$(SYMLINKS): $(LOCAL_INSTALLED_MODULE) $(LOCAL_PATH)/Android.mk
	@echo "Symlink: $@ -> ../$(INIT_BINARY)"
	@mkdir -p $(dir $@)
	@rm -rf $@
	$(hide) ln -sf ../$(INIT_BINARY) $@

ALL_DEFAULT_INSTALLED_MODULES += $(SYMLINKS)

# We need this so that the installed files could be picked up based on the
# local module name
ALL_MODULES.$(LOCAL_MODULE).INSTALLED := \
    $(ALL_MODULES.$(LOCAL_MODULE).INSTALLED) $(SYMLINKS)
