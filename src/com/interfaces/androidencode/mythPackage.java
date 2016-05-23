package com.interfaces.androidencode;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.text.format.Time;

public class mythPackage {
	private Socket socket  = null;
	private String m_ip = null;
	private int m_cameraid = 0;
	private int iFrameCount = 0;
	private boolean m_isConnect = false;
	private boolean m_isFirst = true;
	public boolean isConnect(){
		return m_isConnect;
	}
	//private String Message;
	public mythPackage(String ip,int cameraid){
		m_ip = ip;
		m_cameraid = cameraid;
	}
	 
	public int Connect(){
		try {
			InetAddress theaddress = InetAddress.getByName(m_ip);
			InetSocketAddress address = new InetSocketAddress(theaddress,5834);
			 
			socket = new Socket();
			socket.connect(address,1000);
			m_isConnect = true;
		} catch (IOException e2) {
			m_isConnect = false;
			return -1;
		}
		 return 0;
	}
	public int Close(){
		if(socket != null){
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return 0;
	}
	public int SendPacket(byte[] data,int len){
		try {
			if(isConnect() == true){
				OutputStream out = socket.getOutputStream();
				//out = new OutputStreamWriter(socket.getOutputStream());
				if(m_isFirst == true){
					String tmpfirst = String.format("PUT /CameraID=%d&Type=zyh264 HTTP/1.0\r\n\r\n",m_cameraid);
					out.write(tmpfirst.getBytes());
					out.flush();
					m_isFirst = false;
				}
				Time time = new Time();time.setToNow();
				String tmp = String.format("Content-Type: image/h264\r\nContent_Length: %06d Stamp:%04x%02x%02x %04x%02x%02x %02d %08x\n\n", 
						len,time.year,time.month,time.monthDay,time.hour,time.minute,time.second,0,iFrameCount);
				iFrameCount++;
				out.write(tmp.getBytes());
				out.write(data,0,len);
				out.write(" \n\n--myboundary\n".getBytes());
				out.flush();
			}else{
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				socket.close();
				Connect();
			}
		}catch(IOException e){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Connect();
			e.printStackTrace();
		}
		return 0;
	}
	public int SendPacket(byte[] data){
		return SendPacket(data,data.length);
	}
}
