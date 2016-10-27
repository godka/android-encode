package com.encode.androidencode;
import java.util.Date;

import android.media.MediaCodecInfo;
import android.util.Log;

public class mythSender{
	private boolean blinker = false;
	private AvcEncoder m_avcencoder = null;
	private byte[] out = null;
	private long presentationTimeUs = 0;
	private boolean isrtmpon = false;
	private byte[] frame = null;
	private String _rtmpurl;
	private byte[] sps;
	private byte[] pps;
	public mythSender(mythArgs args) {
		super();
		m_avcencoder = new AvcEncoder(args.getW(), args.getH(),
				args.GetFrameRate(), args.GetBitrate());
		out = new byte[m_avcencoder.GetH() * m_avcencoder.GetW() * 3 / 2];
		_rtmpurl = args.getRTMPLink();
		try {
			ConnectToRtmpUrl(_rtmpurl);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		isrtmpon = false;
	}

	public AvcEncoder GetAvcEncoder() {
		return m_avcencoder;
	}
	public int ConnectToRtmpUrl(String rtmpurl) throws InterruptedException{
		while(RTMPInit(rtmpurl) != 0){
			Log.v("rtmp connect", "connecting to " + rtmpurl);
			Thread.sleep(1000);
		}
		return 0;
	}
	public void Stop() throws InterruptedException {
		RTMPClose();
	}

	public void AddData(byte[] data) {
		if(frame == null)
			frame = new byte[data.length];
		int vcolor = m_avcencoder.GetColor();
		int width = m_avcencoder.GetW();
		int height = m_avcencoder.GetH();
		if (vcolor == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
			YV12toYUV420Planar(data, frame, width, height);
		} else if (vcolor == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar) {
			YV12toYUV420PackedSemiPlanar(data, frame, width, height);
		} else if (vcolor == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
			YV12toYUV420PackedSemiPlanar(data, frame, width, height);
		} else {
			System.arraycopy(data, 0, frame, 0, data.length);
		}
		//m_list.offer(frame);
		int ret1 = m_avcencoder.offerEncoder(frame, out);
		if (ret1 > 0) {
			int nalu = out[4] & 0x1f;
			if(nalu == 7){
				if(sps == null){
					sps = new byte[ret1];
					System.arraycopy(out, 0, sps, 0, ret1);
				}
			}else if(nalu == 8){
				if(pps == null){
					pps = new byte[ret1];
					System.arraycopy(out, 0, pps, 0, ret1);
				}
			}
			if(this.isrtmpon == false){
				if(nalu != 5 || sps == null){
					if(nalu != 5)
						Log.v("skip", "waiting for nalu" + nalu+"");
					if(sps == null)
						Log.v("skip", "waiting for sps");
					if(pps == null)
						Log.v("skip", "waiting for pps");
					return;
				}else{
					this.SendRTMPPacket(sps, sps.length, 0);
					if(pps != null)
						this.SendRTMPPacket(pps, pps.length, 0);
					this.SendRTMPPacket(out, ret1, 0);
					presentationTimeUs = new Date().getTime();
					this.isrtmpon = true;
				}
			}else{
				long pts = new Date().getTime() - presentationTimeUs;
				Log.v("pts","pts:" + pts+"");
				this.SendRTMPPacket(out, ret1, pts);
			}
		}
	}
	private void SendRTMPPacket(byte[] data,int len,long pts){
		int ret = RTMPProcess(data, len, pts);
		if (ret != 0) {
			if (h264_is_dvbsp_error(ret)) {
				Log.v("ignore", "ignore drop video error, code=" + ret);
			}
			else if (is_duplicated_sps_error(ret)) {
				Log.v("ignore", "ignore duplicated sps, code=" + ret);
			}
			else if (is_duplicated_pps_error(ret)) {
				Log.v("ignore", "ignore duplicated pps, code=" + ret);
			}
			else {
				Log.e("error", "send h264 raw data failed, code=" + ret);
				try {
					this.Stop();
					Thread.sleep(1000);
					this.ConnectToRtmpUrl(_rtmpurl);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else{
			Log.v("send h.264 raw success","len = " + len + "");
		}
	}
	private boolean is_duplicated_pps_error(int ret) {
		// TODO Auto-generated method stub
		return ret == 3043;
	}

	private boolean is_duplicated_sps_error(int ret) {
		// TODO Auto-generated method stub
		return ret == 3044;
	}

	private boolean h264_is_dvbsp_error(int ret) {
		// TODO Auto-generated method stub
		return ret == 3045;
	}

	// the color transform, @see
	// http://stackoverflow.com/questions/15739684/mediacodec-and-camera-color-space-incorrect
	private static byte[] YV12toYUV420PackedSemiPlanar(final byte[] input,
			final byte[] output, final int width, final int height) {
		/*
		 * COLOR_TI_FormatYUV420PackedSemiPlanar is NV12 We convert by putting
		 * the corresponding U and V bytes together (interleaved).
		 */
		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		System.arraycopy(input, 0, output, 0, frameSize); // Y

		for (int i = 0; i < qFrameSize; i++) {
			output[frameSize + i * 2] = input[frameSize + i + qFrameSize]; // Cb
																			// (U)
			output[frameSize + i * 2 + 1] = input[frameSize + i]; // Cr (V)
		}
		return output;
	}

	private static byte[] YV12toYUV420Planar(byte[] input, byte[] output,
			int width, int height) {
		/*
		 * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V
		 * reversed. So we just have to reverse U and V.
		 */
		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		System.arraycopy(input, 0, output, 0, frameSize); // Y
		System.arraycopy(input, frameSize, output, frameSize + qFrameSize,
				qFrameSize); // Cr (V)
		System.arraycopy(input, frameSize + qFrameSize, output, frameSize,
				qFrameSize); // Cb (U)

		return output;
	}
	
	static {
		System.loadLibrary("main");
	}

	public static native int RTMPInit(String rtmpurl);

	public static native int RTMPProcess(byte[] data, int len, long pts);

	public static native void RTMPClose();
}