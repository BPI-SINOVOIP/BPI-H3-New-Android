#ifndef __HWCOMPOSER_PRIV_H__
#define __HWCOMPOSER_PRIV_H__

#include <hardware/hardware.h>
#include <hardware/hwcomposer.h>

#include <hardware/hal_public.h>
#include "drv_display.h"
#include <fb.h>
#include "g2d_driver.h"

#include <fcntl.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <stdio.h>
#include <cutils/log.h>
#include <cutils/atomic.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include <poll.h>
#include <cutils/properties.h>
#include <hardware_legacy/uevent.h>
#include <sys/resource.h>
#include <EGL/egl.h>
#include <linux/ion.h>
#include <ion/ion.h>
#include <sys/ioctl.h>
#include <parse_edid_sunxi.h>

#define NUMBEROFDISPLAY  3
#define NUMBEROFPIPE     2
#define ALLDISPLAY       255
#define DISPLAY_MAX_LAYER_NUM 4



#define AW_DIS_00    0
#define AW_DIS_01   1
#define AW_DIS_02   2

#define HDMI_USED   1
#define CVBS_USED   0
#define LCD_USED    0

#define INVALID_VALUE    -1

#define HAL_PIXEL_FORMAT_AW_NV12    0x101
#define HAL_PIXEL_FORMAT_BGRX_8888  0x1ff
#define ION_IOC_SUNXI_PHYS_ADDR     7
#define ALIGN(x,a)	(((x) + (a) - 1L) & ~((a) - 1L))
#define HW_ALIGN	32
#define YV12_ALIGN 16
typedef enum
{
	ASSIGN_OK=0,
	ASSIGN_FAILED=1,
	ASSIGN_NOHWCLAYER=2,
	ASSIGN_FB_PIPE=4,
	ASSIGN_NO_DISP=8

}HwcPipeAssignStatusType;

enum
{
	FPS_SHOW = 1,
	LAYER_DUMP = 2,
	SHOW_ALL = 3
};

typedef struct head_list
{
    struct head_list* pre;
    struct head_list* next;
}head_list_t;

typedef struct layer_list
{
    head_list_t         head;
    hwc_layer_1_t*      pslayer;
    int                 order;
    int                 pipe;
    bool                usedfe;
}layer_list_t;

typedef struct
{
    int layer_num[3];
    disp_layer_info layer_info[3][4];
    void* hConfigData;
}setup_dispc_data_t;


typedef struct
{
    int                 virtualToHWDisplay;
    bool                vsyncEnable;
    bool                displayPlugin;
	bool                DisplayEnable;

    unsigned char       hwLayerNum;
    unsigned char       hwPipeNum;

	unsigned int    	varDisplayWidth;
	unsigned int    	varDisplayHeight;

    unsigned int        displayDPI_X;
    unsigned int        displayDPI_Y;
    unsigned int        displayVsyncP;
    unsigned char       displayPercentWT;
    unsigned char       displayPercentHT;
    unsigned char       displayPercentW;
    unsigned char       displayPercentH;
    int                 displayType;
    int                 displayMode;
    __display_3d_mode   current3DMode;
    int                 screenRadio;
}DisplayInfo;

typedef struct
{
    /*
    usually:  display 1: LCD
              display 2:HDMI   fixed
              display 3: mabe HDMI or other
              We assume that all display could hot_plug,but there is only one PrimaryDisplay,0 is the PrimaryDisplay.
    */
	hwc_procs_t	const*  psHwcProcs;
	pthread_t           sVsyncThread;
    int                 displayFd;
    int                 fbFd;
    int                 ionFd;
    int                 g2dFd;

    unsigned int        hwcFrameCount;

    bool                canForceGPUCom;
    bool                forceGPUComp;
    bool                detectError;
    bool                layer0usfe;
    char                hwcdebug;
    unsigned char       gloFEisUsedCnt;
    unsigned int        timeStamp;
    unsigned int	    uiPrivateDataSize;
    bool                bDisplayReady;
    DisplayInfo         sunxiDisplay[NUMBEROFDISPLAY];
    head_list_t         hwcLayerHead[NUMBEROFDISPLAY];

	unsigned int		uiBeginFrame;
	double				fBeginTime;

    setup_dispc_data_t* pvPrivateData;

    int                 mainDisp;
	int                 secDisp;
	int                 mainDispMode;
	int                 secDispMode;
	unsigned int        mainDispWidth;
	unsigned int        mainDispHeight;
	unsigned int        secDispWidth;
	unsigned int        secDispHeight;

	float               fe_clk;
	unsigned int        de_fps;
}SUNXI_hwcdev_context_t;

typedef struct{
    unsigned char       feIsUsedCnt;
    unsigned char       hwLayerCnt;
    unsigned int        usedFB;
    unsigned char       hwPipeUsedCnt;
    head_list_t         fbLayerHead;
    hwc_rect_t          pipeRegion[NUMBEROFPIPE];
}hwcDevCntContext_t;

typedef struct
{
    int type;// bit3:cvbs, bit2:ypbpr, bit1:vga, bit0:hdmi
    disp_tv_mode mode;
    int width;
    int height;
	int refreshRate;
}tv_para_t;

typedef enum
{
    WIDTH=2,
    HEIGHT,
    REFRESHRAE,

}ModeInfo;

typedef
 enum{
    FIND_HWDISPNUM=0,
    FIND_HWTYPE,
    NULL_DISPLAY,
    SET_DISP,
    FREE_DISP,

}ManageDisp;


typedef struct {
    void *handle;
    unsigned int phys_addr;
    unsigned int size;
}SunxiPhysData;

extern SUNXI_hwcdev_context_t gSunxiHwcDevice;

extern int hwcdev_reset_device(SUNXI_hwcdev_context_t *psDevice, size_t disp);
extern HwcPipeAssignStatusType hwcTrytoAssignLayer(hwcDevCntContext_t *ctx, hwc_layer_1_t *psLayer,size_t disp,int zOrder);
extern SUNXI_hwcdev_context_t* hwcCreateDevice(void);
extern int _hwcdev_layer_config_3d(int disp, disp_layer_info *layer_info, bool isVideoFormat);
extern disp_tv_mode getSuitableTvMode(int select, disp_tv_mode mode);
extern int  getWidthFromMode(int mode);
extern int  getHeightFromMode(int mode);
extern void *vsyncThreadWrapper(void *priv);
extern int hwcSetupLayer(SUNXI_hwcdev_context_t *ctx, hwc_layer_1_t *layer,int zOrder, size_t disp,int pipe);
extern int hwcResetDispData(SUNXI_hwcdev_context_t *ctx);
extern int _hwc_device_set_3d_mode(int disp, __display_3d_mode mode);
extern int _hwc_device_set_backlight_mode(int disp, int mode);
extern int _hwc_device_set_backlight_demomode(int disp, int mode);
extern int _hwc_device_set_enhancemode(int disp, int mode);
extern int _hwc_device_set_enhancedemomode(int disp, int mode);
extern int hwcTwoRegionIntersect(hwc_rect_t *rect0, hwc_rect_t *rect1);
extern int hwcDestroyDevice(SUNXI_hwcdev_context_t *psDevice);
int getValueFromProperty(char const* propName);
int isDisplayP2P(void);
int getTvMode4SysResolution(void);
int getDispPolicy(void);
int getDispModeFromFile(int type);
int saveDispModeToFile(int type, int mode);
int getDispMarginFromFile(unsigned char * percentWidth,unsigned char * percentHeight);
int getInfoOfMode(int mode,ModeInfo info);
float getFeClk(int mode);
unsigned int ionGetAddr(int fd);
int hwcOutputSwitch(int hwDisp, int type, int mode);
extern int resetDispMode(int disp, int type,int mode);
DisplayInfo* manageDisplay(DisplayInfo *hwDisplayInfo, int dispInfo,ManageDisp mode);
int _hwc_device_set_output_mode(int disp, int out_type, int out_mode);
int initAddLayerTail(head_list_t* layerHead,hwc_layer_1_t *psLayer, int Order,int pipe,bool feused);
int _hwc_device_set_saturation(int disp,int saturation);
int _hwc_device_set_hue(int disp,int hue);
int _hwc_device_set_bright(int disp,int bright);
int _hwc_device_set_contrast(int disp,int contrast);
int _hwc_device_set_margin(int disp,int hpercent,int vpercent);
int _hwc_device_is_support_hdmi_mode(int disp,int mode);
int _hwc_device_get_output_type(int disp);
int _hwc_device_get_output_mode(int disp);
int _hwc_device_get_saturation(int disp);
int _hwc_device_get_hue(int disp);
int _hwc_device_get_bright(int disp);
int _hwc_device_get_contrast(int disp);
int _hwc_device_get_margin_w(int disp);
int _hwc_device_get_margin_h(int disp);
int _hwc_device_set_screenradio(int disp, int screen_radio);
int _hwc_device_convert_mb_to_nv21(hwc_layer_1_t *layer, unsigned int dst_phy_addr);

#endif
