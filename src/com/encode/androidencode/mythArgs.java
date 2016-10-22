package com.encode.androidencode;

public class mythArgs {
	private String mip;
	private int mcameraid;
	private int mwidth;
	private int mheight;
	private int mframerate;
	private int mbitrate;
	public mythArgs(String RTMPLink,int width,int height,int framerate,int bitrate){
		mip = RTMPLink;mwidth=width;mheight = height;
		mframerate = framerate;mbitrate=bitrate;
	}
	public String getRTMPLink(){
		return mip;
	}
	public int getCameraID(){
		return mcameraid;
	}
	public int getW(){
		return mwidth;
	}
	public int getH(){
		return mheight;
	}
	public int GetFrameRate(){
		return mframerate;
	}
	public int GetBitrate(){
		return mbitrate;
	}
}
