package com.mobilehci.palmtypingtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.google.android.glass.timeline.DirectRenderingCallback;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class LiveCardRenderer extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = "LiveCardRenderer";

	private int mSurfaceWidth;
	private int mSurfaceHeight;
	private SurfaceHolder mHolder;
	private RenderThread mRenderThread;

	private Paint paint;
	
	private FrameLayout mLayout;
	private Bitmap BackgroundImage;q
	
	private int frameCount=0;
	List<Point> point_list= new ArrayList<Point>(3);
	public static final long FRAME_TIME_MILLIS = 1;
	private boolean mPaused;
	private LiveCardActivity liveCardActivity;
	private Matrix matrix;
	private int charIndex=0;
	
	//test
	private char[] alphabet = "OBILEHCI".toCharArray();
		
	
	public LiveCardRenderer(Context context)
    {
        super(context);
        liveCardActivity = (LiveCardActivity)context;
        init();
    }
    public LiveCardRenderer(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        liveCardActivity = (LiveCardActivity)context;
        init();
    }
    public LiveCardRenderer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        liveCardActivity = (LiveCardActivity)context;
        init();
    }
	
	
	private void init(){
		
		this.setBackgroundColor(Color.TRANSPARENT);                 
	    this.setZOrderOnTop(true); //necessary                
	   
		mHolder = getHolder();
		mHolder.setFormat(PixelFormat.TRANSPARENT);
		mHolder.addCallback(this);
		
		BackgroundImage = BitmapFactory.decodeResource(liveCardActivity.getResources(), R.drawable.user20);
		   
		matrix = new Matrix();
		//matrix.postScale(640F/980F, 360F/551F);
		matrix.postScale(640F/1962F, 360F/1102F);
		
				
	}
	

	// Surface callbacks
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		doLayout();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		updateRendering();
	
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mRenderThread.quit();
	}

	// Layout
	private void doLayout() {
		// TODO Auto-generated method stub
		
	}
	
	
	private synchronized void updateRendering() {
        boolean shouldRender = (mHolder != null) && !mPaused;
        boolean rendering = mRenderThread != null;

        if (shouldRender != rendering) {
            if (shouldRender) {
            	Log.v("drawthread", "thread start");
                mRenderThread = new RenderThread(this);
                mRenderThread.start();
            } else {
            	Log.v("drawthread", "thread stop");
                mRenderThread.quit();
                mRenderThread = null;
            }
        }
    }
	
	// Painting
	synchronized void repaint() {
		Canvas canvas = null;
		
		try {
			canvas = mHolder.lockCanvas();
		} catch (RuntimeException e) {
			Log.d(TAG, "lockCanvas failed", e);
		}

		if (canvas != null) {
			drawKeyboard(canvas);

			try {
				mHolder.unlockCanvasAndPost(canvas);
			} catch (RuntimeException e) {
				Log.d(TAG, "unlockCanvasAndPost failed", e);
			}
		}
	}

	private void drawKeyboard(Canvas canvas) {
		
		
		Paint mPaint = new Paint();
		Path path = new Path();
		/*
		canvas.drawBitmap(BackgroundImage, matrix, mPaint);
		*/
		
		canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
		
		//75px height text area
		Rect rt = new Rect(0, 0, 640, 50);
		mPaint.setColor(Color.WHITE);
		mPaint.setAlpha(255);
		
		mPaint.setStyle(Paint.Style.FILL);
	    canvas.drawRect(rt, mPaint);
	    
	    //draw border
	    mPaint.setColor(Color.BLACK);
	    mPaint.setStyle(Paint.Style.STROKE);
	    mPaint.setStrokeWidth(3);
	    canvas.drawRect(rt, mPaint);
		
		//draw state
	    if(liveCardActivity.isStart){
	    	mPaint.setColor(Color.RED);
	    	mPaint.setStyle(Paint.Style.FILL);
	        canvas.drawCircle(620, 70, 10, mPaint);
	    }else{
	    	mPaint.setColor(Color.GREEN);
	    	mPaint.setStyle(Paint.Style.FILL);
	        canvas.drawCircle(620, 70, 10, mPaint);
	    }
	    
        mPaint.setColor(Color.RED);
        canvas.drawCircle(liveCardActivity.p_x, liveCardActivity.p_y, 3, mPaint);
	    
	    
    	String ckey ="";
    	
        //draw keyboard edge and  fill polygon
    	/*
		for (Iterator i = liveCardActivity.keyboard_edges.keySet().iterator(); i.hasNext();) {  
        	
        	ckey = (String) i.next();
        	path = new Path();
        	//Log.v("key",ckey);
        	path.moveTo(liveCardActivity.keyboard_edges.get(ckey).get(0).x, liveCardActivity.keyboard_edges.get(ckey).get(0).y);
        	
        	for(int j=0; j < liveCardActivity.keyboard_edges.get(ckey).size(); j++){
        		
        		path.lineTo(liveCardActivity.keyboard_edges.get(ckey).get(j).x, liveCardActivity.keyboard_edges.get(ckey).get(j).y);
        		
        	}
        	
        	path.lineTo(liveCardActivity.keyboard_edges.get(ckey).get(0).x, liveCardActivity.keyboard_edges.get(ckey).get(0).y);
        	
        	//draw edge
    		mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        	mPaint.setColor(Color.DKGRAY);
            mPaint.setAlpha(255);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(2);
        	
            canvas.drawPath(path, mPaint);
            
            
            //fill polygon
        	if(liveCardActivity.key.compareTo(ckey.toLowerCase()) == 0){
        		
            	mPaint.setColor(Color.rgb(245, 152, 157));
                mPaint.setAlpha(150);               
                mPaint.setStyle(Paint.Style.FILL);
                canvas.drawPath(path, mPaint);    
         
        	}
            
            
   	
		}
		*/
    	
    	for (Iterator i = liveCardActivity.keyboard_edges.keySet().iterator(); i.hasNext();) {  
    		ckey = (String) i.next();
    		
        	path = new Path();
        	
        	if(liveCardActivity.key.compareTo(ckey.toLowerCase()) == 0){
        		path.moveTo(liveCardActivity.keyboard_edges.get(ckey).get(0).x, liveCardActivity.keyboard_edges.get(ckey).get(0).y);
            	
            	for(int j=0; j < liveCardActivity.keyboard_edges.get(ckey).size(); j++){
            		
            		path.lineTo(liveCardActivity.keyboard_edges.get(ckey).get(j).x, liveCardActivity.keyboard_edges.get(ckey).get(j).y);
            		
            	}
            	
            	mPaint.setColor(Color.rgb(245, 152, 157));
                mPaint.setAlpha(150);               
                mPaint.setStyle(Paint.Style.FILL);
                canvas.drawPath(path, mPaint); 
        	}
        	
    	}
    	
    	
		 //draw key char
    	/*
        mPaint.setTextSize(26);
        mPaint.setStyle(Paint.Style.FILL);
		mPaint.setColor(Color.DKGRAY);
		
		for (Iterator it = liveCardActivity.key_centers.keySet().iterator(); it.hasNext();) { 
			String key = (String) it.next();
			if(key.compareTo("space")==0 || key.compareTo("delete")==0)
			{
				mPaint.setTextSize(20);
				canvas.drawText(key,liveCardActivity.key_centers.get(key).x-20,liveCardActivity.key_centers.get(key).y+10,mPaint);
			}
			else
			{
				mPaint.setTextSize(26);
				canvas.drawText(key,liveCardActivity.key_centers.get(key).x-10,liveCardActivity.key_centers.get(key).y+10,mPaint);
			}
		}
		*/
		
		//draw task sentence
		mPaint.setTextSize(32);
		mPaint.setColor(Color.RED);
		canvas.drawText(liveCardActivity.taskStr,10,40,mPaint);
		
		//typing character
    	mPaint.setTextSize(32);
    	mPaint.setColor(Color.GREEN);
    	canvas.drawText(liveCardActivity.typeText,10,40, mPaint);
		
		//draw touch point		
        mPaint.setColor(Color.BLUE);
        canvas.drawCircle(liveCardActivity.p_x, liveCardActivity.p_y, 3, mPaint);
	
		
		
	}

	

}
