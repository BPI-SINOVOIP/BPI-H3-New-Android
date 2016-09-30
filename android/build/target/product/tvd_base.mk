#
# Copyright (C) 2007 The Android Open Source Project
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

# This is a generic tvd product that isn't specialized for a specific device.

PRODUCT_PACKAGES := \
    libfwdlockengine

# Additional settings used in all AOSP builds
PRODUCT_PROPERTY_OVERRIDES := \
    ro.com.android.dateformat=MM-dd-yyyy \
    ro.config.ringtone=Ring_Synth_04.ogg \
    ro.config.notification_sound=pixiedust.ogg

PRODUCT_LOCALES := zh_CN

# Include drawables for all densities
PRODUCT_AAPT_CONFIG := normal hdpi xhdpi xxhdpi
PRODUCT_AAPT_PREF_CONFIG := hdpi

#  PRODUCT_POLICY is useless?
PRODUCT_POLICY := android.policy_phone

PRODUCT_PACKAGES := \
    Camera2 \
    CertInstaller \
    Gallery2 \
    InputDevices \
    Keyguard \
    LatinIME \
    Music \
    MusicFX \
    OneTimeInitializer \
    PrintSpooler \
    Provision \
    SystemUI \
    hostapd \
    wpa_supplicant.conf \
    VpnDialogs

PRODUCT_PACKAGES += \
    audio \
    clatd \
    clatd.conf \
    dhcpcd.conf \
    network \
    pand \
    pppd \
    sdptool \
    wpa_supplicant

PRODUCT_PACKAGES += \
    librs_jni \

PRODUCT_PACKAGES += \
    audio.primary.default \
    audio_policy.default \
    local_time.default \
    power.default

PRODUCT_PACKAGES += \
    local_time.default

PRODUCT_COPY_FILES := \
    frameworks/av/media/libeffects/data/audio_effects.conf:system/etc/audio_effects.conf

# Get some sounds
$(call inherit-product, frameworks/base/data/sounds/AllAudio.mk)

# Get the TTS language packs
$(call inherit-product-if-exists, external/svox/pico/lang/all_pico_languages.mk)

# Get a list of languages.
$(call inherit-product-if-exists, $(SRC_TARGET_DIR)/product/locales_full.mk)


$(call inherit-product-if-exists, frameworks/base/data/fonts/fonts.mk)
$(call inherit-product-if-exists, external/noto-fonts/fonts.mk)
$(call inherit-product-if-exists, external/naver-fonts/fonts.mk)
$(call inherit-product-if-exists, external/sil-fonts/fonts.mk)
$(call inherit-product-if-exists, frameworks/base/data/keyboards/keyboards.mk)
$(call inherit-product-if-exists, frameworks/webview/chromium/chromium.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/tvd_core.mk)

# to be override
PRODUCT_BRAND := Allwinner
PRODUCT_DEVICE := Tvd
PRODUCT_NAME := Tvd
