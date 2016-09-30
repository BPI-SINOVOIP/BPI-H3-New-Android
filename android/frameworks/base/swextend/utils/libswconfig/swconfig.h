#ifndef __AW_CONFIG__
#define __AW_CONFIG__

#ifdef __cplusplus
extern "C"
{
#endif

#define PLATFORM_CMCCWASU	0x08
#define PLATFORM_ALIYUN		0x09
#define PLATFORM_TVD		0x0A
#define PLATFORM_IPTV		0x0B
#define UNKNOWN_PLATFORM	0xFF

uint32_t getBoardPlatform();
uint32_t getBusinessPlatform();

#ifdef __cplusplus
}
#endif

#endif	// __AW_CONFIG__

