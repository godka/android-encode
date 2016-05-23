#include "mythFFmpegEncoder.hh"
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#define LOG_TAG "com.encode.androidencode.h264"

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
//com.encode.androidencode
extern "C"{
JNIEXPORT void JNICALL Java_com_encode_androidencode_mythSender_NativeEncoderInit(JNIEnv * env, jobject obj,int width,int height);
JNIEXPORT jbyteArray JNICALL Java_com_encode_androidencode_mythSender_NativeProcessFrame(JNIEnv * env, jobject obj,jbyteArray data);
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
	file = fopen("/sdcard/test.h264","w");
}
JNIEXPORT jbyteArray JNICALL Java_com_encode_androidencode_mythSender_NativeProcessFrame(JNIEnv * env, jobject obj,
		jbyteArray data){
	char* mdata= (char*)env->GetByteArrayElements(data, 0);
	int len = env->GetArrayLength(data);
	//LOGE("width = %d,height = %d,len=%d\n",mwidth,mheight,len);
	char* src[] = {mdata,mdata + mwidth * mheight + mwidth * mheight / 4,mdata + mwidth * mheight};
	int srclen[] = {mwidth,mwidth / 2,mwidth / 2};
	char* retdata;int retlen;
	if(ffmpegenc->ProcessFrameAsync((unsigned char**)src,srclen,&retdata,&retlen) > 0){
		if(retlen > 0){
			fwrite(retdata,1,retlen,file);
			LOGE("write file success,filelen = %d\n",retlen);
			jbyteArray byteArray = env->NewByteArray(retlen);
			env->SetByteArrayRegion(byteArray, 0, retlen, (jbyte*)retdata);
			env->ReleaseByteArrayElements(data, (jbyte*)mdata, 0);
			return byteArray;
		}
	}
	env->ReleaseByteArrayElements(data, (jbyte*)mdata, 0);
	return NULL;
}

JNIEXPORT void JNICALL Java_com_encode_androidencode_mythSender_NativeEncoderClose(JNIEnv * env, jobject obj){
	if(ffmpegenc){
		delete ffmpegenc;
		ffmpegenc = NULL;
	}
}
