#include "mythFFmpegEncoder.hh"
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#define LOG_TAG "com.encode.androidencode.h264"
void swapYV12toI420(char* yv12bytes,int width, int height);
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
//com.encode.androidencode
extern "C"{
JNIEXPORT void JNICALL Java_com_encode_androidencode_mythSender_NativeEncoderInit(JNIEnv * env, jobject obj,int width,int height);
JNIEXPORT int JNICALL Java_com_encode_androidencode_mythSender_NativeProcessFrame(JNIEnv * env, jobject obj,jbyteArray data);
JNIEXPORT void JNICALL Java_com_encode_androidencode_mythSender_NativeEncoderClose(JNIEnv * env, jobject obj);
}
//void swap(char* yv12bytes, char* i420bytes, int width, int height);
FILE* file = NULL;
mythFFmpegEncoder* ffmpegenc = NULL;
int mwidth = 0;
int mheight = 0;
JNIEXPORT void JNICALL Java_com_encode_androidencode_mythSender_NativeEncoderInit(JNIEnv * env, jobject obj,
		int width,int height){
	mwidth = width;mheight = height;
	ffmpegenc = mythFFmpegEncoder::CreateNew(NULL,mwidth,mheight);
}

void swapYV12toI420(char* yv12bytes,int width, int height) {
    for(int i = width * height ; i < width * height + width * height  / 2 ;i+= 2){
    	char tmp = yv12bytes[i + 1];
    	yv12bytes[i] = yv12bytes[i + 1];
    	yv12bytes[i + 1] = tmp;
    }
}

JNIEXPORT int JNICALL Java_com_encode_androidencode_mythSender_NativeProcessFrame(JNIEnv * env, jobject obj,
		jbyteArray data){
	char* mdata= (char*)env->GetByteArrayElements(data, 0);
	int len = env->GetArrayLength(data);
	char* src[] = {mdata,mdata + mwidth * mheight + mwidth * mheight / 4,mdata + mwidth * mheight};
	swapYV12toI420(mdata,mwidth,mheight);
	int srclen[] = {mwidth,mwidth / 2,mwidth / 2};
	char* retdata;int retlen;
	if(ffmpegenc->ProcessFrameAsync((unsigned char**)src,srclen,&retdata,&retlen) > 0){
		if(retlen > 0){
			env->SetByteArrayRegion(data, 0, retlen, (jbyte*)retdata);
			return retlen;
		}
	}
	return 0;
}

JNIEXPORT void JNICALL Java_com_encode_androidencode_mythSender_NativeEncoderClose(JNIEnv * env, jobject obj){
	if(ffmpegenc){
		delete ffmpegenc;
		ffmpegenc = NULL;
	}
}
