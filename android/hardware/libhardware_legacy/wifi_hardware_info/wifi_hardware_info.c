/*
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <dirent.h>
#include <sys/socket.h>
#include <unistd.h>
#include <poll.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include <poll.h>
#include <linux/if_ether.h>
#include <net/if.h>

#include "hardware_legacy/wifi.h"
#define LOG_TAG "WifiHWInfo"
#include "cutils/log.h"
#include "cutils/properties.h"

#define CMDLINE_PATH               "/proc/cmdline"
#define MAC_KEY_VALUE              "wifi_mac"
#define RTW_MAC_SETVALUE           "rtw_initmac="
#define SSV_MAC_SETVALUE           "ssv_initmac="

#define ARRAY_SIZE(arr) (sizeof(arr) / sizeof((arr)[0]))
#define WIFI_HARDWARE_INFO_PATH "/data/misc/wifi/wifi_hardware_info"
#define WIFI_SCAN_DEVICE_PATH   "/sys/devices/virtual/misc/sunxi-wlan/rf-ctrl/scan_device"
#define WIFI_POWER_STATE_PATH   "/sys/devices/virtual/misc/sunxi-wlan/rf-ctrl/power_state"

static pthread_mutex_t  mutex = PTHREAD_MUTEX_INITIALIZER;

struct wifi_hardware_info {
    unsigned long device_id;
    char *module_name;
    char *driver_name;
    char *vendor_name;
    char *fw_path_sta;
    char *fw_path_ap;
    char *fw_path_p2p;
};
static const struct wifi_hardware_info wifi_list[] = {
    {
        .device_id    = 0x00179,
        .module_name  = "rtl8188eu",
        .driver_name  = "8188eu",
        .vendor_name  = "realtek",
        .fw_path_sta = "STA",
        .fw_path_ap  = "AP",
        .fw_path_p2p = "P2P",
    },
    {
        .device_id    = 0x08179,
        .module_name  = "rtl8188etv",
        .driver_name  = "8188eu",
        .vendor_name  = "realtek",
        .fw_path_sta = "STA",
        .fw_path_ap  = "AP",
        .fw_path_p2p = "P2P",
    },
    {
        .device_id   = 0x18179,
        .module_name = "rtl8189es",
        .driver_name = "8189es",
        .vendor_name = "realtek",
        .fw_path_sta = "STA",
        .fw_path_ap  = "AP",
        .fw_path_p2p = "P2P",
    },
    {
        .device_id   = 0x1f179,
        .module_name = "rtl8189fs",
        .driver_name = "8189fs",
        .vendor_name = "realtek",
        .fw_path_sta = "STA",
        .fw_path_ap  = "AP",
        .fw_path_p2p = "P2P",
    },
    {
        .device_id   = 0x1b723,
        .module_name = "rtl8723bs",
        .driver_name = "8723bs_vq0",
        .vendor_name = "realtek",
        .fw_path_sta = "STA",
        .fw_path_ap  = "AP",
        .fw_path_p2p = "P2P",
    },
    {
        .device_id   = 0x1a9a6,
        .module_name = "ap6212",
        .driver_name = "bcmdhd",
        .vendor_name = "broadcom",
        .fw_path_sta = "/system/vendor/modules/fw_bcm43438a0.bin",
        .fw_path_ap  = "/system/vendor/modules/fw_bcm43438a0_apsta.bin",
        .fw_path_p2p = "/system/vendor/modules/fw_bcm43438a0_p2p.bin",
    },
    {
        .device_id   = 0x1a962,
        .module_name = "ap6210",
        .driver_name = "bcmdhd",
        .vendor_name = "broadcom",
        .fw_path_sta = "/system/vendor/modules/fw_bcm40181a2.bin",
        .fw_path_ap  = "/system/vendor/modules/fw_bcm40181a2_apsta.bin",
        .fw_path_p2p = "/system/vendor/modules/fw_bcm40181a2_p2p.bin",
    },
    {
        .device_id   = 0x14330,
        .module_name = "ap6330",
        .driver_name = "bcmdhd",
        .vendor_name = "broadcom",
        .fw_path_sta = "/system/vendor/modules/fw_bcm40183b2_ag.bin",
        .fw_path_ap  = "/system/vendor/modules/fw_bcm40183b2_ag_apsta.bin",
        .fw_path_p2p = "/system/vendor/modules/fw_bcm40183b2_ag_p2p.bin",
    },
    {
        .device_id   = 0x14335,
        .module_name = "ap6335",
        .driver_name = "bcmdhd",
        .vendor_name = "broadcom",
        .fw_path_sta = "/system/vendor/modules/fw_bcm4339a0_ag.bin",
        .fw_path_ap  = "/system/vendor/modules/fw_bcm4339a0_ag_apsta.bin",
        .fw_path_p2p = "/system/vendor/modules/fw_bcm4339a0_ag_p2p.bin",
    },
    {
        .device_id   = 0x13030,
        .module_name = "ssv6051",
        .driver_name = "ssv6051",
        .vendor_name = "southsv",
        .fw_path_sta = "STA",
        .fw_path_ap  = "AP",
        .fw_path_p2p = "P2P",
    },
    {
        .device_id   = 0x12281,
        .module_name = "xr819",
        .driver_name = "xradio_wlan",
        .vendor_name = "xradio",
        .fw_path_sta = "STA",
        .fw_path_ap  = "AP",
        .fw_path_p2p = "P2P",
    },
};
/* default select rtl8189es if get wifi_hardware_info failed */
static struct wifi_hardware_info selected_wifi = {
    0x18179, "rtl8189es", "8189es", "realtek", "STA", "AP", "P2P"
};

static enum{running, exiting, exited} thread_state = exited;

static int get_hardware_info_by_module_name(const char *module_name)
{
    unsigned int i = 0;
    if(strcmp(selected_wifi.module_name, module_name) == 0) {
        return 0;
    }
    for(i = 0; i < ARRAY_SIZE(wifi_list); i++) {
        if(strcmp(wifi_list[i].module_name, module_name) == 0) {
            selected_wifi = wifi_list[i];
            return 0;
        }
    }
    return -1;
}

static int get_hardware_info_by_device_id(const unsigned long device_id)
{
    unsigned int i = 0;
    if(selected_wifi.device_id == device_id) {
        return 0;
    }
    for(i = 0; i < ARRAY_SIZE(wifi_list); i++) {
        if(wifi_list[i].device_id == device_id) {
            selected_wifi = wifi_list[i];
            return 0;
        }
    }
    return -1;
}

static int wifi_power_on(void)
{
    int fd = 0;
    int size = 0;
    char store_state = 0;
    char to_write = '1';

    fd = open(WIFI_POWER_STATE_PATH, O_RDWR);
    if (fd < 0) {
        return -1;
    }

    size = read(fd, &store_state, sizeof(store_state));
    if (size <= 0) {
        close(fd);
        return -1;
    }

    size = write(fd, &to_write, sizeof(to_write));
    if (size < 0) {
        close(fd);
        return -1;
    }
    close(fd);
    return(store_state - '0');
}

static int wifi_power_off(void)
{
    int fd = 0;
    int size = 0;
    char to_write = '0';

    fd = open(WIFI_POWER_STATE_PATH, O_WRONLY);
    if (fd < 0) {
        return -1;
    }
    size = write(fd, &to_write, sizeof(to_write));
    if (size < 0) {
        close(fd);
        return -1;
    }
    close(fd);
    return 0;
}

static int wifi_scan_device(int val)
{
    int fd = 0;
    int size = 0;
    char to_write = val ? '1' : '0';

    fd = open(WIFI_SCAN_DEVICE_PATH, O_WRONLY);
    if (fd < 0) {
        return -1;
    }

    size = write(fd, &to_write, sizeof(to_write));
    if (size < 0) {
        close(fd);
        return -1;
    }
    close(fd);
    return 0;
}

static void parse_uevent(char *msg)
{
    char sdio_device_id[10] = {0};
    char device_type[10] = {0};
    char *subsystem = "";
    char *sdio_id = "";
    char *usb_product = "";
    unsigned long device_id = 0;

    while(*msg) {
        if(!strncmp(msg, "SUBSYSTEM=", 10)) {
            msg += 10;
            subsystem = msg;
        } else if(!strncmp(msg, "SDIO_ID=", 8)) {
            msg += 8;
            sdio_id = msg;
        } else if(!strncmp(msg, "PRODUCT=", 8)) {
            msg += 8;
            usb_product = msg;
        }

        /* advance to after the next \0 */
        while(*msg++) {
            /* do nothing */
        }
    }

    if(!strncmp(subsystem, "sdio", 4)) {
        ALOGI("get uevent, sdio_id = %s", sdio_id);
        strcpy(device_type, "sdio");
        char *subid = strrchr(sdio_id, ':');
        if(subid == NULL) {
            return;
        }
        subid++;
        strcpy(sdio_device_id, subid);
        device_id = strtoul(sdio_device_id, NULL, 16);
        device_id += 0x10000;
    } else if(!strncmp(subsystem, "usb", 3)) {
        strcpy(device_type, "usb");
        char *subid = NULL;

        strtok(usb_product, "/");
        subid = strtok( NULL, "/");
        if(subid == NULL) {
            return;
        }
        device_id = strtoul(subid, NULL, 16);
    } else {
        return;
    }

    if(!get_hardware_info_by_device_id(device_id)) {
        FILE *file = fopen(WIFI_HARDWARE_INFO_PATH, "w");
        char info[PROPERTY_VALUE_MAX] = {0};
        snprintf(info, sizeof(info), "%s:%s", selected_wifi.vendor_name, selected_wifi.module_name);
        if(file == NULL){
            ALOGE("cannot open file %s to write", WIFI_HARDWARE_INFO_PATH);
            return;
        }
        ALOGD("write wifi_hardware_info into file: %s:%s", selected_wifi.vendor_name,
                selected_wifi.module_name);
        fprintf(file,"%s\n", info);
        fflush(file);
        fsync(fileno(file));
        fclose(file);
        property_set("wlan.hardware.info", info);
        thread_state = exiting;
    }
}

#define UEVENT_MSG_LEN  1024
static void * ls_device_thread()
{
    char buf[UEVENT_MSG_LEN + 2] = {0};
    int count;
    int err;
    int retval;
    struct sockaddr_nl snl;
    int sock;
    struct pollfd fds;
    const int buffersize = 32*1024;

    thread_state = running;
    memset(&snl, 0x0, sizeof(snl));
    snl.nl_family = AF_NETLINK;
    snl.nl_pid = getpid();
    snl.nl_groups = 0xffffffff;
    sock = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if (sock < 0) {
        ALOGE("####socket is failed in %s error:%d %s###", __FUNCTION__, errno, strerror(errno));
        return((void *)-1);
    }
    setsockopt(sock, SOL_SOCKET, SO_RCVBUFFORCE, &buffersize, sizeof(buffersize));
    retval = bind(sock, (struct sockaddr *)&snl, sizeof(struct sockaddr_nl));
    if(retval < 0) {
        snl.nl_pid = getpid() + 1; //maybe pid has been used
        if (bind(sock, (struct sockaddr *)&snl, sizeof(struct sockaddr_nl)) < 0) {
            ALOGE("####bind is failed in %s error:%d %s###", __FUNCTION__, errno, strerror(errno));
            close(sock);
            return((void *)-1);
        }
    }

    while (running == thread_state) {
        fds.fd = sock;
        fds.events = POLLIN;
        fds.revents = 0;
        err = poll(&fds, 1, 1000);
        memset(buf, '\0', sizeof(char) * 1024);
        if(err > 0 && (fds.revents & POLLIN)) {
            count = recv(sock, buf, sizeof(char) * 1024,0);
            if(count > 0) {
                parse_uevent(buf);
            }
        }
    }

    close(sock);
    thread_state = exited;
    return((void *)0);
}

static void get_wifi_hardware_info()
{
    char info[PROPERTY_VALUE_MAX] = {0};
    int ret = 0;
    pthread_t ls_device_thread_fd;
    int store_power_state = 0;
    int size = 0;
    int fd = -1;

    pthread_mutex_lock(&mutex);

start:
    if (property_get("wlan.hardware.info", info, NULL) > 0) {
        char *module_name = strchr(info, ':');
        if(module_name == NULL) {
            property_set("wlan.hardware.info", "");//clear wlan.hardware.info
            goto start;
        }

        module_name++;
        if(get_hardware_info_by_module_name(module_name)) {
            property_set("wlan.hardware.info", "");//clear wlan.hardware.info
            goto start;
        }
    } else if(access(WIFI_HARDWARE_INFO_PATH, 0) == -1) {
        ALOGD("%s not exist, try to create it!", WIFI_HARDWARE_INFO_PATH);
        ret = pthread_create(&ls_device_thread_fd, NULL, ls_device_thread, NULL);
        if(ret) {
            ALOGE("Create ls_device_thread error!\n");
            pthread_mutex_unlock(&mutex);
            return;
        }

        do {
            wifi_power_on();
            if(store_power_state < 0) {
                pthread_mutex_unlock(&mutex);
                return;
            } else {
                usleep(500000);
                wifi_scan_device(1);
            }
            usleep(500000);
            wifi_power_off();
            wifi_scan_device(0);
            usleep(500000);
        } while (running == thread_state);
        pthread_join(ls_device_thread_fd, NULL);
    } else if(access(WIFI_HARDWARE_INFO_PATH, 0) == 0) {
        ALOGD("%s exist, try to get wifi_hardware_info", WIFI_HARDWARE_INFO_PATH);
        fd = TEMP_FAILURE_RETRY(open(WIFI_HARDWARE_INFO_PATH, O_RDONLY));
        if (fd < 0) {
            ALOGE("Failed to open wifi_hardware_info (%s)", strerror(errno));
            pthread_mutex_unlock(&mutex);
            return;
        }

        if (TEMP_FAILURE_RETRY(read(fd, info, sizeof(info) - 1)) < 0) {
            ALOGE("Failed to read wifi_hardware_info (%s)", strerror(errno));
            close(fd);
            pthread_mutex_unlock(&mutex);
            return;
        }

        close(fd);
        size = strlen(info);
        if(size > 0 && info[size - 1] == '\n') {
            size--;
            info[size] = 0;
        }
        char *module_name = strchr(info, ':');
        if(module_name == NULL) {
            if(remove(WIFI_HARDWARE_INFO_PATH) < 0) { // remove saved file because it contain error info
                pthread_mutex_unlock(&mutex);
                return;
            }
            goto start;
        }

        module_name++;
        if(!get_hardware_info_by_module_name(module_name)) {
            ALOGD("wifi_hardware_info from %s: %s", WIFI_HARDWARE_INFO_PATH, info);
            property_set("wlan.hardware.info", info);
        } else {
            if(remove(WIFI_HARDWARE_INFO_PATH) < 0) { // remove saved file because it contain error info
                pthread_mutex_unlock(&mutex);
                return;
            }
            goto start;
        }
    }
    pthread_mutex_unlock(&mutex);
}

const char *get_wifi_vendor_name()
{
#if defined(WIFI_VENDOR_NAME)
    return WIFI_VENDOR_NAME;
#else
    get_wifi_hardware_info();
    return selected_wifi.vendor_name;
#endif
}

const char *get_wifi_module_name()
{
#if defined(WIFI_MODULE_NAME)
    return WIFI_MODULE_NAME;
#else
    get_wifi_hardware_info();
    return selected_wifi.module_name;
#endif
}

const char *get_wifi_driver_name()
{
#if defined(WIFI_DRIVER_NAME)
    return WIFI_DRIVER_NAME;
#else
    get_wifi_hardware_info();
    return selected_wifi.driver_name;
#endif
}

const char *get_fw_path_sta()
{
#if defined(WIFI_DRIVER_FW_PATH_STA)
    return WIFI_DRIVER_FW_PATH_STA;
#else
    get_wifi_hardware_info();
    return selected_wifi.fw_path_sta;
#endif
}

const char *get_fw_path_ap()
{
#if defined(WIFI_DRIVER_FW_PATH_AP)
    return WIFI_DRIVER_FW_PATH_AP;
#else
    get_wifi_hardware_info();
    return selected_wifi.fw_path_ap;

#endif
}

const char *get_fw_path_p2p()
{
#if defined(WIFI_DRIVER_FW_PATH_P2P)
    return WIFI_DRIVER_FW_PATH_P2P;
#else
    get_wifi_hardware_info();
    return selected_wifi.fw_path_p2p;

#endif
}

static int get_mac_address(char *name, char *mac_value)
{
    char *value = strchr(name, '=');
    if(value == 0){
        return -1;
    }
    *value++ = 0;
    int name_len = strlen(name);
    if(name_len == 0){
        return -1;
    }
    if(!strcmp(MAC_KEY_VALUE, name)){
        strcpy(mac_value, value);
        return 0;
    }
    return -1;
}
static void parse_cmdline(char* value)
{
    char cmdline[1024];
    char *ptr = NULL;
    int fd = -1;

    fd = open(CMDLINE_PATH, O_RDONLY);
    if(fd >= 0){
        int n = read(fd, cmdline, 1023);
        if(n < 0){
            n = 0;
        }
        if(n > 0 && cmdline[n-1] == '\n'){
            n--;
        }
        cmdline[n] = 0;
        close(fd);
    }else{
        cmdline[0] = 0;
        ALOGD("read cmdline fail,");
    }
    ptr = cmdline;
    while (ptr && *ptr) {
        char *x = strchr(ptr, ' ');
        if(x != 0){
            *x++ = 0;
        }
        if(get_mac_address(ptr, value) == 0)
            return;
        ptr = x;
    }
}

static int check_invalid_mac_address(char *mac_str) {
    char null_mac_addr[ETH_ALEN] = {0, 0, 0, 0, 0, 0};
    char multi_mac_addr[ETH_ALEN] = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff};
    char mac_addr[ETH_ALEN] = {0, 0, 0, 0, 0, 0};
    char *p = mac_str;
    int res = 0, i = 0;

    for (i=0; i < ETH_ALEN; i++, p++) {
        mac_addr[i] = strtoul(p, &p, 16);
    }

    if (memcmp(mac_addr, null_mac_addr, ETH_ALEN) == 0) {
        res = 1;
        goto func_exit;
    }

    if (memcmp(mac_addr, multi_mac_addr, ETH_ALEN) == 0) {
        res = 1;
        goto func_exit;
    }

    if (mac_addr[0] & 0x01) {
        res = 1;
        goto func_exit;
    }

    if (mac_addr[0] & 0x02) {
        res = 1;
        goto func_exit;
    }

func_exit:
    return res;
}
void get_driver_module_arg(char* arg)
{
    char module_arg[256] = {0};
    char mac[18] = {0};
    int check_mac = 0;

    const char *vendor_name = get_wifi_vendor_name();
    parse_cmdline(mac);
    check_mac = check_invalid_mac_address(mac);

    if (strcmp(vendor_name, "realtek") == 0) {
        const char *driver_module_arg = "ifname=wlan0 if2name=p2p0";
        if (!check_mac) {
            snprintf(module_arg, sizeof(module_arg), "%s %s%s", driver_module_arg, RTW_MAC_SETVALUE, mac);
        } else {
            snprintf(module_arg, sizeof(module_arg), "%s", driver_module_arg);
        }
    } else if(strcmp(vendor_name, "broadcom") == 0) {
        const char *nvram_path = "nvram_path=/system/vendor/modules/nvram";
        const char *config_path = "config_path=/system/vendor/modules/config";
        snprintf(module_arg, sizeof(module_arg), "%s_%s.txt %s_%s.txt",
                nvram_path, get_wifi_module_name(), config_path, get_wifi_module_name());
    } else if (strcmp(vendor_name, "southsv") == 0) {
        const char *driver_module_arg = "stacfgpath=\"/etc/firmware/ssv6051-wifi.cfg\"";
        if (!check_mac) {
            snprintf(module_arg, sizeof(module_arg), "%s %s%s", driver_module_arg, SSV_MAC_SETVALUE, mac);
        } else {
            snprintf(module_arg, sizeof(module_arg), "%s", driver_module_arg);
        }
    } else if(strcmp(vendor_name, "xradio") == 0){
        if (!check_mac) {
            snprintf(module_arg, sizeof(module_arg), "%s%s", "macaddr=", mac);
        } else {
            const char *driver_module_arg = "";
            snprintf(module_arg, sizeof(module_arg), "%s", driver_module_arg);
        }

    }
    strcpy(arg, module_arg);
}
