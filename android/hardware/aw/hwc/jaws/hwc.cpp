/*-------------------------------------------------------------------------

-------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------

-------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------

-------------------------------------------------------------------------*/
/*
 * Copyright (C) 2010 The Android Open Source Project
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

//#define LOG_NDEBUG 0

#include "hwc.h"
#include "cutils/properties.h"

static int Framecount = 0;
/*****************************************************************************/

static int hwc_device_open(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device);

static struct hw_module_methods_t hwc_module_methods = {
    open: hwc_device_open
};

hwc_module_t HAL_MODULE_INFO_SYM = {
    common: {
        tag: HARDWARE_MODULE_TAG,
        version_major: 1,
        version_minor: 0,
        id: HWC_HARDWARE_MODULE_ID,
        name: "Allwinner hwcomposer module",
        author: "The Homlet Display Team",
        methods: &hwc_module_methods,
        dso:NULL,
        reserved:{0},
    }
};

/*****************************************************************************/

static void dump_layer(hwc_layer_1_t const* l,int pipe,bool usedfe)
{
    static char const* compositionTypeName[] = {
                            "GLES",
                            "HWC",
                            "BACKGROUND",
                            "FB TARGET",
                            "UNKNOWN"};

    IMG_native_handle_t* handle = (IMG_native_handle_t*)l->handle;
    ALOGD(" %10s |  % 2d  | %s | %08x | %08x | %08x | %02x | %05x | %08x | [%7d,%7d,%7d,%7d] | [%5d,%5d,%5d,%5d] \n",
            compositionTypeName[l->compositionType],pipe,usedfe?"Yes":"No ",(unsigned int)l->handle,l->hints, l->flags, l->transform, l->blending, handle==0?0:handle->iFormat ,
            l->sourceCrop.left,
            l->sourceCrop.top,
            l->sourceCrop.right,
            l->sourceCrop.bottom,
            l->displayFrame.left,
            l->displayFrame.top,
            l->displayFrame.right,
            l->displayFrame.bottom);
}

static void dump_displays(size_t numDisplays,hwc_display_contents_1_t **displays)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    layer_list_t* layerTmp = NULL;
	if(Globctx->hwcdebug & LAYER_DUMP)
	{
        int disp, i,pipe;
        bool feused;
		for(disp = 0; disp < (int)numDisplays; disp++)
		{
		    hwc_display_contents_1_t *psDisplay = displays[disp];
            layerTmp = (layer_list_t*)Globctx->hwcLayerHead[disp].pre;
		    if(psDisplay)
		    {
		        ALOGD("\n\n\ndisp:%d  the framecount:%d \n    type    | pipe | fe  |  handle  |   hints  |   flags  | tr | blend |  format  |          source crop            |           frame            \n"
                                                         "------------+------+-----+----------+----------+----------+----+-------+----------+---------------------------------+--------------------------------\n", disp,Globctx->hwcFrameCount);
                for(i = 0; i < (int)psDisplay->numHwLayers; i++)
                {
                    hwc_layer_1_t *psLayer = &psDisplay->hwLayers[i];
                    if(psLayer->compositionType == 1 || psLayer->compositionType == 3)
                    {
                        if(layerTmp != (layer_list_t*)&Globctx->hwcLayerHead[disp])
                        {
                            pipe = layerTmp->pipe;
                            feused = layerTmp->usedfe;
                            layerTmp = (layer_list_t*)layerTmp->head.pre;
                        }
                    }else{
                        feused = 0;
                        pipe = -1;
                    }
                    dump_layer(psLayer,pipe,feused);
                }
            }
        }
    }
}

static int hwc_blank(struct hwc_composer_device_1* dev, int disp, int blank)
{
    return 0;
}

static int hwc_setParameter(struct hwc_composer_device_1* dev, int disp, int cmd,
            int para0, int para1, int para2)
{
    switch(disp){
    case HWC_DISPLAY_PRIMARY:
    case HWC_DISPLAY_EXTERNAL:
        break;
    default:
        ALOGE("Do not support setParemeter API of the virtual display device.");
        return -EINVAL;
    }
    int ret = -EINVAL;
    ALOGD("disp=%d,cmd=%d,para[%d,%d,%d]\n",disp, cmd, para0, para1, para2);
    switch(cmd)
    {
	    case DISPLAY_CMD_SET3DMODE:
		    ret = _hwc_device_set_3d_mode(disp, (__display_3d_mode)para0);
		    break;
	    case DISPLAY_CMD_SETBACKLIGHTMODE:
		    ret = _hwc_device_set_backlight_mode(disp, para0);
		    break;
	    case DISPLAY_CMD_SETBACKLIGHTDEMOMODE:
		    ret = _hwc_device_set_backlight_demomode(disp, para0);
		    break;
	    case DISPLAY_CMD_SETDISPLAYENHANCEMODE:
		    ret = _hwc_device_set_enhancemode(disp, para0);
		    break;
	    case DISPLAY_CMD_SETDISPLAYENHANCEDEMOMODE:
		    ret = _hwc_device_set_enhancedemomode(disp, para0);
		    break;
	    case DISPLAY_CMD_SETOUTPUTMODE:
		    ret = _hwc_device_set_output_mode(disp, para0, para1);
		    break;
        case DISPLAY_CMD_SETMARGIN:
            ret = _hwc_device_set_margin(disp, para0, para1);
            break;
        case DISPLAY_CMD_SETSATURATION:
            ret = _hwc_device_set_saturation(disp,para0);
            break;
        case DISPLAY_CMD_SETHUE:
            ret = _hwc_device_set_hue(disp,para0);
            break;
        case DISPLAY_CMD_SETBRIGHT:
            ret = _hwc_device_set_bright(disp,para0);
            break;
        case DISPLAY_CMD_SETCONTRAST:
            ret = _hwc_device_set_contrast(disp, para0);
            break;
        case DISPLAY_CMD_SETSCREENRADIO:
            ret = _hwc_device_set_screenradio(disp, para0);
            break;
        default:
            break;
    }
    return ret;
}


static int hwc_getParameter(struct hwc_composer_device_1* dev, int disp, int cmd,
            int para0, int para1)
{
    int ret = 0;

    switch(cmd)
    {
    case DISPLAY_CMD_GETOUTPUTTYPE:
	    ret = _hwc_device_get_output_type(disp);
	    break;
    case DISPLAY_CMD_GETOUTPUTMODE:
	    ret = _hwc_device_get_output_mode(disp);
	    break;
    case DISPLAY_CMD_ISSUPPORTHDMIMODE:
	    ret = _hwc_device_is_support_hdmi_mode(disp,para0);
	    break;
    case DISPLAY_CMD_GETSUPPORT3DMODE:
	    ret = _hwc_device_is_support_hdmi_mode(disp, DISP_TV_MOD_1080P_24HZ_3D_FP);
	    break;
    case DISPLAY_CMD_GETSATURATION:
	    ret = _hwc_device_get_saturation(disp);
	    break;
    case DISPLAY_CMD_GETHUE:
	    ret = _hwc_device_get_hue(disp);
	    break;
    case DISPLAY_CMD_GETBRIGHT:
	    ret = _hwc_device_get_bright(disp);
	    break;
    case DISPLAY_CMD_GETCONTRAST:
	    ret = _hwc_device_get_contrast(disp);
	    break;
    case DISPLAY_CMD_GETMARGIN_W:
	    ret = _hwc_device_get_margin_w(disp);
	    break;
    case DISPLAY_CMD_GETMARGIN_H:
	    ret = _hwc_device_get_margin_h(disp);
        break;
    case DISPLAY_CMD_GETDISPFPS:
        ret = 255;
        for(int i =0; i < NUMBEROFDISPLAY; i++){
            int modeFps = getInfoOfMode(gSunxiHwcDevice.sunxiDisplay[i].displayMode, REFRESHRAE);
            if(modeFps > 0 && ret > modeFps){
                ret = modeFps;
            }
        }
        break;
    default:
	    break;
    }
    return ret;
}

static inline void resetPipeInfo(hwcDevCntContext_t * ctx,int start,int end)
{
    while(start < end)
     {
        ctx->pipeRegion[start].left = 10000;
        ctx->pipeRegion[start].top = 10000;
        ctx->pipeRegion[start].right = 0;
        ctx->pipeRegion[start].bottom = 0;
        start++;
     }
}

static bool inline reCountPresent(DisplayInfo *psDisplayInfo )
{
    if((psDisplayInfo->displayType == DISP_OUTPUT_TYPE_HDMI
        && psDisplayInfo->current3DMode == DISPLAY_2D_ORIGINAL)
        || psDisplayInfo->displayType == DISP_OUTPUT_TYPE_TV)
    {
        psDisplayInfo->displayPercentW = psDisplayInfo->displayPercentWT;
        psDisplayInfo->displayPercentH = psDisplayInfo->displayPercentHT;
        if(psDisplayInfo->displayPercentW != 100 || psDisplayInfo->displayPercentH != 100)
        {
            return 1;
        }
    }
    else
    {
        psDisplayInfo->displayPercentW = 100;
        psDisplayInfo->displayPercentH = 100;
    }

    return 0;
}


static void
resetGlobDevice(SUNXI_hwcdev_context_t * Globctx)
{
    int i;
    setup_dispc_data_t* displayData=Globctx->pvPrivateData;
    Globctx->gloFEisUsedCnt = 0;

	for (i = 0; i < NUMBEROFDISPLAY; i++)
	{
		displayData->layer_num[i] = 0;
        memset(displayData->hConfigData,-1,12*sizeof(int));
        int j;
        for(j = 0; j < DISPLAY_MAX_LAYER_NUM; j++)
        {
            memset(&(displayData->layer_info[i][j]),0,sizeof(disp_layer_info));
            displayData->layer_info[i][j].mode = DISP_LAYER_WORK_MODE_NORMAL;
        }
        Globctx->hwcLayerHead[i].next = &(Globctx->hwcLayerHead[i]);
        Globctx->hwcLayerHead[i].pre = &(Globctx->hwcLayerHead[i]);
        if(Globctx->sunxiDisplay[i].virtualToHWDisplay != -1)
        {
            if(reCountPresent(&Globctx->sunxiDisplay[i]))
            {
                Globctx->gloFEisUsedCnt++;
            }
            if(Globctx->layer0usfe)
            {
                Globctx->gloFEisUsedCnt++;
            }
        }
	}
}

static void
resetLocalCnt(hwcDevCntContext_t * ctx,int usedfb)
{

    resetPipeInfo(ctx,0,NUMBEROFPIPE);

    ctx->feIsUsedCnt = 0;
    ctx->hwLayerCnt = 0;
    ctx->hwPipeUsedCnt = 0;
    ctx->usedFB = usedfb;
    ctx->fbLayerHead.next = &(ctx->fbLayerHead);
    ctx->fbLayerHead.pre = &(ctx->fbLayerHead);
}

int initAddLayerTail(head_list_t* layerHead,hwc_layer_1_t *psLayer, int order,int pipe,bool feused)
{

    int i=3;
    layer_list_t* layerTmp = NULL;
    while((i--)&&(layerTmp == NULL))
    {
        layerTmp=(layer_list_t* )calloc(1, sizeof(layer_list_t));
    }
    layerTmp->pslayer = psLayer;
    layerTmp->order = order;
    layerTmp->pipe = pipe;
    layerTmp->usedfe= feused;
    head_list_t *tmp = layerHead->next;
    while(tmp != layerHead)
    {
        head_list_t *tmp2=tmp->next;
        if(order > ((layer_list_t *)tmp)->order)
        {
            layerTmp->head.next = tmp;
            layerTmp->head.pre = tmp->pre;
            tmp->pre->next = &(layerTmp->head);
            tmp->pre = &(layerTmp->head);
            return 0;
        }
        tmp = tmp2;
    }
    layerTmp->head.next = tmp;
    layerTmp->head.pre = tmp->pre;
    tmp->pre->next = &(layerTmp->head);
    tmp->pre = &(layerTmp->head);
    return 0;
}

int freeLayerList(head_list_t * layerHead)
{
    head_list_t *head,*next;
    head = layerHead->next;
    while(head != layerHead)
    {
        next = head->next;
        free(head);
        head = next;
    }
    layerHead->next = layerHead;
    layerHead->pre = layerHead;
    return 0;
}

static inline  void resetLayerInfo(disp_layer_info* layerInfo)
{
    memset(layerInfo,0,sizeof(disp_layer_info));
    layerInfo->mode = DISP_LAYER_WORK_MODE_NORMAL;
}
static inline int cntOfLayer(SUNXI_hwcdev_context_t * ctx)
{
    int i=0, numOfLayer = 0;
    while(i<NUMBEROFDISPLAY)
    {
       numOfLayer += ctx->pvPrivateData->layer_num[i];
       i++;
    }
    return numOfLayer;
}

static unsigned int  cntOfLayerMem(hwc_layer_1_t *psLayer)
{
    unsigned int width;
    IMG_native_handle_t* handle = (IMG_native_handle_t*)psLayer->handle;
    if(handle->iFormat == HAL_PIXEL_FORMAT_YV12 || handle->iFormat == HAL_PIXEL_FORMAT_YCrCb_420_SP)
    {
       width = ALIGN(handle->iWidth, YV12_ALIGN);
    }else{
       width = ALIGN(handle->iWidth, HW_ALIGN);
    }

    return handle->uiBpp * width * handle->iHeight;

}


static void reset_layer_type(hwc_display_contents_1_t* displays)
{
    hwc_display_contents_1_t *list = displays;
    if (list && list->numHwLayers > 1)
    {
        unsigned int j = 0;
        for(j = 0; j < list->numHwLayers; j++)
        {
            if(list->hwLayers[j].compositionType != HWC_FRAMEBUFFER_TARGET)
            {
               list->hwLayers[j].compositionType = HWC_FRAMEBUFFER;
            }
        }
    }
}
static
int hwc_prepare(hwc_composer_device_1_t *dev, size_t numDisplays,
					   hwc_display_contents_1_t **displays)
{
	int forceSoftwareRendering = 0;
	hwc_display_contents_1_t *psDisplay;
	size_t disp, i, hwDisp=0;
	SUNXI_hwcdev_context_t *globctx = &gSunxiHwcDevice;
    hwcDevCntContext_t cntInfo;
    hwcDevCntContext_t *localctx = &cntInfo;
	int err = 0;
    resetGlobDevice(globctx);


    for(disp = 0; disp < numDisplays; disp++)
    {

        psDisplay = displays[disp];
        reset_layer_type(psDisplay);
        resetLocalCnt(localctx,0);

    	if(!psDisplay)
    	{
    		//ALOGV("%s: display[%d] was unexpectedly NULL",
    		//						__func__, disp);
    		continue;
    	}

        if(psDisplay->outbuf != NULL || psDisplay->outbufAcquireFenceFd != 0)
        {
            if (psDisplay->retireFenceFd >= 0)
            {
                close(psDisplay->retireFenceFd);
                psDisplay->retireFenceFd = -1;
            }
            if (psDisplay->outbuf != NULL)
            {
                psDisplay->outbuf = NULL;
            }
            if (psDisplay->outbufAcquireFenceFd >= 0)
            {
                close(psDisplay->outbufAcquireFenceFd);
                psDisplay->outbufAcquireFenceFd = -1;
            }
            //ALOGV("%s: Virtual displays are not supported",
    		//						__func__);
        }
        switch(disp){
        case HWC_DISPLAY_PRIMARY:
            for(i = 0; i < NUMBEROFDISPLAY; i++){
                if(globctx->sunxiDisplay[i].virtualToHWDisplay != INVALID_VALUE
                    && globctx->sunxiDisplay[i].virtualToHWDisplay == globctx->mainDisp){
                    hwDisp = i;
                    break;
                }
            }
            break;
        case HWC_DISPLAY_EXTERNAL:
            for(i = 0; i < NUMBEROFDISPLAY; i++){
                if(globctx->sunxiDisplay[i].virtualToHWDisplay != INVALID_VALUE
                    && globctx->sunxiDisplay[i].virtualToHWDisplay == globctx->secDisp){
                    hwDisp = i;
                    break;
                }
            }
            break;
        case HWC_DISPLAY_VIRTUAL:
        default:
            ALOGV("%s: Virtual displays are not supported", __func__);
            return -1;
        }
    	if(psDisplay->numHwLayers < 2)
    	{
    		ALOGV("%s: display[%d] numHwLayer:%d less then 2",
    								__func__, disp, psDisplay->numHwLayers);
            forceSoftwareRendering = 1;
    	}
        for(i = 0; i < psDisplay->numHwLayers; i++)
	    {
	    	hwc_layer_1_t *psLayer = &psDisplay->hwLayers[i];

	    	if(psLayer->handle == NULL && !(psLayer->flags & HWC_SKIP_LAYER))
	    	{
	    	    ALOGV("%s: handle is NULL", __func__);
	    		forceSoftwareRendering = 1;
	    		break;
	    	}
            if (psLayer->videoFormat == HAL_PIXEL_FORMAT_AW_FORCE_GPU)
            {
                forceSoftwareRendering = 1;
                break;
            }
        }
        int ret = 0;
        unsigned long arg[4] = {0};
        ret = ioctl(globctx->displayFd,DISP_CMD_HWC_GET_DISP_READY,(unsigned long)arg);
        if(ret == 0 || globctx->bDisplayReady == 0)
        {
            ALOGD("%s:Display is not ready yet!, ret=%d",__func__, ret);
            forceSoftwareRendering = 1;
        }
        globctx->bDisplayReady = ret;

    	if (forceSoftwareRendering)
    	{
           hwc_layer_1_t *psLayer = &psDisplay->hwLayers[psDisplay->numHwLayers-1];
           if(hwcTrytoAssignLayer(localctx, psLayer, hwDisp, 0) != ASSIGN_OK)
           {
                ALOGE("Use GPU composite FB failed ");
                continue;
           }
    	}else{
            int theFBCnt = 0;
            int setOrder = 0;
            unsigned int sizeOfMem = 0;
            HwcPipeAssignStatusType assignStatus;
            int needReAssignedLayer = 0;
            hwc_layer_1_t *psLayer;

ReAssignedLayer:

            reset_layer_type(psDisplay);
    	    for(i = 0; i < psDisplay->numHwLayers; i++)
    	    {
    		    psLayer = &psDisplay->hwLayers[i];
                if(i >= psDisplay->numHwLayers-1)
                {
                    if(localctx->usedFB || sizeOfMem > cntOfLayerMem(&psDisplay->hwLayers[psDisplay->numHwLayers-1]))
                    {
                        if(globctx->forceGPUComp)
                        {
                            reset_layer_type(psDisplay);
                            setOrder = 0;
                            freeLayerList(&localctx->fbLayerHead);
                            freeLayerList(&(globctx->hwcLayerHead[hwDisp]));
                            resetLocalCnt(localctx, ASSIGN_FB_PIPE);
                        }
                    }else{
                        break;
                    }
                }
                assignStatus = hwcTrytoAssignLayer(localctx,psLayer, hwDisp, setOrder);
                if(assignStatus == ASSIGN_NO_DISP)
                {
                    continue;
                }
    			if (assignStatus == ASSIGN_OK)
    			{
                    if(psLayer->compositionType == HWC_FRAMEBUFFER)
                    {
                        setOrder++;
                        psLayer->compositionType = HWC_OVERLAY;
                        if(globctx->forceGPUComp && !localctx->usedFB)
                        {
                            sizeOfMem += cntOfLayerMem(psLayer);
                        }
                    }
    			}else{
    			    if(needReAssignedLayer == 0)
    			    {
                        needReAssignedLayer++;
                        setOrder = 0;
                        sizeOfMem = 0;
                        globctx->gloFEisUsedCnt -= localctx->feIsUsedCnt;
                        freeLayerList(&(globctx->hwcLayerHead[hwDisp]));
                        freeLayerList(&localctx->fbLayerHead);
                        resetLocalCnt(localctx, ASSIGN_FB_PIPE);
                        goto ReAssignedLayer;
    			    }
    		    }
    	    }
            freeLayerList(&localctx->fbLayerHead);
        }

        for(i = 0; i < psDisplay->numHwLayers; i++)
        {
            hwc_layer_1_t *pslayer = &psDisplay->hwLayers[i];
            IMG_native_handle_t *handle = (IMG_native_handle_t *)pslayer->handle;
            if(NULL == handle)
            {
                continue;
            }
            if((HWC_FRAMEBUFFER == pslayer->compositionType)
              && ((HAL_PIXEL_FORMAT_AW_MB420 == pslayer->videoFormat)
                  || (HAL_PIXEL_FORMAT_AW_MB422 == pslayer->videoFormat)))
            {
                int phyaddress = ionGetAddr(handle->fd[0]);
                if(0 != phyaddress)
                {
                    _hwc_device_convert_mb_to_nv21(pslayer, phyaddress);
                }
            }
        } //for convert mb to nv21 when switch video

    }
    dump_displays(numDisplays, displays);
err_out:
	return err;
}

static bool checkScaleFormat(int format)
{

    switch(format)
    {
    case HAL_PIXEL_FORMAT_YV12:
	case HAL_PIXEL_FORMAT_YCrCb_420_SP:
    case HAL_PIXEL_FORMAT_AW_NV12:
        return 1;
    default:
        return 0;
    }
}

static int hwc_set(hwc_composer_device_1_t *dev,
        size_t numDisplays, hwc_display_contents_1_t** displays)
{
    int ret = 0;
	int releaseFenceFd = -1;
	size_t disp, i, hwDisp = 0;
	hwc_display_contents_1_t *psDisplay;
	SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    head_list_t *head,*next;
    int *fds;
    for(disp = 0 ; disp < numDisplays ; disp++)
    {

        psDisplay = displays[disp];
        if(!psDisplay)
    	{
    		//ALOGV("%s: display[%d] was unexpectedly NULL",
    		//						__func__, disp);
    		continue;
    	}
        switch(disp){
        case HWC_DISPLAY_PRIMARY:
            for(i = 0; i < NUMBEROFDISPLAY; i++){
                if(Globctx->sunxiDisplay[i].virtualToHWDisplay != INVALID_VALUE
                    && Globctx->sunxiDisplay[i].virtualToHWDisplay == Globctx->mainDisp){
                    hwDisp = i;
                    break;
                }
            }
            break;
        case HWC_DISPLAY_EXTERNAL:
            for(i = 0; i < NUMBEROFDISPLAY; i++){
                if(Globctx->sunxiDisplay[i].virtualToHWDisplay != INVALID_VALUE
                    && Globctx->sunxiDisplay[i].virtualToHWDisplay == Globctx->secDisp){
                    hwDisp = i;
                    break;
                }
            }
            break;
        case HWC_DISPLAY_VIRTUAL:
        default:
            return -1;
        }

        int fdCnt = 0;
        head = Globctx->hwcLayerHead[hwDisp].pre;
        while(head != &(Globctx->hwcLayerHead[hwDisp]))
        {
            hwc_layer_1_t *psLayer = ((layer_list_t *)head)->pslayer;
            fds = (int *)(Globctx->pvPrivateData->hConfigData);
            int pipe = ((layer_list_t *)head)->pipe;
            int order = ((layer_list_t *)head)->order;
            if(psLayer->acquireFenceFd >= 0)
            {
                *(fds+fdCnt) = psLayer->acquireFenceFd;//dup(psLayer->acquireFenceFd);
                fdCnt++;
            }
            if(hwcSetupLayer(Globctx,psLayer,order,hwDisp,pipe) == -1)
            {
                break;
            }
            head = head->pre;
        }
        freeLayerList(&(Globctx->hwcLayerHead[hwDisp]));

    }
    if(Globctx->detectError == 0)
    {
        unsigned long arg[4] = {0};
        arg[0] = 0;
        arg[1] = (unsigned int)(Globctx->pvPrivateData);
        releaseFenceFd = ioctl(Globctx->displayFd,DISP_CMD_HWC_COMMIT,(unsigned long)arg);
    }

	Globctx->hwcFrameCount++;
	for(disp = 0; disp < numDisplays; disp++)
	{
		psDisplay = displays[disp];
		if(!psDisplay)
		{
			//ALOGV("%s: display[%d] was unexpectedly NULL",
    		//	    					__func__, disp);
    		continue;
		}

		for(i=0 ; i<psDisplay->numHwLayers ; i++)
		{
            if(psDisplay->hwLayers[i].acquireFenceFd>=0)
            {
               close(psDisplay->hwLayers[i].acquireFenceFd);
               psDisplay->hwLayers[i].acquireFenceFd=-1;
             }
		    if((psDisplay->hwLayers[i].compositionType == HWC_OVERLAY) || ((psDisplay->hwLayers[i].compositionType == HWC_FRAMEBUFFER_TARGET)))
			{
				if(releaseFenceFd >= 0)
				{
					psDisplay->hwLayers[i].releaseFenceFd = dup(releaseFenceFd);
			    }else{
					    psDisplay->hwLayers[i].releaseFenceFd = -1;
				}
	        }else{
				psDisplay->hwLayers[i].releaseFenceFd = -1;
		    }
		}
    }
    if(releaseFenceFd >= 0)
    {
        close(releaseFenceFd);
	    releaseFenceFd = -1;
    }

    return ret;
}


static int hwc_eventControl(struct hwc_composer_device_1* dev, int disp,
            int event, int enabled){
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    unsigned long               arg[4]={0};
    for(int i = 0; i< NUMBEROFDISPLAY; i++){
        DisplayInfo *psDisplayInfo = &Globctx->sunxiDisplay[i];
        if(psDisplayInfo->virtualToHWDisplay == -1
            || psDisplayInfo->virtualToHWDisplay != Globctx->mainDisp){
            continue;
        }

        switch(event){
            case HWC_EVENT_VSYNC:
                arg[0] = psDisplayInfo->virtualToHWDisplay;
                arg[1] = !!enabled;
                ioctl(Globctx->displayFd, DISP_CMD_VSYNC_EVENT_EN, (unsigned long)arg);
                psDisplayInfo->vsyncEnable = (!!enabled);
                ALOGV("hwc   vsync: %d ",psDisplayInfo->vsyncEnable);
                return 0;
            default:
                return -EINVAL;
        }
    }
    return -EINVAL;
}

static void hwc_register_procs(struct hwc_composer_device_1* dev,
            hwc_procs_t const* procs){
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    Globctx->psHwcProcs = const_cast<hwc_procs_t *>(procs);
}


static int hwc_getDisplayConfigs(struct hwc_composer_device_1 *dev,
        int disp, uint32_t *configs, size_t *numConfigs){

	int err = -EINVAL;
	SUNXI_hwcdev_context_t *Globctx= &gSunxiHwcDevice;
    switch(disp){
    case HWC_DISPLAY_PRIMARY:
        if(numConfigs){
    		*numConfigs = 1;
        }
    	if(configs){
            configs[0] = 0;
        }
        break;
    case HWC_DISPLAY_EXTERNAL:
        if(Globctx->secDisp != -1){
            if(numConfigs){
        		*numConfigs = 1;
            }
        	if(configs){
        		configs[0] = 0;
            }
        }else{
            *numConfigs = 0;
	        goto err_out;
        }
        break;
    default:
        goto err_out;
    }

	err = 0;
err_out:
	return err;
}


static int32_t getHWAttribute(const uint32_t attribute,
         DisplayInfo *psDisplayInfo){
	SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    switch(attribute){
    case HWC_DISPLAY_VSYNC_PERIOD:
        return psDisplayInfo->displayVsyncP;
    case HWC_DISPLAY_WIDTH:
        if(psDisplayInfo->virtualToHWDisplay == Globctx->mainDisp){
            return Globctx->mainDispWidth;
        }else if(psDisplayInfo->virtualToHWDisplay == Globctx->secDisp){
            return Globctx->secDispWidth;
        }
        break;
    case HWC_DISPLAY_HEIGHT:
        if(psDisplayInfo->virtualToHWDisplay == Globctx->mainDisp){
            return Globctx->mainDispHeight;
        }else if(psDisplayInfo->virtualToHWDisplay == Globctx->secDisp){
            return Globctx->secDispHeight;
        }
        break;
    case HWC_DISPLAY_DPI_X:
        return psDisplayInfo->displayDPI_X;
    case HWC_DISPLAY_DPI_Y:
        return psDisplayInfo->displayDPI_Y;
    case HWC_DISPLAY_IS_SECURE:
		return 1;
    default:
        ALOGE("unknown display attribute %u", attribute);
        return -EINVAL;
    }
    return -EINVAL;
}

static int hwc_getDisplayAttributes(struct hwc_composer_device_1 *dev,
        int disp, uint32_t config, const uint32_t *attributes, int32_t *values)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo *psDisplayInfo = NULL;
    int i;
    for(i = 0; i < NUMBEROFDISPLAY; i++){
        psDisplayInfo = &Globctx->sunxiDisplay[i];
        if(psDisplayInfo->virtualToHWDisplay == -1){
            continue;
        }
        if((psDisplayInfo->virtualToHWDisplay == Globctx->mainDisp && disp == HWC_DISPLAY_PRIMARY)
            || (psDisplayInfo->virtualToHWDisplay == Globctx->secDisp && disp == HWC_DISPLAY_EXTERNAL)){
            psDisplayInfo = &Globctx->sunxiDisplay[i];
            break;
        }
    }
    if(i == NUMBEROFDISPLAY){
        ALOGE("No hareware display ");
        return -EINVAL;
    }

    for (int i = 0; attributes[i] != HWC_DISPLAY_NO_ATTRIBUTE; i++)
    {
        values[i] = getHWAttribute(attributes[i], psDisplayInfo);
    }
    return 0;
}

static int hwc_device_close(struct hw_device_t *dev)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    hwcDestroyDevice(Globctx);
    return 0;
}

/*****************************************************************************/

static int hwc_device_open(const struct hw_module_t* module, const char* name,
        struct hw_device_t** device)
{
	hwc_composer_device_1_t *psHwcDevice;
	hw_device_t *psHwDevice;
	int err = 0;

    if (strcmp(name, HWC_HARDWARE_COMPOSER))
    {
        return -EINVAL;
    }

	psHwcDevice = (hwc_composer_device_1_t *)malloc(sizeof(hwc_composer_device_1_t));
	if(!psHwcDevice)
	{
		ALOGD("%s: Failed to allocate memory", __func__);
		return -ENOMEM;
	}

	memset(psHwcDevice, 0, sizeof(hwc_composer_device_1_t));
    psHwDevice = (hw_device_t *)psHwcDevice;

    psHwcDevice->common.tag      = HARDWARE_DEVICE_TAG;
    psHwcDevice->common.version  = HWC_DEVICE_API_VERSION_1_1;
    psHwcDevice->common.module   = const_cast<hw_module_t*>(module);
    psHwcDevice->common.close    = hwc_device_close;

    psHwcDevice->prepare         = hwc_prepare;
    psHwcDevice->set             = hwc_set;
    psHwcDevice->setDisplayParameter    = hwc_setParameter;
    psHwcDevice->getDisplayParameter    = hwc_getParameter;
    psHwcDevice->registerProcs   = hwc_register_procs;
    psHwcDevice->eventControl	= hwc_eventControl;
	psHwcDevice->blank			= hwc_blank;
	psHwcDevice->getDisplayConfigs = hwc_getDisplayConfigs;
	psHwcDevice->getDisplayAttributes = hwc_getDisplayAttributes;

    *device = psHwDevice;

	hwcCreateDevice();

    return err;
}


