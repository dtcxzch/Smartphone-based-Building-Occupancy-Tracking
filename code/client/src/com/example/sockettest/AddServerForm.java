package com.example.sockettest;

import android.os.Bundle;
import android.app.Activity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v4.app.NavUtils;
import android.content.Intent;
import android.content.res.Resources;

/**
 * This activity is a simple form to add the server ip address
 */
public class AddServerForm extends Activity {
	
	String server = "";
	public final static String EXTRA_COUNT = "com.example.sockettest.COUNT";
	public final static String EXTRA_SERVER = "com.example.sockettest.SERVER";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_server_form);
		
		Intent intent = getIntent();
		server = intent.getStringExtra(MainActivity.EXTRA_SERVER);
		
		
		setContentView(R.layout.activity_add_server_form);
		
		Resources res = getResources(); // string of servers to display
		String serverString = server;
		
//		for (String s : servers) {
//			serverString += s + "\n";
//		}
		
	    String update = String.format(res.getString(R.string.server_list), serverString);
	    ((TextView)(this.findViewById(R.id.serverList))).setText(update);
		
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	// return to MainActivity
	public void backPage(View view) {
		Intent intent = new Intent(this,MainActivity.class);
		intent.putExtra(EXTRA_SERVER, server);
		
		startActivity(intent);
	}
	
	//add servers to the list of servers to send messages to
	public void addServer(View view) { 
		EditText newServer = (EditText) findViewById(R.id.editText1);
		String serverName = newServer.getText().toString();
		if (serverName != null && serverName != "")
			server = serverName;
		
		Intent intent = new Intent(this,AddServerForm.class);
		intent.putExtra(EXTRA_SERVER, server);
		startActivity(intent);
	}
	
	//remove server name
	public void deleteAll(View view) { 
		
		server = null;
		//servers = new HashSet<String>();
		
		Intent intent = new Intent(this,AddServerForm.class);
		intent.putExtra(EXTRA_SERVER, server);
		startActivity(intent);
	}

}
