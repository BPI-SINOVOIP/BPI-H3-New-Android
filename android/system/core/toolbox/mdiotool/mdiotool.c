#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "mii.h"
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <linux/sockios.h>
#include <linux/types.h>
#include <netinet/in.h>

#define help() \
    printf("mdiotool:\n");                  \
    printf("phy read operation: mdiotool phy_read reg_addr\n");          \
    printf("phy write operation: mdiotool phy_write reg_addr value\n");    \
    printf("mdio read operation: mdiotool mdio_read device_addr reg_addr\n"); \
    printf("mdio write operation: mdiotool mdio_write device_addr reg_addr value\n"); \
    printf("For example:\n");            \
    printf("mdiotool phy_read 0x1\n");             \
    printf("mdiotool phy_write 0x0 0x12\n");      \
    printf("mdiotool mdio_read 0x7 0x3c\n");      \
    printf("mdiotool mdio_write 0x7 0x3c 0x12\n\n");      \
    exit(0);

static int phy_read(uint16_t reg_addr) {
    struct mii_ioctl_data *mii = NULL;
    struct ifreq ifr;
    int ret;
    int sockfd;

    memset(&ifr, 0, sizeof(ifr));
    strncpy(ifr.ifr_name, "eth0", IFNAMSIZ - 1);
    sockfd = socket(PF_LOCAL, SOCK_DGRAM, 0);
    if(sockfd < 0) {
        return -1;
    }
    //get phy address in smi bus
    ret = ioctl(sockfd, SIOCGMIIPHY, &ifr);
    if(ret < 0) {
        goto lab;
    }
    mii = (struct mii_ioctl_data*)&ifr.ifr_data;
    mii->reg_num = reg_addr;
    ret = ioctl(sockfd, SIOCGMIIREG, &ifr);
    if(!ret) {
        return mii->val_out;
    }

lab:
    close(sockfd);
    return -1;
}

static void phy_write(uint16_t reg_addr, uint16_t wdata) {
    struct mii_ioctl_data *mii = NULL;
    struct ifreq ifr;
    int ret;
    int sockfd;

    memset(&ifr, 0, sizeof(ifr));
    strncpy(ifr.ifr_name, "eth0", IFNAMSIZ - 1);
    sockfd = socket(PF_LOCAL, SOCK_DGRAM, 0);
    if(sockfd < 0) {
        return;
    }
    //get phy address in smi bus
    ret = ioctl(sockfd, SIOCGMIIPHY, &ifr);
    if(ret < 0) {
        goto lab;
    }
    mii = (struct mii_ioctl_data*)&ifr.ifr_data;
    mii->reg_num = reg_addr;
    mii->val_in = wdata;
    ret = ioctl(sockfd, SIOCSMIIREG, &ifr);

lab:
    close(sockfd);
}

static int mdio_read(uint16_t device_addr, uint16_t reg_addr) {
    int read_value;

    phy_write(0xd, device_addr);
    phy_write(0xe, reg_addr);
    phy_write(0xd, 0x1<<14 | device_addr);
    read_value = phy_read(0xe);
    return read_value;
}

static void mdio_write(uint16_t device_addr, uint16_t reg_addr, uint16_t wdata) {
    phy_write(0xd, device_addr);
    phy_write(0xe, reg_addr);
    phy_write(0xd, 0x1<<14 | device_addr);
    phy_write(0xe, wdata);
}


int mdiotool_main(int argc, char *argv[]) {
        
    if(argc < 2 || !strcmp(argv[1], "-h")) {
        help();
    }

    if(!strcmp(argv[1], "phy_read")) {
        if(argc == 3) {
            uint16_t reg_addr = (uint16_t) strtoul(argv[2], NULL, 0);
            int reg_val = phy_read(reg_addr);
            printf("read phy reg_addr: 0x%x   value : 0x%x\n\n", reg_addr, reg_val);
        } else {
            help();
        }
    } else if(!strcmp(argv[1], "phy_write")) {
        if(argc == 4) {
            uint16_t reg_addr = (uint16_t) strtoul(argv[2], NULL, 0);
            uint16_t wdata = (uint16_t) strtoul(argv[3], NULL, 0);
            printf("write phy reg_addr: 0x%x  value : 0x%x\n\n", reg_addr, wdata);
        } else {
            help();
        }
    } else if(!strcmp(argv[1], "mdio_read")) {
        if(argc == 4) {
            uint16_t device_addr = (uint16_t) strtoul(argv[2], NULL, 0);
            uint16_t reg_addr = (uint16_t) strtoul(argv[3], NULL, 0);
            int mdio_val = mdio_read(device_addr, reg_addr);
            printf("read mdio device_addr: 0x%x  reg_addr: 0x%x value : 0x%x\n\n", device_addr, reg_addr, mdio_val);
        } else {
            help();
        }
    } else if(!strcmp(argv[1], "mdio_write")) {
    	if(argc == 5) {
            uint16_t device_addr = (uint16_t) strtoul(argv[2], NULL, 0);
            uint16_t reg_addr = (uint16_t) strtoul(argv[3], NULL, 0);
            uint16_t wdata = (uint16_t) strtoul(argv[4], NULL, 0);
            mdio_write(device_addr, reg_addr, wdata);
            printf("read mdio device_addr: 0x%x  reg_addr: 0x%x value : 0x%x\n\n", device_addr, reg_addr, wdata);
        } else {
            help();
        }
    } else {
        help();
    }
    return 0;
}
