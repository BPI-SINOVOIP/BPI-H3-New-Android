LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	bootanimation_main.cpp \
	BootAnimation.cpp \
	bootAudioManager.cpp

LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES -DEGL_EGLEXT_PROTOTYPES

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	liblog \
	libandroidfw \
	libutils \
	libbinder \
    libui \
	libskia \
    libEGL \
    libGLESv1_CM \
    libgui \
    libswconfig \
    libmedia \
    libsqlite

LOCAL_C_INCLUDES := \
	frameworks/base/swextend/utils/libswconfig \
	$(call include-path-for, corecg graphics)

LOCAL_C_INCLUDES += \
	external/sqlite/dist

LOCAL_MODULE:= bootanimation


include $(BUILD_EXECUTABLE)
