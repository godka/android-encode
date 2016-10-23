package com.encode.androidencode;

import java.util.LinkedList;
import java.util.Queue;

import android.media.MediaCodecInfo;
import android.os.SystemClock;
import android.util.Log;

public class mythSender implements Runnable {
	private boolean blinker = false;
	private AvcEncoder m_avcencoder = null;
	private byte[] h264 = null;
	Queue<byte[]> m_list = null;
	private long presentationTimeUs;

	public mythSender(mythArgs args) {
		super();
		m_avcencoder = new AvcEncoder(args.getW(), args.getH(),
				args.GetFrameRate(), args.GetBitrate());
		h264 = new byte[m_avcencoder.GetH() * m_avcencoder.GetW() * 3 / 2];
		RTMPInit(args.getRTMPLink());
		Log.v("rtmplink", args.getRTMPLink());
		m_list = new LinkedList<byte[]>();
		presentationTimeUs = SystemClock.uptimeMillis();
	}

	public AvcEncoder GetAvcEncoder() {
		return m_avcencoder;
	}

	public void Stop() {
		blinker = true;
	}

	public void AddData(byte[] data) {
		byte[] frame = new byte[data.length];
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
		m_list.offer(frame);
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

	@Override
	public void run() {
		while (blinker == false) {
			try {
				byte[] tmp = m_list.poll();
				if (tmp != null) {
					if (m_avcencoder != null) {
						int ret1 = m_avcencoder.offerEncoder(tmp, h264);
						if (ret1 > 0) {
							long pts = SystemClock.uptimeMillis()
									- presentationTimeUs;
							RTMPProcess(h264, ret1, pts);
						}
					}
				}
				if (m_list.size() >= 10) {
					for (int i = 0; i < 9; i++)
						m_list.poll();
				}
				System.gc();
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		RTMPClose();
	}

	static {
		System.loadLibrary("main");
	}

	public static native int RTMPInit(String rtmpurl);

	public static native void RTMPProcess(byte[] data, int len, long pts);

	public static native void RTMPClose();
}