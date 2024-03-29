package com.aripuca.tracker.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.aripuca.tracker.Constants;

public class AppServiceConnection {

	private Context context;

	/**
	 * GPS service connection
	 */
	private AppService appService;
	
	private Runnable runnable;

	/**
	 * ServiceConnection object
	 */
	private final ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			Log.d(Constants.TAG, "ServiceConnection: onServiceConnected " + this.toString());

			appService = ((AppService.LocalBinder) service).getService();

			// executing activity's callback 
			new Handler().post(runnable);
		}

		public void onServiceDisconnected(ComponentName className) {
			Log.d(Constants.TAG, "ServiceConnection: onServiceDisconnected");
		}

	};
	
	public AppServiceConnection(Context c, Runnable r) {
		
		this.context = c; 
		
		this.runnable = r;
		
	}
	
	public void bindAppService() {
		
		if (appService!=null) {
			//return;
		}
		
		Log.v(Constants.TAG, "AppServiceConnection: bindAppService");
		
		Intent i = new Intent(context, AppService.class);
		if (!context.bindService(i, serviceConnection, 0)) {
			Toast.makeText(context, "Can't connect to GPS service", Toast.LENGTH_SHORT).show();
		}

	}

	public void unbindAppService() {

		Log.v(Constants.TAG, "AppServiceConnection: unbindAppService");

		// detach our existing connection
		context.unbindService(serviceConnection);

		appService = null;

	}
	
	public AppService getService() {
		return appService;
	}
	
	/**
	 * start application service
	 */
	public void startService() {
		
		Intent i = new Intent(context, AppService.class);
		context.startService(i);
		
	}
	
	/**
	 * stop application service
	 */
	public void stopService() {

		Intent i = new Intent(context, AppService.class);
		context.stopService(i);

	}

	
}
