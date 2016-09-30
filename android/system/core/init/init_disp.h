#ifndef _INIT_DISP_H_
#define _INIT_DISP_H_

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fb.h>
#include <linux/kd.h>
#include "log.h"
#include "property_service.h"

#define SW_JAWS 0x00
#define SW_EAGLE 0x02
#define SW_DOLPHIN 0x03

enum {
    DISP_NONE_POLICY = 0,
    DISP_SINGLE_POLICY,
    DISP_DUAL_POLICY,
    DISP_ADAPT_POLICY,
    DISP_POLICY_NUM,
};


#if(SW_CHIP_PLATFORM == SW_JAWS)

    #include "drv_display.h"

    #define HW_NUM_DISP 3

    static disp_layer_info g_layer_info;

    int _disp_get_output(int fd, unsigned long *args)
    {
        disp_output output;
        disp_output *pt = (disp_output *)args[1];
        output.type = ioctl(fd, DISP_CMD_GET_OUTPUT_TYPE, args);
        switch(output.type) {
        case DISP_OUTPUT_TYPE_HDMI:
            output.mode = ioctl(fd, DISP_CMD_HDMI_GET_MODE, args);
            break;
        case DISP_OUTPUT_TYPE_TV:
            output.mode = ioctl(fd, DISP_CMD_TV_GET_MODE, args);
            break;
        default:
            output.mode = -1;
        }
        memcpy((void *)pt, (void *)&output, sizeof(disp_output));
        return 0;
    }

    int _disp_device_switch(int fd, unsigned long *args)
    {
        unsigned long type = args[1];
        args[1] = args[2];
        switch(type) {
        case DISP_OUTPUT_TYPE_HDMI:
            ioctl(fd, DISP_CMD_HDMI_DISABLE, args);
            ioctl(fd, DISP_CMD_HDMI_SET_MODE, args);
            ioctl(fd, DISP_CMD_HDMI_ENABLE, args);
            return 0;
        case DISP_OUTPUT_TYPE_TV:
            ioctl(fd, DISP_CMD_TV_OFF, args);
            ioctl(fd, DISP_CMD_TV_SET_MODE, args);
            ioctl(fd, DISP_CMD_TV_ON, args);
            return 0;
        default:
            ioctl(fd, DISP_CMD_TV_OFF, args);
            ioctl(fd, DISP_CMD_HDMI_DISABLE, args);
            return 0;
        }
    }

    int _disp_get_scn_win(int fd, unsigned long *args)
    {
        int screen_id, layer_id;
        disp_rect *pt = (disp_rect *)args[1];
        for(layer_id = 0; layer_id < 4; layer_id++) {
            args[1] = layer_id;
            args[2] = (unsigned long)&g_layer_info;
            if(!ioctl(fd, DISP_CMD_LAYER_GET_INFO, args)) {
                memcpy((void *)pt, (void *)&g_layer_info.screen_win, sizeof(disp_rect));
                return 0;
            }
        }
        ERROR("%d_disp_get_scn_win: failed!\n", SW_CHIP_PLATFORM);
        return -1;
    }

    int _disp_set_scn_win(int fd,unsigned long * args)
    {
        disp_layer_info layer_info;
        memcpy((void *)&layer_info, (void *)&g_layer_info, sizeof(disp_layer_info));
        memcpy((void *)&layer_info.screen_win, (void *)args[1], sizeof(disp_rect));
        layer_info.mode = DISP_LAYER_WORK_MODE_SCALER;
        args[1] = 0;
        args[2] = (unsigned long)&layer_info;
        ioctl(fd, DISP_CMD_LAYER_SET_INFO, args);
        return 0;
    }

    inline int _disp_enable_layer(int fd, unsigned long *args)
    {
        return ioctl(fd, DISP_CMD_LAYER_ENABLE, args);
    }

    inline int _disp_disable_layer(int fd, unsigned long *args)
    {
        return ioctl(fd, DISP_CMD_LAYER_DISABLE, args);
    }


#elif ((SW_CHIP_PLATFORM == SW_EAGLE) || (SW_CHIP_PLATFORM == SW_DOLPHIN))

    #include "sunxi_display2.h"

    #define HW_NUM_DISP 2

    static disp_layer_config g_layer_config;

    inline int _disp_get_output(int fd, unsigned long *args)
    {
        return ioctl(fd, DISP_GET_OUTPUT, args);
    }

    inline int _disp_device_switch(int fd, unsigned long *args)
    {
        return ioctl(fd, DISP_DEVICE_SWITCH, args);
    }

    int _disp_get_scn_win(int fd, unsigned long *args)
    {
        int layer_id, channel_id;
        disp_rect *pt = (disp_rect *)args[1];
        int const channel_num = (0 == args[0]) ? (4) : ((1 == args[0]) ? 2 : 0);

        for(channel_id = 0; channel_id < channel_num; channel_id++)
            for(layer_id = 0; layer_id < 4; layer_id++) {
                g_layer_config.layer_id = 0;
                g_layer_config.channel = channel_id;
                g_layer_config.enable = 0;
                args[1] = (unsigned long)&g_layer_config;
                args[2] = 1;
                ioctl(fd, DISP_LAYER_GET_CONFIG, args);
                if(g_layer_config.enable) {
                    memcpy((void *)pt, (void *)&g_layer_config.info.screen_win, sizeof(disp_rect));
                    return 0;
                }
            }
        ERROR("%d_disp_get_scn_win: failed!\n", SW_CHIP_PLATFORM);
        return -1;
    }

    int _disp_set_scn_win(int fd, unsigned long *args)
    {
        disp_layer_config layer_config;
        memcpy((void *)&layer_config, (void *)&g_layer_config, sizeof(disp_layer_config));
        memcpy((void *)&layer_config.info.screen_win, (void *)args[1], sizeof(disp_rect));
        args[1] = (unsigned long)&layer_config;
        args[2] = 1;
        ioctl(fd, DISP_LAYER_SET_CONFIG, args);
        return 0;
    }

    inline int _disp_enable_layer(int fd, unsigned long *args)
    {
        return 0;
    }

    inline int _disp_disable_layer(int fd, unsigned long *args)
    {
        return 0;
    }

#else
#error "please select a platform\n"
#endif

#endif
