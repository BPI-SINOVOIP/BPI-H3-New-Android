
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <errno.h>

#include "BootHead.h"
#include "Utils.h"

#define NAND_BLKBURNBOOT0 		_IO('v',127)
#define NAND_BLKBURNUBOOT 		_IO('v',128)

#define SECTOR_SIZE	512
/*Normal boot 0 and boot 1 */
#define SD_BOOT0_SECTOR_START	16
#define SD_BOOT0_BACKUP_SECTOR_START	256
#define SD_BOOT0_SIZE_KBYTES	32

#define SD_UBOOT_SECTOR_START   32800
#define SD_UBOOT_BACKUP_SECTOR_START   24576
#define SD_UBOOT_SIZE_KBYTES	640

/*TOC0 : sector 16 - 256*/
#define SD_TOC0_SECTOR_START   16
#define SD_TOC0_BACKUP_SECTOR_START   256
#define SD_TOC0_SIZE_KBYTES	   120

/*TOC1 : sector 32800 - 40960 */
#define SD_TOC1_SECTOR_START   32800
#define SD_TOC1_BACKUP_SECTOR_START   24576
#define SD_TOC1_SIZE_KBYTES	   4080 
static int sd_boot0_start , sd_boot0_backup_start , sd_boot0_len;
static int sd_boot1_start , sd_boot1_backup_start , sd_boot1_len;

/*some chip (e.g. H8) BOOT0 save in mmcblk0boot0 */
#define SD_BOOT0_PERMISION "/sys/block/mmcblk0boot0/force_ro"
#define DEVNODE_PATH_SD_BOOT0 "/dev/block/mmcblk0boot0"

void SdBootInit(void)
{
	static int inited = 0;

	if(inited == 1)
		return ;
    
	if(check_soc_is_secure()){ //secure
		sd_boot0_start	=	SD_TOC0_SECTOR_START *  SECTOR_SIZE;
		sd_boot0_backup_start	=	SD_TOC0_BACKUP_SECTOR_START *  SECTOR_SIZE;
		sd_boot0_len	=	SD_TOC0_SIZE_KBYTES * 1024;
		sd_boot1_start	=	SD_TOC1_SECTOR_START * SECTOR_SIZE ;
		sd_boot1_backup_start	=	SD_TOC1_BACKUP_SECTOR_START * SECTOR_SIZE ;
		sd_boot1_len	=	SD_TOC1_SIZE_KBYTES * 1024;
	}else{
		sd_boot0_start	=	SD_BOOT0_SECTOR_START *  SECTOR_SIZE;
		sd_boot0_backup_start	=	SD_BOOT0_BACKUP_SECTOR_START *  SECTOR_SIZE;
		sd_boot0_len	=	SD_BOOT0_SIZE_KBYTES * 1024;
		sd_boot1_start	=	SD_TOC1_SECTOR_START * SECTOR_SIZE ;
		sd_boot1_backup_start	=	SD_UBOOT_BACKUP_SECTOR_START * SECTOR_SIZE ;
		sd_boot1_len	=	SD_UBOOT_SIZE_KBYTES * 1024;
	}
	inited =1 ;
	return ;
}

static int writeSdBoot(int fd, void *buf, off_t offset, size_t bootsize){
	if (lseek(fd, 0, SEEK_SET) == -1) {
		bb_debug("reset the cursor failed! the error num is %d:%s\n",errno,strerror(errno));
		return -1;
	}

	if (lseek(fd, offset, SEEK_CUR) == -1) {
		bb_debug("lseek failed! the error num is %d:%s\n",errno,strerror(errno));
		return -1;
	}
	bb_debug("Write sd boot : offset = 0x%x, len= 0x%x\n", offset, bootsize);
	int result = write(fd, buf, bootsize);
	fsync(fd);
	return result;
}

static int readSdBoot(int fd ,off_t offset, size_t bootsize, void *buffer){
	memset(buffer, 0, bootsize);
	if (lseek(fd, 0, SEEK_SET) == -1) {
		bb_debug("reset the cursor failed! the error num is %d:%s\n",errno,strerror(errno));
		return -1;
	}

	if (lseek(fd, offset, SEEK_CUR) == -1) {
		bb_debug("lseek failed! the error num is %d:%s\n",errno,strerror(errno));
		return -1;
	}

	return	read(fd,buffer,bootsize);
}

static int openDevNode(const char *path){
	int fd = open(path, O_RDWR);
	if (fd == -1){
		bb_debug("open device node(%s) failed ! errno is %d : %s\n", path,errno, strerror(errno));
	}
	return fd;
}

size_t readSdBoot0(char *path, void **buffer){

	*buffer = malloc(sd_boot0_len);
	int fd = openDevNode(path);
	if (fd == -1)
		return -1;
	
	return readSdBoot(fd, sd_boot0_start, sd_boot0_len, *buffer);

}

#if defined CONFIG_DUMP_DRAM_PARM
static void dump_buffer(char *buffer,int len)
{
	int i = 0;
	for(i=0;i<len/4;i++)
		printf("dram[%d] = %08x\n",i,*(unsigned int *)(buffer+i*4));
	return;
}
#endif

//read old boot0/toc0 dram freq para,and use it to replace the new boot0/toc
static int updateSdBoot0(char *new_boot0)
{
	int ret = -1;
	int dram_para_offset = 0;
	//prepare buffer for read old boot0 in the flash
	BufferExtractCookie* old_boot0 = malloc(sizeof(BufferExtractCookie));
	if(old_boot0 == NULL)
	{
		printf("malloc for BufferExtractCookie failed\n");
		goto clean;
	}
	old_boot0->buffer = malloc(sd_boot0_len);
	if(old_boot0->buffer == NULL)
	{
		printf("malloc for BufferExtractCookie->buffer failed\n");
		free(old_boot0);
		goto clean;
	}
    old_boot0->len = sd_boot0_len;
	//read the old boot0 and check it
	ret = readSdBoot0("/dev/block/mmcblk0",&old_boot0->buffer);
	if(ret == -1)
	{
		printf("read sd boot0 failed\n");
		goto clean;
	}
	if(checkBoot0Sum(old_boot0))
	{
		printf("crc check of old boot0 failed\n");
		goto clean;
	}
	//get old boot0 dram freq and put it into new boot0
	//dram freq para offset in old boot0 is 56-59 byte in normal
	//dram freq para offset in old toc0 is 132-135 byte in secure
	if(check_soc_is_secure()){
	  dram_para_offset = 132;
	}
	else
	{
	  dram_para_offset = 56;
	}
	//use old dram_clk to replace new dram para
	memcpy(new_boot0+dram_para_offset,old_boot0->buffer+dram_para_offset,4);
	//use old dram_tpr7 to replace new dram para
	memcpy(new_boot0+dram_para_offset+17*4,old_boot0->buffer+dram_para_offset +17*4,4);
	#if defined CONFIG_DUMP_DRAM_PARM
	printf("dump dram para from old boot0\n");
	dump_buffer(old_boot0->buffer+dram_para_offset,96);
	printf("dump dram para from new boot0\n");
	dump_buffer(new_boot0+dram_para_offset,96);
	#endif

	ret = genBoot0CheckSum(new_boot0);
	if(ret == -1)
	{
		printf("genBoot0CheckSum failed\n");
		goto clean;
	}
	ret = 0;
	clean:
	if(old_boot0->buffer != NULL)
		free(old_boot0->buffer);
	if(old_boot0 != NULL)
		free(old_boot0);
	return ret;
}

//just for boot2.0
size_t readSdUboot(char *path, void **buffer){
	bb_debug("reading SD Uboot!\n");
	
	int fd = openDevNode(path);
	if (fd == -1) 
		return -1;
	uboot_file_head *ubootHead;
	ubootHead = malloc(sizeof(uboot_file_head));
	readSdBoot(fd, sd_boot1_start, sizeof(uboot_file_head), ubootHead);
	size_t length = (size_t)ubootHead->length;
	free(ubootHead);

	*buffer = malloc(length);
	bb_debug("the inner uboot length is %d\n", length);
	return readSdBoot(fd, sd_boot1_start, length, *buffer);
}

int burnSdBoot0(BufferExtractCookie *cookie, char *path)
{
	if (checkBoot0Sum(cookie)){
		bb_debug("illegal binary file!\n");
		return -1;
	}
    int ret = 0;
    int fd = -1;

	/*read the old boot0 in flash,then get the dram freq to replace the new boot0*/
	/*ret = updateSdBoot0(cookie->buffer);
	if(ret == -1)
	{
		printf("updateSdBoot0 failed\n");
		return -1;
	}*/
    /*Try to write boot0 on DEVNODE_PATH_SD_BOOT0*/
    fd = openDevNode(DEVNODE_PATH_SD_BOOT0);
    if (fd > 0)
    {
        int pmsFd = open(SD_BOOT0_PERMISION,O_WRONLY);
        if (pmsFd > 0)
        {
            ret = write(pmsFd,"0",1);
            close(pmsFd);
        }
        else
        {
	        bb_debug("can't open %s :%s \n", SD_BOOT0_PERMISION,strerror(errno));
            close(fd);
            return -1;
        }
        if (ret < 0)
        {
	        bb_debug("can't write 0 to %s :%s \n", SD_BOOT0_PERMISION,strerror(errno));
            close(fd);
            return ret;
        }

	    bb_debug("burnSdBoot0 in mmcblk0boot0 %d\n", ret);
        ret = writeSdBoot(fd, cookie->buffer, 0, sd_boot0_len);
        if (ret > 0){
		    bb_debug("burnSdBoot0 succeed! on %d writed %d bytes\n", 0, ret);
	    }
        fsync(fd);

	    close(fd);
    }
    fd = openDevNode(path);
	if (fd == -1){
       return -1;
	}

    bb_debug("burnSdBoot0 in mmcblk0:offset = 0x%x, len =0x%x\n", sd_boot0_start, cookie->len);
	ret = writeSdBoot(fd, cookie->buffer, sd_boot0_start , cookie->len);
	if (ret > 0){
	    bb_debug("burnSdBoot0 succeed! on %d writed %d bytes\n",sd_boot0_start, ret);
    }
	ret = writeSdBoot(fd, cookie->buffer, sd_boot0_backup_start , cookie->len);
	if (ret > 0){
	    bb_debug("burnSdBoot0 backup succeed! on %d writed %d bytes\n",sd_boot0_backup_start, ret);
    }
    fsync(fd);
    close(fd);

	if (ret > 0){
		bb_debug("burnSdBoot0 succeed! writed %d bytes\n", ret);
	}
	return ret;
}

int burnSdUboot(BufferExtractCookie *cookie, char *path){
	if (checkUbootSum(cookie)){
		bb_debug("illegal uboot binary file!\n");
		return -1;
	}

	bb_debug("uboot binary length is %ld\n",cookie->len);
	int fd = openDevNode(path);
	if (fd == -1){
		return -1;
	}
	int ret = -1;
	int defaultOffset = -1;
	

	ret = writeSdBoot(fd, cookie->buffer, sd_boot1_start, cookie->len);
	if (ret > 0){
		bb_debug("burnSdUboot succeed! on %d writed %d bytes\n",sd_boot1_start, ret);
	}
	ret = writeSdBoot(fd, cookie->buffer, sd_boot1_backup_start, cookie->len);
	if (ret > 0){
		bb_debug("burnSdUboot backup succeed! on %d writed %d bytes\n",sd_boot1_backup_start, ret);
	}

	fsync(fd);
	close(fd);

	return ret;
}

int burnNandBoot0(BufferExtractCookie *cookie, char *path){

	if (checkBoot0Sum(cookie)){
		bb_debug("illegal boot0 binary file!\n");
		return -1;
	}

	int fd = openDevNode(path);
	if (fd == -1){
		return -1;
	}

	clearPageCache();

	int ret = ioctl(fd,NAND_BLKBURNBOOT0,(unsigned long)cookie);

	if (ret) {
		bb_debug("burnNandBoot0 failed ! errno is %d : %s\n", errno, strerror(errno));
	}else{
		bb_debug("burnNandBoot0 succeed!\n");
	}

	close(fd);
	return ret;
}

int burnNandUboot(BufferExtractCookie *cookie, char *path){
	if (checkUbootSum(cookie)){
		bb_debug("illegal uboot binary file!\n");
		return -1;
	}

	int fd = openDevNode(path);
	if (fd == -1){
		return -1;
	}

	clearPageCache();

	int ret = ioctl(fd,NAND_BLKBURNUBOOT,(unsigned long)cookie);
	if (ret) {
		bb_debug("burnNandUboot failed ! errno is %d : %s\n", errno, strerror(errno));
	}else{
		bb_debug("burnNandUboot succeed!\n");
	}
	close(fd);

	return ret;
}
