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

static tv_para_t g_tv_para[]=
{
    {8, DISP_TV_MOD_NTSC,             720,    480, 60,0},
    {8, DISP_TV_MOD_PAL,              720,    576, 50,0},

    {5, DISP_TV_MOD_480I,             720,    480, 60,0},
    {5, DISP_TV_MOD_576I,             720,    576, 60,0},
    {5, DISP_TV_MOD_480P,             720,    480, 60,0},
    {5, DISP_TV_MOD_576P,             720,    576, 50,0},
    {5, DISP_TV_MOD_720P_50HZ,        1280,   720, 50,0},
    {5, DISP_TV_MOD_720P_60HZ,        1280,   720, 60,0},

    {1, DISP_TV_MOD_1080P_24HZ,       1920,   1080, 24,0},
    {5, DISP_TV_MOD_1080P_50HZ,       1920,   1080, 50,0},
    {5, DISP_TV_MOD_1080P_60HZ,       1920,   1080, 60,0},
    {5, DISP_TV_MOD_1080I_50HZ,       1920,   1080, 50,0},
    {5, DISP_TV_MOD_1080I_60HZ,       1920,   1080, 60,0},


	{5, DISP_TV_MOD_3840_2160P_25HZ,  3840,   2160, 25,0xff},
	{5, DISP_TV_MOD_3840_2160P_24HZ,  3840,   2160, 24,0xff},
    {5, DISP_TV_MOD_3840_2160P_30HZ,  3840,   2160, 30,0xff},

    {1, DISP_TV_MOD_1080P_24HZ_3D_FP, 1920,   1080, 24,0},
    {1, DISP_TV_MOD_720P_50HZ_3D_FP,  1280,   720, 50,0},
    {1, DISP_TV_MOD_720P_60HZ_3D_FP,  1280,   720, 60,0},
    {1, DISP_TV_MODE_NUM,  0,   0, 0,0},
};

static tv_para_t g_vga_para[]=
{
    {8, DISP_VGA_MOD_640x480P_60HZ,  640,  480,  60,  0},
    {8, DISP_VGA_MOD_800x600P_60HZ,  800,  600,  60,  0},
    {8, DISP_VGA_MOD_1024x768P_60HZ, 1024,  768,  60,  0},
    {8, DISP_VGA_MOD_1280x768P_60HZ, 1280,  768,  60,  0},
    {8, DISP_VGA_MOD_1280x800P_60HZ, 1280,  800,  60,  0},
    {8, DISP_VGA_MOD_1366x768P_60HZ, 1366,  768,  60,  0},
    {8, DISP_VGA_MOD_1440x900P_60HZ, 1440,  900,  60,  0},
    {8, DISP_VGA_MOD_1920x1080P_60HZ, 1920, 1080, 60,  0},
    {8, DISP_VGA_MOD_1920x1200P_60HZ, 1920, 1200, 60,  0},
};

int get_info_mode(int mode,MODEINFO info)
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

int getVgaInfo(int mode, MODEINFO info)
{
    unsigned int i = 0;

    for(i = 0; i < sizeof(g_vga_para) / sizeof(tv_para_t); i++)
        if(g_vga_para[i].mode == mode)
            return *(((int *)(g_vga_para + i)) + info);
    ALOGE("no find the vga mode[%d].", mode);
    return -1;
}

int getValueFromProperty(char const* propName)
{
    char property[PROPERTY_VALUE_MAX] = {0};
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

int getDispPolicy(void)
{
    int policy = getValueFromProperty(RT_DISP_POLICY_PROP_NAME);
    if(-1 == policy)
        return 0;
    return policy;
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

int getMainDisplay()
{
    // ---------------------------------------------------------------------------------------------//
    // how to decide which display-device is the main disp:                                         //
    //    a) let the disp-driver tells us.                                                          //
    //    b) let the configs tell us.                                                               //
    //    c) if a) and b) does not work, init in order by hwdisp = 0/1...                           //
    //----------------------------------------------------------------------------------------------//

    return 0;
}

DisplayInfo* hwc_manage_display(DisplayInfo *LocDisplayInfo[], int DispInfo,ManageDisp mode)
{

    DisplayInfo* PsDisplayInfo = NULL;
    DisplayInfo* TmpDisplayInfo =NULL;
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    int find = 0;
    int disp;
    for(disp = 0; disp < Globctx->NumberofDisp; disp++)
    {
        PsDisplayInfo = Globctx->SunxiDisplay+disp;
        switch(mode)
        {
            case FIND_HWDISPNUM:
                if(PsDisplayInfo->VirtualToHWDisplay == DispInfo)
                {
                    return PsDisplayInfo;
                }
                break;

            case FIND_HWTYPE:
                if(PsDisplayInfo->VirtualToHWDisplay != -EINVAL && PsDisplayInfo->DisplayType == DispInfo)
                {
                    LocDisplayInfo[find++] = PsDisplayInfo;
                }
                break;

            case NULL_DISPLAY:
                if(PsDisplayInfo->VirtualToHWDisplay != -EINVAL )
                {
                    return PsDisplayInfo;
                }
                break;

            case SET_DISP:
                if(PsDisplayInfo->VirtualToHWDisplay == DispInfo)
                {
                    return PsDisplayInfo;
                }

                if(PsDisplayInfo->VirtualToHWDisplay == -EINVAL && TmpDisplayInfo == NULL)
                {
                    TmpDisplayInfo = PsDisplayInfo;
                }
                if(disp == NUMBEROFDISPLAY-1)
                {
                    TmpDisplayInfo->VirtualToHWDisplay = DispInfo;
                    return TmpDisplayInfo;
                }
                break;

            case FREE_DISP:
                if(PsDisplayInfo->VirtualToHWDisplay == DispInfo)
                {
                    PsDisplayInfo->VirtualToHWDisplay = -EINVAL;
                    if(PsDisplayInfo->Current3DMode == DISPLAY_3D_LEFT_RIGHT_HDMI || PsDisplayInfo->Current3DMode == DISPLAY_3D_TOP_BOTTOM_HDMI)
                    {
                        PsDisplayInfo->Current3DMode = DISPLAY_2D_ORIGINAL;
                    }
                    return PsDisplayInfo;
                }
                break;

            default:
                ALOGD("Error  usage in ManageDisplay");
        }
    }

    PsDisplayInfo = NULL;
    return PsDisplayInfo;

}

disp_tv_mode checkout_mode(int select,bool reset)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
	unsigned long arg[4]={0};
    arg[0] =select;
    int ret, i,j = -1;
    int theMostMode = DISP_TV_MODE_NUM;
    i = sizeof(g_tv_para)/sizeof(g_tv_para[0]);

    if(!reset)
    {
        while(i > 0)
        {
            i--;
            if(g_tv_para[i].mode == DISP_TV_MOD_1080P_60HZ)
            {
                j = i;
            }
            if(j != -1)
            {
                arg[1] = DISP_OUTPUT_TYPE_HDMI;
	            arg[2] = g_tv_para[i].mode;
                ret = ioctl(Globctx->DisplayFd, DISP_DEVICE_SWITCH, arg);
	            if(ret >= 0)
	            {
                    if(theMostMode == DISP_TV_MODE_NUM)
                    {
                         g_tv_para[sizeof(g_tv_para)/sizeof(g_tv_para[0])-1].support = 1<<select;
                         theMostMode = g_tv_para[i].mode;
                    }
                    g_tv_para[i].support = 1<<select;
	            }
            }
        }
    }else{
        g_tv_para[i-1].support = 0<<select;
    }
    if(theMostMode != DISP_TV_MODE_NUM)
    {
        return (disp_tv_mode)theMostMode;
    }else{
        return DISP_TV_MOD_1080P_60HZ;
    }

}

disp_tv_mode get_suitable_hdmi_mode(int select,bool check)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
	unsigned long arg[4]={0};
    arg[0] =select;
    int HdmiMode;
    int ret = -1;
    DisplayInfo   *PsDisplayInfo = Globctx->SunxiDisplay;
    PsDisplayInfo = hwc_manage_display(NULL, select, FIND_HWDISPNUM);
    if(PsDisplayInfo != NULL && !check)
    {
        if(PsDisplayInfo->setDispMode >= DISP_TV_MOD_480I && PsDisplayInfo->setDispMode < DISP_TV_MODE_NUM)
        {
            return  (disp_tv_mode)(PsDisplayInfo->setDispMode);
        }
        if(PsDisplayInfo->DisplayMode >= DISP_TV_MOD_480I && PsDisplayInfo->DisplayMode < DISP_TV_MODE_NUM)
        {
		   return  (disp_tv_mode)(PsDisplayInfo->DisplayMode);
	    }
    }

    return checkout_mode(select,check);
}

int getHwDispByType(int type)
{
    switch(type)
    {
    case DISP_OUTPUT_TYPE_HDMI:
        return HDMI_USED;
    case DISP_OUTPUT_TYPE_TV:
        return CVBS_USED;
    case DISP_OUTPUT_TYPE_VGA:
        return VGA_USED;
    case DISP_OUTPUT_TYPE_LCD:
        return LCD_USED;
    default:
        ALOGE("getHwDispByType ERR:type=%d", type);
        return -EINVAL;
    }
}

int resetDispMode(int disp, int type, int mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;

    if((DISP_OUTPUT_TYPE_HDMI == type) && (0xFF == mode)) {
        vendor_info_t vendorInfo;
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

int hwcOutputSwitch(DisplayInfo *psDisplayInfo, int type, int mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long arg[4] = {0};
    int ret = -1;

    ALOGD("###%s: type=%d, mode=%d", __func__, type, mode);
    switch(type)
    {
    case DISP_OUTPUT_TYPE_HDMI:
    case DISP_OUTPUT_TYPE_TV:
        psDisplayInfo->setblank = 1;
        psDisplayInfo->VirtualToHWDisplay = getHwDispByType(type);
        psDisplayInfo->VarDisplayWidth = get_info_mode(mode,WIDTH);
        psDisplayInfo->VarDisplayHeight = get_info_mode(mode,HEIGHT);
        psDisplayInfo->DisplayFps = get_info_mode(mode, REFRESHRAE);
        psDisplayInfo->DisplayVsyncP = 1000000000 / psDisplayInfo->DisplayFps;
        psDisplayInfo->DisplayType = type;
        psDisplayInfo->DisplayMode = mode;
        psDisplayInfo->HwChannelNum = psDisplayInfo->VirtualToHWDisplay ? 2 : NUMCHANNELOFDSP;
        psDisplayInfo->de_clk = 432000000;
        arg[0] = psDisplayInfo->VirtualToHWDisplay;
        arg[1] = type;
        arg[2] = mode;
        ret = ioctl(Globctx->DisplayFd, DISP_DEVICE_SWITCH, (unsigned long)arg);
        psDisplayInfo->setblank = 0;
        Globctx->psHwcProcs->invalidate(Globctx->psHwcProcs);
        if((DISP_TV_MOD_1080P_24HZ_3D_FP != mode) && (0 == ret))
            saveDispModeToFile(type, mode);
        break;
    case DISP_OUTPUT_TYPE_LCD:
        ALOGD("%s:%d\n", __func__, __LINE__);
        break;
    case DISP_OUTPUT_TYPE_VGA:
        psDisplayInfo->setblank = 1;
        psDisplayInfo->VirtualToHWDisplay = getHwDispByType(type);
        psDisplayInfo->VarDisplayWidth = getVgaInfo(mode,WIDTH);
        psDisplayInfo->VarDisplayHeight = getVgaInfo(mode,HEIGHT);
        psDisplayInfo->DisplayFps = getVgaInfo(mode, REFRESHRAE);
        psDisplayInfo->DisplayVsyncP = 1000000000 / psDisplayInfo->DisplayFps;
        psDisplayInfo->DisplayType = type;
        psDisplayInfo->DisplayMode = mode;
        psDisplayInfo->HwChannelNum = psDisplayInfo->VirtualToHWDisplay ? 2 : NUMCHANNELOFDSP;
        psDisplayInfo->de_clk = 432000000;
        arg[0] = psDisplayInfo->VirtualToHWDisplay;
        arg[1] = type;
        arg[2] = mode;
        ret = ioctl(Globctx->DisplayFd, DISP_DEVICE_SWITCH, (unsigned long)arg);
        psDisplayInfo->setblank = 0;
        Globctx->psHwcProcs->invalidate(Globctx->psHwcProcs);
        if((DISP_TV_MOD_1080P_24HZ_3D_FP != mode) && (0 == ret))
            saveDispModeToFile(type, mode);
        break;
    case DISP_OUTPUT_TYPE_NONE:
        psDisplayInfo->setblank = 1;
        Globctx->psHwcProcs->invalidate(Globctx->psHwcProcs);
        usleep(50 * 1000);
        ALOGD("disp[%d]:chnNum[%d],lyrNum[%d]", psDisplayInfo->VirtualToHWDisplay, psDisplayInfo->HwChannelNum, psDisplayInfo->LayerNumofCH);
        int channelId, layerId;
        disp_layer_config layerConfig;
        arg[0] = psDisplayInfo->VirtualToHWDisplay;
        arg[1] = (unsigned long)&layerConfig;
        arg[2] = 1;
        layerConfig.enable = 0;
        for(channelId = 0; channelId < psDisplayInfo->HwChannelNum; channelId++) {
            for(layerId = 0; layerId< psDisplayInfo->LayerNumofCH; layerId++) {
                layerConfig.channel = channelId;
                layerConfig.layer_id = layerId;
                ioctl(Globctx->DisplayFd, DISP_LAYER_SET_CONFIG, (unsigned long)arg);
            }
        }
        psDisplayInfo->DisplayType = 0;
        psDisplayInfo->DisplayMode = 0;
        psDisplayInfo->setDispMode = 0;
        psDisplayInfo->VirtualToHWDisplay = -EINVAL;
        ret = 0;
        break;
    default:
        ret = -1;
    }

    return ret;
}

int hwcOutputExchange()
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    pthread_mutex_lock(&Globctx->lock);
    Globctx->exchangeDispChannel = 1;
    pthread_mutex_unlock(&Globctx->lock);
    Globctx->psHwcProcs->invalidate(Globctx->psHwcProcs);
    unsigned int count = 0;
    do
    {
        usleep(1000);
        count++;
    }while(Globctx->exchangeDispChannel);
    ALOGD("use time = %d ms", count);
    return 0;
}

int hwc_hotplug_switch(int DisplayNum, bool plug)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    int disp=0;
    DisplayInfo   *PsDisplayInfo = NULL;

    unsigned long arg[4] = {0};
    bool AllreadyPlugin = 0;
    if(plug)
    {
        PsDisplayInfo = hwc_manage_display(NULL, DisplayNum, FIND_HWDISPNUM);
        if (PsDisplayInfo != NULL)
        {
            ALOGD("###AllreadyPlugin####");
            AllreadyPlugin = 1;

        }else{

            PsDisplayInfo = hwc_manage_display(NULL, DisplayNum, SET_DISP);
        }

        disp_tv_mode ExtDisplayMode;
        if(PsDisplayInfo->Current3DMode == DISPLAY_3D_LEFT_RIGHT_HDMI || PsDisplayInfo->Current3DMode == DISPLAY_3D_TOP_BOTTOM_HDMI)
        {
            ExtDisplayMode = DISP_TV_MOD_1080P_24HZ_3D_FP;
        }else{
            ExtDisplayMode = get_suitable_hdmi_mode(DisplayNum,!AllreadyPlugin);
        }
        if(ExtDisplayMode != DISP_TV_MODE_NUM){

            PsDisplayInfo->VarDisplayWidth = get_info_mode(ExtDisplayMode,WIDTH);
            PsDisplayInfo->VarDisplayHeight = get_info_mode(ExtDisplayMode,HEIGHT);
            PsDisplayInfo->DisplayType = DISP_OUTPUT_TYPE_HDMI;
            PsDisplayInfo->DisplayMode = ExtDisplayMode;
            PsDisplayInfo->DiplayDPI_X = 213000;
            PsDisplayInfo->DiplayDPI_Y = 213000;
            PsDisplayInfo->DisplayFps = get_info_mode(ExtDisplayMode, REFRESHRAE);
            PsDisplayInfo->DisplayVsyncP = 1000000000 / PsDisplayInfo->DisplayFps;
            PsDisplayInfo->HwChannelNum = DisplayNum?2:4;
            PsDisplayInfo->LayerNumofCH = NUMLAYEROFCHANNEL;
            PsDisplayInfo->VideoCHNum =1;

            arg[0] = DisplayNum;
            ioctl(Globctx->DisplayFd, DISP_BLANK, (unsigned long)arg);

            arg[0] = DisplayNum;
            arg[1] = DISP_OUTPUT_TYPE_HDMI;
            arg[2] = ExtDisplayMode;
            ioctl(Globctx->DisplayFd, DISP_DEVICE_SWITCH, (unsigned long)arg);
            //ioctl(Globctx->DisplayFd, DISP_HDMI_ENABLE, (unsigned long)arg);
        }else{

            ALOGD("###has no fix HDMI Mode###");
            return 0;
        }
        ALOGD( "###hdmi plug in, Type:%d, Mode:0x%08x###", PsDisplayInfo->DisplayType,PsDisplayInfo->DisplayMode);

    }else{
        arg[0] = DisplayNum;
        arg[1] = DISP_OUTPUT_TYPE_NONE;
        hwc_manage_display(NULL, DisplayNum ,FREE_DISP);

        if(hwc_manage_display(NULL, 0 ,NULL_DISPLAY) == NULL)
        {
            //Globctx->DetectError = 1;
            ALOGD( "###ALL Display has plug out###");
        }
            ALOGD( "###hdmi plug out###");
    }
    ALOGD("###psHwcProcs  No register.###");

    return 0;
}

int check4KBan(void)
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
	int ban = 1;
	int i;

	dev = open(infodev, O_RDONLY);
	if (dev < 0) {
		ALOGE("can not open '%s'", infodev);
		return 1;
	}

	memset(buf, 0, sizeof(buf));
	if (ioctl(dev, 3, buf) < 0) {
		ALOGE("%s: ioctl error", __func__);
		return 1;
	}

	ALOGD("soc info: %s", buf);
	sscanf(buf, "%08x", &chipid);
	chipid &= 0x000000ff;

	for (i = 0; i < sizeof(permit_table)/sizeof(permit_table[0]); i++) {
		ALOGD("chid: %02x, permit: %02x", chipid, permit_table[i]);
		if (permit_table[i] == chipid) {
			ban = 0;
			break;
		}
	}

	for (i = 0; i < sizeof(ban_table)/sizeof(ban_table[0]); i++) {
		ALOGD("chid: %02x, ban: %02x", chipid, ban_table[i]);
		if (ban_table[i] == chipid) {
			ban = 1;
			break;
		}
	}

	ALOGD("4k ban: %d", ban);
	return ban;
}

static void updateFps(SUNXI_hwcdev_context_t *psCtx)
{

	double fCurrentTime = 0.0;
	timeval tv = { 0, 0 };
	gettimeofday(&tv, NULL);
	fCurrentTime = tv.tv_sec + tv.tv_usec / 1.0e6;

	if(fCurrentTime - psCtx->fBeginTime >= 1)
	{
		char property[PROPERTY_VALUE_MAX]={0};
		int  show_fps_settings = 0;

		if (property_get("debug.hwc.showfps", property, NULL) >= 0)
		{
			if(property[0] == '1'){
				show_fps_settings = 1;
			}else if(property[0] == '2'){
				show_fps_settings = 2;
			}else{
				show_fps_settings = 0;
			}
		}else{
		    ALOGD("No hwc debug attribute node.");
			return;
		}
		if((show_fps_settings & FPS_SHOW) != (psCtx->hwcdebug & FPS_SHOW))
		{
		    ALOGD("###### %s hwc fps print ######",(show_fps_settings & FPS_SHOW) != 0 ? "Enable":"Disable");
		}
        psCtx->hwcdebug = show_fps_settings&SHOW_ALL;
        if(psCtx->hwcdebug&1)
	    {
	        ALOGD(">>>fps:: %d\n", (int)((psCtx->HWCFramecount - psCtx->uiBeginFrame) * 1.0f
				                      / (fCurrentTime - psCtx->fBeginTime)));
	    }
        psCtx->uiBeginFrame = psCtx->HWCFramecount;
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
	char *buf;
	buf=(char *)malloc(sizeof(char) * 1024);
	struct pollfd fds;
	int err;
	unsigned int cout;
	char *s = NULL;
	int IsVsync, IsHdmi;
	unsigned int new_hdmi_hpd;
	uint64_t timestamp = 0;
	int display_id = -1,cnt = 2;
	int count = 0;
	while(1)
	{
        fds.fd = hotplug_sock;
        fds.events = POLLIN;
        fds.revents = 0;
        cout = Globctx->HWCFramecount;
        err = poll(&fds, 1, 1000);
        memset(buf, '\0', sizeof(char) * 1024);
        if(err > 0 && (fds.revents & POLLIN))
        {
    		count = recv(hotplug_sock, buf, sizeof(char) * 1024,0);
    		if(count > 0)
    		{
                IsVsync = !strcmp(buf, "change@/devices/platform/disp");
                IsHdmi = !strcmp(buf, "change@/devices/virtual/switch/hdmi");

                if(IsVsync)
                {
                    display_id = -1,cnt = 2;
                    s = buf;

                    if(!Globctx->psHwcProcs || !Globctx->psHwcProcs->vsync){
                        free(buf);
                        buf = NULL;
                        return 0;
                    }
                    s += strlen(s) + 1;
                    while(s)
                    {
                        if (!strncmp(s, "VSYNC0=", strlen("VSYNC0=")))
                        {
                            timestamp = strtoull(s + strlen("VSYNC0="), NULL, 0);
                            display_id = 0;
                        }
                        else if (!strncmp(s, "VSYNC1=", strlen("VSYNC1=")))
                        {
                            timestamp = strtoull(s + strlen("VSYNC1="), NULL, 0);
                            display_id = 1;
                        }
                        s += strlen(s) + 1;
                        if(s - buf >= count)
                        {
                            break;
                        }
                    }
                    while(cnt--)
                    {
                        if(Globctx->SunxiDisplay[0].VirtualToHWDisplay == display_id)
                        {
                            Globctx->SunxiDisplay[0].mytimestamp = timestamp;
                        }
                    }
                    if((display_id == Globctx->SunxiDisplay[0].VirtualToHWDisplay && Globctx->SunxiDisplay[0].VsyncEnable == 1)
                        || (display_id == Globctx->SunxiDisplay[1].VirtualToHWDisplay && Globctx->SunxiDisplay[1].VsyncEnable == 1))
                    {
                        Globctx->psHwcProcs->vsync(Globctx->psHwcProcs, 0, timestamp);
                    }
                }

                if(IsHdmi)
                {
                    s = buf;
                    s += strlen(s) + 1;
                    while(s)
                    {
                        if (!strncmp(s, "SWITCH_STATE=", strlen("SWITCH_STATE=")))
                        {
                            new_hdmi_hpd = strtoull(s + strlen("SWITCH_STATE="), NULL, 0);
                            ALOGD( "#### disp[%d]   hotplug[%d]###",HDMI_USED ,!!new_hdmi_hpd);
                            if(new_hdmi_hpd)
                            {
                                resetParseEdidForDisp(HDMI_USED);
                            }
                            //hwc_hotplug_switch(1, !!new_hdmi_hpd);
                        }
                        s += strlen(s) + 1;
                        if(s - buf >= count)
                        {
                            break;
                        }
                    }
                }
            }
            if(Globctx->SunxiDisplay[0].VsyncEnable == 1)
            {
                Globctx->ForceGPUComp = 0;
            }
        }
#if 0 // do not need to ForceGPUComp in homlet product
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
	free(buf);
	buf = NULL;
	return 0;
}

void *VsyncThreadWrapper(void *priv)
{
	setpriority(PRIO_PROCESS, 0, HAL_PRIORITY_URGENT_DISPLAY);

	hwc_uevent();

	return NULL;
}

