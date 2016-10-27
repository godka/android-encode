#include "srs_librtmp.h"
#include <unistd.h>
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#define LOG_TAG "com.encode.androidencode.h264"

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
//com.encode.androidencode
extern "C"{
	JNIEXPORT int JNICALL Java_com_encode_androidencode_mythSender_RTMPInit(JNIEnv * env, jobject obj, jstring rtmpurl);
	JNIEXPORT int JNICALL Java_com_encode_androidencode_mythSender_RTMPProcess(JNIEnv * env, jobject obj, jbyteArray data, int len, long pts);
	JNIEXPORT void JNICALL Java_com_encode_androidencode_mythSender_RTMPClose(JNIEnv * env, jobject obj);
}
int InitSrsRTMP(const char* rtmpurl);
srs_rtmp_t rtmp = NULL;
FILE* file = NULL;
int Java_com_encode_androidencode_mythSender_RTMPProcess(JNIEnv * env, jobject obj, jbyteArray data, int len,long pts){
	if (rtmp){
		char* pdata = (char*) env->GetByteArrayElements(data, 0);
		int plength = len;
		int ret = srs_h264_write_raw_frames(rtmp, pdata, plength, pts, pts);
		env->ReleaseByteArrayElements(data, (jbyte*) pdata, 0);
		return ret;
	}
	return -1;
}
void Java_com_encode_androidencode_mythSender_RTMPClose(JNIEnv * env, jobject obj){
	if (rtmp){
		srs_rtmp_destroy(rtmp);
	}
	rtmp = NULL;
}
int InitSrsRTMP(const char* rtmpurl)
{
	do {
		rtmp = srs_rtmp_create(rtmpurl);

		if (srs_rtmp_handshake(rtmp) != 0) {
			rtmp = NULL;
			break;
		}

		if (srs_rtmp_connect_app(rtmp) != 0) {
			rtmp = NULL;
			break;
		}
		int ret = srs_rtmp_publish_stream(rtmp);
		if (ret != 0) {
			rtmp = NULL;
			break;
		}
		return 0;
	} while (0);
	return 1;
}
JNIEXPORT int JNICALL Java_com_encode_androidencode_mythSender_RTMPInit(JNIEnv * env, jobject obj, 
	jstring rtmpurl)
{
	int ret = 0;
	char* url = (char*) env->GetStringUTFChars(rtmpurl, 0);
	ret = InitSrsRTMP(url);
	env->ReleaseStringUTFChars(rtmpurl, url);
	return ret;
	//file = fopen("/sdcard/test.h264", "wb");
}
