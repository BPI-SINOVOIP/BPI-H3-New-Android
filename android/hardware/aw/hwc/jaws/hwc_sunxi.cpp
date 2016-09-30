/*-------------------------------------------------------------------------

-------------------------------------------------------------------------*/
/*-------------------------------------------------------------------------

-------------------------------------------------------------------------*/
/*************************************************************************/ /*!
@Copyright      Copyright (c) Imagination Technologies Ltd. All Rights Reserved
@License        Strictly Confidential.
*/ /**************************************************************************/

#include "hwc.h"
// PHY_OFFSET is 0x20000000 at A80 platform
#define PHY_OFFSET 0x20000000
//#define LOG_NDEBUG 0
#define MARGIN_DEFAULT_PERCENT_WIDTH 95
#define MARGIN_DEFAULT_PERCENT_HEIGHT 95

SUNXI_hwcdev_context_t gSunxiHwcDevice;

static inline int hwcUsageSW(IMG_native_handle_t *psHandle)
{
	return psHandle->usage & (GRALLOC_USAGE_SW_READ_OFTEN |
							  GRALLOC_USAGE_SW_WRITE_OFTEN);
}

static inline int hwcUsageSWwrite(IMG_native_handle_t *psHandle)
{
	return psHandle->usage & GRALLOC_USAGE_SW_WRITE_OFTEN;
}

static inline int hwcUsageProtected(IMG_native_handle_t *psHandle)
{
	return psHandle->usage & GRALLOC_USAGE_PROTECTED;
}


static inline int hwcValidFormat(int format)
{
    switch(format)
    {
    case HAL_PIXEL_FORMAT_RGBA_8888:
    case HAL_PIXEL_FORMAT_RGBX_8888:
    case HAL_PIXEL_FORMAT_RGB_888:
    case HAL_PIXEL_FORMAT_RGB_565:
    case HAL_PIXEL_FORMAT_BGRA_8888:
    case HAL_PIXEL_FORMAT_sRGB_A_8888:
    case HAL_PIXEL_FORMAT_sRGB_X_8888:
    case HAL_PIXEL_FORMAT_YV12:
	case HAL_PIXEL_FORMAT_YCrCb_420_SP:
    case HAL_PIXEL_FORMAT_BGRX_8888:
        return 1;
    default:
        return 0;
    }
}

static inline int hwcisBlended(hwc_layer_1_t* psLayer)
{
	return (psLayer->blending != HWC_BLENDING_NONE);
}

static inline int hwcisPremult(hwc_layer_1_t* psLayer)
{
    return (psLayer->blending == HWC_BLENDING_PREMULT);
}

static void inline calculateFactor(DisplayInfo *psDisplayInfo,float *XWidthFactor, float *XHighetfactor)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    float widthFactor = (float)psDisplayInfo->displayPercentW / 100;
    float highetfactor = (float)psDisplayInfo->displayPercentH / 100;
    if(psDisplayInfo->virtualToHWDisplay == Globctx->mainDisp){
        if(Globctx->mainDispWidth && Globctx->mainDispHeight)
        {
            widthFactor = (float)psDisplayInfo->varDisplayWidth / Globctx->mainDispWidth * psDisplayInfo->displayPercentW / 100;
            highetfactor = (float)psDisplayInfo->varDisplayHeight / Globctx->mainDispHeight * psDisplayInfo->displayPercentH / 100;
        }

        *XWidthFactor = widthFactor;
        *XHighetfactor = highetfactor;

    }else if(psDisplayInfo->virtualToHWDisplay == Globctx->secDisp){
        *XWidthFactor = 1.0f;
        *XHighetfactor = 1.0f;
    }

}


static int hwcisScaled(DisplayInfo *psDisplayInfo, hwc_layer_1_t *layer)
{
    float XWidthFactor = 1;
    float XHighetfactor = 1;

    calculateFactor(psDisplayInfo, &XWidthFactor, &XHighetfactor);

    int w = layer->sourceCrop.right - layer->sourceCrop.left;
    int h = layer->sourceCrop.bottom - layer->sourceCrop.top;

    if (layer->transform & HWC_TRANSFORM_ROT_90)
    {
        int tmp = w;
        w = h;
        h = tmp;
    }

    return ((layer->displayFrame.right - layer->displayFrame.left) * XWidthFactor != w)
        || ((layer->displayFrame.bottom - layer->displayFrame.top) * XHighetfactor != h);
}

static int hwcisValidLayer(hwc_layer_1_t *layer)
{
    IMG_native_handle_t *handle = (IMG_native_handle_t *)layer->handle;

    if ((layer->flags & HWC_SKIP_LAYER))
    {
        return 0;
    }

    if (!hwcValidFormat(handle->iFormat))
    {
        return 0;
    }

    if (layer->compositionType == HWC_BACKGROUND)
    {
        return 0;
    }
    if(layer->transform)
    {
        return 0;
    }

    return 1;
}

 int hwcTwoRegionIntersect(hwc_rect_t *rect0, hwc_rect_t *rect1)
{
    int mid_x0, mid_y0, mid_x1, mid_y1;
    int mid_diff_x, mid_diff_y;
    int sum_width, sum_height;

    mid_x0 = (rect0->right + rect0->left)/2;
    mid_y0 = (rect0->bottom + rect0->top)/2;
    mid_x1 = (rect1->right + rect1->left)/2;
    mid_y1 = (rect1->bottom + rect1->top)/2;

    mid_diff_x = (mid_x0 >= mid_x1)? (mid_x0 - mid_x1):(mid_x1 - mid_x0);
    mid_diff_y = (mid_y0 >= mid_y1)? (mid_y0 - mid_y1):(mid_y1 - mid_y0);

    sum_width = (rect0->right - rect0->left) + (rect1->right - rect1->left);
    sum_height = (rect0->bottom - rect0->top) + (rect1->bottom - rect1->top);

    if(mid_diff_x < (sum_width/2) && mid_diff_y < (sum_height/2))
    {
        return 1;//return 1 is intersect
    }

    return 0;
}
static inline int  hwcInRegion(hwc_rect_t *rectUp, hwc_rect_t *rectDw)
{
    return ((rectDw->left <= rectUp->left)&&(rectDw->right > rectUp->right) && (rectDw->top < rectUp->top)&&(rectDw->bottom > rectUp->bottom));
}

static int hwcRegionMerge(hwc_rect_t *rect_from, hwc_rect_t *rect1_to, int bound_width, int bound_height)
{
    if(rect_from->left < rect1_to->left)
	{
		rect1_to->left = (rect_from->left<0)?0:rect_from->left;
	}
	if(rect_from->right > rect1_to->right)
	{
		rect1_to->right = (rect_from->right>bound_width)?bound_width:rect_from->right;
	}
	if(rect_from->top < rect1_to->top)
	{
		rect1_to->top = (rect_from->top<0)?0:rect_from->top;
	}
	if(rect_from->bottom > rect1_to->bottom)
	{
		rect1_to->bottom = (rect_from->bottom>bound_height)?bound_height:rect_from->bottom;
	}

    return 1;
}

static int hwcFeUseAble(DisplayInfo   *psDisplayInfo,hwc_layer_1_t * psLayer)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    float XWidthFactor = 1;
    float XHighetfactor = 1;

    calculateFactor(psDisplayInfo, &XWidthFactor, &XHighetfactor);

	int src_w = psLayer->sourceCrop.right - psLayer->sourceCrop.left;
	int src_h = psLayer->sourceCrop.bottom - psLayer->sourceCrop.top;
	int dst_w = (int)(psLayer->displayFrame.right * XWidthFactor + 0.5) - (int)(psLayer->displayFrame.left * XWidthFactor + 0.5);
	int dst_h = (int)(psLayer->displayFrame.bottom * XHighetfactor +0.5) - (int)(psLayer->displayFrame.top *XHighetfactor +0.5);
	float efficience = 0.8;
	float fe_clk = Globctx->fe_clk;
	int de_fps = Globctx->de_fps;

	switch(psDisplayInfo->current3DMode){
	case DISPLAY_2D_LEFT:
		src_w = src_w >> 1;
		break;
	case DISPLAY_2D_TOP:
		src_h = src_h >> 1;
		break;
	default:
		break;
	}
	float scale_factor_w = src_w/dst_w ;
	float scale_factor_h = src_h/dst_h ;

	float fe_pro_w = (scale_factor_w >= 1)? src_w : dst_w;
	float fe_pro_h = (scale_factor_h >= 1)? src_h : dst_h;

	float required_fe_clk = (fe_pro_w * fe_pro_h)/(dst_w * dst_h)*(psDisplayInfo->varDisplayWidth * psDisplayInfo->varDisplayHeight * de_fps)/efficience;
    // must THK thether the small initdisplay  and  the biggest display  can use fe?(1280*720 ---> 3840 * 2160  ---622080000   3840 * 2160 --->1280 *720  cann't....  error,so just can surpport the 1080p screen )
	if(required_fe_clk > fe_clk) {
		return 0;//cann't
	} else {
		return 1;//can
	}
}


int check_X_FB(hwc_layer_1_t *psLayer,head_list_t *fbLayerHead)
{

    head_list_t *head,*next;
    head = fbLayerHead->next;
    hwc_layer_1_t *tmplayer;
    while(head != fbLayerHead)
    {
        next = head->next;
        tmplayer = ((layer_list_t *)(head))->pslayer;
        if(hwcTwoRegionIntersect(&tmplayer->displayFrame, &psLayer->displayFrame))
        {
            return 1;
        }
        head = next;
    }
    return 0;
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



HwcPipeAssignStatusType
hwcTrytoAssignLayer(hwcDevCntContext_t *localctx,hwc_layer_1_t *psLayer, size_t disp,int zOrder)
{

    IMG_native_handle_t* handle = (IMG_native_handle_t*)psLayer->handle;
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    DisplayInfo   *psDisplayInfo = &Globctx->sunxiDisplay[disp];
    bool feused = 0;
    if(psDisplayInfo->virtualToHWDisplay == -1)
    {
         ALOGE("Display[%d]  No Display",disp);
         return ASSIGN_NO_DISP;
    }

    if(psLayer->compositionType == HWC_FRAMEBUFFER_TARGET )
    {

        if(handle != NULL)
        {
            if(hwcTwoRegionIntersect(&localctx->pipeRegion[localctx->hwPipeUsedCnt], &psLayer->displayFrame))
            {
                localctx->hwPipeUsedCnt++;
            }
            feused = hwcisScaled(psDisplayInfo,psLayer);
            ALOGV("HwLayerCnt:%d   HwLayerNum:%d ",localctx->hwLayerCnt,psDisplayInfo->hwLayerNum );
            goto assign_ok;
        }else{
            ALOGV("%s:We have a framebuffer MULL  Handle",__func__);
            return ASSIGN_FAILED;
        }
    }

    ALOGV("%s: handle=%x", __func__, handle);
    if(handle == NULL || (handle->usage  & GRALLOC_USAGE_PRIVATE_3))
    {
        ALOGV("%s:not continuous Memory", __func__);
        localctx->usedFB = localctx->usedFB|ASSIGN_FAILED;
	    goto assign_failed;
    }

	if(hwcUsageProtected(handle) && (psDisplayInfo->displayType == DISP_OUTPUT_TYPE_HDMI))
	{
        ALOGV("%s:HDMI Protected", __func__);
        localctx->usedFB = localctx->usedFB|ASSIGN_FAILED;
	    goto assign_failed;
	}

    if(!hwcisValidLayer(psLayer))
    {
        ALOGV("%s:HwcisValidLayer:0x%08x", __func__,handle->iFormat);
        localctx->usedFB = localctx->usedFB|ASSIGN_FAILED;
        goto assign_failed;
    }

    if(hwcUsageSW(handle) && !checkScaleFormat(handle->iFormat))
    {
        ALOGV("not video  and   GRALLOC_USAGE_SW_WRITE_OFTEN");
        localctx->usedFB = localctx->usedFB|ASSIGN_FAILED;
        goto assign_failed;
    }

    if((localctx->hwLayerCnt) >= psDisplayInfo->hwLayerNum - !!localctx->usedFB)
    {
        ALOGV("Have too manly HwLayer:disp:%d HwLayerCnt:%d   HwLayerNum:%d ",disp,Localctx->HwLayerCnt,PsDisplayInfo->HwLayerNum);
        localctx->usedFB = localctx->usedFB|ASSIGN_NOHWCLAYER;
        goto assign_failed;
    }

    if(hwcisBlended(psLayer) || hwcisPremult(psLayer))
    {
        if(hwcisScaled(psDisplayInfo,psLayer))
	    {
            if(checkScaleFormat(handle->iFormat))
            {
		        if(Globctx->gloFEisUsedCnt < NUMBEROFDISPLAY )
		        {
			        if(hwcFeUseAble(psDisplayInfo,psLayer))
			        {
			            localctx->feIsUsedCnt++;
                        Globctx->gloFEisUsedCnt++;
                        feused =1;
			        }else{
			            ALOGV("%s:have   fe can not used", __func__);
			            goto assign_failed;
			        }
		        }else{
			        ALOGV("%s:No enough  fe", __func__);
			        goto assign_failed;
		        }
            }else{
                    ALOGV("%s:not support alpha scale layer", __func__);
			        goto assign_failed;
            }
        }

        if(localctx->usedFB)
        {
            if(check_X_FB(psLayer,&localctx->fbLayerHead))
            {
                if(feused)
                {
                    localctx->feIsUsedCnt--;
                    Globctx->gloFEisUsedCnt--;
                }
                goto assign_failed;
            }
        }

        if(hwcTwoRegionIntersect(&localctx->pipeRegion[localctx->hwPipeUsedCnt], &psLayer->displayFrame))
        {
            if(localctx->hwPipeUsedCnt < psDisplayInfo->hwPipeNum - !!localctx->usedFB -1)
            {
                localctx->hwPipeUsedCnt++;
            }
            else
            {
                ALOGV("%s:No enough HwPipe", __func__);
                if(feused)
                {
                    localctx->feIsUsedCnt--;
                    Globctx->gloFEisUsedCnt--;
                }
			    goto assign_failed;
            }
        }
    }else if(hwcisScaled(psDisplayInfo,psLayer) || checkScaleFormat(handle->iFormat))

    {
        if(checkScaleFormat(handle->iFormat))
        {
		    if(Globctx->gloFEisUsedCnt < NUMBEROFDISPLAY )
		    {
			    if(hwcFeUseAble(psDisplayInfo,psLayer))
			    {
			        localctx->feIsUsedCnt++;
                    Globctx->gloFEisUsedCnt++;
                    feused = 1;
			    }
                else
                {
			        ALOGV("%s:fe can not used", __func__);
			        goto assign_failed;
			    }
		    }
            else
            {
			    ALOGV("%s:not enough de fe", __func__);
			    goto assign_failed;
		    }
        }
        else
        {
            ALOGV("%s:not support scale layer", __func__);
			goto assign_failed;
        }
    }
    if(localctx->hwPipeUsedCnt == psDisplayInfo->hwPipeNum - 1 && localctx->usedFB)
    {
        if(feused)
        {
            localctx->feIsUsedCnt--;
            Globctx->gloFEisUsedCnt--;
        }
        goto assign_failed;
    }
    hwcRegionMerge(&psLayer->displayFrame,&localctx->pipeRegion[localctx->hwPipeUsedCnt],psDisplayInfo->varDisplayWidth,psDisplayInfo->varDisplayHeight);

    if(localctx->usedFB && check_X_FB(psLayer, &localctx->fbLayerHead))
    {
        psLayer->hints |= HWC_HINT_CLEAR_FB;
    }

assign_ok:
    if(localctx->hwLayerCnt == 0 && Globctx->layer0usfe)
    {
        feused = 1;
    }
    initAddLayerTail(&(Globctx->hwcLayerHead[disp]),psLayer, zOrder,localctx->hwPipeUsedCnt,feused);
    if(localctx->hwLayerCnt == 0 && Globctx->layer0usfe)
    {
        localctx->hwPipeUsedCnt++;
    }
    localctx->hwLayerCnt++;
	return ASSIGN_OK;

assign_failed:

    initAddLayerTail(&localctx->fbLayerHead, psLayer, zOrder, 0,0);
    return ASSIGN_FAILED;
}


void recomputeForPlatform(int screenRadio, disp_layer_info *layer_info, IMG_native_handle_t *handle){
    if(!checkScaleFormat(handle->iFormat)){
        return;
    }
    switch(screenRadio)
    {
    case SCREEN_AUTO:{
        float radio = ((float)(layer_info->fb.src_win.width)) / layer_info->fb.src_win.height;
        float scn_radio = ((float)layer_info->screen_win.width) / layer_info->screen_win.height;
        int tmp = 0, div;
        if(radio >= scn_radio)
        {
            /* change screen window, keep x and width as the same, modified the y and height.
            *                                 *************************
            * ************************        *                       *       *************************
            * *                      *        *                       *       *                       *
            * *        layer         *  --->  *      screen window    * ----> *     screen window     *
            * *                      *        *                       *       *                       *
            * ************************        *                       *       *************************
            *                                 *************************
            */
            tmp = layer_info->screen_win.width * layer_info->fb.src_win.height / layer_info->fb.src_win.width;
            div = layer_info->screen_win.height - tmp;
            if(div >= 0)
            {//we must check the div to be positive.
                layer_info->screen_win.y += div/2;
                layer_info->screen_win.height = tmp;
            }
        }
        else
        {
            /* or keep y and height as the same, modified the x and width.
            *      *******        *************************       ********
            *      *     *        *                       *       *      *
            *      *     *        *                       *       *      *
            *      *layer*  --->  *      screen window    * ----> *screen*
            *      *     *        *                       *       *window*
            *      *     *        *                       *       *      *
            *      *******        *************************       ********
            */
            tmp = layer_info->screen_win.height * layer_info->fb.src_win.width / layer_info->fb.src_win.height;
            div = layer_info->screen_win.width - tmp;
            if(div >= 0)
            {
                layer_info->screen_win.x += div/2;
                layer_info->screen_win.width = tmp;
            }
        }
        }
        break;
    case SCREEN_FULL:
        break;
    default:
        break;
    }
}

static int calc_point_byPercent(const unsigned char percent, const int middle_point, const int src_point)
{
    int condition = (src_point > middle_point) ? 1 : 0;
    int length = condition ? (src_point - middle_point) : (middle_point - src_point);
    length = length * percent / 100;
    return condition ? (middle_point + length) : (middle_point - length);
}

int hwcSetupLayer(SUNXI_hwcdev_context_t *Globctx, hwc_layer_1_t *layer,int zOrder, size_t disp,int pipe)
{
    disp_layer_info *layer_info;
    DisplayInfo   *psDisplayInfo = &Globctx->sunxiDisplay[disp];
    if(psDisplayInfo->virtualToHWDisplay == -1)
    {
        ALOGE("Display[%d]  No Display",disp);
        return -1;
    }

    layer_info = &(Globctx->pvPrivateData->layer_info[psDisplayInfo->virtualToHWDisplay][zOrder]);
    Globctx->pvPrivateData->layer_num[psDisplayInfo->virtualToHWDisplay]++;
    IMG_native_handle_t *handle = (IMG_native_handle_t *)layer->handle;
    switch(handle->iFormat)
    {
        case HAL_PIXEL_FORMAT_RGBA_8888:
            layer_info->fb.format = DISP_FORMAT_ABGR_8888;
            break;
        case HAL_PIXEL_FORMAT_RGBX_8888:
            layer_info->fb.format = DISP_FORMAT_XBGR_8888;
            break;
        case HAL_PIXEL_FORMAT_RGB_888:
            layer_info->fb.format = DISP_FORMAT_BGR_888;
            break;
        case HAL_PIXEL_FORMAT_RGB_565:
            layer_info->fb.format = DISP_FORMAT_RGB_565;
            break;
        case HAL_PIXEL_FORMAT_BGRA_8888:
            layer_info->fb.format = DISP_FORMAT_ARGB_8888;
            break;
		case HAL_PIXEL_FORMAT_BGRX_8888:
			layer_info->fb.format = DISP_FORMAT_XRGB_8888;
			break;
        case HAL_PIXEL_FORMAT_YV12:
            layer_info->fb.format = DISP_FORMAT_YUV420_P;
            break;
        case HAL_PIXEL_FORMAT_YCrCb_420_SP:
            layer_info->fb.format = DISP_FORMAT_YUV420_SP_VUVU;
            break;
        case HAL_PIXEL_FORMAT_AW_NV12:
            layer_info->fb.format = DISP_FORMAT_YUV420_SP_UVUV;
            break;
        default:
            ALOGE("Not support format 0x%x in %s", handle->iFormat, __FUNCTION__);

            goto ERR;
    }

    layer_info->fb.interlace = layer->interlace ? 1 : 0;
    layer_info->fb.top_field_first = layer->topFieldFirst ? 1 : 0;

    if(hwcisBlended(layer)){
        layer_info->alpha_mode  = 2;
        layer_info->alpha_value = layer->planeAlpha;
    }else{
        layer_info->alpha_mode  = 1;
        layer_info->alpha_value = 0xff;
    }

    if(layer->blending == HWC_BLENDING_PREMULT)
    {
        layer_info->fb.pre_multiply = 1;
    }
	if((psDisplayInfo->displayType == DISP_OUTPUT_TYPE_HDMI)
        && (psDisplayInfo->current3DMode == DISPLAY_3D_LEFT_RIGHT_HDMI
        ||psDisplayInfo->current3DMode == DISPLAY_3D_TOP_BOTTOM_HDMI) )
	{
		layer_info->ck_enable = 0;
	}else{
		layer_info->ck_enable = 0;
	}
    if(handle->iFormat==HAL_PIXEL_FORMAT_YV12||handle->iFormat==HAL_PIXEL_FORMAT_YCrCb_420_SP)
    {
        layer_info->fb.size.width= ALIGN(handle->iWidth, YV12_ALIGN);
    }else{
        layer_info->fb.size.width = ALIGN(handle->iWidth, HW_ALIGN);
    }
    layer_info->fb.size.height = handle->iHeight;

    switch(layer->videoFormat)
    {
        case HAL_PIXEL_FORMAT_AW_MB420:
            layer_info->fb.format = DISP_FORMAT_YUV420_SP_TILE_UVUV;
            layer_info->fb.addr[0] = (unsigned int)(layer->videoAddr0) + PHY_OFFSET;
            layer_info->fb.addr[1] = (unsigned int)(layer->videoAddr1) + PHY_OFFSET;
            layer_info->fb.addr[2] = (unsigned int)(layer->videoAddr2) + PHY_OFFSET;
            layer_info->fb.trd_right_addr[0] = (unsigned int)(layer->videoAddr3) + PHY_OFFSET;
            layer_info->fb.trd_right_addr[1] = (unsigned int)(layer->videoAddr4) + PHY_OFFSET;
            layer_info->fb.trd_right_addr[2] = (unsigned int)(layer->videoAddr5) + PHY_OFFSET;
            break;
        case HAL_PIXEL_FORMAT_AW_MB411:
            layer_info->fb.format = DISP_FORMAT_YUV411_SP_TILE_UVUV;
            layer_info->fb.addr[0] = (unsigned int)(layer->videoAddr0) + PHY_OFFSET;
            layer_info->fb.addr[1] = (unsigned int)(layer->videoAddr1) + PHY_OFFSET;
            break;
        case HAL_PIXEL_FORMAT_AW_MB422:
            layer_info->fb.format = DISP_FORMAT_YUV422_SP_TILE_UVUV;
            layer_info->fb.addr[0] = (unsigned int)(layer->videoAddr0) + PHY_OFFSET;
            layer_info->fb.addr[1] = (unsigned int)(layer->videoAddr1) + PHY_OFFSET;
            break;
        case HAL_PIXEL_FORMAT_AW_YUV_PLANNER420:		// YU12
            layer_info->fb.format = DISP_FORMAT_YUV420_P;
            layer_info->fb.addr[0] = (unsigned int)(layer->videoAddr0) + PHY_OFFSET;
            layer_info->fb.addr[1] = (unsigned int)(layer->videoAddr1) + PHY_OFFSET;
            layer_info->fb.addr[2] = (unsigned int)(layer->videoAddr2) + PHY_OFFSET;
            break;
        default:
		{
            layer_info->fb.addr[0] = ionGetAddr(handle->fd[0]);

		    if(layer_info->fb.addr[0] == 0)
		    {
		         goto ERR;
		    }

		    if(layer_info->fb.format == DISP_FORMAT_YUV420_P)			// YV12
		    {
		        layer_info->fb.addr[2] = layer_info->fb.addr[0] +
		                                layer_info->fb.size.width * layer_info->fb.size.height;
		        layer_info->fb.addr[1] = layer_info->fb.addr[2] +
		                                (layer_info->fb.size.width * layer_info->fb.size.height)/4;
		    }else if(layer_info->fb.format == DISP_FORMAT_YUV420_SP_VUVU
		        || layer_info->fb.format == DISP_FORMAT_YUV420_SP_UVUV)	// NV12/NV21
		    {
		        layer_info->fb.addr[1] = layer_info->fb.addr[0] +
		            layer_info->fb.size.height * layer_info->fb.size.width;
		    }
        }
    }

    layer_info->fb.src_win.x = layer->sourceCrop.left;
    layer_info->fb.src_win.y = layer->sourceCrop.top;
    layer_info->fb.src_win.width = layer->sourceCrop.right - layer->sourceCrop.left;
    layer_info->fb.src_win.height = layer->sourceCrop.bottom - layer->sourceCrop.top;
#if 0
	//the caculation is fault
    layer_info->screen_win.x = (int)(layer->displayFrame.left * XWidthFactor + 0.5) + (PsDisplayInfo->VarDisplayWidth * (100 - PsDisplayInfo->DisplayPercentW) / 100 / 2);
    layer_info->screen_win.y = (int)(layer->displayFrame.top *XHighetfactor +0.5) + (PsDisplayInfo->VarDisplayHeight * (100 - PsDisplayInfo->DisplayPercentH) / 100 / 2);
    layer_info->screen_win.width = (int)((layer->displayFrame.right - layer->displayFrame.left )* XWidthFactor + 0.5);
    layer_info->screen_win.height = (int)((layer->displayFrame.bottom - layer->displayFrame.top )*XHighetfactor +0.5);
#else
	int midPoint;
    int bufWidth;
    int bufHeight;
    if(psDisplayInfo->virtualToHWDisplay == Globctx->mainDisp){
        bufWidth = Globctx->mainDispWidth;
        bufHeight = Globctx->mainDispHeight;
    }else if(psDisplayInfo->virtualToHWDisplay == Globctx->secDisp){
        bufWidth = Globctx->secDispWidth;
        bufHeight = Globctx->secDispHeight;
    }
    midPoint = bufWidth >> 1;
    layer_info->screen_win.x = calc_point_byPercent(psDisplayInfo->displayPercentW, midPoint, layer->displayFrame.left)
		* psDisplayInfo->varDisplayWidth / bufWidth;
    layer_info->screen_win.width = calc_point_byPercent(psDisplayInfo->displayPercentW, midPoint, layer->displayFrame.right)
		* psDisplayInfo->varDisplayWidth / bufWidth;
    layer_info->screen_win.width -= layer_info->screen_win.x;
    midPoint = bufHeight >> 1;
    layer_info->screen_win.y = calc_point_byPercent(psDisplayInfo->displayPercentH, midPoint, layer->displayFrame.top)
		* psDisplayInfo->varDisplayHeight / bufHeight;
    layer_info->screen_win.height = calc_point_byPercent(psDisplayInfo->displayPercentH, midPoint, layer->displayFrame.bottom)
		* psDisplayInfo->varDisplayHeight / bufHeight;
    layer_info->screen_win.height -= layer_info->screen_win.y;
#endif
    //add for homlet product
    recomputeForPlatform(psDisplayInfo->screenRadio, layer_info, handle);
    ALOGV("handle=%x, src_win=(%d,%d,%d,%d), screen_win=(%d,%d,%d,%d)",
        handle,
        layer_info->fb.src_win.x, layer_info->fb.src_win.y,
        layer_info->fb.src_win.width, layer_info->fb.src_win.height,
        layer_info->screen_win.x, layer_info->screen_win.y,
        layer_info->screen_win.width, layer_info->screen_win.height);

    if(hwcisScaled(psDisplayInfo,layer) || checkScaleFormat(handle->iFormat))
    {
        int cut_size_scn, cut_size_src;
        hwc_rect_t scn_bound;
        layer_info->mode = DISP_LAYER_WORK_MODE_SCALER;
        scn_bound.left = psDisplayInfo->varDisplayWidth * (100 - psDisplayInfo->displayPercentW) / 100/ 2;
        scn_bound.top = psDisplayInfo->varDisplayHeight * (100 - psDisplayInfo->displayPercentH) /100 / 2;
        scn_bound.right = scn_bound.left + (psDisplayInfo->varDisplayWidth * psDisplayInfo->displayPercentW) / 100;
        scn_bound.bottom = scn_bound.top + (psDisplayInfo->varDisplayHeight * psDisplayInfo->displayPercentH) / 100;

        if(layer_info->fb.src_win.x < 0)
        {
            cut_size_src = (0 - layer_info->fb.src_win.x);

            layer_info->fb.src_win.x += cut_size_src;
            layer_info->fb.src_win.width -= cut_size_src;
        }
        if((layer_info->fb.src_win.x + layer_info->fb.src_win.width) > (unsigned int)handle->iWidth)
        {
            cut_size_src = (layer_info->fb.src_win.x + layer_info->fb.src_win.width) - handle->iWidth;
            layer_info->fb.src_win.width -= cut_size_src;
        }
        if(layer_info->fb.src_win.y < 0)
        {
            cut_size_src = (0 - layer_info->fb.src_win.y);

            layer_info->fb.src_win.y += cut_size_src;
            layer_info->fb.src_win.height -= cut_size_src;
        }
        if((layer_info->fb.src_win.y + layer_info->fb.src_win.height) > (unsigned int)handle->iHeight)
        {
            cut_size_src = (layer_info->fb.src_win.x + layer_info->fb.src_win.height) - handle->iHeight;
            layer_info->fb.src_win.height -= cut_size_src;
        }

        if(layer_info->screen_win.x < scn_bound.left)
        {
            cut_size_scn = (scn_bound.left - layer_info->screen_win.x);
            cut_size_src = cut_size_scn * layer_info->fb.src_win.width / layer_info->screen_win.width;

            layer_info->fb.src_win.x += cut_size_src;
            layer_info->fb.src_win.width -= cut_size_src;

            layer_info->screen_win.x += cut_size_scn;
            layer_info->screen_win.width -= cut_size_scn;
        }
        if((layer_info->screen_win.x + layer_info->screen_win.width) > (unsigned int)scn_bound.right)
        {
            cut_size_scn = (layer_info->screen_win.x + layer_info->screen_win.width) - scn_bound.right;
            cut_size_src = cut_size_scn * layer_info->fb.src_win.width / layer_info->screen_win.width;

            layer_info->fb.src_win.width -= cut_size_src;
            layer_info->screen_win.width -= cut_size_scn;
        }
        if(layer_info->screen_win.y < scn_bound.top)
        {
            cut_size_scn = (scn_bound.top - layer_info->screen_win.y);
            cut_size_src = cut_size_scn * layer_info->fb.src_win.height / layer_info->screen_win.height;

            layer_info->fb.src_win.y += cut_size_src;
            layer_info->fb.src_win.height -= cut_size_src;

            layer_info->screen_win.y += cut_size_scn;
            layer_info->screen_win.height -= cut_size_scn;
        }
        if((layer_info->screen_win.y + layer_info->screen_win.height) > (unsigned int)scn_bound.bottom)
        {
            cut_size_scn = (layer_info->screen_win.y + layer_info->screen_win.height) - scn_bound.bottom;
            cut_size_src = cut_size_scn * layer_info->fb.src_win.height / layer_info->screen_win.height;

            layer_info->fb.src_win.height -= cut_size_src;
            layer_info->screen_win.height -= cut_size_scn;
        }
    }
    else
    {
        layer_info->mode = DISP_LAYER_WORK_MODE_NORMAL;
    }
    if(zOrder == 0 && Globctx->layer0usfe)
    {
        layer_info->mode = DISP_LAYER_WORK_MODE_SCALER;
    }
    layer_info->pipe = pipe;
    layer_info->zorder = zOrder;
    _hwcdev_layer_config_3d(disp, layer_info, checkScaleFormat(handle->iFormat));

    return 1;
ERR:
    Globctx->pvPrivateData->layer_num[psDisplayInfo->virtualToHWDisplay] = 0;
    memset(&(Globctx->pvPrivateData->layer_info[psDisplayInfo->virtualToHWDisplay]),0,sizeof(disp_layer_info)*4);
    return -1;
}

unsigned int ionGetAddr(int sharefd)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    struct ion_custom_data custom_data;
	SunxiPhysData phys_data;
    ion_handle_data freedata;

    struct ion_fd_data data ;
    data.fd = sharefd;
    int ret = ioctl(Globctx->ionFd, ION_IOC_IMPORT, &data);
    if (ret < 0)
    {
        ALOGE("#######ion_import  error#######");
        return 0;
    }
    custom_data.cmd = ION_IOC_SUNXI_PHYS_ADDR;
	phys_data.handle = data.handle;
	custom_data.arg = (unsigned long)&phys_data;
	ret = ioctl(Globctx->ionFd, ION_IOC_CUSTOM,&custom_data);
	if(ret < 0){
        ALOGE("ION_IOC_CUSTOM(err=%d)",ret);
        return 0;
    }
    freedata.handle = data.handle;
    ret = ioctl(Globctx->ionFd, ION_IOC_FREE, &freedata);
    if(ret < 0){
        ALOGE("ION_IOC_FREE(err=%d)",ret);
        return 0;
    }
    return phys_data.phys_addr;
}

int _hwc_device_convert_mb_to_nv21(hwc_layer_1_t *layer, unsigned int dst_phy_addr)
{
    g2d_blt blit;
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    IMG_native_handle_t *handle = (IMG_native_handle_t *)layer->handle;
    unsigned int src_stride = (handle->iWidth+31)/32*32;
    unsigned int dst_stride = (handle->iWidth+15)/16*16;
    unsigned int dst_phy_addr_c = dst_phy_addr + ALIGN(dst_stride * handle->iHeight, 1);
    int ret = 0;

    ALOGD( "#### %s src:[%x,%x] dst:%x w:%d h:%d src_stride:%d dst_stride:%d",
        __func__, layer->videoAddr0, layer->videoAddr1, dst_phy_addr,
        handle->iWidth, handle->iHeight, src_stride, dst_stride);

    blit.flag = G2D_BLT_NONE;
    blit.src_image.addr[0] = layer->videoAddr0 + PHY_OFFSET;
    blit.src_image.addr[1] = layer->videoAddr1  + PHY_OFFSET;
    blit.src_image.w = src_stride;
    //blit.src_image.h = handle->iHeight;
    blit.src_image.h = (handle->iHeight+31)&~31;
    if(layer->videoFormat ==HAL_PIXEL_FORMAT_AW_MB422)
    {
        blit.src_image.format = G2D_FMT_PYUV422UVC_MB32;
    }
    else if(layer->videoFormat == HAL_PIXEL_FORMAT_AW_MB420)
    {
        blit.src_image.format = G2D_FMT_PYUV420UVC_MB32;
    }
    else
    {
        ALOGE("%s,%d: error format: %d", __func__, __LINE__, layer->videoFormat);
        return -1;
    }

    blit.src_image.pixel_seq = G2D_SEQ_NORMAL;
    blit.src_rect.x = 0;
    blit.src_rect.y = 0;
    //blit.src_rect.w = src_stride;
    blit.src_rect.w = dst_stride;
    blit.src_rect.h = handle->iHeight;
    blit.dst_image.addr[0] = dst_phy_addr;
    blit.dst_image.addr[1] = dst_phy_addr_c;
    blit.dst_image.w = dst_stride;
    blit.dst_image.h = handle->iHeight;
    blit.dst_image.format = G2D_FMT_PYUV420UVC;
    blit.dst_image.pixel_seq = G2D_SEQ_VUVU;
    blit.dst_x = 0;
    blit.dst_y = 0;
    blit.color = 0xFF;
    blit.alpha = 0x89;
    ret = ioctl(Globctx->g2dFd, G2D_CMD_BITBLT, (unsigned long)(&blit));
    if(ret != 0)
    {
        ALOGD("#### %s g2d[%d] op failed", __func__, Globctx->g2dFd);
    }
    else
    {
        ALOGV("#### %s g2d[%d] op ok!\n", __func__, Globctx->g2dFd);
    }

    long arr[2];
    arr[0] = (long)dst_phy_addr;
    arr[1] = arr[0] + (dst_stride * handle->iHeight) - 1;
    ioctl(Globctx->g2dFd, G2D_CMD_MEM_FLUSH_CACHE, arr);

    arr[0] = (long)dst_phy_addr_c;
    arr[1] = arr[0] + (dst_stride * handle->iHeight)/2 - 1;
    ioctl(Globctx->g2dFd, G2D_CMD_MEM_FLUSH_CACHE, arr);

    return 0;
}

int aw_get_composer0_use_fe()
{
	int usefe = -1;
	char property[PROPERTY_VALUE_MAX];
	if (property_get("persist.sys.layer0usefe", property, NULL) >= 0)
	{

	    usefe = atoi(property);

	}
    if(usefe == 1)
    {
        return 1;
    }else{
	    return 0;
	}
}

int initDisplayDeviceInfo()
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;
    int refreshRate;
    int arg[4] = {0};
    int i;
    int tvmode4sysrsl;
    unsigned char percentWidth, percentHeight;
    ALOGD("#########The MainDisplay:%d ", Globctx->sunxiDisplay[0].virtualToHWDisplay);

    for(i = 0; i < NUMBEROFDISPLAY; i++)
    {
        DisplayInfo   *psDisplayInfo = &Globctx->sunxiDisplay[i];
        if(psDisplayInfo->virtualToHWDisplay != -1)
        {
            switch(psDisplayInfo->displayType)
            {
                case DISP_OUTPUT_TYPE_LCD:
                    arg[0] = psDisplayInfo->virtualToHWDisplay;
                    refreshRate = 60;
                    psDisplayInfo->displayDPI_X = 160000;
                    psDisplayInfo->displayDPI_Y = 160000;

                    psDisplayInfo->displayVsyncP = 1000000000 / refreshRate;
                    psDisplayInfo->displayPercentHT = 100;
                    psDisplayInfo->displayPercentWT = 100;
                    psDisplayInfo->current3DMode = DISPLAY_2D_ORIGINAL;
                    psDisplayInfo->varDisplayWidth = ioctl(Globctx->displayFd, DISP_CMD_GET_SCN_WIDTH, arg);
                    psDisplayInfo->varDisplayHeight = ioctl(Globctx->displayFd, DISP_CMD_GET_SCN_HEIGHT, arg);
                    if(LCD_USED == Globctx->mainDisp)
                    {
                        Globctx->mainDispWidth = psDisplayInfo->varDisplayWidth;
                        Globctx->mainDispHeight = psDisplayInfo->varDisplayHeight;
                        ALOGD("LCD, mainDispWidth is %d, mainDispHeight is %d", Globctx->mainDispWidth, Globctx->mainDispHeight);
                    }
                    else if(LCD_USED == Globctx->secDisp)
                    {
                        Globctx->secDispWidth = psDisplayInfo->varDisplayWidth;
                        Globctx->secDispHeight = psDisplayInfo->varDisplayHeight;
                        ALOGD("LCD, secDispWidth is %d, secDispHeight is %d", Globctx->secDispWidth, Globctx->secDispHeight);
                    }
                    psDisplayInfo->hwLayerNum = DISPLAY_MAX_LAYER_NUM;
    	            psDisplayInfo->hwPipeNum = NUMBEROFPIPE;
                    psDisplayInfo->vsyncEnable=1;
                    psDisplayInfo->screenRadio = 1;
                    break;

                case DISP_OUTPUT_TYPE_HDMI:
                    arg[0] = psDisplayInfo->virtualToHWDisplay;

                    psDisplayInfo->displayType = DISP_OUTPUT_TYPE_HDMI;
                    psDisplayInfo->displayMode = ioctl(Globctx->displayFd,DISP_CMD_HDMI_GET_MODE,arg);
                    psDisplayInfo->varDisplayWidth = ioctl(Globctx->displayFd, DISP_CMD_GET_SCN_WIDTH, arg);
                    psDisplayInfo->varDisplayHeight = ioctl(Globctx->displayFd, DISP_CMD_GET_SCN_HEIGHT, arg);
                    Globctx->de_fps = getInfoOfMode(psDisplayInfo->displayMode, REFRESHRAE);
                    Globctx->fe_clk = getFeClk(psDisplayInfo->displayMode);
                    if(Globctx->mainDisp == HDMI_USED)
                    {
                        if(0 == isDisplayP2P())
                        {
                            //not point to point
                            tvmode4sysrsl = getTvMode4SysResolution();
                            Globctx->mainDispWidth = getInfoOfMode(tvmode4sysrsl, WIDTH);
                            Globctx->mainDispHeight = getInfoOfMode(tvmode4sysrsl, HEIGHT);
                            ALOGD("HDMI tvMode is %x, mainDispWidth is %d, mainDispHeight is %d", tvmode4sysrsl, Globctx->mainDispWidth, Globctx->mainDispHeight);
                        }
                        else
                        {
                            Globctx->mainDispWidth = psDisplayInfo->varDisplayWidth;
                            Globctx->mainDispHeight = psDisplayInfo->varDisplayHeight;
                            ALOGD("HDMI tvMode is %x, secDispWidth is %d, secDispHeight is %d", tvmode4sysrsl, Globctx->secDispWidth, Globctx->secDispHeight);
                        }
                    }
                    else if(Globctx->secDisp == HDMI_USED)
                    {
                        Globctx->secDispWidth = psDisplayInfo->varDisplayWidth;
                        Globctx->secDispHeight = psDisplayInfo->varDisplayHeight;
                    }
                    psDisplayInfo->displayDPI_X = 213000;
                    psDisplayInfo->displayDPI_Y = 213000;
                    psDisplayInfo->displayVsyncP = 1000000000/getInfoOfMode(psDisplayInfo->displayMode,REFRESHRAE);
                    psDisplayInfo->current3DMode = DISPLAY_2D_ORIGINAL;
                    psDisplayInfo->hwLayerNum = DISPLAY_MAX_LAYER_NUM;
    	            psDisplayInfo->hwPipeNum = NUMBEROFPIPE;
                    psDisplayInfo->vsyncEnable = 1;
                    psDisplayInfo->screenRadio = 1;
                    saveDispModeToFile(DISP_OUTPUT_TYPE_HDMI,psDisplayInfo->displayMode);
                    break;

                case DISP_OUTPUT_TYPE_TV:
                    arg[0] = psDisplayInfo->virtualToHWDisplay;

                    psDisplayInfo->displayType = DISP_OUTPUT_TYPE_TV;
                    psDisplayInfo->displayMode = ioctl(Globctx->displayFd, DISP_CMD_TV_GET_MODE, arg);
                    psDisplayInfo->varDisplayWidth = ioctl(Globctx->displayFd, DISP_CMD_GET_SCN_WIDTH, arg);
                    psDisplayInfo->varDisplayHeight = ioctl(Globctx->displayFd, DISP_CMD_GET_SCN_HEIGHT, arg);
                    //Caution: do not use point to point in CVBS mode, because it make ui see worst.
                    if(Globctx->mainDisp == CVBS_USED)
                    {
                        tvmode4sysrsl = getTvMode4SysResolution();
                        Globctx->mainDispHeight = getInfoOfMode(tvmode4sysrsl, HEIGHT);
                        Globctx->mainDispWidth = getInfoOfMode(tvmode4sysrsl, WIDTH);
                        ALOGD("CVBS tvMode is %x, mainDispWidth is %d, mainDispHeight is %d", tvmode4sysrsl, Globctx->mainDispWidth, Globctx->mainDispHeight);
                    }
                    else if(Globctx->secDisp == CVBS_USED)
                    {
                        Globctx->secDispWidth = psDisplayInfo->varDisplayWidth;
                        Globctx->secDispHeight = psDisplayInfo->varDisplayHeight;
                        ALOGD("CVBS tvMode is %x, secDispWidth is %d, secDispHeight is %d", tvmode4sysrsl, Globctx->secDispWidth, Globctx->secDispHeight);
                    }

                    Globctx->de_fps = getInfoOfMode(psDisplayInfo->displayMode, REFRESHRAE);
                    Globctx->fe_clk = getFeClk(psDisplayInfo->displayMode);
                    psDisplayInfo->displayDPI_X = 213000;
                    psDisplayInfo->displayDPI_Y = 213000;
                    psDisplayInfo->displayVsyncP = 1000000000/getInfoOfMode(psDisplayInfo->displayMode,REFRESHRAE);
                    psDisplayInfo->current3DMode = DISPLAY_2D_ORIGINAL;
                    psDisplayInfo->hwLayerNum = DISPLAY_MAX_LAYER_NUM;
    	            psDisplayInfo->hwPipeNum = NUMBEROFPIPE;
                    psDisplayInfo->vsyncEnable = 1;
                    psDisplayInfo->screenRadio = 1;
                    saveDispModeToFile(DISP_OUTPUT_TYPE_TV,psDisplayInfo->displayMode);
                    break;

                case DISP_OUTPUT_TYPE_VGA:

                default:
                    ALOGD("not support type");
                    continue;

            }
        }
    }
    if(-1 == getDispMarginFromFile(&percentWidth, &percentHeight))
    {
        percentWidth = MARGIN_DEFAULT_PERCENT_WIDTH;
        percentHeight = MARGIN_DEFAULT_PERCENT_HEIGHT;
    }
    _hwc_device_set_margin(0, percentWidth, percentHeight);

    return 0;
}


SUNXI_hwcdev_context_t* hwcCreateDevice(void)
{
    SUNXI_hwcdev_context_t *Globctx = &gSunxiHwcDevice;

    unsigned long arg[4] = {0};
    int hdmiFd;
    int dispCnt;
    DisplayInfo   *psDisplayInfo0 = &Globctx->sunxiDisplay[0];
    DisplayInfo   *psDisplayInfo1 = &Globctx->sunxiDisplay[1];

    property_set("persist.sys.disp_init_exit", "1");

    memset(Globctx, 0, sizeof(SUNXI_hwcdev_context_t));
    for(dispCnt = 0; dispCnt < NUMBEROFDISPLAY; dispCnt++)
    {
        Globctx->sunxiDisplay[dispCnt].virtualToHWDisplay = -1;
    }
    Globctx->pvPrivateData =(setup_dispc_data_t* )calloc(1, sizeof(setup_dispc_data_t));
    memset(Globctx->pvPrivateData, 0, sizeof(setup_dispc_data_t));
    Globctx->pvPrivateData->hConfigData = calloc(DISPLAY_MAX_LAYER_NUM*NUMBEROFDISPLAY,sizeof(int));
    Globctx->displayFd = open("/dev/disp", O_RDWR);
    if (Globctx->displayFd < 0)
    {
        ALOGE( "Failed to open disp device, ret:%d, errno: %d\n", Globctx->displayFd, errno);
    }

    Globctx->fbFd = open("/dev/graphics/fb0", O_RDWR);
    if (Globctx->fbFd < 0)
    {
        ALOGE( "Failed to open fb0 device, ret:%d, errno:%d\n", Globctx->fbFd, errno);
    }
    Globctx->ionFd = open("/dev/ion",O_RDWR);
    if(Globctx->ionFd < 0)
    {
        ALOGE( "Failed to open  ion device, ret:%d, errno:%d\n", Globctx->ionFd, errno);
    }
    Globctx->g2dFd = open("/dev/g2d", O_RDWR);
    if(Globctx->g2dFd < 0){
        ALOGE("Failed to open  g2d device, ret:%d, errno:%d\n", Globctx->g2dFd, errno);
    }

    arg[0] = HDMI_USED;
    int disp1 = ioctl(Globctx->displayFd, DISP_CMD_GET_OUTPUT_TYPE, arg);
    int mode1 = ioctl(Globctx->displayFd, DISP_CMD_HDMI_GET_MODE, arg);
    arg[0] = CVBS_USED;
    int disp0 = ioctl(Globctx->displayFd, DISP_CMD_GET_OUTPUT_TYPE, arg);
    int mode0 = ioctl(Globctx->displayFd, DISP_CMD_TV_GET_MODE, arg);
    arg[0] = LCD_USED;
    int disp3 = ioctl(Globctx->displayFd, DISP_CMD_GET_OUTPUT_TYPE, arg);
    //init the main display device
    Globctx->mainDisp = INVALID_VALUE;
    if(disp1 == DISP_OUTPUT_TYPE_HDMI)
    {
        psDisplayInfo0->displayType = DISP_OUTPUT_TYPE_HDMI;
        psDisplayInfo0->virtualToHWDisplay = HDMI_USED;
        Globctx->mainDisp = HDMI_USED;
        Globctx->mainDispMode = mode1;
    }
    else if(disp0 == DISP_OUTPUT_TYPE_TV)
    {
        psDisplayInfo0->displayType = DISP_OUTPUT_TYPE_TV;
        psDisplayInfo0->virtualToHWDisplay = CVBS_USED;
        Globctx->mainDisp = CVBS_USED;
        Globctx->mainDispMode = mode0;
    }
    else if(disp3 == DISP_OUTPUT_TYPE_LCD)
    {
        psDisplayInfo0->displayType = DISP_OUTPUT_TYPE_LCD;
        psDisplayInfo0->virtualToHWDisplay = LCD_USED;
        Globctx->mainDisp = LCD_USED;
        Globctx->mainDispMode = DISP_TV_MOD_INVALID;
    }
    else
    {
        psDisplayInfo0->displayType = DISP_OUTPUT_TYPE_NONE;
        psDisplayInfo0->virtualToHWDisplay = INVALID_VALUE;
        Globctx->mainDisp = INVALID_VALUE;
        Globctx->mainDispMode = DISP_TV_MOD_INVALID;
    }
    //init the second display device
    Globctx->secDisp = INVALID_VALUE;
    if(Globctx->mainDisp != CVBS_USED && disp0 == DISP_OUTPUT_TYPE_TV)
    {
        psDisplayInfo1->displayType = DISP_OUTPUT_TYPE_TV;
        psDisplayInfo1->virtualToHWDisplay = CVBS_USED;
        Globctx->secDisp = CVBS_USED;
        Globctx->secDispMode = mode0;
    }
    else if((Globctx->mainDisp != LCD_USED && disp3 == DISP_OUTPUT_TYPE_LCD))
    {
        psDisplayInfo1->displayType = DISP_OUTPUT_TYPE_LCD;
        psDisplayInfo1->virtualToHWDisplay = LCD_USED;
        Globctx->secDisp = LCD_USED;
        Globctx->secDispMode = DISP_TV_MOD_INVALID;
    }

    Globctx->canForceGPUCom = true;
    Globctx->forceGPUComp = 0;
    Globctx->layer0usfe = aw_get_composer0_use_fe();

    initDisplayDeviceInfo();

	Globctx->fBeginTime = 0.0;
    Globctx->uiBeginFrame = 0;
    Globctx->hwcdebug = 0;

    ALOGD( "#### mainDisp is %d, secDisp is %d", Globctx->mainDisp, Globctx->secDisp);
    ALOGD( "#### Type:%d  DisplayMode:%d PrimaryDisplay:%d  DisplayWidth:%d  DisplayHeight:%d ",
		Globctx->sunxiDisplay[0].displayType,Globctx->sunxiDisplay[0].displayMode,
		Globctx->sunxiDisplay[0].virtualToHWDisplay,Globctx->sunxiDisplay[0].varDisplayWidth,
		Globctx->sunxiDisplay[0].varDisplayHeight);
    pthread_create(&Globctx->sVsyncThread, NULL, vsyncThreadWrapper, Globctx);


	return (SUNXI_hwcdev_context_t*)Globctx;
}

int hwcDestroyDevice(SUNXI_hwcdev_context_t *psDevice)
{
	SUNXI_hwcdev_context_t *Globctx = (SUNXI_hwcdev_context_t*)psDevice;

	close(Globctx->displayFd);
	close(Globctx->fbFd);
    close(Globctx->ionFd);
    free(Globctx->pvPrivateData->hConfigData);
	free(Globctx->pvPrivateData);
	return 1;
}
