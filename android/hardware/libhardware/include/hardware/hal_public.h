#ifndef  HAL_PUBLIC_H
#define  HAL_PUBLIC_H

// defined at swextend/utils/libswconfig/swconfig.h
#define SW_JAWS 0x00
#define SW_EAGLE 0x02
#define SW_DOLPHIN 0x03

#if(SW_CHIP_PLATFORM == SW_JAWS)
#include "hal_public_6230.h"
#elif (SW_CHIP_PLATFORM == SW_EAGLE)
#include "hal_public_544.h"
#elif (SW_CHIP_PLATFORM == SW_DOLPHIN)
#include "gralloc_priv.h"
#else
#error "please select a platform\n"
#endif

#endif
