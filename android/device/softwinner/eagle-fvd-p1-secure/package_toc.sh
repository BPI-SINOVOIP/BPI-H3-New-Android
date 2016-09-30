#!/bin/bash

verity_data_init()
{

	simg2img ${OUT}/system.img ${OUT}/system.img.new
	cp -rf $DEVICE/verity ${OUT}
	###cp -vf ${OUT}/system.img.new ${OUT}/system.img
	$DEVICE/verity/gen_dm_verity_data.sh ${OUT}/system.img.new 
	cp -f ${OUT}/verity/verity_block ${OUT}/verity_block.img
	cp -f $OUT/verity/rsa_key/verity_key  $OUT/root/verity_key
}

verity_file_init()
{
	cp -rf $DEVICE/verity ${OUT}
	$DEVICE/verity/gen_file_verify_block.sh ${OUT}/system 
	cp -f ${OUT}/verity/verity_block ${OUT}/verity_block.img
}

verity_file_init
cd $PACKAGE

chip=sun8iw6p1
platform=android
board=eagle-p1-secure
debug=uart0
sigmode=secure

usage()
{
	printf "Usage: pack [-cCHIP] [-pPLATFORM] [-bBOARD] [-d] [-s] [-h]
	-c CHIP (default: $chip)
	-p PLATFORM (default: $platform)
	-b BOARD (default: $board)
	-d pack firmware with debug info output to card0
	-s pack firmware with signature
	-h print this help message
"
}

while getopts "c:p:b:dsh" arg
do
	case $arg in
		c)
			chip=$OPTARG
			;;
		p)
			platform=$OPTARG
			;;
		b)
			board=$OPTARG
			;;
		d)
			debug=card0
			;;
		s)
			sigmode=secure
			;;
		h)
			usage
			exit 0
			;;
		?)
			exit 1
			;;
	esac
done

toc0_test_package=( toc0 boot0 ) 
toc1_test_package=( rootkey u-boot semelis boot recovery )
test_package=secboot_test

if [ ! -d  ${test_package} ]; then
	mkdir ${test_package} 
	openssl genrsa -out  ${test_package}/Trustkey_test.pem 2048 >> /dev/null
	openssl rsa -in Trustkey_test.pem -text -modulus > ${test_package}/Trustkey_test.bin
fi
rm -rf ${test_package}/*.img


for TOC0 in ${toc0_test_package[@]}; do 
	./pack -c $chip -p $platform -b $board -d $debug -s $sigmode -t ${TOC0}
	find ./ -maxdepth 1 -cmin -2 -name "*.img" -exec cp -f {} ${test_package}/${TOC0}_destroy.img \;
	if [ $? -eq 0 ] ;then
		echo -e '\033[0;31;1m'
		echo "Generate $TOC0 Secure Boot Test Package"
		echo -e '\033[0m'
	else
		echo -e "\033[47;31mERROR:\n"  
		echo "Generate $TOC0 Secure Boot Test Package FAIL"
		echo -e '\033[0m'
		exit
	fi

done

for TOC1 in ${toc1_test_package[@]}; do 
	./pack -c $chip -p $platform -b $board -d $debug -s $sigmode -t ${TOC1}
	find ./ -maxdepth 1 -cmin -2 -name "*.img" -exec cp -f {} ${test_package}/${TOC1}_destroy.img \;
	if [ $? -eq 0 ] ;then
		echo -e '\033[0;31;1m'
		echo "Generate $TOC1 Secure Boot Test Package"
		echo -e '\033[0m'
	else
		echo -e "\033[47;31mERROR:\n"  
		echo "Generate $TOC1 Secure Boot Test Package FAIL"
		echo -e '\033[0m'
		exit
	fi
done

# change root key for toc1, toc1 should fail #
cp  ${test_package}/Trustkey_test.pem common/keys/Trustkey.pem
cp  ${test_package}/Trustkey_test.bin common/keys/Trustkey.bin
TOC1=toc1_rootpk
./pack -c $chip -p $platform -b $board -d $debug -s $sigmode -t ${TOC1} 
find ./ -maxdepth 1 -cmin -2 -name "*.img" -exec cp -f {} ${test_package}/toc1_rootkey_destroy.img \;
echo -e '\033[0;31;1m'
echo "Generate TOC1 Rootpublic Key Secure Boot Test Package"
echo -e '\033[0m'

# change root key , boot0 should can't boot-up #
TOC0=toc0_rootpk
./pack -c $chip -p $platform -b $board -d $debug -s $sigmode -t ${TOC0} 
find ./ -maxdepth 1 -cmin -2 -name "*.img" -exec cp -f {} ${test_package}/toc0_rootkey_destroy.img \;
echo -e '\033[0;31;1m'
echo "Generate TOC0 Rootpublic Key Secure Boot Test Package"
echo -e '\033[0m'
