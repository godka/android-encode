package com.encode.androidencode;
import java.util.LinkedList;
import java.util.Queue;
import com.interfaces.androidencode.mythPackage;

import android.os.SystemClock;
import android.util.Log;

public class mythSender implements Runnable{
	private boolean blinker = false;
	private mythPackage m_packet = null;
	private AvcEncoder m_avcencoder = null;
	private byte[] h264 = null;
	Queue<byte[]> m_list = null;
	public mythSender(mythArgs args,EncodeMode Mode){
		super();
		m_avcencoder = new AvcEncoder(args.getW(), args.getH(), args.GetFrameRate(), args.GetBitrate());
		h264 = new byte[m_avcencoder.GetH() * m_avcencoder.GetW() * 3 / 2];
		m_packet = new mythPackage(args.getIP(), args.getCameraID());
		m_packet.Connect();
		m_list = new LinkedList<byte[]>();
	}
	public AvcEncoder GetAvcEncoder(){
		return m_avcencoder;
	}
	public void Stop(){
		blinker = true;
	}
	public void AddData(byte[] data){
		byte[] newbyte = new byte[data.length];
		System.arraycopy(data, 0, newbyte, 0, data.length);
		m_list.offer(newbyte);
	}
	@Override
	public void run() {
        while (blinker == false) {
			try{
				//if(!m_list.isEmpty()){
					byte[] tmp = m_list.poll();
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
						if(m_list.size() >= 10){
							for(int i = 0;i < 9;i++)
								m_list.poll();
						}
						System.gc();
					}
				//}
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		m_packet.Close();
	}  
}