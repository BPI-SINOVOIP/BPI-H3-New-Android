# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWLauncher
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWControlServer
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWGuide
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SHCMCC_Downloader
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWLogin
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWSettings
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
#LOCAL_OVERRIDES_PACKAGES := TvdSettings
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWUpgrade
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWUpgrade_usb
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWXmpp
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWNetworkInfo
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SHCMCC_Installer
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWControlServer_wimo
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SHCMCC_Diagnostic
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SHCMCC_SANPING
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SHCMCC_DeviceManger
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := SWIME2
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_OVERRIDES_PACKAGES := PinyinIME LatinIME
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)


# do not need system signature
####################################
include $(CLEAR_VARS)
LOCAL_MODULE := SWOTT
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

####################################
include $(CLEAR_VARS)
LOCAL_MODULE := SHCMCC_wimo
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

####################################
include $(CLEAR_VARS)
LOCAL_MODULE := ChinaMobileOttItv
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

####################################
include $(CLEAR_VARS)
LOCAL_MODULE := icntv-shmobile-v7.3.9.3
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

####################################
include $(CLEAR_VARS)
LOCAL_MODULE := icntv-shmobile-h3testmac
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

####################################
include $(CLEAR_VARS)
LOCAL_MODULE := SWLocalPlayer
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_CLASS := APPS
#LOCAL_OVERRIDES_PACKAGES := TvdVideo
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

####################################
include $(CLEAR_VARS)
LOCAL_MODULE := SWPlayer
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := com.yd.appstore.ott-1
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#######################
include $(CLEAR_VARS)
LOCAL_MODULE := AppStore_YD_OTT-1.9_RC1
LOCAL_MODULE_TAGS := optional
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_SRC_FILES := $(LOCAL_MODULE).apk
include $(BUILD_PREBUILT)

#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libtxcore.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)

#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libtxcore2014-06-05_33.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)

# cmcc use
#########################################
#include $(CLEAR_VARS)

#LOCAL_PREBUILT_LIBS := libad_audio.so

#LOCAL_MODULE_TAGS := optional

#include $(BUILD_MULTI_PREBUILT)

#########################################
#include $(CLEAR_VARS)

#LOCAL_PREBUILT_LIBS := libaxx.so

#LOCAL_MODULE_TAGS := optional

#include $(BUILD_MULTI_PREBUILT)

#########################################
#include $(CLEAR_VARS)

#LOCAL_PREBUILT_LIBS := libdxx.so

#LOCAL_MODULE_TAGS := optional

#include $(BUILD_MULTI_PREBUILT)

#########################################
#include $(CLEAR_VARS)

#LOCAL_PREBUILT_LIBS := librx.so

#LOCAL_MODULE_TAGS := optional

#include $(BUILD_MULTI_PREBUILT)

#########################################
#include $(CLEAR_VARS)

#LOCAL_PREBUILT_LIBS := librv.so

#LOCAL_MODULE_TAGS := optional

#include $(BUILD_MULTI_PREBUILT)

#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libyst-widgetmanager.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)
#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libyst-widget.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)
#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libyst-service.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)
#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libyst-player.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)
#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libyst-ime.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)
#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libyst-base.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)
#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libIWAVLogin.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)
#########################################
include $(CLEAR_VARS)

LOCAL_PREBUILT_LIBS := libgefo.so

LOCAL_MODULE_TAGS := optional

include $(BUILD_MULTI_PREBUILT)