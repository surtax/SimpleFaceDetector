package com.exercise.AndroidCamera;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AndroidCamera extends Activity implements SurfaceHolder.Callback{

	Camera camera;
	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;
	boolean previewing = false;
	LayoutInflater controlInflater = null;
	
	Button buttonTakePicture;
	TextView prompt;
	
	DrawingView drawingView;
	Face[] detectedFaces;
	
	final int RESULT_SAVEIMAGE = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView)findViewById(R.id.camerapreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        drawingView = new DrawingView(this);
        LayoutParams layoutParamsDrawing 
        	= new LayoutParams(LayoutParams.FILL_PARENT, 
        			LayoutParams.FILL_PARENT);
        this.addContentView(drawingView, layoutParamsDrawing);
        
        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, null);
        LayoutParams layoutParamsControl 
        	= new LayoutParams(LayoutParams.FILL_PARENT, 
        			LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);
        
        buttonTakePicture = (Button)findViewById(R.id.takepicture);
        buttonTakePicture.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				camera.takePicture(myShutterCallback, 
						myPictureCallback_RAW, myPictureCallback_JPG);
			}});
        
        LinearLayout layoutBackground = (LinearLayout)findViewById(R.id.background);
        layoutBackground.setOnClickListener(new LinearLayout.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				buttonTakePicture.setEnabled(false);
				camera.autoFocus(myAutoFocusCallback);
			}});
        
        prompt = (TextView)findViewById(R.id.prompt);
    }
    
    FaceDetectionListener faceDetectionListener
    = new FaceDetectionListener(){

		@Override
		public void onFaceDetection(Face[] faces, Camera camera) {
			
			if (faces.length == 0){
				prompt.setText(" No Face Detected! ");
				drawingView.setHaveFace(false);
			}else{
				prompt.setText(String.valueOf(faces.length) + " Face Detected :) ");
				drawingView.setHaveFace(true);
				detectedFaces = faces;
			}
			
			drawingView.invalidate();
			
		}};
    
    AutoFocusCallback myAutoFocusCallback = new AutoFocusCallback(){

		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			// TODO Auto-generated method stub
			buttonTakePicture.setEnabled(true);
		}};
    
    ShutterCallback myShutterCallback = new ShutterCallback(){

		@Override
		public void onShutter() {
			// TODO Auto-generated method stub
			
		}};
		
	PictureCallback myPictureCallback_RAW = new PictureCallback(){

		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			// TODO Auto-generated method stub
			
		}};
		
	PictureCallback myPictureCallback_JPG = new PictureCallback(){

		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			// TODO Auto-generated method stub
			/*Bitmap bitmapPicture 
				= BitmapFactory.decodeByteArray(arg0, 0, arg0.length);	*/
			
			Uri uriTarget = getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, new ContentValues());

			OutputStream imageFileOS;
			try {
				imageFileOS = getContentResolver().openOutputStream(uriTarget);
				imageFileOS.write(arg0);
				imageFileOS.flush();
				imageFileOS.close();
				
				prompt.setText("Image saved: " + uriTarget.toString());
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			camera.startPreview();
			camera.startFaceDetection();
		}};

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		if(previewing){
			camera.stopFaceDetection();
			camera.stopPreview();
			previewing = false;
		}
		
		if (camera != null){
			try {
				camera.setPreviewDisplay(surfaceHolder);
				camera.startPreview();

				prompt.setText(String.valueOf(
						"Max Face: " + camera.getParameters().getMaxNumDetectedFaces()));
				camera.startFaceDetection();
				previewing = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		camera = Camera.open();
		camera.setFaceDetectionListener(faceDetectionListener);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		camera.stopFaceDetection();
		camera.stopPreview();
		camera.release();
		camera = null;
		previewing = false;
	}
	
	private class DrawingView extends View{
		
		boolean haveFace;
		Paint drawingPaint;

		public DrawingView(Context context) {
			super(context);
			haveFace = false;
			drawingPaint = new Paint();
			drawingPaint.setTextSize(40);
			drawingPaint.setColor(Color.GREEN);
			drawingPaint.setStyle(Paint.Style.STROKE); 
			drawingPaint.setStrokeWidth(2);
		}
		
		public void setHaveFace(boolean h){
			haveFace = h;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// TODO Auto-generated method stub
			if(haveFace){

				// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
				 // UI coordinates range from (0, 0) to (width, height).
				 
				 int vWidth = getWidth();
				 int vHeight = getHeight();
				
				for(int i=0; i<detectedFaces.length; i++){
					
					int l = detectedFaces[i].rect.left;
					int t = detectedFaces[i].rect.top;
					int r = detectedFaces[i].rect.right;
					int b = detectedFaces[i].rect.bottom;
					int left	= (l+1000) * vWidth/2000;
					int top		= (t+1000) * vHeight/2000;
					int right	= (r+1000) * vWidth/2000;
					int bottom	= (b+1000) * vHeight/2000;
					canvas.drawRect(
							left, top, right, bottom,  
							drawingPaint);
					
					canvas.drawCircle((left+right)/2, (top+bottom)/2, (bottom-top)/2, drawingPaint);

					
					Random randomGenerator = new Random();
					
					int min = 8900;
				    int max = 9001;

					int randomNum = randomGenerator.nextInt(max - min + 1) + min;
					
					canvas.drawText(randomNum + "", left, top, drawingPaint);
					
				}
			}else{
				canvas.drawColor(Color.TRANSPARENT);
			}
		}
		
	}
}