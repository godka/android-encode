LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := main
FFMPEG_PATH := ../ffmpeg

LOCAL_C_INCLUDES := $(LOCAL_PATH)/$(FFMPEG_PATH)
	
# Add your application source files here...
LOCAL_SRC_FILES := mythFFmpegEncoder.cpp mythMain.cpp

LOCAL_SHARED_LIBRARIES := ffmpeg

LOCAL_LDLIBS := -lGLESv1_CM -llog

include $(BUILD_SHARED_LIBRARY)
