#include <utils/Log.h>
#include <stdio.h>
#include <malloc.h>
#include <dirent.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <cutils/properties.h>
#include "SystemService.h"
#define CMDLINE_MAC "mac"
#define CMDLINE_SPECIALSTR "specialstr"
#define CMDLINE_SERIALNO "serialno"
#define CMCC_MAC "ro.mac"
#define CMCC_SPECIALSTR "ro.specialstr"
#define CMCC_SERIALNO "ro.serialno"

namespace android{

void SystemService::init(){
    //init mac and serialno
    get_kernel_cmdline_serialno();
    import_kernel_cmdline(import_kernel_nv);
    initMac();
}

void SystemService::import_kernel_cmdline(void (*import_kernel_nv)(char *name)){
    char cmdline[1024];
    char *ptr;
    int fd;

    fd = open("/proc/cmdline", O_RDONLY);
    ALOGD("import_kernel_cmdline");
    if(fd >= 0){
        int n = read(fd, cmdline, 1023);
        if(n < 0){
            n = 0;
        }
        /* get rid of trailing newline, it happens */
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
    while (ptr && *ptr){
        char *x = strchr(ptr, ' ');
        if(x != 0){
            *x++ = 0;
        }
        import_kernel_nv(ptr);
        ptr = x;
    }
}

void SystemService::import_kernel_nv(char *name){
    char *value = strchr(name, '=');
    if(value == 0){
        return;
    }
    *value++ = 0;
    int name_len = strlen(name);
    if(name_len == 0){
        return;
    }
    ALOGD("import_kernel_nv name=%s, value=%s", name, value);
    if(!strcmp(CMDLINE_SERIALNO, name)){
        property_set(CMCC_SERIALNO, value);
    }
	if(!strcmp(CMDLINE_SPECIALSTR, name)){
        property_set(CMCC_SPECIALSTR, value);
    }
}

void SystemService::initMac(){
    char mac[32];
    int fd;
    fd = open("/sys/class/net/eth0/address", O_RDONLY);
    if(fd >= 0){
        int n = read(fd, mac, 31);
        if(n < 0){
            n = 0;
        }
        /* get rid of trailing newline, it happens */
        if(n > 0 && mac[n-1] == '\n'){
            n--;
        }
        mac[n] = 0;
        ALOGD("%d mac address is %saaaa", n, mac);
        close(fd);
    }else{
        ALOGE("get mac address fail.");
    }
	for(int i = 0; i < 32; i ++)
	{
		if(mac[i] >= 'a' && mac[i] <= 'z')
		{
			mac[i] = mac[i] - 'a' + 'A';
		}
	}
    property_set(CMCC_MAC, mac);
}

void SystemService::get_kernel_cmdline_serialno(void)
{
	char cmdline[1024];
	char *ptr1, *ptr2, serialno[1024];
	int fd, len1, len2, ret;
	char *endptr;

	fd = open("/proc/cmdline", O_RDONLY);
	if (fd >= 0) {
		int n = read(fd, cmdline, 1023);
		if (n < 0) n = 0;
		/* get rid of trailing newline, it happens */
		if (n > 0 && cmdline[n-1] == '\n') n--;
		cmdline[n] = 0;
		close(fd);
	} else {
		cmdline[0] = 0;
	}

	ptr1 = strstr(cmdline, "specialstr=");
	ptr2 = strstr(cmdline, "mac_addr=");

	if (ptr1 == NULL) {
        ALOGD("get specialstr = %s, null, return! \n", ptr1);
		return;
	}
	/*
	if (ptr2 == NULL) {
        ALOGD("get mac_addr = %s, null, return! \n", ptr2);
		return;
	}
	*/
	ptr1 += strlen("specialstr=");
	ptr2 += strlen("mac_addr=");

	char *x = strchr(ptr1, ' ');
	len1 = x - ptr1;

	x = strchr(ptr2, ' ');
	len2 = x - ptr2;

	if(len1 < 3/* || len2 < 3*/)
	{
		ALOGD("length not right, len1 = %d, len2 = %d ,return! xxxx",len1, len2);
		return;
	}

	*(ptr1+len1) = 0;
	*(ptr2+len2) = 0;

	strcpy(serialno, ptr1);

	//mac to string,eg:12:12:12:12:12:12 -> 121212121212
    char *mac_buf_temp = ptr2;
    char *catStr;
    while ((catStr = strtok(mac_buf_temp, ":")) != NULL){
        //strcat(serialno, catStr);
        mac_buf_temp = NULL;
    }

    property_set(CMCC_SERIALNO, serialno);
	ALOGD("xx (%s)", serialno);
	char value[256];
	property_get(CMCC_SERIALNO, value, NULL);
    ALOGD("====================================== get serialno! serialno = %s \n", value);
}

}
