package com.mobilehci.palmtypingtest;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.widget.Button;

public class RenderThread extends Thread {
    private LiveCardRenderer liveCardRenderer;
	

    Button startStopButton;
    boolean started = false;
    private boolean mShouldRun;

    Bitmap bitmap;
    Canvas canvas;

    /**
     * Initializes the background rendering thread.
     * @param liveCardRenderer 
     */
    public RenderThread(LiveCardRenderer liveCardRenderer) {
        this.liveCardRenderer = liveCardRenderer;
        /*
		bitmap = Bitmap.createBitmap((int) 640, (int) 360,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        mShouldRun = true;
        */
        mShouldRun = true;
        
    }
    
    private synchronized boolean shouldRun() {
        return mShouldRun;
    }
    
    /**
     * Requests that the rendering thread exit at the next opportunity.
     */
    public synchronized void quit() {
    	mShouldRun = false;
    }

    @Override
    public void run() {
    	while (shouldRun()) {
    		repaint();
            SystemClock.sleep(LiveCardRenderer.FRAME_TIME_MILLIS);
        }
		
    	
    }

	private void repaint() {
		liveCardRenderer.repaint();		
	}
	
}
