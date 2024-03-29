package com.aripuca.tracker;

import com.aripuca.tracker.R;
import com.aripuca.tracker.io.TrackExportTask;
import com.aripuca.tracker.io.TrackGpxExportTask;
import com.aripuca.tracker.io.TrackKmlExportTask;
import com.aripuca.tracker.map.MyMapActivity;
import com.aripuca.tracker.track.ScheduledTrackRecorder;
import com.aripuca.tracker.track.TrackRecorder;
import com.aripuca.tracker.util.Utils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

//TODO: compare 2 tracks on the map, select track to compare
//TODO: show main parameters of the track and segments on the map 

/**
 * Tracks list activity
 */
public class AbstractTracksListActivity extends ListActivity {

	/**
	 * Reference to app object
	 */
	protected App app;

	protected TracksCursorAdapter cursorAdapter;

	protected Cursor cursor;

	protected int listItemResourceId;
	/**
	 * Select all tracks sql query
	 */
	protected String sqlSelectAllTracks;

	protected Boolean infoDisplayed = true;

	/**
	 * Workaround Issue 7139: MenuItem.getMenuInfo() returns null for sub-menu
	 * items
	 */
	protected ContextMenuInfo prevMenuInfo;

	protected TrackExportTask trackExportTask;

	/**
	 * 
	 */
	protected ProgressDialog progressDialog;

	/**
	 * Overridden in child classes
	 */
	protected void deleteAllTracks() {
	}

	/**
	 * View track details
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		this.viewTrackDetails(id);

	}

	/**
	 * Start the track details activity
	 * 
	 * @param id Track id
	 */
	protected void viewTrackDetails(long id) {

		Intent intent = new Intent(this, TrackDetailsActivity.class);

		// using Bundle to pass track id into new activity
		Bundle b = new Bundle();
		b.putLong("track_id", id);

		intent.putExtras(b);

		startActivity(intent);

	}

	protected class TracksCursorAdapter extends CursorAdapter {

		/**
		 * 
		 */
		private LayoutInflater mInflater;

		/**
		 * 
		 */
		public TracksCursorAdapter(Context context, Cursor cursor, boolean autoRequery) {

			super(context, cursor, autoRequery);

			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			// Creates a view to display the items in
			final View view = mInflater.inflate(listItemResourceId, parent, false);

			return view;

		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			if (cursor.getCount() == 0) {
				return;
			}

			String distanceUnit = app.getPreferences().getString("distance_units", "km");

			float distance = cursor.getFloat(cursor.getColumnIndex("distance"));

			String distanceStr = Utils.formatDistance(distance, distanceUnit)
					+ Utils.getLocalizedDistanceUnit(AbstractTracksListActivity.this, distance, distanceUnit);

			String elevationUnits = app.getPreferences().getString("elevation_units", "m");

			String elevationGain = Utils.formatElevation(cursor.getFloat(cursor.getColumnIndex("elevation_gain")),
					elevationUnits) + Utils.getLocalizedElevationUnit(AbstractTracksListActivity.this, elevationUnits);

			String elevationLoss = Utils.formatElevation(cursor.getFloat(cursor.getColumnIndex("elevation_loss")),
					elevationUnits) + Utils.getLocalizedElevationUnit(AbstractTracksListActivity.this, elevationUnits);

			TextView trackTitle = (TextView) view.findViewById(R.id.track_title);
			TextView trackDetails = (TextView) view.findViewById(R.id.track_details);
			TextView scheduledTrackDetails = (TextView) view.findViewById(R.id.scheduled_track_details);

			if (trackTitle != null) {
				trackTitle.setText(Utils.shortenStr(cursor.getString(cursor.getColumnIndex("title")), 32));
			}

			if (trackDetails != null) {
				trackDetails.setText(distanceStr + " | "
						+ Utils.formatInterval(cursor.getLong(cursor.getColumnIndex("total_time")), false) + " | +"
						+ elevationGain + " | -" + elevationLoss);
			}

			if (scheduledTrackDetails != null) {
				scheduledTrackDetails.setText(DateFormat.format("yyyy-MM-dd kk:mm",
						cursor.getLong(cursor.getColumnIndex("start_time")))
						+ " - " +
						DateFormat.format("yyyy-MM-dd kk:mm", cursor.getLong(cursor.getColumnIndex("finish_time"))));
				//"Points: " + cursor.getInt(cursor.getColumnIndex("count"))
			}

		}

	}

	/**
	 * Called when the activity is created
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.v(Constants.TAG, "AbstractTracksListActivity onCreate");

		super.onCreate(savedInstanceState);

		this.app = (App) this.getApplication();

		this.registerForContextMenu(this.getListView());

		this.setQuery();

		this.cursor = app.getDatabase().rawQuery(this.sqlSelectAllTracks, null);

		this.cursorAdapter = new TracksCursorAdapter(this, cursor, false);
		this.setListAdapter(cursorAdapter);

	}

	/**
	 * onResume event handler
	 */
	@Override
	protected void onResume() {

		super.onResume();

		Log.v(Constants.TAG, "AbstractTracksListActivity onResume");

	}

	/**
	 * onPause event handler
	 */
	@Override
	protected void onPause() {

		Log.v(Constants.TAG, "AbstractTracksListActivity onPause");

		super.onPause();
	}
	
	/**
	 * 
	 */
	@Override
	protected void onDestroy() {

		Log.v(Constants.TAG, "AbstractTracksListActivity onDestroy");

		cursor.close();
		cursor = null;

		app = null;

		super.onDestroy();

	}

	/**
	 * onCreateOptionsMenu handler
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.tracks_menu, menu);
		return true;
	}

	/**
     * 
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {

			case R.id.deleteTracksMenuItem:

				// check if track recording is in progress
				if (isRecordingTrack()) {
					return true;
				}

				// delete all tracks with confirmation dialog
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Are you sure?").setCancelable(true)
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {

								deleteTracksInOtherThread(new UpdateAfterDeleteHandler());

							}
						}).setNegativeButton("No", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								Toast.makeText(AbstractTracksListActivity.this, R.string.cancelled, Toast.LENGTH_SHORT)
										.show();
								dialog.cancel();
							}
						});
				AlertDialog alert = builder.create();

				alert.show();

				return true;

			default:

				return super.onOptionsItemSelected(item);

		}

	}

	/**
	 * Update UI after delete thread finished
	 */
	private class UpdateAfterDeleteHandler extends Handler {

		@Override
		public void handleMessage(Message message) {

			cursor.requery();

			Toast.makeText(AbstractTracksListActivity.this, R.string.all_tracks_deleted, Toast.LENGTH_SHORT).show();
		}
	};

	/**
	 * Deleting track records in separate thread with progress dialog
	 */
	protected void deleteTracksInOtherThread(final UpdateAfterDeleteHandler handler) {

		final ProgressDialog progressDailog = ProgressDialog.show(AbstractTracksListActivity.this,
				getString(R.string.please_wait), getString(R.string.deleting_records), true);

		Thread thread = new Thread() {

			@Override
			public void run() {

				deleteAllTracks();

				// sending message to update UI handler
				handler.sendEmptyMessage(0);

				progressDailog.dismiss();
			}
		};

		thread.start();

	}

	/**
	 * 
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);

		// MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.context_menu, menu);

		// AdapterView.AdapterContextMenuInfo info =
		// (AdapterView.AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle(getString(R.string.track));
		// menu.add(Menu.NONE, 1, 1, R.string.view);
		menu.add(Menu.NONE, 2, 2, R.string.edit);
		menu.add(Menu.NONE, 3, 3, R.string.delete);

		SubMenu exportSubMenu = menu.addSubMenu(Menu.NONE, 4, 4, R.string.export);
		exportSubMenu.add(Menu.NONE, 41, 1, R.string.export_to_gpx);
		exportSubMenu.add(Menu.NONE, 42, 2, R.string.export_to_kml);

		SubMenu exportSubMenu2 = menu.addSubMenu(Menu.NONE, 5, 5, R.string.send_as_attachment);
		exportSubMenu2.add(Menu.NONE, 51, 1, R.string.send_as_gpx);
		exportSubMenu2.add(Menu.NONE, 52, 2, R.string.send_as_kml);

		// menu.add(Menu.NONE, 5, 5, R.string.online_sync);

		menu.add(Menu.NONE, 6, 6, R.string.show_on_map);

	}

	/**
	 * Handle activity menu
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterContextMenuInfo mInfo = (AdapterContextMenuInfo) item.getMenuInfo();

		if (mInfo == null) {
			mInfo = (AdapterContextMenuInfo) prevMenuInfo;
		} else {
			prevMenuInfo = item.getMenuInfo();
		}

		final AdapterContextMenuInfo info = mInfo;

		switch (item.getItemId()) {

		// view track info
			case 1:
				this.viewTrackDetails(info.id);
			break;

			// edit track info
			case 2:
				
				if (isRecordingTrack(info.id)) {
					Toast.makeText(AbstractTracksListActivity.this, R.string.cant_edit_track_being_recorded,
							Toast.LENGTH_SHORT)
							.show();
					return true;
				}
				
				this.updateTrack(info.id);
				
			break;

			// delete track
			case 3:

				if (isRecordingTrack(info.id)) {
					Toast.makeText(AbstractTracksListActivity.this, R.string.cant_delete_track_being_recorded,
							Toast.LENGTH_SHORT)
							.show();
					return true;
				}

				this.deleteTrack(info.id);

			break;

			// export to GPX
			case 41:
				this.exportTrackToGpx(info.id, false);
			break;

			// export to KML
			case 42:
				this.exportTrackToKml(info.id, false);
			break;

			// export to GPX and send as attachment
			case 51:
				this.exportTrackToGpx(info.id, true);
			break;

			// export to KML and send as attachment
			case 52:
				this.exportTrackToKml(info.id, true);
			break;

			// sync track online
			case 5:

			break;

			case 6:

				this.showTrackOnMap(info.id);

			break;

			default:
				return super.onContextItemSelected(item);
		}

		return true;

	}

	protected boolean isRecordingTrack(long trackId) {

		if (TrackRecorder.getInstance(app).isRecording()
				&& TrackRecorder.getInstance(app).getTrackId() == trackId) {
			return true;
		}

		if (ScheduledTrackRecorder.getInstance(app).isRecording()
				&& ScheduledTrackRecorder.getInstance(app).getTrackId() == trackId) {
			return true;
		}

		return false;

	}

	protected boolean isRecordingTrack() {

		if (TrackRecorder.getInstance(app).isRecording()) {
			Toast.makeText(AbstractTracksListActivity.this, R.string.track_recording_in_progress,
					Toast.LENGTH_SHORT).show();
			return true;
		}

		if (ScheduledTrackRecorder.getInstance(app).isRecording()) {
			Toast.makeText(AbstractTracksListActivity.this, R.string.scheduled_track_recording_in_progress,
					Toast.LENGTH_SHORT)
					.show();
			return true;
		}

		return false;
		
	}

	/**
	 * Update track in the database
	 */
	protected void updateTrack(long id) {

		Context context = this;

		final long trackId = id;

		// update track in db
		String sql = "SELECT * FROM tracks WHERE _id=" + trackId + ";";
		Cursor tmpCursor = app.getDatabase().rawQuery(sql, null);
		tmpCursor.moveToFirst();

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.edit_track_dialog,
				(ViewGroup) findViewById(R.id.edit_track_dialog_layout_root));

		AlertDialog.Builder builder = new AlertDialog.Builder(context);

		builder.setTitle(R.string.edit_track);
		builder.setView(layout);

		// creating reference to input field in order to use it in onClick
		// handler
		final EditText trTitle = (EditText) layout.findViewById(R.id.titleInputText);
		trTitle.setText(tmpCursor.getString(tmpCursor.getColumnIndex("title")));

		final EditText trDescr = (EditText) layout.findViewById(R.id.descriptionInputText);
		trDescr.setText(tmpCursor.getString(tmpCursor.getColumnIndex("descr")));

		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {

				// track title from input dialog
				String titleStr = trTitle.getText().toString().trim();
				String descrStr = trDescr.getText().toString().trim();

				if (titleStr.equals("")) {
					Toast.makeText(AbstractTracksListActivity.this, R.string.track_title_required, Toast.LENGTH_SHORT)
							.show();
					return;
				}

				String sql = "UPDATE tracks SET " + "title = '" + titleStr + "', " + "descr = '" + descrStr + "' "
						+ "WHERE _id='" + trackId + "';";

				app.getDatabase().execSQL(sql);

				cursor.requery();

				Toast.makeText(AbstractTracksListActivity.this, R.string.track_updated, Toast.LENGTH_SHORT).show();

			}
		});

		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});

		AlertDialog dialog = builder.create();

		dialog.show();

	}

	/**
	 * delete track and all related track points from db
	 */
	private void deleteTrack(long id) {

		final long trackId = id;

		// delete track with confirmation dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.are_you_sure).setCancelable(true)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {

						// delete all track points first
						String sql = "DELETE FROM track_points WHERE track_id=" + trackId + ";";
						app.getDatabase().execSQL(sql);

						// delete track segments
						sql = "DELETE FROM segments WHERE track_id=" + trackId + ";";
						app.getDatabase().execSQL(sql);

						// delete track
						sql = "DELETE FROM tracks WHERE _id=" + trackId + ";";
						app.getDatabase().execSQL(sql);

						cursor.requery();

						Toast.makeText(AbstractTracksListActivity.this, R.string.track_deleted, Toast.LENGTH_SHORT)
								.show();
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();

	}

	// --------------------------------------------------------------------------------------------

	/**
	 * locking device orientation. export process not to be interrupted
	 */
	private void lockOrientationChange() {

		// Stop the screen orientation changing during an event
		switch (this.getResources().getConfiguration().orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;
			case Configuration.ORIENTATION_LANDSCAPE:
				this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		}

	}

	public void unlockOrientationChange() {

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

	}

	private int getTotalTrackPoints(long trackId) {

		// getting total number of points for progress bar
		String sql = "SELECT COUNT(*) AS total FROM track_points WHERE track_id=" + trackId + ";";
		Cursor tmpCursor = app.getDatabase().rawQuery(sql, null);
		tmpCursor.moveToFirst();

		int totalPoints = tmpCursor.getInt(tmpCursor.getColumnIndex("total"));
		tmpCursor.close();

		return totalPoints;
	}

	private ProgressDialog createProgressDialog(int totalPoints, String message) {

		// setting up progress dialog
		ProgressDialog pd = new ProgressDialog(this);

		pd.setMessage(message);
		pd.setCancelable(true);
		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pd.setProgress(0);
		pd.setMax(totalPoints);

		pd.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				Log.i(Constants.TAG, "AsyncTask: onCancel()");
				// cancel exporting track task
				trackExportTask.cancel(false);
			}
		});

		return pd;

	}

	/**
	 * 
	 */
	private void exportTrackToGpx(long trackId, boolean sendAttachment) {

		// lock orientation of the screen during progress
		this.lockOrientationChange();

		int totalPoints = getTotalTrackPoints(trackId);

		progressDialog = createProgressDialog(totalPoints, getString(R.string.creating_gpx));
		progressDialog.show();

		// starting track exporting in separate thread
		trackExportTask = new TrackGpxExportTask(this);
		trackExportTask.setSendAttachment(sendAttachment);
		trackExportTask.setApp(app);
		trackExportTask.setProgressDialog(progressDialog);

		trackExportTask.execute(trackId);

	}

	/**
	 * 
	 */
	private void exportTrackToKml(long trackId, boolean sendAttachment) {

		// lock orientation of the screen during progress
		this.lockOrientationChange();

		int totalPoints = getTotalTrackPoints(trackId);

		progressDialog = createProgressDialog(totalPoints, getString(R.string.creating_gpx));
		progressDialog.show();

		// starting track exporting in separate thread
		trackExportTask = new TrackKmlExportTask(this);
		trackExportTask.setApp(app);
		trackExportTask.setSendAttachment(sendAttachment);
		trackExportTask.setProgressDialog(progressDialog);

		trackExportTask.execute(trackId);

	}

	/**
	 * 
	 */
	private void showTrackOnMap(long trackId) {

		Intent i = new Intent(this, MyMapActivity.class);

		// using Bundle to pass track id into new activity
		Bundle b = new Bundle();
		b.putInt("mode", Constants.SHOW_TRACK);
		b.putLong("track_id", trackId);
		b.putBoolean("display_info", this.infoDisplayed);

		i.putExtras(b);
		startActivity(i);

	}

	protected void setQuery() {

	}

}
