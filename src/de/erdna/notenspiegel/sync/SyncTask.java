package de.erdna.notenspiegel.sync;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.db.DbAdapter;
import de.erdna.notenspiegel.ui.GradesListActivity;

import static de.erdna.notenspiegel.Constants.*;

public class SyncTask extends AsyncTask<Object, Object, Object> {

	private Context context;
	private DbAdapter dbAdapter;
	private GradesApp app;
	private SharedPreferences preferences;

	public SyncTask(Context context, Application application) {
		this.context = context;
		this.app = (GradesApp) application;
	}

	@Override
	protected Object doInBackground(Object... objects) {

		// Connect to DB
		dbAdapter = app.getDbAdapter();
		// dbAdapter.deleteAll();

		// get username and password from SharedPreferences
		preferences = PreferenceManager.getDefaultSharedPreferences(context);

		// get connector, htwdd or tudd
		String university = preferences.getString("listUniversities", "htwdd");

		HttpHandler httpHandler = null;
		if ("htwdd".equals(university)) {
			// build HTW Dresden specific handler
			httpHandler = new HtwHttpHandler();
		} else if ("tudd".equals(university)) {
			// TODO instantiate connector TU Dresden
		}

		// give over username and password
		httpHandler.username = preferences.getString("username", "");
		httpHandler.password = preferences.getString("password", "");

		// start syncing
		HttpConnector connector;
		if (preferences.getBoolean("acceptUntrustedCerts", false)) connector = new SslHttpConnector();
		else connector = new HttpConnector();

		try {
			connector.login(httpHandler);
			if (DEBUG) publishProgress("login");
			connector.moveToGradesGrid(httpHandler);
			if (DEBUG) publishProgress("moved to grades grid");
			connector.saveGradesToDb(httpHandler, dbAdapter);
			if (DEBUG) publishProgress("parsed grid");
		} catch (Exception e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
		} finally {
			connector.logout(httpHandler);
			if (DEBUG) publishProgress("logout");
		}

		if (DEBUG) publishProgress("successfully synced");

		return null;
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		super.onProgressUpdate(values);
		Toast.makeText(context, (String) values[0], Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		// set global sync flag on true
		app.setSyncing(true);
	}

	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);

		// set global sync flag on false
		app.setSyncing(false);

		if (result != null) {

			// error happens
			String errorMsg = (String) result;
			Intent intent = new Intent(ACTION_SYNC_ERROR);
			intent.putExtra(EXTRA_ERROR_MSG, errorMsg);
			context.sendBroadcast(intent);

		} else {

			// send action broadcast to receivers
			Intent intent = new Intent(ACTION_SYNC_DONE);
			context.sendBroadcast(intent);

			// send notification
			// updateNotification();

		}
	}

	private void updateNotification() {
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// TODO add correct notification text
		String text = "BlaBla";

		Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		notification.flags = Notification.FLAG_NO_CLEAR;

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context,
				GradesListActivity.class), 0);

		// String message;
		// if (mLooperToursSyncer != null) {
		// message = getString(R.string.syncing_tours);
		// } else if (mLooperSubmitEventRequest != null) {
		// message = getString(R.string.submitting_data);
		// } else {
		// message = text;
		// }

		notification.setLatestEventInfo(context, text, text, contentIntent);
		manager.notify(NOTIFICATION, notification);
	}
}