package com.example.sockettest;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * This class does much of the work of sending the message, when the accelerometer value is great enough
 *
 */
public class BackgroundService extends Service {
	
	// Read Accelerometer Data
	private SensorManager mSensorManager;
	private float filteredAcceleration; // acceleration apart from gravity with low pass filter
	private float filteredAcclerationCurrent; // current acceleration including gravity
	private float filteredAcclerationLast; // last acceleration including gravity
	  
	private Object lock = new Object(); // for wait/notify purposes
	public static int messageCount = 0; // number of message sent
	  
	//creation of accelerometer listener
	private final SensorEventListener mSensorListener = new SensorEventListener()
	{
		public void onSensorChanged(SensorEvent se) { //
		    float x = se.values[0]; // Raw Acceleration values in xyz directions
		    float y = se.values[1]; // Perform Low Pass Filter on raw values
		    float z = se.values[2];
		    filteredAcclerationLast = filteredAcclerationCurrent;
		    filteredAcclerationCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
		    float delta = filteredAcclerationCurrent - filteredAcclerationLast;
		    filteredAcceleration = filteredAcceleration * 0.9f + delta; // low-cut filter applied (smoothes graph and shrinks slightly)
		      
		    // if true, phone is assumed to be moving
		    if ((filteredAcceleration < -1.0) || (2.0 < filteredAcceleration)) { // moving only if filtered acceleration magnitude great enough
		    	synchronized (lock) {
		    		lock.notify(); //wake up message-sending thread (TCPClient)
		    	}
		    	System.out.println("broke free");
		    	Resources res = getResources(); //set text of UI of number of messages sent
		    	String update = String.format(res.getString(R.string.data_val), messageCount);
		    	// to update string.xml, need layoutInflator
//		    	LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//		    	View layout = li.inflate(R.id.serverList, null);
//		    	((TextView)(layout.findViewById(R.id.serverList))).setText(update); // cannot set text in separate thread eg TCPClient
		    }
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};
	
	/**
	 * Due to the way the hardware is implemented, when the phone is put to sleep (black screen), some phones
	 * automatically sleep the accelerometer sensor. In order to keep it on, we must unregister and reregister the sensor
	 * to ensure we can still detect movement
	 */
	public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	System.err.println("turning off?");
            // Check action just to be on the safe side.
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.v("shake mediator screen off","trying re-registration");
                // Unregisters the listener and registers it again.
                mSensorManager.unregisterListener(mSensorListener);
                mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

            }
        }	
    };
	

	@Override
	public IBinder onBind(Intent intent) { 
		// TODO: Return the communication channel to the service.
		throw new UnsupportedOperationException("Not yet implemented");
	}
	 
//	@Override
	public void onDestroy() {
//		mSensorManager.unregisterListener(mSensorListener);
		System.out.println("I'm dying!");
	}
	
	// Called when Service created, to initialize Accelerometer monitor and message-send thread
	@Override
 	public void onCreate() {
		
        // create the sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        filteredAcceleration = 0.00f;
        filteredAcclerationCurrent = SensorManager.GRAVITY_EARTH; // Intergalectic Compatibility :)
        filteredAcclerationLast = SensorManager.GRAVITY_EARTH;
        
        // create action to turn sensor back on when screen turned off
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver,filter);
        
        // create thread for sending message
        Thread cThread = new Thread(new TCPClient()); 	
        cThread.start();
	}
	

    // make thread wait until the accelerometer tells it to send a message
    // when running, turn on wifi and turn it off (if it started off) after done (not implemented yet)
    
    // thread used to send message to server when necessary
	public class TCPClient implements Runnable {
	    	
		public static final int pollInterval = 5000; //poll interval of accelerometer when moving, in milliseconds
	    	
		public void run() {
			boolean wifiWasOn = true; // true when, before running this app, wifi is off
			while (true) {	
				Log.d("Acc", "" + filteredAcceleration + " " + wifiWasOn + " " + messageCount);
				// cannot turn off wifi now since must check if connected to network before message (see todo)
	//			if (!wifiWasOn)
	//			{
	//				WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	//				// about to wait for accelerometer interrupt, so turn off wifi if it was off before
	//				wifi.setWifiEnabled(false);
	//			}
				Log.d("TCP", "C: Waiting.");
				synchronized (lock) {
					try { //don't send messages when not moving. Wait for indication
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Log.d("TCP", "C: Done Waiting.");
	    		
				WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				wifiWasOn = wifi.isWifiEnabled();
				if (wifiWasOn == false)
					wifi.setWifiEnabled(true); // Enable the WifiRadio
						
				// TODO: check if can send messages through network; below code doesn't work
	//			while (wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLED);
	
				String Message = BuildMessage();
				SendMessageToServer(Message);
						
				try {
					//mSensorManager.unregisterListener(mSensorListener); //unregister to save energy
					Thread.sleep(pollInterval); // MotionTimeout => 
					// when moving, sleep conservatively (send messages every interval)		
					//mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
						
			}
		}
	    	
		//Build the Message String here
	    public String BuildMessage() {
	    	//Read wifi beacon frame data here.
	    	WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	    	WifiInfo info = manager.getConnectionInfo();
	    	String PhoneMAC = info.getMacAddress();
	    	Date TimeStamp = new java.util.Date(System.currentTimeMillis());
	    	manager.startScan(); // Scan for the Available APs
	    	List<ScanResult> results = manager.getScanResults(); //results include access point and signal strength
	    	int N = 0;
	    	if (results != null) 
	    		N = results.size();
	    	String APData = "";
	    	for (int i=0; i<N; ++i) {
	    		APData = APData + "<" + results.get(i).BSSID + results.get(i).level +">"; //Iteratively build <BSSID SS> values in the message	
	    	}
	    		
	    	String Message = PhoneMAC + "::::" + N + "::::" + APData + "::::" + TimeStamp.toString();// Final Message String	
	    	return Message;
	    }
	    	
	    //Send the Message Here
	    public void SendMessageToServer(String Message){
	   		try {
	   			if (MainActivity.server == null)
	   				MainActivity.server = "10.32.38.79"; //default server, for no reason in particular other than it's been an IP address before
	    		InetAddress serverAddr = InetAddress.getByName(MainActivity.server); 
		        //TODO: send to list of servers instead (scan servers that are in range)
		        Log.d("TCP", "C: Connecting...");
		        Socket socket = new Socket(serverAddr, 1395);
		        try {
		        	Log.d("TCP", "C: Sending: '" + Message + "'");
		        	// Put the string message into the socket
		   		    PrintWriter out = new PrintWriter( new BufferedWriter( new OutputStreamWriter(socket.getOutputStream())),true); 
		   		    out.println(Message);
		   		    out.close();
		   		    Log.d("TCP", "C: Sent.");
		   		    messageCount++;
		   	        Log.d("TCP", "C: Done.");
		        } catch (Exception e) {
		        	Log.e("TCP", "S: Error", e);	
		        } finally {
		        	socket.close(); // Clean up after your pet :)
		        }
		    		
	   		} catch (Exception e) {
	                 Log.e("TCP", "C: Error", e);
	   		}
	    		
	    }
	    	
	}
}
