//init system for cmcc-wasu
//the process will be start in cmcc-wasu platform, and do some work befor boot complete
#include <sys/types.h>
#include <unistd.h>
#include <utils/Log.h>
#include "SystemService.h"
#include <private/android_filesystem_config.h>

using namespace android;

int main(int argc, char** argv){
    ALOGD("init system before booting complete");
    SystemService::init();
}