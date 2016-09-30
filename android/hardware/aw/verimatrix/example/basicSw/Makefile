PRJROOT		= ../..
BUILD_TARGET	= vmclient.exe
SRCS		= main.c io.c
EXTRA_INCLUDES	= -I../../include
EXTRA_CFLAGS    = 

include $(PRJROOT)/rules.mk


all:: vmclient

vmclient: vmclient.exe
	@echo "#!/bin/bash" > $@
	@echo "export LD_LIBRARY_PATH=../../libs" >> $@
	@echo "./vmclient.exe" "$$""*" >> $@
	@chmod a+x $@

clean::
	@rm -f vmclient
	
