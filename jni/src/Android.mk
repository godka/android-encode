LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := main
FFMPEG_PATH := ../ffmpeg

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(FFMPEG_PATH)
	
# Add your application source files here...
LOCAL_SRC_FILES := mythMain.cpp srs_librtmp.cpp

LOCAL_SHARED_LIBRARIES := 

LOCAL_LDLIBS := -lGLESv1_CM -llog

include $(BUILD_SHARED_LIBRARY)
