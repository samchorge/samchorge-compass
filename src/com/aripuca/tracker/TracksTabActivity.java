package com.aripuca.tracker;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class TracksTabActivity extends TabActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    setContentView(R.layout.tracks_tabs);

	    Resources res = getResources();
	    TabHost tabHost = getTabHost();
	    TabHost.TabSpec spec;
	    Intent intent; 

	    // creating real-time tracks tab
	    intent = new Intent().setClass(this, TracksListActivity.class);
	    
	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("tracks").setIndicator(getString(R.string.tracks),
	                      res.getDrawable(R.drawable.ic_tab_tracks))
	                  .setContent(intent);
	    
	    tabHost.addTab(spec);
	    
	    // scheduled tracks tab
	    intent = new Intent().setClass(this, ScheduledTracksListActivity.class);
	    
	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("scheduled_tracks").setIndicator(getString(R.string.scheduled_tracks),
	                      res.getDrawable(R.drawable.ic_tab_scheduled_tracks))
	                  .setContent(intent);

	    tabHost.addTab(spec);
	    
	    tabHost.setCurrentTab(0);
	}	

}
