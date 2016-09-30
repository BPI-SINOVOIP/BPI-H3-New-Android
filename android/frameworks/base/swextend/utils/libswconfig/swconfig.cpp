#define LOG_TAG "SW_Config"
#define LOG_NDEBUG 0
#include <stdio.h>
#include <assert.h>

#include "swconfig.h"

extern "C" uint32_t getBoardPlatform()
{
#ifdef SW_CHIP_PLATFORM
    return SW_CHIP_PLATFORM;
#else
    return UNKNOWN_PLATFORM;
#endif
}

extern "C" uint32_t getBusinessPlatform()
{
#ifdef BUSINESS_PLATFORM
    return BUSINESS_PLATFORM;
#else
    return UNKNOWN_PLATFORM;
#endif
}
