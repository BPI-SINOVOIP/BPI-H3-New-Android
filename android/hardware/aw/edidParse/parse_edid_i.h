#ifndef _PARSE_EDID_I_H_
#define _PARSE_EDID_I_H_

#include <cutils/log.h>

#define HDMI1440_480I       6
#define HDMI1440_576I       21
#define HDMI480P            2
#define HDMI576P            17
#define HDMI720P_50         19
#define HDMI720P_60         4
#define HDMI1080I_50        20
#define HDMI1080I_60        5
#define HDMI1080P_50        31
#define HDMI1080P_60        16
#define HDMI1080P_24        32
#define HDMI1080P_25        33
#define HDMI1080P_30        34

#define HDMI3840_2160P_30   1
#define HDMI3840_2160P_25   2
#define HDMI3840_2160P_24   3
#define HDMI3840_2160P_24_SMPTE   4

typedef enum
{
    SUNXI_TV_MOD_480I                = 0,
    SUNXI_TV_MOD_576I                = 1,
    SUNXI_TV_MOD_480P                = 2,
    SUNXI_TV_MOD_576P                = 3,
    SUNXI_TV_MOD_720P_50HZ           = 4,
    SUNXI_TV_MOD_720P_60HZ           = 5,
    SUNXI_TV_MOD_1080I_50HZ          = 6,
    SUNXI_TV_MOD_1080I_60HZ          = 7,
    SUNXI_TV_MOD_1080P_24HZ          = 8,
    SUNXI_TV_MOD_1080P_50HZ          = 9,
    SUNXI_TV_MOD_1080P_60HZ          = 0xa,
    SUNXI_TV_MOD_1080P_24HZ_3D_FP    = 0x17,
    SUNXI_TV_MOD_720P_50HZ_3D_FP     = 0x18,
    SUNXI_TV_MOD_720P_60HZ_3D_FP     = 0x19,
    SUNXI_TV_MOD_1080P_25HZ          = 0x1a,
    SUNXI_TV_MOD_1080P_30HZ          = 0x1b,
    SUNXI_TV_MOD_PAL                 = 0xb,
    SUNXI_TV_MOD_PAL_SVIDEO          = 0xc,
    SUNXI_TV_MOD_NTSC                = 0xe,
    SUNXI_TV_MOD_NTSC_SVIDEO         = 0xf,
    SUNXI_TV_MOD_PAL_M               = 0x11,
    SUNXI_TV_MOD_PAL_M_SVIDEO        = 0x12,
    SUNXI_TV_MOD_PAL_NC              = 0x14,
    SUNXI_TV_MOD_PAL_NC_SVIDEO       = 0x15,
    SUNXI_TV_MOD_3840_2160P_30HZ     = 0x1c,
    SUNXI_TV_MOD_3840_2160P_25HZ     = 0x1d,
    SUNXI_TV_MOD_3840_2160P_24HZ     = 0x1e,
    SUNXI_TV_MODE_NUM                = 0x1f,
}sunxi_tv_mode; // definition in sunxi_disp_driver, as same as disp_tv_mode

typedef struct vendor_info {
    char manufacturer[4];
    unsigned int model;
    unsigned int SN;
    unsigned int week;
    unsigned int year;
    int id;
}vendor_info_t;

typedef struct hdmi_mode_info {
    unsigned long long common_mode; //include native mode
    unsigned long long standard_mode;
    unsigned int native_mode;
    unsigned int hd_2160P_mode;
    unsigned int stereo_present;
}hdmi_mode_info_t;

typedef struct cec_info {
    unsigned int phy_addr;
}cec_info_t;

enum {
    EDID_GET_FINISH = 0,
    EDID_GET_VERDOR_INFO, //see vendor_info_t
    EDID_GET_HDMI_MODE, //see hdmi_mode_info_t
    EDID_GET_CEC_INFO, //see cec_info_t

    EDID_GET_CMD_NUM,
};

typedef struct edid_info {
    int cmd;
    union {
        int length;
        int ret;
    };
    void *data;
}edid_info_t;

#endif
