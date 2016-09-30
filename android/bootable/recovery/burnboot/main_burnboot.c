#include <stdio.h>
#include "BurnBoot.h"

int main(int argc, char **argv) {
    if (argc!=3) {
        printf("error para!\n");
        printf("usage: burnboot [boot0.fex uboot.fex]\n");
        return 0;
    }
    int ret=0;
    char path[256];
    char boot_bin[256];
    char dev_path[256];
    DeviceBurn burnFunc;
    getDeviceInfo(BOOT0, dev_path, boot_bin, &burnFunc);
    printf("boot0 get boot_bin: %s\n",boot_bin);
    BufferExtractCookie* cookie = malloc(sizeof(BufferExtractCookie));
    if (!getBufferExtractCookieOfFile(argv[1], cookie)) {
        burnFunc(cookie, dev_path);
    }
    free(cookie->buffer);
    free(cookie);

    getDeviceInfo(UBOOT, dev_path, boot_bin, &burnFunc);
    printf("uboot get boot_bin: %s\n",boot_bin);
    cookie = malloc(sizeof(BufferExtractCookie));
    if (!getBufferExtractCookieOfFile(argv[2], cookie)) {
        burnFunc(cookie, dev_path);
    }
    return 0;
}
