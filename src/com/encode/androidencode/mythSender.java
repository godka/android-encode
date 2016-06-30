package com.encode.androidencode;
import java.util.LinkedList;
import java.util.List;

import com.interfaces.androidencode.mythPackage;

import android.os.SystemClock;
import android.util.Log;

public class mythSender implements Runnable{
	private boolean blinker = false;
	private EncodeMode m_mode;
	private mythArgs m_args;
	private mythPackage m_packet = null;
	private AvcEncoder m_avcencoder = null;
	private byte[] h264 = null;
	List<byte[]> m_list = null;
	public mythSender(mythArgs args,EncodeMode Mode){
		super();
		m_args = args;
		m_mode = Mode;
		m_avcencoder = new AvcEncoder(args.getW(), args.getH(), args.GetFrameRate(), args.GetBitrate());
		h264 = new byte[m_avcencoder.GetH() * m_avcencoder.GetW() * 3 / 2];
		m_packet = new mythPackage(args.getIP(), args.getCameraID());
		m_packet.Connect();
		m_list = new LinkedList<byte[]>();
	}
	public void Stop(){
		blinker = true;
	}
	public void AddData(byte[] data){
		byte[] newbyte = new byte[data.length];
		System.arraycopy(data, 0, newbyte, 0, data.length);
		m_list.add(newbyte);
	}
	@Override
	public void run() {
        while (blinker == false) {
			try{
				if(!m_list.isEmpty()){
					byte[] tmp = m_list.get(0);
					if(tmp != null){
						if(m_packet != null){
							long t1 = SystemClock.uptimeMillis();
							if(m_avcencoder != null){
								int ret1 = m_avcencoder.offerEncoder(tmp,h264);
								if(ret1 > 0){
									m_packet.SendPacket(h264, ret1);
								}
							}
							long t2 = SystemClock.uptimeMillis();
							Log.v("h264", t2 - t1 + "ms");
						}
						m_list.remove(0);
						if(m_list.size() >= 10){
							for(int i = 0;i < 9;i++)
								m_list.remove(0);
						}
						System.gc();
					}
				}
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		m_packet.Close();
	}  
	//delete software mode
/*
    static {
		System.loadLibrary("ffmpeg");
        System.loadLibrary("main");
    }

    public static native void NativeEncoderInit(int width,int height);
    public static native int NativeProcessFrame(byte[] data);
    public static native void NativeEncoderClose();
    */
	
}