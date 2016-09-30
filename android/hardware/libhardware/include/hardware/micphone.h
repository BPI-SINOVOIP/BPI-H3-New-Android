#ifndef ANDRIOD_MICPHONE_HAL_INTERFACE_H
#define ANDRIOD_MICPHONE_HAL_INTERFACE_H


#include <stdint.h>
#include <strings.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#include <hardware/hardware.h>

__BEGIN_DECLS

/* the id of this module */
#define MICPHONE_HARDWARE_MODULE_ID   "micphone"

/**
 * Name of the hdmi devices to open
 */
#define MIC_HARDWARE_INTERFACE "mic_hw_if"

#define MICPHONE_MODULE_API_VERSION_1_0 HARDWARE_MODULE_API_VERSION(1, 0)
#define MICPHONE_DEVICE_API_VERSION_1_0 HARDWARE_DEVICE_API_VERSION(1, 0)

struct mic_module {
    struct hw_module_t common;
};

/**
 * Every hardware module must have a data structure named HAL_MODULE_INFO_SYM
 * and the fields of this data structure must begin with hw_module_t
 * followed by module specific information.
 */
struct mic_hw_device{
    struct hw_device_t common;
    //start the micphone channel
    int32_t (*start)(const struct mic_hw_device *dev);
    //stop the micphone channels
    int32_t (*stop)(const struct mic_hw_device *dev);
    //set the volume to micphone
    int32_t (*set_volume)(const struct mic_hw_device *dev,int32_t volume);
    //get current micphone volume
    int32_t (*get_volume)(const struct mic_hw_device *dev);

    //for debug
    int32_t (*dump)(const struct mic_hw_device *dev);
};
  

typedef struct mic_hw_device mic_hw_device_t;
 
static inline int32_t mic_hw_device_open(const struct hw_module_t* module,
                struct mic_hw_device** device) {
    return module->methods->open(module,MIC_HARDWARE_INTERFACE, (struct hw_device_t**)device);
}

static inline int32_t mic_hw_device_close(struct mic_hw_device* device) {
    return device->common.close(&device->common);
}

__END_DECLS

#endif  //ANDRIOD_MICPHONE_HAL_INTERFACE_H
