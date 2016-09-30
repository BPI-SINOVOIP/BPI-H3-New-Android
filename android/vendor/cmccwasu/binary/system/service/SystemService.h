#ifndef CMCC_WASU_SYSTEMSERVICE
#define CMCC_WASU_SYSTEMSERVICE

#include <utils/Log.h>
#include <utils/Errors.h>

namespace android{

class SystemService{

public:
    static void init();

private:
    SystemService(){};
    ~SystemService(){};
    static void import_kernel_cmdline(void (*import_kernel_nv)(char *name));
    static void import_kernel_nv(char *name);
    static void initMac();
	static void get_kernel_cmdline_serialno(void);
};

};
#endif