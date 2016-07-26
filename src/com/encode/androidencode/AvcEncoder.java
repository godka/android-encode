package com.encode.androidencode;

import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

public class AvcEncoder 
{

	private MediaCodec mediaCodec;
	int m_width;
	int m_height;
	byte[] m_info = null;
	byte[] m_yv12 = null;
	public int GetW(){
		return m_width;
	}
	public int GetH(){
		return m_height;
	}
	@SuppressLint("NewApi") private static MediaCodecInfo selectCodec(String mimeType) {
	     int numCodecs = MediaCodecList.getCodecCount();
	     for (int i = 0; i < numCodecs; i++) {
	         MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

	         if (!codecInfo.isEncoder()) {
	             continue;
	         }

	         String[] types = codecInfo.getSupportedTypes();
	         for (int j = 0; j < types.length; j++) {
	             if (types[j].equalsIgnoreCase(mimeType)) {
	                 return codecInfo;
	             }
	         }
	     }
	     return null;
	 }

    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) 
    {      
    	System.arraycopy(yv12bytes, 0, i420bytes, 0,width*height);
    	for(int i = width * height ; i < width * height + width * height  / 2 ;i+= 2){
    		i420bytes[i] = yv12bytes[i + 1];
    		i420bytes[i + 1] = yv12bytes[i];
    	}
    	//System.arraycopy(yv12bytes, 0, i420bytes, 0,width*height*3/2);
    	//System.arraycopy(yv12bytes, width*height*5/4, i420bytes, width*height,width*height/4);
    	//System.arraycopy(yv12bytes, width*height, i420bytes, width*height*5/4,width*height/4);
    }
    /**
     * Returns a color format that is supported by the codec and by this test code.  If no
     * match is found, this throws a test failure -- the set of formats known to the test
     * should be expanded for new platforms.
     */
	@SuppressLint("NewApi") private static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
            	Log.v("h264", "colorformat = " + colorFormat + "");
                return colorFormat;
            }
        }
        Log.e("h264","couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;   // not reached
    }

    /**
     * Returns true if this is a color format that this test code understands (i.e. we know how
     * to read and generate frames in this format).
     */private static boolean isRecognizedFormat(int colorFormat) {
        switch (colorFormat) 
        {
            // these are the formats we know how to handle for this testcase MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
        	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
        	case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

	@SuppressLint("NewApi")
	public AvcEncoder(int width, int height, int framerate, int bitrate) { 
		
		m_width  = width;
		m_height = height;
		String mime = "video/avc";
		m_yv12 = new byte[width * height * 3 / 2];
		int format = selectColorFormat(selectCodec(mime),mime);
	    mediaCodec = MediaCodec.createEncoderByType(mime);
	    MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime, width, height);
	    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
	    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
	    if(format > 0)
	    	mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,format);  
	    else
	    	mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
	    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
	    try{
	    	mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
	    }catch(Exception ee){
	    	ee.printStackTrace();
	    }
	    mediaCodec.start();
	}

	@SuppressLint("NewApi")
	public void close() {
	    try {
	        mediaCodec.stop();
	        mediaCodec.release();
	    } catch (Exception e){ 
	        e.printStackTrace();
	    }
	}
	public int offerEncoderFFMPEG(byte[] input,byte[] output){
		return 0;
	}
	@SuppressLint("NewApi") 
	public int offerEncoder(byte[] input,byte[] output){
		int count = 0;
		int pos = 0;
		try 
		{
			swapYV12toI420(input,m_yv12,m_width,m_height);
				ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
				//Log.i("inputBufferssize", "" + inputBuffers.length);
				ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
				int inputBufferIndex = mediaCodec.dequeueInputBuffer(0);

				//Log.i("AvcEncoder inputBufferIndex", inputBufferIndex + "");
				if (inputBufferIndex >= 0) {
						ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
						inputBuffer.clear();
						inputBuffer.put(m_yv12);
						mediaCodec.queueInputBuffer(inputBufferIndex, 0, m_yv12.length,0, 0);
				}

				MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

				int outputBufferIndex = 0;
				while (count <= 10) {
					outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,10000);
					if (outputBufferIndex == -1) {
						count++;
					} else {
						break;
					}
				}
				if (outputBufferIndex >= 0) {
					ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
					byte[] outData = new byte[bufferInfo.size];
					outputBuffer.get(outData);
					if (m_info != null) {
						//System.arraycopy(headdata, 0, output, pos, headdata.length);
						//pos += headdata.length;
						System.arraycopy(outData, 0, output, pos, outData.length);
						pos += outData.length;
					} else {
						ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
						if (spsPpsBuffer.getInt() == 0x00000001) {
							m_info = new byte[outData.length];//first packet
							System.arraycopy(outData, 0, m_info, 0, outData.length);
						}
					}

					mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
					mediaCodec.flush();
				}
				
				int key = output[4] & 0x1F;
				if (key == 5) // key frame2
				{
					System.arraycopy(output, 0, m_yv12, 0, pos);
					System.arraycopy(m_info, 0, output, 0, m_info.length);
					System.arraycopy(m_yv12, 0, output, m_info.length, pos);
					pos += m_info.length;
				}
				

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return pos;
	}
	@SuppressLint("NewApi")
	public byte[] offerEncoder(byte[] input) {
		int count = 0;
		byte[] output = null;
		int pos = 0;
		// swapYV12toI420(input, yuv420, m_width, m_height);
		try 
		{
				ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
				Log.i("inputBufferssize", "" + inputBuffers.length);
				ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
				int inputBufferIndex = mediaCodec.dequeueInputBuffer(0);

				Log.i("AvcEncoder inputBufferIndex", inputBufferIndex + "");
				if (inputBufferIndex >= 0) {
						ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
						inputBuffer.clear();
						inputBuffer.put(input);
						mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length,0, 0);
				}

				MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

				int outputBufferIndex = 0;
				while (count <= 10) {
					outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,10000);
					Log.i("AvcEncoder outputBufferIndex", outputBufferIndex + "");
					if (outputBufferIndex == -1) {
						count++;
					} else {
						break;
					}
				}
				if (outputBufferIndex >= 0) {
					ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
					byte[] outData = new byte[bufferInfo.size];
					Log.i("h264.size", outData.length + "");
					output = new byte[outData.length];
					outputBuffer.get(outData);
					if (m_info != null) {
						System.arraycopy(outData, 0, output, pos, outData.length);
						pos += outData.length;
					} else {
						ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
						if (spsPpsBuffer.getInt() == 0x00000001) {
							m_info = new byte[outData.length];//first packet
							System.arraycopy(outData, 0, m_info, 0, outData.length);
						}
					}
					mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
					mediaCodec.flush();
				}
				int key = output[4] & 0x1F;
				if (key == 5)// || key == 7 || key == 8) // key frame2
				{
					System.arraycopy(output, 0, input, 0, pos);

					output = new byte[output.length + m_info.length];

					System.arraycopy(m_info, 0, output, 0, m_info.length);
					System.arraycopy(input, 0, output, m_info.length, pos);
					pos += m_info.length;
				}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return output;
	}
	/*
	 public int offerEncoder(byte[] input,byte[] output) {
	        int pos = 0;          
	        try {
	            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
	            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
	            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
	            if (inputBufferIndex >= 0) {
	                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
	                Log.d("h264","offerEncoder InputBufSize: " +inputBuffer.capacity()+" inputSize: "+input.length + " bytes");
	                inputBuffer.clear();
	                inputBuffer.put(input);
	                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);

	            }

	            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
	            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0);
	            while (outputBufferIndex >= 0) {
	                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

	                byte[] data = new byte[bufferInfo.size];
	                outputBuffer.get(data);

	                Log.d("h264","offerEncoder InputBufSize:"+outputBuffer.capacity()+" outputSize:"+ data.length + " bytes written");

	                if(mMediaHead != null)  
	                {                 
	                    System.arraycopy(data, 0,  output, pos, data.length);  
	                    pos += data.length;  
	                } else// 保存pps sps 只有开始时 第一个帧里有， 保存起来后面用
	                {
	                    Log.d("h264", "offer Encoder save sps head,len:"+data.length);
	                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(data);
	                    if (spsPpsBuffer.getInt() == 0x00000001) {
	                        mMediaHead = new byte[data.length];
	                        System.arraycopy(data, 0, mMediaHead, 0, data.length);
	                    } else {
	                        Log.e("h264","not found media head.");
	                        return -1;
	                    }
	                }

	                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
	                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);


	            }

	            if(output[4] == 0x65) //key frame   编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上  
	            {
	                System.arraycopy(output, 0,  input, 0, pos);  
	                System.arraycopy(mMediaHead, 0,  output, 0, mMediaHead.length);  
	                System.arraycopy(input, 0,  output, mMediaHead.length, pos);  
	                pos += mMediaHead.length;  
	            }  

	        } catch (Throwable t) {
	            t.printStackTrace();
	        }
	        return pos;
	    }
    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) 
    {      
    	System.arraycopy(yv12bytes, 0, i420bytes, 0,width*height);
    	System.arraycopy(yv12bytes, width*height+width*height/4, i420bytes, width*height,width*height/4);
    	System.arraycopy(yv12bytes, width*height, i420bytes, width*height+width*height/4,width*height/4);  
    }  
*/
	
}


