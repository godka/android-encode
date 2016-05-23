#pragma once
extern "C"{
#ifndef INT64_C
#define INT64_C(c) (c ## LL)
#define UINT64_C(c) (c ## ULL)
#endif
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libavutil/avutil.h"
#include "libavutil/opt.h"       // for av_opt_set  
}
class mythFFmpegEncoder
{
public:
	static void RGB2yuv(int width, int height, int stride, const void* src, void** dst);
	typedef void (responseHandler)(void *myth, char* pdata, int plength);
	mythFFmpegEncoder(void* phwnd, int width, int height);
	~mythFFmpegEncoder(void);
	bool Init();
	void Cleanup();
	int ProcessFrameAsync(unsigned char** src, int* srclinesize, char** data,int* length);
	void ProcessFrame(unsigned char** src, int* srclinesize, responseHandler* response);
	void yuv2RGB(int width, int height, const void** src, int* src_linesize, void** dst);
	void yuv2RGB(int width, int height,
		const void* ysrc, const void* usrc, const void* vsrc,
		int ysize, int usize, int vsize, void** dst);
	static mythFFmpegEncoder* CreateNew(void* hwnd, int width, int height);//º¯ÊýÈë¿Ú
public:
	void* hwnd;
protected:
	AVCodec *video_codec;
	AVCodecContext *c;
	AVFrame *frame;
	AVPacket avpkt;
	int mwidth;
	int mheight;
};

