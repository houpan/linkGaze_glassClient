package com.mobilehci.palmtypingtest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.glass.app.Card;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.media.Sounds;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;


public class LiveCardActivity extends Activity {
	
	private LiveCardRenderer mLiveCardRenderer;
	private ImageView bgImageView;
	
	private final WebSocketConnection mConnection = new WebSocketConnection();
	
	public ConcurrentHashMap<String, PointF> key_centers = new ConcurrentHashMap<String, PointF>();
	public ConcurrentHashMap<String, List<PointF>> keyboard_edges = new ConcurrentHashMap<String, List<PointF>>();
	
	private List<PointF> centers_points = new ArrayList<PointF>();
	
	private File externalStorageDir = Environment.getExternalStorageDirectory();
	private File myFile; 
	
	private float glassRatioX = 640F/980F;
	private float glassRatioY = 360F/551F;
	
	public float imgStartX = 264F *glassRatioX;
	public float imgStartY = 280F *glassRatioY;
	public float imgEndX = 233F *glassRatioX;
	public float imgEndY = 449F *glassRatioY;
	public float scaleRatio = 0.9F;
	public float rotationDegree = 100.39F;
	public float calibStartX = 170F*glassRatioX;
	public float calibStartY = 410F*glassRatioY;
	
	public String key="";
	public float p_x;
	public float p_y;
	public boolean lift;
	public String taskStr= "user_study";
	public String typeText="use"; 
	public String username="default";
	public String keyboard="test";
	
	public Long startTime;
	public boolean isStart;
	private int delCount = 0;
	
	private char charMap[] = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
            'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
            'Z', 'X', 'C', 'V', 'B', 'N', 'M', '+', '-'};
	    
	private GestureDetector mGestureDetector;
	
	private void drawkeyboard(){
		//Bitmap bmp = Bitmap.createBitmap(bgImageView.getWidth(), bgImageView.getHeight(), Config.ARGB_8888);
		//clear
		bgImageView.setImageDrawable(null);
		
		
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inMutable = true;
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.user1022, opt);
		
	    Canvas canvas = new Canvas(bmp);
	    bgImageView.draw(canvas);
	    
	    
	    Paint mPaint = new Paint();
		Path path = new Path();
	    String ckey ="";
	    
	    //draw keyboard edge
		for (Iterator i =keyboard_edges.keySet().iterator(); i.hasNext();) {  
        	
        	ckey = (String) i.next();
        	path = new Path();
        	//Log.v("key",ckey);
        	path.moveTo(keyboard_edges.get(ckey).get(0).x, keyboard_edges.get(ckey).get(0).y);
        	
        	for(int j=0; j < keyboard_edges.get(ckey).size(); j++){
        		
        		path.lineTo(keyboard_edges.get(ckey).get(j).x,keyboard_edges.get(ckey).get(j).y);
        		
        	}
        	
        	path.lineTo(keyboard_edges.get(ckey).get(0).x, keyboard_edges.get(ckey).get(0).y);
        	
        	//draw edge
    		mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        	mPaint.setColor(Color.DKGRAY);
            mPaint.setAlpha(255);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(2);
        	
            canvas.drawPath(path, mPaint);
		}
		
		mPaint.setTextSize(26);
	    mPaint.setStyle(Paint.Style.FILL);
		mPaint.setColor(Color.DKGRAY);
			
		for (Iterator it = key_centers.keySet().iterator(); it.hasNext();) { 
			String key = (String) it.next();
			if(key.compareTo("space")==0 || key.compareTo("delete")==0)
			{
				mPaint.setTextSize(20);
				canvas.drawText(key,key_centers.get(key).x-20,key_centers.get(key).y+10,mPaint);
			}
			else
			{
				mPaint.setTextSize(26);
				canvas.drawText(key,key_centers.get(key).x-10,key_centers.get(key).y+10,mPaint);
			}
		}
		
		
		bgImageView.setImageBitmap(bmp);
		
	}
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        //SurfaceView Created
 		setContentView(R.layout.keyboard);
 		
 		mLiveCardRenderer = (LiveCardRenderer)findViewById(R.id.livecardrenderer);
 		
 		bgImageView = (ImageView)this.findViewById(R.id.background);
 		//bgImageView.setImageResource(R.drawable.user2);
 		mGestureDetector = createGestureDetector(this);
 		
        
        //websocket client
        final String wsuri = "ws://10.0.1.2:8080";

	      try {
	         mConnection.connect(wsuri, new WebSocketHandler() {

	            @Override
	            public void onOpen() {
	               Log.v("SocketIO", "Status: Connected to " + wsuri);
	               //mConnection.sendTextMessage("Hello, world!");
	            }

	            @Override
	            public void onTextMessage(String payload) {
	               Log.v("SocketIO", "Got echo: " + payload);
	               
	               try {
					String action = new JSONObject(payload).getString("action");
					
					if(action.compareTo("dumpVertices") == 0){
						
						init();
						
						JSONObject voronoi = new JSONObject(payload).getJSONObject("voronoi");
						JSONObject centers = new JSONObject(payload).getJSONObject("center");
						//vornoi 1 jsonobject -> 28 jsonobject -> jasonarray
					
						Iterator<String> it = centers.keys();
						while (it.hasNext()) {
							String key = it.next();			        		
							key_centers.put(key,new PointF((float)centers.getJSONObject(key).getDouble("x")*glassRatioX, (float)centers.getJSONObject(key).getDouble("y")*glassRatioY));
						}
					
					
						Iterator<String> iter = voronoi.keys();
						while (iter.hasNext()) {
							String key = iter.next();
							try {
								JSONArray key_edges = voronoi.getJSONArray(key);
								List<PointF> edge_points = new ArrayList<PointF>();
								for(int i=0; i<key_edges.length(); i++){
									//web 980 * 551
									float x = (float) key_edges.getJSONObject(i).getDouble("x")*glassRatioX;
									float y = (float) key_edges.getJSONObject(i).getDouble("y")*glassRatioY;
									edge_points.add(new PointF(x,y));
									
								}
				        	
								keyboard_edges.put(key, edge_points);
								
							
								
							} catch (JSONException e) {
								// Something went wrong!
							  }
						}
						drawkeyboard();
						
					}
					else if(action.compareTo("dumpQWERTY") == 0){
						
						init();
						
						JSONObject voronoi = new JSONObject(payload).getJSONObject("QWERTY");
						JSONObject centers = new JSONObject(payload).getJSONObject("center");
						//vornoi 1 jsonobject -> 28 jsonobject -> jasonarray
					
						Iterator<String> it = centers.keys();
						while (it.hasNext()) {
							String key = it.next();			        		
							key_centers.put(key,new PointF((float)centers.getJSONObject(key).getDouble("x")*glassRatioX, (float)centers.getJSONObject(key).getDouble("y")*glassRatioY));
						}
					
					
						Iterator<String> iter = voronoi.keys();
						while (iter.hasNext()) {
							String key = iter.next();
							try {
								JSONArray key_edges = voronoi.getJSONArray(key);
								List<PointF> edge_points = new ArrayList<PointF>();
								for(int i=0; i<key_edges.length(); i++){
									//web 980 * 551
									float x = (float) key_edges.getJSONObject(i).getDouble("x")*glassRatioX;
									float y = (float) key_edges.getJSONObject(i).getDouble("y")*glassRatioY;
									edge_points.add(new PointF(x,y));
									
									
								}
				        	
								keyboard_edges.put(key, edge_points);
								
								//test fill pologon
								
							} catch (JSONException e) {
								// Something went wrong!
							  }
						}
					
						drawkeyboard();
					//for demo only
					//key = "M";
					//mLiveCardRenderer.typeText = "M";	
					
					}
					
					else if(action.compareTo("ViconData")==0 && isStart){
						key	=  new JSONObject(payload).getString("key").toLowerCase();
						p_x =  (float)new JSONObject(payload).getDouble("x")*glassRatioX;
						p_y =  (float)new JSONObject(payload).getDouble("y")*glassRatioY;
						lift = new JSONObject(payload).getBoolean("lift");
						
						
			    		if(lift){
			        		
				    		if(key.compareTo("space")==0)
				    			typeText = typeText+ "_";
				    		else if(key.compareTo("delete")==0){		
				    			if(typeText.length() > 0)
				    			{
				    				delCount++;
				    				typeText = typeText.substring(0,typeText.length()-1);
				    			}
				    		}
				    		else
				    			typeText = typeText+ key;
				    		
				    		lift = false;
			    		}
						
						//type text record and overall time write file here
						// calculate time and save to file can't here
			    		/*
				        if(typeText.compareTo(taskStr)==0){
				        	 Long overallTime = System.currentTimeMillis()- startTime;
				        	 
				        	 String string = taskStr+","+overallTime+","+delCount;

				        	 if(myFile.exists())
				        	 {
				        	    try
				        	    {
				        	        FileOutputStream fOut = new FileOutputStream(myFile,true);
				        	        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
				        	        BufferedWriter bwriter = new BufferedWriter(myOutWriter);
				        	        bwriter.append(string);
				        	        bwriter.newLine();				       
				        	        bwriter.close(); 
				        	        myOutWriter.close();
				        	        
				        	        fOut.close();
				        	     } catch(Exception e)
				        	     {

				        	     }
				        	 }
				        	 else
				        	 {
				        	     try {
									myFile.createNewFile();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				        	 }
				        	 //isStart = false;
				        	 delCount=0;
				        	 		        	 
				        }*/
						
										 
					}
					else if(action.compareTo("sentence")==0){				
						taskStr =  new JSONObject(payload).getString("sentence");
						taskStr = taskStr.toLowerCase();
						taskStr = taskStr.replace(" ", "_");
						typeText="";
						//startTime = System.currentTimeMillis();
						//isStart = true;
					}
					else if(action.compareTo("setName")==0){
						username =  new JSONObject(payload).getString("userName");
						Log.v("set", "username= "+ username);
					}
					else if(action.compareTo("whichLayout")==0){
						keyboard =  new JSONObject(payload).getString("which");
						Log.v("set", "keyboard= "+ keyboard);
						//myFile = new File(externalStorageDir , "record_"+username+"_"+keyboard+".txt");	
						
					}
					
					//List<PointF> test_points = keyboard_edges.get("T");
					//Log.v("keyboard", test_points.get(0).x+","+test_points.get(0).y);
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	               
	               
	            }

	            @Override
	            public void onClose(int code, String reason) {
	               Log.v("SocketIO", "Connection lost.");
	            }
	         });
	      } catch (WebSocketException e) {

	         Log.v("SocketIO", e.toString());
	      }
 		
        
    }
    
    private GestureDetector createGestureDetector(Context context) {
        GestureDetector gestureDetector = new GestureDetector(context);
            //Create a base listener for generic gestures
            gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
                @Override
                public boolean onGesture(Gesture gesture) {
                    if (gesture == Gesture.TAP) {
                    	
                        // do something on tap
                    	/*
                    	AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    	audio.playSoundEffect(Sounds.TAP);  	
                    	isStart = !isStart;
                    	Log.v("tap", new Boolean(isStart).toString());
                    	if(!isStart){
                    		
                    		Long overallTime = System.currentTimeMillis()- startTime;
				        	 
				        	 String string = taskStr+","+typeText+","+overallTime+","+delCount;

				        	 if(myFile.exists())
				        	 {
				        	    try
				        	    {
				        	        FileOutputStream fOut = new FileOutputStream(myFile,true);
				        	        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
				        	        BufferedWriter bwriter = new BufferedWriter(myOutWriter);
				        	        bwriter.append(string);
				        	        bwriter.newLine();
				        	        bwriter.flush();
				        	        bwriter.close(); 
				        	        myOutWriter.close();
				        	        
				        	        fOut.close();
				        	     } catch(Exception e)
				        	     {

				        	     }
				        	 }
				        	 else
				        	 {
				        	     try {
									myFile.createNewFile();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				        	 }
				        	 //isStart = false;
				        	 delCount=0;
                    		
                    		
                    	}else{
                    		startTime = System.currentTimeMillis();
    						isStart = true;
    						Log.v("timer", username+"_"+keyboard);
    						myFile = new File(externalStorageDir , "record_"+username+"_"+keyboard+".txt");	
    						
    						//test filename
    						//String testfile = "record_test_test.txt";
    						//myFile = new File(externalStorageDir , "record_test_test.txt");     		
                    	}
                    	*/
                    	
                        return true;
                    } else if (gesture == Gesture.TWO_TAP) {
                        // do something on two finger tap
                    	
                    	//test
                    	/*
                    	Random r = new Random();
                		int index = r.nextInt(27 - 0) + 0;
                		
                		key = ""+charMap[index];
                		key = key.toLowerCase();
                		typeText = typeText+ key;
                		*/
                    	taskStr = "";
	                   	typeText = "";
	                   	key_centers.clear();
	                   	keyboard_edges.clear();                   
	                   	
                    	AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    	audio.playSoundEffect(Sounds.TAP); 
                    	finish();
                		
                        return true;
                    } else if (gesture == Gesture.SWIPE_RIGHT) {
                    	 // do something on left (backwards) swipe
	                   	 // do something on right (forward) swipe
	                   	/*
	                   	taskStr = "";
	                   	typeText = "";
	                   	key_centers.clear();
	                   	keyboard_edges.clear();                   
	                   	
                    	AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    	audio.playSoundEffect(Sounds.TAP); 
                    	finish();
                    	*/
                    	
                        return true;
                    } else if (gesture == Gesture.SWIPE_LEFT) {
                        // do something on left (backwards) swipe
                    	 // do something on right (forward) swipe
                    	/*
                    	taskStr = "";
                    	typeText = "";
                    	key_centers.clear();
                    	keyboard_edges.clear();
                    	AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    	audio.playSoundEffect(Sounds.TAP);  
                    	*/
                    	
                    	AudioManager audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    	audio.playSoundEffect(Sounds.TAP);  	
                    	isStart = !isStart;
                    	Log.v("tap", new Boolean(isStart).toString());
                    	if(!isStart){
                    		
                    		Long overallTime = System.currentTimeMillis()- startTime;
				        	 
				        	 String string = taskStr+","+typeText+","+overallTime+","+delCount;

				        	 if(myFile.exists())
				        	 {
				        	    try
				        	    {
				        	        FileOutputStream fOut = new FileOutputStream(myFile,true);
				        	        OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
				        	        BufferedWriter bwriter = new BufferedWriter(myOutWriter);
				        	        bwriter.append(string);
				        	        bwriter.newLine();
				        	        bwriter.flush();
				        	        bwriter.close(); 
				        	        myOutWriter.close();
				        	        
				        	        fOut.close();
				        	     } catch(Exception e)
				        	     {

				        	     }
				        	 }
				        	 else
				        	 {
				        	     try {
									myFile.createNewFile();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
				        	 }
				        	 //isStart = false;
				        	 delCount=0;
                    		
                    		
                    	}else{
                    		startTime = System.currentTimeMillis();
    						isStart = true;
    						Log.v("timer", username+"_"+keyboard);
    						
    						myFile = new File(externalStorageDir , "record_"+username+"_"+keyboard+".txt");
    						
    						if(!myFile.exists())
				        	{
    							try {
    								myFile.createNewFile();
    							} catch (IOException e) {
    								// TODO Auto-generated catch block
    								e.printStackTrace();
    							}
        						
				        	}
    						
    						
    						//test filename
    						//String testfile = "record_test_test.txt";
    						//myFile = new File(externalStorageDir , "record_test_test.txt");     		
                    	}
                    	
                    	
                    	
                    	
                        return true;
                    }
                    return false;
                }
            });
            gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
                @Override
                public void onFingerCountChanged(int previousCount, int currentCount) {
                  // do something on finger count changes
                }
            });
            
            /*
            gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
                @Override
                public boolean onScroll(float displacement, float delta, float velocity) {
                    // do something on scrolling
                }
            });
            */
            return gestureDetector;
    }
    
    
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            return mGestureDetector.onMotionEvent(event);
        }
        return false;
    }
    
    @Override
    public void onPause() {
       
        mConnection.disconnect();
        super.onPause();
    }

    @Override
    public void onDestroy() {
       
        mConnection.disconnect();
        super.onDestroy();
    }
    
    private void init(){
    	
    	key_centers.clear();
    	keyboard_edges.clear();
    }
    
}
