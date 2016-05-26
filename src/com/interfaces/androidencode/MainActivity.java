package com.interfaces.androidencode;

import java.io.IOException;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.StrictMode;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import com.encode.androidencode.AvcEncoder;
import com.encode.androidencode.EncodeMode;
import com.encode.androidencode.mythArgs;
import com.encode.androidencode.mythSender;

public class MainActivity extends Activity implements SurfaceHolder.Callback, PreviewCallback 
{	
	AvcEncoder avcCodec;
    public Camera m_camera;  
    SurfaceView   m_prevewview;
    SurfaceHolder m_surfaceHolder;
    Thread t;
    int width = 640;
    int height = 480;
    int framerate = 25;
    int bitrate = 400000;
    mythPackage packet = null;
    mythSender sender = null;
    //private byte[] h264 = new byte[width * height * 3 / 2];
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads()
        .detectDiskWrites()
        .detectAll()   // or .detectAll() for all detectable problems
        .penaltyLog()
        .build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        .detectLeakedSqlLiteObjects()
        .detectLeakedClosableObjects()
        .penaltyLog()
        .penaltyDeath()
        .build());
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		m_prevewview = (SurfaceView) findViewById(R.id.SurfaceViewPlay);
		m_surfaceHolder = m_prevewview.getHolder();
		m_surfaceHolder.setFixedSize(width, height);
		m_surfaceHolder.addCallback((Callback) this);	

		Bundle bundle = this.getIntent().getExtras();
		String ip = bundle.getString("ip");
		String id = bundle.getString("id");
		mythArgs args = new mythArgs(ip,Integer.parseInt(id),width,height,framerate,bitrate);
		//mythArgs args = new mythArgs("192.168.0.124",10023,width,height,framerate,bitrate);
		sender = new mythSender(args,EncodeMode.HardwareMode);

		t = new Thread(sender);
		t.start();
		//packet.Connect();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) 
	{
	
	}

	@SuppressLint("NewApi")
	@Override
	public void surfaceCreated(SurfaceHolder arg0) 
	{
		try 
		{
			m_camera = Camera.open();
			m_camera.setPreviewDisplay(m_surfaceHolder);
			Camera.Parameters parameters = m_camera.getParameters();
			parameters.setPreviewSize(width, height);
			parameters.setPictureSize(width, height);
			parameters.setPreviewFormat(ImageFormat.NV21);
			m_camera.setParameters(parameters);	
			m_camera.setPreviewCallback((PreviewCallback) this);
			m_camera.startPreview();
			
		} catch (IOException e) 
		{
			e.printStackTrace();
		}	
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) 
	{
		m_camera.setPreviewCallback(null); 
		m_camera.stopPreview(); 
		m_camera.release();
		m_camera = null; 
		sender.Stop();
		//t.stop();
		finish();
	}

	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) 
	{
		sender.AddData(data);
	}

}
