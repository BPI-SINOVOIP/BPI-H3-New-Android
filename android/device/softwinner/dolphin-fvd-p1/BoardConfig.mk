# BoardConfig.mk
#
# Product-specific compile-time definitions.
#

include device/softwinner/dolphin-common/BoardConfigCommon.mk

# image related
TARGET_NO_BOOTLOADER := true
TARGET_NO_RECOVERY := false
TARGET_NO_KERNEL := false

INSTALLED_KERNEL_TARGET := kernel
BOARD_KERNEL_BASE := 0x40000000
BOARD_KERNEL_CMDLINE :=
TARGET_USERIMAGES_USE_EXT4 := true
BOARD_FLASH_BLOCK_SIZE := 4096
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 805306368
#BOARD_USERDATAIMAGE_PARTITION_SIZE :=

# wifi and bt configuration
# 1. wifi Configuration
WPA_SUPPLICANT_VERSION := VER_0_8_X
BOARD_WPA_SUPPLICANT_DRIVER := NL80211
BOARD_WPA_SUPPLICANT_PRIVATE_LIB := lib_driver_cmd
BOARD_HOSTAPD_DRIVER        := NL80211
BOARD_HOSTAPD_PRIVATE_LIB   := lib_driver_cmd

include hardware/broadcom/wlan/bcmdhd/firmware/firmware-bcm.mk

# 2. Bluetooth Configuration
# make sure BOARD_HAVE_BLUETOOTH is true for every bt vendor
include device/softwinner/common/hardware/realtek/bluetooth/rtl8723bs/firmware/rtlbtfw_cfg.mk
BOARD_HAVE_BLUETOOTH := true
BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := device/softwinner/common/
