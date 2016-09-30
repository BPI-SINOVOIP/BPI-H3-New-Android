/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "init_disp.h"

#define EXIT_PROP_NAME "persist.sys.disp_init_exit"
#define SYSRSL_PROP_NAME "persist.sys.sysrsl"
#define RT_DISP_POLICY_PROP_NAME "persist.sys.disp_policy"
#define HDMI_HPD_PROP_NAME "persist.sys.hdmi_hpd"
#define HDMI_RVTHPD_PROP_NAME "persist.sys.hdmi_rvthpd"
#define CVBS_HPD_PROP_NAME "persist.sys.cvbs_hpd"
#define CVBS_RVTHPD_PROP_NAME "persist.sys.cvbs_rvthpd"
#define HDMI_WITHOUT_4K_PROP_NAME "persist.sys.hdmi_4k_ban"
#define HDMI_HPD_STATE_FILENAME "/sys/class/switch/hdmi/state"
#define CVBS_HPD_STATE_FILENAME "/sys/class/switch/cvbs/state"

struct DISP {
    int fd;
    int opened_id;
    disp_rect scn_win;
    //disp_rect src_win;

    void (*init_policy)(struct DISP *disp);
};

static int check_exit()
{
    char value[PROP_VALUE_MAX] = {0};
    property_get(EXIT_PROP_NAME, value);
    if(0 == value[0])
        return 0;
    return atoi(value);
}

static int get_disp_policy()
{
    char value[PROP_VALUE_MAX] = {0};
    property_get(RT_DISP_POLICY_PROP_NAME, value);
    ERROR("get_disp_policy: %s for modify configs.", value);
    if(0 == value[0])
        return 0;
    return atoi(value);
}

static unsigned char has_cvbs_hpd()
{
    FILE *fp;
    if(NULL == (fp = fopen(CVBS_HPD_STATE_FILENAME, "r"))) {
        ERROR("%s is not exist", CVBS_HPD_STATE_FILENAME);
        return 0; //cvbs has no hpd
    }
    fclose(fp);
    ERROR("%s is exist", CVBS_HPD_STATE_FILENAME);
    return 1;
}

static unsigned char is_low_ram()
{
    char buffer[1024];
    int memTotal = 0;
    int fd = open("/proc/meminfo", O_RDONLY);

    if (fd < 0) {
        ERROR("Unable to open /proc/meminfo. fd = %d",fd);
    } else {
        int len = read(fd, buffer, sizeof(buffer)-1);
        close(fd);
        char* p = buffer;
        if (len >0 && strncmp(p, "MemTotal:", 9) == 0) {
            p += 9;
            while (*p == ' ') p++;
            char* num = p;
            while (*p >= '0' && *p <= '9') p++;
            if (*p != 0) {
                *p = 0;
            }
            memTotal = atoll(num)/1024;
        }
    }
    if(memTotal <= 512) {
        ERROR("The device is low memory %d", memTotal);
        return 1;
    } else {
        ERROR("The device is not low memory %d", memTotal);
        return 0;
    }
}

int get_4k_capability(void)
{
	const char *infodev = "/dev/sunxi_soc_info";
	const unsigned int ban_table[] = {
		0x42,
		0x83,
	};
	const unsigned int permit_table[] = {
		0x00,
		0x81,
		0x58,
	};

	int dev;
	char buf[16];
	unsigned int chipid = 0;
	int prohibit = 0;
	unsigned int i;

	dev = open(infodev, O_RDONLY);
	if (dev < 0) {
		ERROR("can not open '%s'", infodev);
		return 1;
	}

	memset(buf, 0, sizeof(buf));
	if (ioctl(dev, 3, buf) < 0) {
		ERROR("%s: ioctl error", __func__);
		return 1;
	}

	INFO("soc info: %s", buf);
	sscanf(buf, "%08x", &chipid);
	chipid &= 0x000000ff;

	for (i = 0; i < sizeof(permit_table)/sizeof(permit_table[0]); i++) {
		INFO("chid: %02x, permit: %02x", chipid, permit_table[i]);
		if (permit_table[i] == chipid) {
			prohibit = 0;
			break;
		}
	}
	for (i = 0; i < sizeof(ban_table)/sizeof(ban_table[0]); i++) {
		INFO("chid: %02x, ban: %02x", chipid, ban_table[i]);
		if (ban_table[i] == chipid) {
			prohibit = 1;
			break;
		}
	}

	int capability = prohibit ? 0 : 1;
	return capability;
}

static int get_strings_from_file(char const * fileName, char *values, unsigned int num)
{
	FILE *fp;
	int i = 0;

	if(NULL ==(fp = fopen(fileName, "r")))	{
		//ERROR("init_disp: cannot open this file:%s\n", fileName);
		return -1;
	}
	while(!feof(fp) && (i < num - 1)) {
		values[i] = fgetc(fp);
		i++;
	}
	values[i] = '\0';
	fclose(fp);

	return i;
}

static unsigned int is_cvbs_hpd(int disp_id)
{
    char valueString[32] = {0};
    int state = 0;

    memset(valueString, 0, 32);
    if((get_strings_from_file(CVBS_HPD_STATE_FILENAME, valueString, 32) > 0)
      && strncmp(valueString, "0", 1))
        return 1;
    return 0;
}

static unsigned int is_hdmi_hpd(int disp_id)
{
    char valueString[32] = {0};
    int state = 0;

    memset(valueString, 0, 32);
    if((get_strings_from_file(HDMI_HPD_STATE_FILENAME, valueString, 32) > 0)
      && strncmp(valueString, "0", 1))
        return 1;
    return 0;
}

static int disp_reset_scn_win(disp_rect *win, int mode)
{// only support full screen window, meaning x/y of rect are both 0
    win->x = 0; win->y = 0;
    switch(mode) {
    case DISP_TV_MOD_PAL:
        win->width = 720; win->height = 576;
        break;
    case DISP_TV_MOD_NTSC:
        win->width = 720; win->height = 480;
        break;
    case DISP_TV_MOD_720P_50HZ:
    case DISP_TV_MOD_720P_60HZ:
        win->width = 1280; win->height = 720;
        break;
    case DISP_TV_MOD_1080P_50HZ:
    case DISP_TV_MOD_1080P_60HZ:
        win->width = 1920; win->height = 1080;
        break;
    default:
        win->width = 0; win->height = 0;
        ERROR("disp_reset_scn_win: not support the mode[%d]\n", mode);
    }
    return 0;
}

static int disp_get_rsl(int type, int id)
{
    char cmdline[1024];
    char *ptr;
    int fd;
    unsigned int datas;

    fd = open("/proc/cmdline", O_RDONLY);
    if(fd >= 0) {
        int n = read(fd, cmdline, 1023);
        if (n < 0) n = 0;
        /* get rid of trailing newline, it happens */
        if (n > 0 && cmdline[n-1] == '\n') n--;
        cmdline[n] = 0;
        close(fd);
    } else {
        cmdline[0] = 0;
    }

    ptr = strstr(cmdline, "init_disp=");
    if(ptr != NULL) {
        ptr += strlen("init_disp=");
        datas = (unsigned int)strtoul(ptr, NULL, 16);
        ERROR("init_disp=0x%x, type=%d, id=%d\n", datas, type, id);
        if(((type & 0xFF) << 8) == (0xFF00 & (datas >> (id << 4))))
            return 0xFF & (datas >> (id << 4));
    }
    return 0;
}

static inline int disp_valid_id(int disp_id)
{
    return (0 <= disp_id && HW_NUM_DISP > disp_id);
}

static int disp_device_open_show(struct DISP *disp, int disp_id, disp_output output)
{
    disp_rect scn_win;
    unsigned long args[4] = {0};

    args[0] = disp->opened_id;
    args[1] = (unsigned long)(&scn_win);
    _disp_get_scn_win(disp->fd, args);
    disp_reset_scn_win(&scn_win, output.mode);
    if(!check_exit()) {
        args[1] = 0;
        _disp_disable_layer(disp->fd, args);
        args[1] = 0;
        args[2] = 0;
        _disp_device_switch(disp->fd, args);
        args[0] = (unsigned long)disp_id;
        args[1] = output.type;
        args[2] = output.mode;
        _disp_device_switch(disp->fd, args);
        args[1] = (unsigned long)(&scn_win);
        _disp_set_scn_win(disp->fd, args);
        args[1] = 0;
        _disp_enable_layer(disp->fd, args);
    }

    return 0;
}

static int disp_show_logo(struct DISP *disp, int disp_id, disp_output output)
{
    disp_rect scn_win;
    unsigned long args[4] = {0};

    args[0] = disp->opened_id;
    args[1] = (unsigned long)(&scn_win);
    _disp_get_scn_win(disp->fd, args);

    disp_reset_scn_win(&scn_win, output.mode);

    args[0] = disp_id;
    args[1] = (unsigned long)(&scn_win);
    _disp_set_scn_win(disp->fd, args);
    return 0;
}

static int disp_device_switch(struct DISP *disp, int disp_id, disp_output output)
{
    unsigned long args[4] = {0};
    ERROR("disp=%d,type=%d,mode=%d\n", disp_id, output.type, output.mode);
    args[0] = disp_id;
    args[1] = output.type;
    args[2] = output.mode;
    _disp_device_switch(disp->fd, args);
    return 0;
}

static int disp_devices_check_open(struct DISP *disp)
{// opene the unopened device through check hpd, only support hdmi and cvbs
    disp_output output;
    unsigned int hdmi_hpd = is_hdmi_hpd(HDMI_CHANNEL);
    unsigned int cvbs_hpd = is_cvbs_hpd(CVBS_CHANNEL);

    if(HDMI_CHANNEL == disp->opened_id) {
        if(!disp_valid_id(CVBS_CHANNEL))
            return 1;
        if(!hdmi_hpd && cvbs_hpd) { //open cvbs
            output.type = DISP_OUTPUT_TYPE_TV;
            output.mode = disp_get_rsl(DISP_OUTPUT_TYPE_TV, CVBS_CHANNEL);
            if(0 == output.mode)
                output.mode = DISP_DEFAULT_CVBS_MODE;
            disp_device_open_show(disp, CVBS_CHANNEL, output);
            return 1;
        }
    }

    if(CVBS_CHANNEL == disp->opened_id) {
        if(!disp_valid_id(HDMI_CHANNEL))
            return 1;
        if(hdmi_hpd) { //open hdmi
            output.type = DISP_OUTPUT_TYPE_HDMI;
            output.mode = disp_get_rsl(DISP_OUTPUT_TYPE_HDMI, HDMI_CHANNEL);
            if(0 == output.mode)
                output.mode = DISP_DEFAULT_HDMI_MODE;
            disp_device_open_show(disp, HDMI_CHANNEL, output);
            return 1;
        }
    }

    return check_exit();
}

static void disp_devices_force_open(struct DISP *disp)
{// opene the unopened device, only support hdmi and cvbs
    disp_output output;

    if(disp_valid_id(HDMI_CHANNEL) && HDMI_CHANNEL != disp->opened_id) {
        output.type = DISP_OUTPUT_TYPE_HDMI;
        output.mode = disp_get_rsl(DISP_OUTPUT_TYPE_HDMI, HDMI_CHANNEL);
        if(0 == output.mode)
            output.mode = DISP_DEFAULT_HDMI_MODE;
        disp_device_switch(disp, HDMI_CHANNEL, output);
        disp_show_logo(disp, HDMI_CHANNEL, output);
    }

    if(disp_valid_id(CVBS_CHANNEL) && CVBS_CHANNEL != disp->opened_id) {
        output.type = DISP_OUTPUT_TYPE_TV;
        output.mode = disp_get_rsl(DISP_OUTPUT_TYPE_TV, CVBS_CHANNEL);
        if(0 == output.mode)
            output.mode = DISP_DEFAULT_CVBS_MODE;
        disp_device_switch(disp, CVBS_CHANNEL, output);
        disp_show_logo(disp, CVBS_CHANNEL, output);
    }
}

static int disp_find_device(struct DISP *disp)
{//find only one opened device
    disp_output output;
    int i;
    unsigned long args[4] = {0};

    for(i = 0; i < HW_NUM_DISP; i++) {
        args[0] = (unsigned long)i;
        args[1] = (unsigned long)&output;
        _disp_get_output(disp->fd, args);
        switch(output.type) {
        case DISP_OUTPUT_TYPE_HDMI:
            if(HDMI_CHANNEL == i) {
                disp->opened_id = i;
                return 0;
            }
            ERROR("disp_find_device: HDMI_CHANNEL[%d] != %d\n", HDMI_CHANNEL, i);
            break;
        case DISP_OUTPUT_TYPE_TV:
            if(CVBS_CHANNEL == i) {
                disp->opened_id = i;
                return 0;
            }
            ERROR("disp_find_device: CVBS_CHANNEL[%d] != %d\n", CVBS_CHANNEL, i);
            break;
        case DISP_OUTPUT_TYPE_VGA:
        case DISP_OUTPUT_TYPE_LCD:
            ERROR("disp_find_device: no support this type = %d\n", output.type);
            break;
        default: ;
        }
    }
    return -1;
}

static void none_display_policy(struct DISP * disp) {return;}

static void single_display_policy(struct DISP *disp)
{
    if(0 <= disp_find_device(disp)) {
        property_set(EXIT_PROP_NAME, "0");
        while(!disp_devices_check_open(disp)) {
            usleep(100000);
        }
    }
    ERROR("single_display_policy exit@@@@@@");
}

static void dual_display_policy(struct DISP *disp)
{
    if(0 <= disp_find_device(disp))
        disp_devices_force_open(disp);
}

static void adapt_display_policy(struct DISP *disp)
{// this function for auto adapt of self-define
    unsigned char cvbs_hpd_exist = has_cvbs_hpd(); // 0: cvbs has no hpd
    unsigned char dram_size_limit = is_low_ram(); // 1:512M;  0:>=1G
    if(dram_size_limit || !cvbs_hpd_exist) {
        single_display_policy(disp);
    } else {
        dual_display_policy(disp);
    }
}

static int disp_open(struct DISP *disp)
{
    disp->fd = open("/dev/disp", O_RDWR);
    if (disp->fd < 0)
        return -1;
    // choose policy according to DISPLAY_INIT_POLICY which is configed in device/softwinner/xxx/xxx.mk
    switch(DISP_POLICY) {
    case DISP_SINGLE_POLICY:
        disp->init_policy = single_display_policy;
        break;
    case DISP_DUAL_POLICY:
        disp->init_policy = dual_display_policy;
        break;
    case DISP_ADAPT_POLICY:
        disp->init_policy = adapt_display_policy;
        break;
    default:
        disp->init_policy = none_display_policy;
    }
    return 0;
}

static void disp_close(struct DISP *disp)
{
    close(disp->fd);
}

int init_initdisplay()
{
    int child = fork();
    //parent process
    if(child > 0 || child < 0)
        return child > 0 ? 0 : -1;

    //child process
    struct DISP disp;
    if(disp_open(&disp))
        exit(0);
    disp.init_policy(&disp);
    disp_close(&disp);
    exit(0);
}

int init_set_disp_policy(int nargs, char **args)
{
    if((DISP_ADAPT_POLICY == get_disp_policy())
      && (DISP_ADAPT_POLICY == DISP_POLICY)) {
		unsigned char cvbs_hpd_exist = has_cvbs_hpd(); // 0: cvbs has no hpd
		unsigned char dram_size_limit = is_low_ram(); // 1:512M;  0:>=1G
		unsigned char capability_of_4k = get_4k_capability();

		if (!capability_of_4k)
			property_set(HDMI_WITHOUT_4K_PROP_NAME, "1");

        if(dram_size_limit || !cvbs_hpd_exist || !capability_of_4k) {
            property_set(RT_DISP_POLICY_PROP_NAME, "1");
            if(!cvbs_hpd_exist) {
                property_set(CVBS_HPD_PROP_NAME, "0");
                property_set(HDMI_RVTHPD_PROP_NAME, "2");
            }
        } else {
            property_set(RT_DISP_POLICY_PROP_NAME, "2");
        }

	} else if (DISP_DUAL_POLICY == DISP_POLICY) {
		unsigned char dram_size_limit = is_low_ram(); // 1:512M;  0:>=1G
		unsigned char capability_of_4k = get_4k_capability();

		if (!capability_of_4k)
			property_set(HDMI_WITHOUT_4K_PROP_NAME, "1");

		if (dram_size_limit || !capability_of_4k) {
			ERROR("display policy: force to single policy for low memory device\n");
			property_set(RT_DISP_POLICY_PROP_NAME, "1");
			property_set(CVBS_HPD_PROP_NAME, "0");
			property_set(HDMI_RVTHPD_PROP_NAME, "2");
		} else {
			ERROR("display policy: force to dual policy\n");
			property_set(RT_DISP_POLICY_PROP_NAME, "2");
			property_set(CVBS_HPD_PROP_NAME, "0");
			property_set(HDMI_RVTHPD_PROP_NAME, "0");
		}
	}

    return 0;
}

