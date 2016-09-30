/*************************************************************************/ /*!
@Copyright      Copyright (c) Imagination Technologies Ltd. All Rights Reserved
@License        Strictly Confidential.
*/ /**************************************************************************/

#include "hwc.h"

#include <sys/resource.h>
#include <sys/time.h>

#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <time.h>
#define LOG_NDEBUG 0
#define DISP_P2P_PROPERTY  "persist.sys.hwc_p2p"
#define SYSTEM_RSL_PROPERTY "ro.hwc.sysrsl"
#define DISPLAY_RSL_FILENAME "/mnt/Reserve0/disp_rsl.fex"
#define DISPLAY_MARGIN_FILENAME "/mnt/Reserve0/disp_margin.fex"
#define HDMI_HPD_STATE_FILENAME "/sys/class/switch/hdmi/state"
#define RT_DISP_POLICY_PROP_NAME "persist.sys.disp_policy"
#define VENDOR_ID_FILENAME "/mnt/Reserve0/tv_vdid.fex"

static int repaint_request = 0;

static tv_para_t g_tv_para[]=
{
    {8, DISP_TV_MOD_NTSC,             720,    480, 60},
    {8, DISP_TV_MOD_PAL,              720,    576, 50},

    {5, DISP_TV_MOD_480I,             720,    480, 60},
    {5, DISP_TV_MOD_576I,             720,    576, 60},
    {5, DISP_TV_MOD_480P,             720,    480, 60},
    {5, DISP_TV_MOD_576P,             720,    576, 50},
    {5, DISP_TV_MOD_720P_50HZ,        1280,   720, 50},
    {5, DISP_TV_MOD_720P_60HZ,        1280,   720, 60},
    {5, DISP_TV_MOD_1080I_50HZ,       1920,   1080, 50},
    {5, DISP_TV_MOD_1080I_60HZ,       1920,   1080, 60},
    {1, DISP_TV_MOD_1080P_24HZ,       1920,   1080, 24},
    {5, DISP_TV_MOD_1080P_50HZ,       1920,   1080, 50},
    {5, DISP_TV_MOD_1080P_60HZ,       1920,   1080, 60},



	{5, DISP_TV_MOD_3840_2160P_25HZ,  3840,   2160, 25},
	{5, DISP_TV_MOD_3840_2160P_24HZ,  3840,   2160, 24},
    {5, DISP_TV_MOD_3840_2160P_30HZ,  3840,   2160, 30},

    {1, DISP_TV_MOD_1080P_24HZ_3D_FP, 1920,   1080, 24},
    {1, DISP_TV_MOD_720P_50HZ_3D_FP,  1280,   720, 50},
    {1, DISP_TV_MOD_720P_60HZ_3D_FP,  1280,   720, 60},

};
int getValueFromProperty(char const* propName)
{
    char property[PROPERTY_VALUE_MAX];
    int value = -1;
    if (property_get(propName, property, NULL) > 0)
    {
        value = atoi(property);
    }
    ALOGD("###%s: propName:%s,value=%d", __func__, propName, value);
    return value;
}

//check whether we config that display point to point
//-->Size of DisplayMode == Size of System Resolution
int isDisplayP2P(void)
{
    if(1 != getValueFromProperty(DISP_P2P_PROPERTY))
    {
        return 0; // not support display point2p
    }
    else
    {
        return 1;
    }
}

//get the tv mode for system resolution
//the system resolution desides the Buffer Size of the App.
int getTvMode4SysResolution(void)
{
    int tvmode = getValueFromProperty(SYSTEM_RSL_PROPERTY);
    switch(tvmode)
    {
        case DISP_TV_MOD_PAL:
            break;
        case DISP_TV_MOD_NTSC:
            break;
        case DISP_TV_MOD_720P_50HZ:
        case DISP_TV_MOD_720P_60HZ:
            break;
        case DISP_TV_MOD_1080I_50HZ:
        case DISP_TV_MOD_1080I_60HZ:
        case DISP_TV_MOD_1080P_24HZ:
        case DISP_TV_MOD_1080P_50HZ:
        case DISP_TV_MOD_1080P_60HZ:
            break;
        case DISP_TV_MOD_3840_2160P_24HZ:
        case DISP_TV_MOD_3840_2160P_25HZ:
        case DISP_TV_MOD_3840_2160P_30HZ:
            break;
        default:
            tvmode = DISP_TV_MOD_1080P_60HZ;
            break;
    }
    return tvmode;
}

int getStringsFromFile(char const * fileName, char *values, unsigned int num)
{
	FILE *fp;
	int i = 0;

	if(NULL ==(fp = fopen(fileName, "r")))
	{
		ALOGW("cannot open this file:%s\n", fileName);
		return -1;
	}
	while(!feof(fp) && (i < num - 1))
	{
		values[i] = fgetc(fp);
		i++;
	}
	values[i] = '\0';
	fclose(fp);

	return i;
}

#define ARRAYLENGTH 32
int getDispModeFromFile(int type)
{
	char valueString[ARRAYLENGTH] = {0};
	char datas[ARRAYLENGTH] = {0};
	int i = 0;
	int j = 0;
	int data = 0;

	memset(valueString, 0, ARRAYLENGTH);
	if(getStringsFromFile(DISPLAY_RSL_FILENAME, valueString, ARRAYLENGTH) == -1)
	{
		return -1;
	}
	for(i = 0; valueString[i] != '\0'; i++)
	{
		if('\n' == valueString[i])
		{
			datas[j] = '\0';
			//ALOGD("datas = %s\n", datas);
			data = (int)strtoul(datas, NULL, 16);
			if(type == ((data >> 8) & 0xFF))
			{
				return (data & 0xFF);
			}
			j = 0;
		}
		else
		{
			datas[j++] = valueString[i];
		}
	}
	return -1;
}

int saveDispModeToFile(int type,int mode)
{
    char valueString[ARRAYLENGTH] = {0};
    char *pValue, *pt, *ptEnd;
    int index = 0;
    int i = 0;
    int len = 0;
    int ret = 0;
    int format = ((type & 0xFF) << 8) | (mode & 0xFF);
    int values[3] = {0, 0, 0};
    FILE *fp = NULL;

    switch(type) {
    case DISP_OUTPUT_TYPE_HDMI:
        index = 1;
        break;
    case DISP_OUTPUT_TYPE_TV:
        index = 0;
        break;
    case DISP_OUTPUT_TYPE_VGA:
        index = 2;
        break;
    default:
        return 0;
    }
    len = getStringsFromFile(DISPLAY_RSL_FILENAME, valueString, ARRAYLENGTH);
    if(0 < len) {
        pValue = valueString;
        pt = valueString;
        ptEnd = valueString + len;
        for(;(i < 3) && (pt != ptEnd); pt++) {
            if('\n' == *pt) {
                *pt = '\0';
                values[i] = (int)strtoul(pValue, NULL, 16);
                ALOGD("pValue=%s, values[%d]=0x%x",
                    pValue, i, values[i]);
                pValue = pt + 1;
                i++;
            }
        }
    }
    if(NULL == (fp = fopen(DISPLAY_RSL_FILENAME, "w"))) {
        ALOGW("open this file:%s  for writing failed\n", DISPLAY_RSL_FILENAME);
        return -1;
    }
    values[index] = format;
    sprintf(valueString, "%x\n%x\n%x\n", values[0], values[1], values[2]);
    len = strlen(valueString);
    ret = fwrite(valueString, len, 1, fp);
    //ALOGD("saveDispModeToFile:valueString=%s,len=%d,ret=%d",
    //    valueString, len, ret);
    fflush(fp);
    fsync(fileno(fp));
    fclose(fp);
    return 0;
}

int getDispMarginFromFile(unsigned char *percentWidth, unsigned char *percentHeight)
{
    char valueString[ARRAYLENGTH] = {0};
    char datas[ARRAYLENGTH] = {0};
    int i = 0;
    int j = 0;
    int num = 0;
    int data[4] = {0};

    memset(valueString, 0, ARRAYLENGTH);
    if(getStringsFromFile(DISPLAY_MARGIN_FILENAME, valueString, ARRAYLENGTH) == -1)
    {
        return -1;
    }
    for(i = 0; valueString[i] != '\0' && 4 > num; i++)
    {
        if('\n' == valueString[i])
        {
            datas[j] = '\0';
            //ALOGD("datas = %s\n", datas);
            data[num] = (int)strtoul(datas, NULL, 16);
            num++;
            j = 0;
        }
        else
        {
            datas[j++] = valueString[i];
        }
    }
    if(2 > num)
    {
        ALOGD("need 2 parameters only. num = %d.", num);
        return -1;
    }
    *percentWidth = (unsigned char)data[0];
    *percentHeight = (unsigned char)data[1];
    return 0;
}

unsigned int isHdmiHpd(int disp)
{
    char valueString[ARRAYLENGTH] = {0};
    int state = 0;

    memset(valueString, 0, ARRAYLENGTH);
    if(getStringsFromFile(HDMI_HPD_STATE_FILENAME, valueString, ARRAYLENGTH) == -1)
    {
        return 0;
    }
    if(!strncmp(valueString, "0", 1))
    {
        return 0;
    }
    else
    {
        return 1;
    }
}

int getSavedHdmiVendorID(int disp)
{
    char valueString[ARRAYLENGTH] = {0};
    char *pVendorId, *pValueString, *pValueStringEnd;
    int vendorID = 0;
    int i = 0;
    int ret = 0;

    memset(valueString, 0, ARRAYLENGTH);
    ret = getStringsFromFile(VENDOR_ID_FILENAME, valueString, ARRAYLENGTH);
    if(0 >= ret)
        return 0;
    pVendorId = valueString;
    pValueString = valueString;
    pValueStringEnd = pValueString + ret;
    for(;(i < 4) && (pValueString != pValueStringEnd); pValueString++) {
        if('\n' == *pValueString) {
            *pValueString = '\0';
            ret = (int)strtoul(pVendorId, NULL, 16);
            vendorID |= ((ret & 0xFF) << (8 * (3 - i)));
            //ALOGD("pVendorId=%s, ret=0x%x, vendorID=0x%x",
            //    pVendorId, ret, vendorID);
            pVendorId = pValueString + 1;
            i++;
        }
    }
    return vendorID;
}

int savedHdmiVendorID(int disp, int vendorId)
{
    char value[4] = {0};
    char valueString[ARRAYLENGTH] = {0};
    FILE *fp = NULL;
    int len = 0;
    int ret = 0;

    value[0] = (vendorId >> 24) & 0xFF;
    value[1] = (vendorId >> 16) & 0xFF;
    value[2] = (vendorId >> 8) & 0xFF;
    value[3] = vendorId & 0xFF;
    sprintf(valueString, "%x\n%x\n%x\n%x\n",
        value[0], value[1], value[2], value[3]);
    if(NULL == (fp = fopen(VENDOR_ID_FILENAME, "w"))) {
        ALOGW("open this file:%s  for writing failed\n", VENDOR_ID_FILENAME);
        return -1;
    }
    len = strlen(valueString);
    ret = fwrite(valueString, len, 1, fp);
    //ALOGD("savedHdmiVendorID:valueString=%s,len=%d,ret=%d",
    //    valueString, len, ret);
    fflush(fp);
    fsync(fileno(fp));
    fclose(fp);
    return 0;
}
#undef ARRAYLENGTH

int getInfoOfMode(int mode,ModeInfo info)
{
    unsigned int i = 0;

    for(i=0; i<sizeof(g_tv_para)/sizeof(tv_para_t); i++)
    {
        if(g_tv_para[i].mode == mode)
        {
            return *(((int *)(g_tv_para+i))+info);
        }
    }
    return -1;
}

//get the available tv mode of disp[select]
//when the current mode {@link gSunxiHwcDevice->mode} is avalaible, we just
//return it, otherwise we return a default mode.
disp_tv_mode getSuitableTvMode(int select, disp_tv_mode mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    //if gSunxiHwcDevice->mode is available, we just return it;
    if(mode >= DISP_TV_MOD_480I && mode < DISP_TV_MODE_NUM)
    {
        return  mode;
	}
    //otherwise we return the default mode based on the disp[select].
    switch(select)
    {
    case CVBS_USED:
        return DISP_TV_MOD_PAL;
    case HDMI_USED:
        unsigned long arg[4]={0};
        arg[0] = select;
        arg[1] = DISP_TV_MOD_1080P_60HZ;
        int ret = ioctl(Globctx->displayFd, DISP_CMD_HDMI_SUPPORT_MODE, arg);
        if(ret > 0)
        {
            return DISP_TV_MOD_1080P_60HZ;
        }
        else
        {
            return DISP_TV_MOD_720P_60HZ;
        }
    }
    return DISP_TV_MOD_720P_60HZ;
}

float getFeClk(int mode)
{
    switch(mode)
    {
    case DISP_TV_MOD_3840_2160P_30HZ:
    case DISP_TV_MOD_3840_2160P_25HZ:
    case DISP_TV_MOD_3840_2160P_24HZ:
        return 396000000;
    default:
        return 297000000;
    }
}

DisplayInfo* manageDisplay(DisplayInfo *HWDisplayInfo, int dispInfo,ManageDisp mode)
{

    DisplayInfo* psDisplayInfo = NULL;
    DisplayInfo* tmpDisplayInfo =NULL;
    int disp;
    for(disp = 0; disp < NUMBEROFDISPLAY; disp++)
    {
        psDisplayInfo = HWDisplayInfo++;
        switch(mode)
        {
            //return the info of display based on display num.
            case FIND_HWDISPNUM:
                if(psDisplayInfo->virtualToHWDisplay == dispInfo)
                {
                    return psDisplayInfo;
                }
                break;
            //return the info of display based on display type.
            case FIND_HWTYPE:
                if(psDisplayInfo->virtualToHWDisplay != -1 && psDisplayInfo->displayType == dispInfo)
                {
                    return psDisplayInfo;
                }
                break;
            //check whether all displays are unconnected.
            //return NULL if true, otherwise return not NULL when anyone is connected.
            case NULL_DISPLAY:
                if(psDisplayInfo->virtualToHWDisplay != -1)
                {
                    return psDisplayInfo;
                }
                break;
            //set display info
            //if the dest display device is already connected, just return it's display info
            //otherwise return a new empty Data Struct.
            case SET_DISP:
                if(psDisplayInfo->virtualToHWDisplay == dispInfo)
                {
                    return psDisplayInfo;
                }

                if(psDisplayInfo->virtualToHWDisplay == -1 && tmpDisplayInfo == NULL)
                {
                    tmpDisplayInfo = psDisplayInfo;
                }
                if(disp == NUMBEROFDISPLAY-1)
                {
                    tmpDisplayInfo->virtualToHWDisplay = dispInfo;
                    return tmpDisplayInfo;
                }
                break;
            //Free display device.
            case FREE_DISP:
                if(psDisplayInfo->virtualToHWDisplay == dispInfo)
                {
                    psDisplayInfo->virtualToHWDisplay = -1;
                    return psDisplayInfo;
                }
                break;

            default:
                ALOGD("Error  usage in ManageDisplay");
        }
    };
    return NULL;

}

int resetDispMode(int disp, int type, int mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;

    if((DISP_OUTPUT_TYPE_HDMI == type) && (0xFF == mode)) {
        vendor_info_t vendorInfo;
        resetParseEdidForDisp(HDMI_USED);
        int savedVendorID = getSavedHdmiVendorID(disp);
        getVendorInfo(HDMI_USED, &vendorInfo);
        mode = getDispModeFromFile(DISP_OUTPUT_TYPE_HDMI);
        mode = (-1 == mode) ? DISP_DEFAULT_HDMI_MODE : mode;
        ALOGD("vendorInfo.id=0x%x, savedVendorID=0x%x",
            vendorInfo.id, savedVendorID);
        if(vendorInfo.id && (vendorInfo.id != savedVendorID)) {
            // fixme: savedVendorID is 0 at first, that can cause to run here.
            if(!isHdmiModeSupport(HDMI_USED, mode)) {
                int bestmode = getBestHdmiMode(HDMI_USED);
                ALOGD("bestmode=%d", bestmode);
                mode = (-1 != bestmode) ? bestmode : mode;
            }
            savedHdmiVendorID(disp, vendorInfo.id);
        }
    }
    if((DISP_OUTPUT_TYPE_TV == type) && (0xFF == mode)) {
        mode = getDispModeFromFile(DISP_OUTPUT_TYPE_TV);
        mode = (-1 == mode) ? DISP_DEFAULT_CVBS_MODE : mode;
    }
    return mode;
}

/** switch the output of the display device
* @param hwDisp     the hardware disp channel, eg. 0, 1, 2
* @param type       disp type:hdmi, cvbs, lcd, vga ...
* @param mode       disp mode
* @return 1:operate success; 0:operate fail.
*/
int hwcOutputSwitch(int hwDisp, int type, int mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long arg[4] = {0};
    DisplayInfo   *psDisplayInfo = NULL;
    psDisplayInfo = manageDisplay(Globctx->sunxiDisplay, hwDisp, SET_DISP);
    ALOGD("###hwcOutputSwitch hwDisp=%d, type=%d, mode=%d", hwDisp, type, mode);
    switch(type)
    {
    case DISP_OUTPUT_TYPE_HDMI:
        ALOGD("switch to HDMI[0x%x] begin!", mode);
        arg[0] = hwDisp;
        ioctl(Globctx->displayFd, DISP_CMD_HDMI_DISABLE, (unsigned long)arg);
        usleep(300 * 1000);
        if(Globctx->mainDisp == HDMI_USED){
            Globctx->fe_clk = getFeClk(mode);
            Globctx->de_fps = getInfoOfMode(mode, REFRESHRAE);
        }
        psDisplayInfo->virtualToHWDisplay = hwDisp;
        psDisplayInfo->varDisplayWidth = getInfoOfMode(mode,WIDTH);
        psDisplayInfo->varDisplayHeight = getInfoOfMode(mode,HEIGHT);
        psDisplayInfo->displayType = DISP_OUTPUT_TYPE_HDMI;
        psDisplayInfo->displayMode = mode;
        psDisplayInfo->displayDPI_X = 213000;
        psDisplayInfo->displayDPI_Y = 213000;
        psDisplayInfo->displayVsyncP = 1000000000/getInfoOfMode(mode, REFRESHRAE);
        psDisplayInfo->hwLayerNum = DISPLAY_MAX_LAYER_NUM;
        psDisplayInfo->hwPipeNum = NUMBEROFPIPE;

        arg[1] = mode;
        ioctl(Globctx->displayFd, DISP_CMD_HDMI_SET_MODE, (unsigned long)arg);
        ioctl(Globctx->displayFd, DISP_CMD_HDMI_ENABLE, (unsigned long)arg);
        repaint_request = 10;
        if(Globctx->psHwcProcs){
            Globctx->psHwcProcs->invalidate(Globctx->psHwcProcs);
        }
        if(DISP_TV_MOD_1080P_24HZ_3D_FP != mode)
            saveDispModeToFile(type, mode);
        ALOGD("switch to HDMI[0x%x] finish!", mode);
        break;
    case DISP_OUTPUT_TYPE_TV:
        ALOGD("switch to CVBS[0x%x] begin!", mode);
        arg[0] = hwDisp;
        ioctl(Globctx->displayFd, DISP_CMD_TV_OFF, (unsigned long)arg);

        if(Globctx->mainDisp == CVBS_USED){
            Globctx->fe_clk = getFeClk(mode);
            Globctx->de_fps = getInfoOfMode(mode, REFRESHRAE);
        }
        psDisplayInfo->virtualToHWDisplay = hwDisp;
        psDisplayInfo->varDisplayWidth = getInfoOfMode(mode, WIDTH);
        psDisplayInfo->varDisplayHeight = getInfoOfMode(mode, HEIGHT);
        psDisplayInfo->displayType = DISP_OUTPUT_TYPE_TV;
        psDisplayInfo->displayMode = mode;
        psDisplayInfo->displayDPI_X = 213000;
        psDisplayInfo->displayDPI_Y = 213000;
        psDisplayInfo->displayVsyncP = 1000000000/getInfoOfMode(mode, REFRESHRAE);
        psDisplayInfo->hwLayerNum = DISPLAY_MAX_LAYER_NUM;
        psDisplayInfo->hwPipeNum = NUMBEROFPIPE;

        arg[1] = mode;
        ioctl(Globctx->displayFd, DISP_CMD_TV_SET_MODE, (unsigned long)arg);
        ioctl(Globctx->displayFd, DISP_CMD_TV_ON, (unsigned long)arg);
        repaint_request = 10;
        if(Globctx->psHwcProcs){
            Globctx->psHwcProcs->invalidate(Globctx->psHwcProcs);
        }
        if(DISP_TV_MOD_1080P_24HZ_3D_FP != mode)
            saveDispModeToFile(type, mode);
        ALOGD("switch to CVBS[0x%x] finish!", mode);
        break;
    case DISP_OUTPUT_TYPE_LCD:
        if(Globctx->mainDisp == LCD_USED){
            Globctx->fe_clk = getFeClk(mode);
            Globctx->de_fps = 60;
        }
        arg[0] = hwDisp;
        ioctl(Globctx->displayFd, DISP_CMD_LCD_DISABLE, arg);
        ioctl(Globctx->displayFd, DISP_CMD_LCD_ENABLE, arg);
        //If the hwDisp is used as the LCD, then the width and height should be never change.
        psDisplayInfo->virtualToHWDisplay = hwDisp;
        psDisplayInfo->varDisplayWidth = ioctl(Globctx->displayFd, DISP_CMD_GET_SCN_WIDTH, arg);
        psDisplayInfo->varDisplayHeight = ioctl(Globctx->displayFd, DISP_CMD_GET_SCN_HEIGHT, arg);
        ALOGD("###mainDisp=%d, secDisp=%d, LCD width=%d, height=%d", Globctx->mainDisp, Globctx->secDisp,
            psDisplayInfo->varDisplayWidth, psDisplayInfo->varDisplayHeight);
        psDisplayInfo->displayType = DISP_OUTPUT_TYPE_LCD;
        psDisplayInfo->displayMode = DISP_TV_MOD_INVALID;
        psDisplayInfo->displayDPI_X = 160000;
        psDisplayInfo->displayDPI_Y = 160000;
        psDisplayInfo->displayVsyncP = 1000000000/60;
        psDisplayInfo->hwLayerNum = DISPLAY_MAX_LAYER_NUM;
        psDisplayInfo->hwPipeNum = NUMBEROFPIPE;
        repaint_request = 10;
        break;
    case DISP_OUTPUT_TYPE_VGA:
        //To do
        break;
    case DISP_OUTPUT_TYPE_NONE:
        if(psDisplayInfo->virtualToHWDisplay == INVALID_VALUE){
            return 0;
        }
        switch(psDisplayInfo->displayType){
        case DISP_OUTPUT_TYPE_HDMI:
            arg[0] = HDMI_USED;
            ioctl(Globctx->displayFd, DISP_CMD_HDMI_DISABLE, (unsigned long)arg);
            break;
        case DISP_OUTPUT_TYPE_TV:
            arg[0] = CVBS_USED;
            ioctl(Globctx->displayFd, DISP_CMD_TV_OFF, (unsigned long)arg);
            break;
        case DISP_OUTPUT_TYPE_LCD:
            arg[0] = LCD_USED;
            ioctl(Globctx->displayFd, DISP_CMD_LCD_DISABLE, (unsigned long)arg);
            break;
        case DISP_OUTPUT_TYPE_VGA:
            //To do
            break;
        default:
            break;
        }
        break;
    }

    ALOGD("###########  setDisplayMode disp=%d, mode=%d", hwDisp, mode);
    Globctx->detectError = 0;
    return 0;
}

static void updateFps(SUNXI_hwcdev_context_t *psCtx)
{
	timeval tv = { 0, 0 };
	gettimeofday(&tv, NULL);
	double fCurrentTime = tv.tv_sec + tv.tv_usec / 1.0e6;

	if(fCurrentTime - psCtx->fBeginTime >= 1)
	{
		char property[PROPERTY_VALUE_MAX];
		int  show_fps_settings = 0;
		if(0 <= repaint_request) {
			if(psCtx->psHwcProcs && psCtx->psHwcProcs->invalidate){
				psCtx->psHwcProcs->invalidate(psCtx->psHwcProcs);
			}
			ALOGD("[hwc]repaineverything. repaint_request=%d", repaint_request);
			repaint_request --;
		}
		if (property_get("debug.hwc.showfps", property, NULL) >= 0)
		{
			show_fps_settings = atoi(property);
		}else{
		    psCtx->uiBeginFrame = psCtx->hwcFrameCount;
		    ALOGD("No hwc debug attribute node.");
			return;
		}
		if((show_fps_settings&FPS_SHOW) != (psCtx->hwcdebug&FPS_SHOW))
		{
		    ALOGD("###### %s hwc fps print ######",(show_fps_settings&1) != 0 ? "Enable":"Disable");
		}
        psCtx->hwcdebug = show_fps_settings&SHOW_ALL;
        if(psCtx->hwcdebug&1)
	    {
	        ALOGD(">>>fps %d\n", int((psCtx->hwcFrameCount - psCtx->uiBeginFrame) * 1.0f
				                      / (fCurrentTime - psCtx->fBeginTime)));
	    }
        psCtx->uiBeginFrame = psCtx->hwcFrameCount;
	    psCtx->fBeginTime = fCurrentTime;
	}
}

static int hwc_uevent(void)
{
	struct sockaddr_nl snl;
	const int buffersize = 32*1024;
	int retval;
	int hotplug_sock;
	SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;

	initParseEdid(HDMI_USED);

	memset(&snl, 0x0, sizeof(snl));
	snl.nl_family = AF_NETLINK;
	snl.nl_pid = 0;
	snl.nl_groups = 0xffffffff;

	hotplug_sock = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
	if (hotplug_sock == -1) {
		ALOGE("####socket is failed in %s error:%d %s###", __FUNCTION__, errno, strerror(errno));
		return -1;
	}

	setsockopt(hotplug_sock, SOL_SOCKET, SO_RCVBUFFORCE, &buffersize, sizeof(buffersize));

	retval = bind(hotplug_sock, (struct sockaddr *)&snl, sizeof(struct sockaddr_nl));

	if (retval < 0) {
		ALOGE("####bind is failed in %s error:%d %s###", __FUNCTION__, errno, strerror(errno));
		close(hotplug_sock);
		return -1;
	}
	while(1)
	{
		char buf[4096*2] = {0};
        struct pollfd fds;
        int err;
        //unsigned int cout;

        fds.fd = hotplug_sock;
        fds.events = POLLIN;
        fds.revents = 0;
        //cout = Globctx->hwcFrameCount;
        err = poll(&fds, 1, 1000);

        if(err > 0 && fds.revents == POLLIN)
        {
    		int count = recv(hotplug_sock, &buf, sizeof(buf),0);
    		if(count > 0)
    		{
    		    int isVsync;

                isVsync = !strcmp(buf, "change@/devices/platform/disp");

                if(isVsync)
                {
                    uint64_t timestamp = 0;
                    int display_id = -1;
                    const char *s = buf;

                    if(!Globctx->psHwcProcs || !Globctx->psHwcProcs->vsync)
                    {
                       ALOGD("####unable to call Globctx->psHwcProcs->vsync, should not happened");
                       continue;
                    }

                    s += strlen(s) + 1;
                    while(s)
                    {
                        if (!strncmp(s, "VSYNC0=", strlen("VSYNC0=")))
                        {
                            timestamp = strtoull(s + strlen("VSYNC0="), NULL, 0);
                            ALOGV("#### %s display0 timestamp:%lld###", s,timestamp);
                            display_id = AW_DIS_00;
                        }
                        else if (!strncmp(s, "VSYNC1=", strlen("VSYNC1=")))
                        {
                            timestamp = strtoull(s + strlen("VSYNC1="), NULL, 0);
                            ALOGV("#### %s display1 timestamp:%lld###", s,timestamp);
                            display_id = AW_DIS_01;
                        }else if(!strncmp(s, "VSYNC2=", strlen("VSYNC2="))){
                            timestamp = strtoull(s + strlen("VSYNC1="), NULL, 0);
                            ALOGV("#### %s display2 timestamp:%lld###", s,timestamp);
                            display_id = AW_DIS_02;
                        }

                        s += strlen(s) + 1;
                        if(s - buf >= count)
                        {
                            break;
                        }
                    }
                    //we just support single display for now
                    if(display_id == Globctx->mainDisp)
                    {
                        DisplayInfo *psDisplayInfo = NULL;
                        for(int i = 0; i < NUMBEROFDISPLAY; i++)
                        {
                            if(display_id == Globctx->sunxiDisplay[i].virtualToHWDisplay)
                            {
                                psDisplayInfo = &(Globctx->sunxiDisplay[i]);
                                break;
                            }
                        }

                        if(psDisplayInfo != NULL)
                        {
                            display_id = 0;
                            Globctx->psHwcProcs->vsync(Globctx->psHwcProcs, display_id, timestamp);
                        }
                    }
                }
            }
            Globctx->forceGPUComp = 0;
        }
#if 0 //  need not to forceGpuComp in homlet
        else if(err == 0) {
            if(Globctx->HWCFramecount == cout)
            {
	            if(Globctx->ForceGPUComp == 0 && Globctx->CanForceGPUCom)
		        {
                    Globctx->ForceGPUComp = 1;
                    Globctx->psHwcProcs->invalidate(Globctx->psHwcProcs);
		        }
            }else{
                if((Globctx->HWCFramecount > cout ? Globctx->HWCFramecount-cout : cout-Globctx->HWCFramecount) > 2)
		        {
                	Globctx->ForceGPUComp = 0;
		        }
            }
        }
#endif
	    updateFps(Globctx);
    }

	return 0;
}

void *vsyncThreadWrapper(void *priv)
{
	setpriority(PRIO_PROCESS, 0, HAL_PRIORITY_URGENT_DISPLAY);

	hwc_uevent();

	return NULL;
}


