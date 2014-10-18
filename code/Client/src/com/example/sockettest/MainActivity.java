package com.example.sockettest;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

//
/**
 * This application is intended to sleep until the accelerometer detects movement
 * On movement, the application will scan for nearby servers and pertinent data (AP and signal strength)
 * From this, a message will be sent to servers (where data will be interpreted to location)
 * This particular class creates a background service which checks the accelerometer and sends messages, while
 * this class displays messages and allows server name to be inputed
 * @author Abhi Mahagaonkar
 * @editor Jason Setter
 *
 */
public class MainActivity extends Activity { 
	
	
	public static String server; // server messages are sent to
	// TODO: make list of servers (and send to one that is within range)
	public final static String EXTRA_COUNT = "com.example.sockettest.COUNT";
	public final static String EXTRA_SERVER = "com.example.sockettest.SERVER";
	
	@Override
	protected void onResume() { 
		super.onResume();
	}

//	@Override
//	protected void onPause() { 
//	    super.onPause();
//	}

	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        
        return true;
    }
	
	// method for add server button (save message Count and server data)
	public void addServerForm(View view) {
		Intent intent = new Intent(this, AddServerForm.class);
		intent.putExtra(EXTRA_SERVER, server);
		startActivity(intent);
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) { //Note: this is run every time returning back to this page
    	super.onCreate(savedInstanceState);
    	
    	// for extracting message count and server data
		Intent intent = getIntent();
		server = intent.getStringExtra(MainActivity.EXTRA_SERVER);
		
        setContentView(R.layout.activity_main);
        
        // start background service to measure accelerometer and send wifi messages
        Intent intentbackground = new Intent(this, BackgroundService.class);
        startService(intentbackground);
        
        // properly display message count//
        Resources res = getResources();
	    String update = String.format(res.getString(R.string.data_val), BackgroundService.messageCount);
	    ((TextView)(findViewById(R.id.serverList))).setText(update);
        
        // Uncomment the following piece of code to do Battery Analysis
	    // Due to using service instead, please you saved version of MainActivity name MainAcitivyBackupBeforeService
        //###############BATTERY TESTING################################
        /*BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
            int scale = -1;
            int level = -1;
            int voltage = -1;
            int temp = -1;
            @Override
            public void onReceive(Context context, Intent intent) {
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                System.out.println("BatteryManager level is "+level+"/"+scale+", temp is "+temp+", voltage is "+voltage);
                
                
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);*/
        //###############BATTERY TESTING################################
        
        // Dispatch the communication to a new thread. As it is bad practice to do networking on the main thread
    }
    
}