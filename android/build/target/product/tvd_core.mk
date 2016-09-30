#
# Copyright (C) 2013 The Android Open Source Project
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
#

# Core configuration for Tv box devices.

PRODUCT_PACKAGES += \
    BasicDreams \
    DocumentsUI \
	DownloadProvider \
    DownloadProviderUi \
    ExternalStorageProvider \
    KeyChain \
    PacProcessor \
    SharedStorageBackup 

PRODUCT_PACKAGES += \
    BackupRestoreConfirmation \
    DownloadProvider \
    MediaProvider \
    PackageInstaller \
    SettingsProvider \
    Shell \
    bu \
    com.android.location.provider \
    com.android.location.provider.xml \
    com.android.media.remotedisplay \
    com.android.media.remotedisplay.xml \
    drmserver \
    framework-res \
    installd \
    ip \
    ip-up-vpn \
    ip6tables \
    iptables \
    keystore \
    keystore.default \
    libOpenMAXAL \
    libOpenSLES \
    libdownmix \
    libdrmframework \
    libdrmframework_jni \
    libfilterfw \
    libsqlite_jni \
    libwilhelm \
    make_ext4fs \
    screencap \
    sensorservice \
    uiautomator

PRODUCT_RUNTIMES := runtime_libdvm_default

PRODUCT_PROPERTY_OVERRIDES := \
    ro.config.notification_sound=OnTheHunt.ogg \
    ro.config.alarm_alert=Alarm_Classic.ogg

PRODUCT_PACKAGES += \
    DefaultContainerService \
    UserDictionaryProvider \
    atrace \
    libandroidfw \
    libaudiopreprocessing \
    libaudioutils \
    libbcc \
    libfilterpack_imageproc \
    libgabi++ \
    libkeystore \
    libmdnssd \
    libnfc_ndef \
    libportable \
    libpowermanager \
    libspeexresampler \
    libstagefright_chromium_http \
    libstagefright_soft_aacdec \
    libstagefright_soft_aacenc \
    libstagefright_soft_amrdec \
    libstagefright_soft_amrnbenc \
    libstagefright_soft_amrwbenc \
    libstagefright_soft_flacenc \
    libstagefright_soft_g711dec \
    libstagefright_soft_gsmdec \
    libstagefright_soft_h264dec \
    libstagefright_soft_h264enc \
    libstagefright_soft_mp3dec \
    libstagefright_soft_mpeg4dec \
    libstagefright_soft_mpeg4enc \
    libstagefright_soft_rawdec \
    libstagefright_soft_vorbisdec \
    libstagefright_soft_vpxdec \
    libstagefright_soft_vpxenc \
    libvariablespeed \
    libwebrtc_audio_preprocessing \
    mdnsd \
    screenrecord

ifndef PRIVATE_BOOT_JARS
PRODUCT_BOOT_JARS := core:conscrypt:okhttp:core-junit:bouncycastle:ext:framework:framework2:android.policy:services:apache-xml:webviewchromium
else
PRODUCT_BOOT_JARS := core:conscrypt:okhttp:core-junit:bouncycastle:ext:framework:framework2:android.policy:services:apache-xml:webviewchromium:$(PRIVATE_BOOT_JARS)
endif

$(call inherit-product, $(SRC_TARGET_DIR)/product/base.mk)
