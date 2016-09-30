#define bb_debug(fmt, args...) printf("burnboot:"fmt, ## args)

typedef struct {
    unsigned char* buffer;
    long len;
} BufferExtractCookie;

typedef int (*DeviceBurn)(BufferExtractCookie *cookie, char* path);

#define STAMP_VALUE             0x5F0A6C39

int getBufferExtractCookieOfFile(const char* path, BufferExtractCookie* cookie);

int getDeviceInfo(int boot_num, char* dev_node, char* boot_bin, DeviceBurn* burnFunc);

int checkBoot0Sum(BufferExtractCookie* cookie);

int checkUbootSum(BufferExtractCookie* cookie);

int getUbootstartsector(BufferExtractCookie* cookie);

int getDramPara(void *newBoot0, void *innerBoot0);

int genBoot0CheckSum(void *cookie);

void clearPageCache();

int check_soc_is_secure(void) ;

void SdBootInit(void);

int eraseSDPartition(char *dev_path);
  
int getFlashType();
