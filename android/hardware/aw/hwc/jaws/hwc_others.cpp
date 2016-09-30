
#include "hwc.h"
#define MARGIN_MIN_PERCENT  50
#define MARGIN_MAX_PERCENT  100

static int is3DMode(__display_3d_mode mode)
{
    switch(mode)
    {
    case DISPLAY_3D_LEFT_RIGHT_HDMI:
    case DISPLAY_3D_TOP_BOTTOM_HDMI:
    case DISPLAY_3D_DUAL_STREAM:
        return 1;
    default:
        return 0;
    }
}

int _hwcdev_layer_config_3d(int disp, disp_layer_info *layer_info, bool isVideoFormat)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo   *psDisplayInfo = &Globctx->sunxiDisplay[disp];
    __display_3d_mode cur_3d_mode = psDisplayInfo->current3DMode;

    if(layer_info->mode == DISP_LAYER_WORK_MODE_SCALER && isVideoFormat)
    {
        switch(cur_3d_mode)
        {
            case DISPLAY_2D_ORIGINAL:
                layer_info->fb.b_trd_src = 0;
                layer_info->b_trd_out = 0;
                break;
            case DISPLAY_2D_LEFT:
                layer_info->fb.b_trd_src = 1;
                layer_info->fb.trd_mode = DISP_3D_SRC_MODE_SSF;
                layer_info->b_trd_out = 0;
                break;
            case DISPLAY_2D_TOP:
                layer_info->fb.b_trd_src = 1;
                layer_info->fb.trd_mode = DISP_3D_SRC_MODE_TB;
                layer_info->b_trd_out = 0;
                break;
            case DISPLAY_3D_LEFT_RIGHT_HDMI:
                layer_info->fb.b_trd_src = 1;
                layer_info->fb.trd_mode = DISP_3D_SRC_MODE_SSF;
                layer_info->b_trd_out = 1;
                layer_info->out_trd_mode = DISP_3D_OUT_MODE_FP;
                break;
            case DISPLAY_3D_TOP_BOTTOM_HDMI:
                layer_info->fb.b_trd_src = 1;
                layer_info->fb.trd_mode = DISP_3D_SRC_MODE_TB;
                layer_info->b_trd_out = 1;
                layer_info->out_trd_mode = DISP_3D_OUT_MODE_FP;
                break;
            case DISPLAY_2D_DUAL_STREAM:
                layer_info->fb.b_trd_src = 0;
                layer_info->b_trd_out = 0;
                break;
            case DISPLAY_3D_DUAL_STREAM:
                layer_info->fb.b_trd_src = 1;
                layer_info->fb.trd_mode = DISP_3D_SRC_MODE_FP;
                layer_info->b_trd_out = 1;
                layer_info->out_trd_mode = DISP_3D_OUT_MODE_FP;
                break;
            default:
                break;
        }

        if(is3DMode(cur_3d_mode))
        {
            layer_info->screen_win.x = 0;
            layer_info->screen_win.y = 0;
            layer_info->screen_win.width = 1920;
            layer_info->screen_win.height = 1080 * 2;
            //TMP:do not do de-interlace when play 3D video, we will fix it after the de-interlace driver
            //is in work.
            layer_info->fb.interlace = 0;
            layer_info->fb.top_field_first = 0;
        }
    }
    else
    {
        if(is3DMode(cur_3d_mode))
        {
            layer_info->mode = DISP_LAYER_WORK_MODE_SCALER;
            layer_info->screen_win.x = layer_info->screen_win.x * 1920 / psDisplayInfo->varDisplayWidth;
            layer_info->screen_win.y = layer_info->screen_win.y * 1080 / psDisplayInfo->varDisplayHeight;
            layer_info->screen_win.width = layer_info->screen_win.width * 1920 / psDisplayInfo->varDisplayWidth;
            layer_info->screen_win.height = layer_info->screen_win.height * 1080 / psDisplayInfo->varDisplayHeight;
        }
    }

   return 0;
}

static int _hwc_device_set_3d_mode_per_display(int disp, __display_3d_mode new_mode){
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo   *psDisplayInfo = &Globctx->sunxiDisplay[disp];
    __display_3d_mode old_mode = psDisplayInfo->current3DMode;

    if(old_mode == new_mode){
        return 0;
    }

    if(psDisplayInfo->virtualToHWDisplay != -1 ){
        if(new_mode == DISPLAY_2D_ORIGINAL){
            Globctx->canForceGPUCom = 1;
        }else{
            Globctx->canForceGPUCom = 0;
        }
        //only the hdmi can set the 3D output mode.
        if(psDisplayInfo->displayType == DISP_OUTPUT_TYPE_HDMI){
            psDisplayInfo->current3DMode = new_mode;
            if(is3DMode(old_mode) != is3DMode(new_mode)){
                int hwDisp = psDisplayInfo->virtualToHWDisplay;
                int tv_mode = DISP_TV_MOD_INVALID;
                ALOGD("###old_mode[%d] --> new_mode[%d]###", old_mode, new_mode);
                if(psDisplayInfo->current3DMode == DISPLAY_3D_LEFT_RIGHT_HDMI
                    || psDisplayInfo->current3DMode == DISPLAY_3D_TOP_BOTTOM_HDMI
                    || psDisplayInfo->current3DMode == DISPLAY_3D_DUAL_STREAM){
                    tv_mode = DISP_TV_MOD_1080P_24HZ_3D_FP;
                }else if(psDisplayInfo->virtualToHWDisplay == Globctx->mainDisp){
                    tv_mode = Globctx->mainDispMode;
                }else if(psDisplayInfo->virtualToHWDisplay == Globctx->secDisp){
                    tv_mode = Globctx->secDispMode;
                }
                hwcOutputSwitch(hwDisp, DISP_OUTPUT_TYPE_HDMI, tv_mode);
            }
        }
    }

    return 0;
}

int _hwc_device_set_3d_mode(int disp, __display_3d_mode mode)
{
    for(int i = 0; i < NUMBEROFDISPLAY; i++)
    {
        _hwc_device_set_3d_mode_per_display(i, mode);
    }
    return 0;
}

//only the lcd can set backlight
int _hwc_device_set_backlight_mode(int disp, int mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    //the HDMI and Tv can not set backlight.
    for(int i = 0; i < NUMBEROFDISPLAY; i++){
        if(Globctx->sunxiDisplay[i].virtualToHWDisplay == -1){
            continue;
        }
        if(Globctx->sunxiDisplay[i].displayType != DISP_OUTPUT_TYPE_HDMI
            && Globctx->sunxiDisplay[i].displayType != DISP_OUTPUT_TYPE_TV
            && Globctx->sunxiDisplay[i].displayType != DISP_OUTPUT_TYPE_VGA){
            unsigned long arg[4] = {0};
            arg[0] = Globctx->sunxiDisplay[i].virtualToHWDisplay;
            switch(mode){
            case 1:
                return ioctl(Globctx->displayFd, DISP_CMD_DRC_ENABLE, arg);
            case 0:
                return ioctl(Globctx->displayFd, DISP_CMD_DRC_DISABLE, arg);
            }
        }
    }
    return 0;
}

//only the lcd can set backlight
int _hwc_device_set_backlight_demomode(int disp, int mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    disp_window window;
    for(int i = 0; i < NUMBEROFDISPLAY; i++){
        if(Globctx->sunxiDisplay[i].virtualToHWDisplay == -1){
            continue;
        }
        if(Globctx->sunxiDisplay[i].displayType != DISP_OUTPUT_TYPE_HDMI
            && Globctx->sunxiDisplay[i].displayType != DISP_OUTPUT_TYPE_TV
            && Globctx->sunxiDisplay[i].displayType != DISP_OUTPUT_TYPE_VGA){
            unsigned long arg[4]={0};
            arg[0] = Globctx->sunxiDisplay[disp].virtualToHWDisplay;
            window.x = 0;
            window.y = 0;
            window.width = ioctl(Globctx->displayFd,DISP_CMD_GET_SCN_WIDTH,arg);
            window.height = ioctl(Globctx->displayFd,DISP_CMD_GET_SCN_HEIGHT,arg);
            if(mode == 1)
            {
                window.width /= 2;
                arg[1] = (unsigned long)&window;
                return ioctl(Globctx->displayFd, DISP_CMD_DRC_SET_WINDOW,arg);
            }
            else
            {
                arg[1] = (unsigned long)&window;
                return ioctl(Globctx->displayFd, DISP_CMD_DRC_SET_WINDOW,arg);
            }
        }
    }
    return 0;
}

static int getHwDispId(int disp){
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    switch(disp){
    case HWC_DISPLAY_PRIMARY:
        return Globctx->mainDisp;
    case HWC_DISPLAY_EXTERNAL:
        return Globctx->secDisp;
    default:
        return INVALID_VALUE;
    }
}

int _hwc_device_set_enhancemode(int disp, int mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long arg[4]={0};
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    switch(mode){
    case 1:
        return ioctl(Globctx->displayFd, DISP_CMD_ENHANCE_ENABLE, arg);
    case 0:
        return ioctl(Globctx->displayFd, DISP_CMD_ENHANCE_DISABLE, arg);
    }
    return 0;
}

int _hwc_device_set_enhancedemomode(int disp, int mode)
{
    SUNXI_hwcdev_context_t *Globctx= &gSunxiHwcDevice;
    unsigned long arg[4]={0};
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    disp_window window;
    window.x = 0;
    window.y = 0;
    window.width = ioctl(Globctx->displayFd,DISP_CMD_GET_SCN_WIDTH,arg);
    window.height = ioctl(Globctx->displayFd,DISP_CMD_GET_SCN_HEIGHT,arg);
    switch(mode){
    case 1:
        window.width /= 2;
        arg[1] = (unsigned long)&window;
        return ioctl(Globctx->displayFd,DISP_CMD_SET_ENHANCE_WINDOW,arg);
    case 0:
        arg[1] = (unsigned long)&window;
        return ioctl(Globctx->displayFd,DISP_CMD_SET_ENHANCE_WINDOW,arg);
    }
    return 0;
}

int _hwc_device_set_output_mode(int disp, int out_type, int out_mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo   *psDisplayInfo = NULL;
    int hwDisp = -1;
    int tmp = -1;

    if ((Globctx->mainDispMode == out_mode && disp == HWC_DISPLAY_PRIMARY)
        || disp >= HWC_DISPLAY_VIRTUAL) {
        return 0;
    }
    //set the output mode of main/second display
    switch(out_type)
    {
    case DISP_OUTPUT_TYPE_HDMI:
        hwDisp = HDMI_USED;
        break;
    case DISP_OUTPUT_TYPE_TV:
        hwDisp = CVBS_USED;
        break;
    case DISP_OUTPUT_TYPE_LCD:
        hwDisp = LCD_USED;
        break;
    default:
        return 0;
    }
    out_mode = resetDispMode(disp, out_type, out_mode);

    psDisplayInfo = manageDisplay(Globctx->sunxiDisplay, hwDisp ,SET_DISP);
    if (psDisplayInfo != NULL)
    {
        switch(disp){
        case HWC_DISPLAY_PRIMARY:
            //Close the pre mainDisp
            tmp = Globctx->mainDisp;
            Globctx->mainDisp = hwDisp;
            Globctx->mainDispMode = out_mode;

            if(tmp != hwDisp && tmp != INVALID_VALUE){
                hwcOutputSwitch(tmp, DISP_OUTPUT_TYPE_NONE, 0);
            }
            //If the hwDisp is also used in second disp,we must remove the secDisp.
            if(Globctx->secDisp == hwDisp){
                Globctx->secDisp = INVALID_VALUE;
                if(Globctx->psHwcProcs != NULL){
                    Globctx->psHwcProcs->hotplug(Globctx->psHwcProcs, HWC_DISPLAY_EXTERNAL, 0);
                }
            }
            //Open the cur mainDisp
            hwcOutputSwitch(hwDisp, out_type, getSuitableTvMode(hwDisp, (disp_tv_mode)out_mode));
            break;
        case HWC_DISPLAY_EXTERNAL:
            //If the hwDisp is also used in main disp, we do not set it as the secDisp.
            if(Globctx->mainDisp == hwDisp){
                return 0;
            }
            //Close the pre secDisp, and unplug it
            if(Globctx->secDisp != hwDisp && Globctx->secDisp != INVALID_VALUE){
                hwcOutputSwitch(Globctx->secDisp, DISP_OUTPUT_TYPE_NONE, 0);
            }
            if(Globctx->psHwcProcs != NULL){
                Globctx->psHwcProcs->hotplug(Globctx->psHwcProcs, HWC_DISPLAY_EXTERNAL, 0);
            }
            Globctx->secDisp = hwDisp;
            Globctx->secDispMode = out_mode;
            //Open the cur mainDisp
            hwcOutputSwitch(hwDisp, out_type, getSuitableTvMode(hwDisp, (disp_tv_mode)out_mode));
            Globctx->secDispWidth = psDisplayInfo->varDisplayWidth;
            Globctx->secDispHeight = psDisplayInfo->varDisplayHeight;
            //broadcast plugin message for surfaceflinger
            if(Globctx->psHwcProcs != NULL){
                Globctx->psHwcProcs->hotplug(Globctx->psHwcProcs, HWC_DISPLAY_EXTERNAL, 1);
            }
        default:
            return 0;
        }

    }

    return 0;
}

int _hwc_device_set_saturation(int disp, int saturation)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
	unsigned long arg[4] = {0};
    int ret = 0;
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    arg[1] = saturation;
    ret = ioctl(Globctx->displayFd, DISP_CMD_SET_SATURATION, (unsigned long)arg);
    return  ret;
}

int _hwc_device_set_hue(int disp, int hue)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
	unsigned long arg[4] = {0};
    int ret = 0;
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    arg[1] = hue;

    ret = ioctl(Globctx->displayFd, DISP_CMD_SET_HUE, (unsigned long)arg);

    return  ret;
}

int _hwc_device_set_bright(int disp, int bright)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
	unsigned long arg[4] = {0};
    int ret = 0;
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    arg[1] = bright;

    ret = ioctl(Globctx->displayFd, DISP_CMD_SET_BRIGHT, (unsigned long)arg);

    return  ret;
}

int _hwc_device_set_contrast(int disp, int contrast)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
	unsigned long arg[4] = {0};
    int ret = 0;
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    arg[1] = contrast;

    ret = ioctl(Globctx->displayFd, DISP_CMD_SET_CONTRAST, (unsigned long)arg);

    return  ret;
}

int  _hwc_device_set_margin(int disp,int hpercent, int vpercent)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo   *psDisplayInfo = NULL;

    for(int i = 0; i < NUMBEROFDISPLAY; i++)
    {
        psDisplayInfo = &Globctx->sunxiDisplay[i];
        psDisplayInfo->displayPercentWT = (hpercent < MARGIN_MIN_PERCENT) ? MARGIN_MIN_PERCENT :
            ((hpercent > MARGIN_MAX_PERCENT) ? MARGIN_MAX_PERCENT : hpercent);
        psDisplayInfo->displayPercentHT = (vpercent < MARGIN_MIN_PERCENT) ? MARGIN_MIN_PERCENT :
            ((vpercent > MARGIN_MAX_PERCENT) ? MARGIN_MAX_PERCENT : vpercent);
    }
    return 0;
}

int _hwc_device_is_support_hdmi_mode(int disp,int mode)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long arg[4] = {0};

    arg[0] = HDMI_USED;
    arg[1] = mode;
    if(ioctl(Globctx->displayFd, DISP_CMD_HDMI_SUPPORT_MODE, (unsigned long)arg))
    {
        return 1;
    }
    return 0;
}

int _hwc_device_get_output_type(int disp)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo *psDisplayInfo = NULL;
    int hwDisp = INVALID_VALUE;
    for(int i = 0; i < NUMBEROFDISPLAY; i++)
    {
        psDisplayInfo = &Globctx->sunxiDisplay[i];
        hwDisp = psDisplayInfo->virtualToHWDisplay;
        if(hwDisp == INVALID_VALUE){
            continue;
        }
        if((hwDisp == Globctx->mainDisp && disp == HWC_DISPLAY_PRIMARY)
            ||(hwDisp == Globctx->secDisp && disp == HWC_DISPLAY_EXTERNAL))
        {
            return psDisplayInfo->displayType;
        }
    }
    return DISP_OUTPUT_TYPE_NONE;
}

int _hwc_device_get_output_mode(int disp)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    switch(disp){
    case HWC_DISPLAY_PRIMARY:
        return Globctx->mainDispMode;
    case HWC_DISPLAY_EXTERNAL:
        return Globctx->secDispMode;
    default:
        return INVALID_VALUE;
    }
}

int _hwc_device_get_saturation(int disp)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long arg[4] = {0};
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    arg[1] = 0;
    return ioctl(Globctx->displayFd, DISP_CMD_GET_SATURATION, (unsigned long)arg);
}

int _hwc_device_get_hue(int disp)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long arg[4] = {0};
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    arg[1] = 0;
    return ioctl(Globctx->displayFd, DISP_CMD_GET_HUE, (unsigned long)arg);
}

int _hwc_device_get_bright(int disp)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long arg[4] = {0};
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    arg[1] = 0;
    return ioctl(Globctx->displayFd, DISP_CMD_GET_BRIGHT, (unsigned long)arg);
}

int _hwc_device_get_contrast(int disp)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long arg[4] = {0};
    arg[0] = getHwDispId(disp);
    if(arg[0] == INVALID_VALUE){
        return 0;
    }
    arg[1] = 0;
    return ioctl(Globctx->displayFd, DISP_CMD_GET_CONTRAST, (unsigned long)arg);
}

int _hwc_device_get_margin_w(int disp)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    //As all the margin size of displays are the same , we can just returm
    //one of them.
    DisplayInfo   *psDisplayInfo = &Globctx->sunxiDisplay[0];
    return (int)(psDisplayInfo->displayPercentWT);
}

int _hwc_device_get_margin_h(int disp)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo   *psDisplayInfo = &Globctx->sunxiDisplay[0];
    return (int)(psDisplayInfo->displayPercentHT);
}

int _hwc_device_set_screenradio(int disp, int radioType)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo *psDisplayInfo = NULL;
    switch(radioType)
    {
    case SCREEN_AUTO:
    case SCREEN_FULL:
        for(int i = 0; i < NUMBEROFDISPLAY; i++)
        {
            psDisplayInfo = &(Globctx->sunxiDisplay[i]);
            psDisplayInfo->screenRadio = radioType;
        }
        break;
    default:
        break;
    }
    return 0;
}
