package de.erdna.notenspiegel.sync;

import static de.erdna.notenspiegel.Constants.*;

import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.db.DbAdapter;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SyncService extends Service {

	private GradesApp app;
	private Context context;

	private class SyncTask extends AsyncTask<Object, Object, Object> {

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

			// get username and password from SharedPreferences
			preferences = PreferenceManager.getDefaultSharedPreferences(context);

			// if grades table is empty deactivate vibrate
			Editor editor = preferences.edit();
			editor.putBoolean(PREF_VIBRATE, !dbAdapter.isTableGradesEmpty());
			editor.commit();

			// get connector, htwdd or tudd
			String university = preferences.getString(PREF_UNIVERSITIES, "htwdd");

			HttpHandler httpHandler = null;
			if ("htwdd".equals(university)) {
				// build HTW Dresden specific handler
				httpHandler = new HtwHttpHandler();
			} else if ("tudd".equals(university)) {
				// build TU Dresden specific handler
				httpHandler = new TuddHttpHandler();
			} else if ("unipa".equals(university)) {
				// TODO instantiate connector Uni Passau
			}

			// give over username and password
			httpHandler.username = preferences.getString("username", "");
			httpHandler.password = preferences.getString("password", "");

			// start syncing
			HttpConnector connector;
			// Instantiate connector
			if (preferences.getBoolean(PREF_CERT, false)) {
				// trust all certificates
				connector = new SslHttpConnector();
			} else {
				// truts just certificates with root certs
				connector = new HttpConnector();
			}

			try {
				// doing the dirty stuff
				connector.login(httpHandler, context);
				if (DEBUG) publishProgress("login");
				connector.moveToGradesGrid(httpHandler);
				if (DEBUG) publishProgress("moved to grades grid");
				connector.saveGradesToDb(httpHandler, dbAdapter);
				if (DEBUG) publishProgress("parsed grid");
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return context.getString(R.string.server_error, e.getLocalizedMessage());
			} catch (SSLException e) {
				e.printStackTrace();
				return context.getString(R.string.ssl_error, "HTW Dresden");
			} catch (Exception e) {
				e.printStackTrace();
				return e.getLocalizedMessage();
			} finally {
				connector.logout(httpHandler);
				if (DEBUG) publishProgress("logout");
			}

			publishProgress(getString(R.string.successfully_synced));

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

			// send broadcast that sync starts
			Intent intent = new Intent(ACTION_SYNC_STARTED);
			context.sendBroadcast(intent);

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

				// send action broadcast to receivers that syncing is done
				Intent intent = new Intent(ACTION_SYNC_DONE);
				context.sendBroadcast(intent);

			}

		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		app = (GradesApp) getApplication();
		context = getApplicationContext();
		if (app.isSyncing()) stopSelf();
		new SyncTask(context, app).execute();
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		if (DEBUG) Toast.makeText(context, "Service destroyed", Toast.LENGTH_LONG).show();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
