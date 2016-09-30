#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <string.h>
#include <fcntl.h>
#include <sys/ioctl.h>

#include "BurnBoot.h"
#include "BootHead.h"

#define DEVNODE_PATH_NAND   "/dev/block/by-name/bootloader"
#define DEVNODE_PATH_SD "/dev/block/mmcblk0"

#define SD_UBOOT_SECTOR_START_DEFAULT 32800

#define CMDLINE_FILE_PATH "/proc/cmdline"

typedef struct mmc_ioc_erase_cmd {
    unsigned int flags;
    unsigned int start_sec;
    unsigned int size_sec;
    /*
     * For 64-bit machines, the next member, ``__u64 data_ptr``, wants to
     * be 8-byte aligned.  Make sure this struct is the same size when
     * built for 32-bit.
     */
    __u32 __pad;
    /* DAT buffer */
    __u64 data_ptr;
}mmc_ioc_erase_cmd;
#define MMC_IOC_ERASE_CMD _IOWR(MMC_BLOCK_MAJOR, 1, struct mmc_ioc_erase_cmd)
#define MMC_BLOCK_MAJOR         179

#define UBOOT_MAGIC				"uboot"
#define BOOT1_MAGIC             "eGON.BT1"
#define BOOT0_MAGIC             "eGON.BT0"

static int spliteKeyAndValue(char* str, char** key, char** value){
	int elocation = strcspn(str,"=");
	if (elocation < 0){
		return -1;
	}
	str[elocation] = '\0';
	*key = str;
	*value = str + elocation + 1;
	return 0;
}

static int getInfoFromCmdline(char* key, char* value){
	FILE* fp;
    char cmdline[1024];
	//read partition info from /proc/cmdline
    if ((fp = fopen(CMDLINE_FILE_PATH,"r")) == NULL) {
        bb_debug("can't open /proc/cmdline \n");
        // goto error;
        return -1;
    }
    fgets(cmdline,1024,fp);
    fclose(fp);
    // bb_debug("%s\n", cmdline); 
    //splite the cmdline by space
    char* p = NULL;
    char* lkey = NULL;
    char* lvalue = NULL;
    p = strtok(cmdline, " ");
    if (!spliteKeyAndValue(p, &lkey, &lvalue)){
    	if (!strcmp(lkey,key)){
    		goto done;
    	}
    }
    // bb_debug("the first k-v is %s\n", p);

    while ((p = strtok(NULL, " "))){
    	// bb_debug("the other k-v is %s\n", p);
    	if (!spliteKeyAndValue(p, &lkey, &lvalue)){
	    	if (!strcmp(lkey,key)){
	    		goto done;
	    	}
	    }
    }

    bb_debug("no key named %s in cmdline.\n", key);
    strcpy(value, "-1");
    return -1;

    done:
    strcpy(value, lvalue);
    return 0;
}

int getFlashType(){
	char ctype[8];
	getInfoFromCmdline("boot_type", ctype);
	bb_debug("flash type = %s\n", ctype);

	int flash_type = atoi(ctype);
	//atoi出错时会返回0,当ctype字符串为0时也会返回0，所以这里要判断是否出错.
	if (flash_type == 0 && ctype[0] != '0'){
		return FLASH_TYPE_UNKNOW;
	}

	return flash_type;
}

int getBufferExtractCookieOfFile(const char* path, BufferExtractCookie* cookie){

	if (cookie == NULL){
		// printf("get file stat failed!\n");
		return -1;
	}

	struct stat statbuff;
	if(stat(path, &statbuff) < 0){
		bb_debug("get file stat failed!!\n");
		return -1;
	}
	cookie->len = statbuff.st_size;
	// bb_debug("file size is %d\n",(int)cookie->len);
	
	unsigned char* buffer = malloc(cookie->len);

	FILE* fp = fopen(path,"r");
	if (fp == NULL){
		bb_debug("open file failed!\n");
		return -1;
	}
	
	if (!fread(buffer, cookie->len, 1, fp)){
		bb_debug("read file failed!\n");
		return -1;
	}

	cookie->buffer = buffer;

	return 0;

}

//获取flash类型，根据flash类型选择烧写哪个boot0和确定设备文件地址

int getDeviceInfo(int boot_num, char *dev_node, char *boot_bin, DeviceBurn *burnFunc){
	
	int flash_type = getFlashType();

	switch (flash_type) {
        //因为旧有A31设备中的uboot是不会传递上boot_type这个参数的，而旧有设备全部是nand，没有tsd，所以当检测不了boot_type时默认调用nand接口。
        //这里这样做是为了兼容以前的机器能够顺利升级到当前版本。这个是A31用于过渡升级的逻辑。
        case FLASH_TYPE_UNKNOW:
		case FLASH_TYPE_NAND:
			strcpy(dev_node, DEVNODE_PATH_NAND);
			bb_debug("use nand flash!!\n");
            switch (boot_num) {
                case BOOT0:
                    if (flash_type == FLASH_TYPE_UNKNOW) {
                        goto error;
                    }
                    if (check_soc_is_secure())
                        strcpy(boot_bin, "toc0.fex");
                    else
                        strcpy(boot_bin, "boot0_nand.fex");
                    *burnFunc = burnNandBoot0;
                    break;
                case UBOOT:
                    if (check_soc_is_secure()) {
                        strcpy(boot_bin, "toc1.fex");
                    } else {
                        strcpy(boot_bin, "uboot_nand.fex");
                    }
                    *burnFunc = burnNandUboot;
                    break;
            }
            flash_type = FLASH_TYPE_NAND;
			break;
		case FLASH_TYPE_SD1:
        case FLASH_TYPE_SD2:
			SdBootInit();
        	strcpy(dev_node, DEVNODE_PATH_SD);
        	bb_debug("use sd flash!!\n");
            switch (boot_num) {
                case BOOT0:
                    if (check_soc_is_secure()) {
                        strcpy(boot_bin, "toc0.fex");
                    } else {
                        strcpy(boot_bin, "boot0_sdcard.fex");
                    }
                    *burnFunc = burnSdBoot0;
					bb_debug("boot0_bin = %s!!\n",boot_bin);
                    break;
                case UBOOT:
                    if (check_soc_is_secure()) {
                        strcpy(boot_bin, "toc1.fex");
                    } else {
                        strcpy(boot_bin, "uboot_sdcard.fex");
                    }
                    *burnFunc = burnSdUboot;
					bb_debug("uboot_bin = %s!!\n",boot_bin);
                    break;
            }
        	break;
        default:
        	goto error;
	}

    return flash_type;
    error:
    return FLASH_TYPE_UNKNOW;
}

#define CHECK_SOC_SECURE_ATTR 0x00 
#define CHECK_BOOT_SECURE_ATTR 0x04 
/*
 * Check secure solution or not
 * Return 0 if normal , return 1 if secure
 */ 
int check_soc_is_secure(void) 
{                
	int fd, ret; 

	fd = open("/dev/sunxi_soc_info", O_RDWR); 
	if (fd == -1) { 
		bb_debug("open /dev/sunxi_soc_info failed!\n"); 
		return 0 ; 
	}       

	ret = ioctl(fd, CHECK_SOC_SECURE_ATTR, NULL); 
	if(ret){// ret=1 in secure case
		bb_debug("soc is secure. (return value:%x)\n", ret); 
	}else{
		bb_debug("soc is normal. (return value:%x)\n", ret); 
		ret = ioctl(fd, CHECK_BOOT_SECURE_ATTR, NULL);
		if(ret)
			bb_debug("secure boot for normal case\n");
	}

	close(fd);
	return ret;
}

static int verify_toc_addsum( void *mem_base, unsigned int size, unsigned int *psum )
{
	unsigned int *buf;
	unsigned int count;
	unsigned int src_sum;
	unsigned int sum;

	/* 生成校验和 */
	src_sum = *psum;                  // 从Boot_file_head中的“check_sum”字段取出校验和
	bb_debug("read sum :0x%x\n",src_sum);
	*psum = STAMP_VALUE;              // 将STAMP_VALUE写入Boot_file_head中的“check_sum”字段

	count = size >> 2;                         // 以 字（4bytes）为单位计数
	sum = 0;
	buf = (unsigned int *)mem_base;
	do
	{
		sum += *buf++;                         // 依次累加，求得校验和
		sum += *buf++;                         // 依次累加，求得校验和
		sum += *buf++;                         // 依次累加，求得校验和
		sum += *buf++;                         // 依次累加，求得校验和
	}while( ( count -= 4 ) > (4-1) );

	while( count-- > 0 )
		sum += *buf++;

	*psum = src_sum;                  // 恢复Boot_file_head中的“check_sum”字段的值
	bb_debug("calc sum :0x%x\n",sum);

	if( sum == src_sum )
		return 0;                           // 校验成功
	else
		return -1;                          // 校验失败
}

int checkBoot0Sum(BufferExtractCookie* cookie){

	standard_boot_file_head_t  *head_p;
	unsigned int length;
	unsigned int *buf;
	unsigned int loop;
	unsigned int i;
	unsigned int sum;
	unsigned int csum;

	if(check_soc_is_secure()){
		unsigned int *psum;
		psum = &(((toc0_private_head_t *)(cookie->buffer))->check_sum); 
		return verify_toc_addsum(cookie->buffer, cookie->len, psum);

	}else{
		head_p = (standard_boot_file_head_t *)cookie->buffer;

		length = head_p->length;
		if( ( length & 0x3 ) != 0 )                   // must 4-byte-aligned
			return -1;
		if((length > 32 * 1024) != 0 ) {
			bb_debug("boot0 file length over size!!\n");
		}
		if (length & ( 512 - 1 )  != 0){
			bb_debug("boot0 file did not aliged!!\n");
		}   
		buf = (unsigned int *)cookie->buffer;
		csum = head_p->check_sum;
		head_p->check_sum = STAMP_VALUE;              // fill stamp
		loop = length >> 2;

		for( i = 0, sum = 0;  i < loop;  i++ )
			sum += buf[i];

		head_p->check_sum = csum;
		bb_debug("Boot0 -> File length is %u,original sum is %u,new sum is %u\n", length, head_p->check_sum, sum);
		return !(csum == sum);
	}
}

int getUbootstartsector(BufferExtractCookie* cookie){
	if(check_soc_is_secure()){
        return -1;
    }
	uboot_file_head_t  *head_p = NULL;
	unsigned int  start_sector = 0;
	head_p = (uboot_file_head_t *)cookie->buffer;

    if (head_p == NULL)
        return -1;

    start_sector = head_p->prvt_head.uboot_start_sector_in_mmc;

    if(start_sector == 0)
    {
        start_sector = SD_UBOOT_SECTOR_START_DEFAULT;
    }

    return (int)start_sector;
}

int checkUbootSum(BufferExtractCookie* cookie){
	uboot_file_head  *head_p;
	unsigned int length;
	unsigned int *buf;
	unsigned int loop;
	unsigned int i;
	unsigned int sum;
	unsigned int csum;

	if(check_soc_is_secure()){
		unsigned int *psum;
		psum = &(((sbrom_toc1_head_info_t *)(cookie->buffer))->add_sum); 
		return verify_toc_addsum(cookie->buffer, cookie->len, psum);

	}else{

		head_p = (uboot_file_head *)cookie->buffer;
		length = head_p->length;
		if( ( length & 0x3 ) != 0 )                   // must 4-byte-aligned
			return -1;

		buf = (unsigned int *)cookie->buffer;
		csum = head_p->check_sum;

		head_p->check_sum = STAMP_VALUE;              // fill stamp
		loop = length >> 2;

		for( i = 0, sum = 0;  i < loop;  i++ )
			sum += buf[i];

		head_p->check_sum = csum;
		bb_debug("Uboot -> File length is %u,original sum is %u,new sum is %u\n", length, head_p->check_sum, sum);
		return !(csum == sum);
	}
}


int getDramPara(void *newBoot0, void *innerBoot0)
{

	if(check_soc_is_secure()){
		sbrom_toc0_config_t *srcHead;
		sbrom_toc0_config_t *dstHead ;

		srcHead = (sbrom_toc0_config_t*)((int)innerBoot0 + 0x30);
		dstHead = (sbrom_toc0_config_t*)((int)newBoot0 + 0x30);
	
		memcpy(dstHead, 
				srcHead, 
				sizeof(sbrom_toc0_config_t));

	}else{
		boot0_file_head_t *srcHead;
		boot0_file_head_t *dstHead;

		srcHead = (boot0_file_head_t *)innerBoot0;
		dstHead = (boot0_file_head_t *)newBoot0;

		memcpy(&((dstHead->prvt_head).dram_para[0]), &((srcHead->prvt_head).dram_para[0]), 32 * 4);
	}

	return 0;
}

int genBoot0CheckSum(void *cookie)
{
	standard_boot_file_head_t  *head_p;
	unsigned int length;
	unsigned int *buf;
	unsigned int loop;
	unsigned int i;
	unsigned int sum;	
	unsigned int *psum;

	if(check_soc_is_secure()){
		toc0_private_head_t *head_t = (toc0_private_head_t *)cookie; 
		psum = &(head_t->check_sum); 
		length = head_t->length; 

	}else{
		head_p = (standard_boot_file_head_t *)cookie;
		psum = &head_p->check_sum ;
		length = head_p->length;
	}

	if( ( length & 0x3 ) != 0 )                   // must 4-byte-aligned
		return -1;
	buf = (unsigned int *)cookie;
	*psum = STAMP_VALUE;              // fill stamp
	loop = length >> 2;
	sum = 0 ;
	for( i = 0, sum = 0;  i < loop;  i++ )
		sum += buf[i];

	/* write back check sum */
	*psum = sum;
	return 0 ;
}

void clearPageCache(){
    FILE *fp = fopen("/proc/sys/vm/drop_caches", "w+");
    char *num = "1";
    fwrite(num, sizeof(char), 1, fp);
    fclose(fp);
}

int eraseSDPartition(char *dev_path)
{
	int fd = open(dev_path, O_RDWR);
	if (fd == -1){
        bb_debug("earseSDPartition open %s fail! \n",dev_path);
		return -1;
	}
    mmc_ioc_erase_cmd *cmd = (mmc_ioc_erase_cmd*) malloc(sizeof(mmc_ioc_erase_cmd));
    memset(cmd,0,sizeof(mmc_ioc_erase_cmd));
    cmd->flags=0xa;

	int ret = ioctl(fd, MMC_IOC_ERASE_CMD, (unsigned long)(cmd));

    bb_debug("MMC_IOC_ERASE_CMD ioctl result %d\n",ret);
	close(fd);
    free( cmd);
	return ret;

}
