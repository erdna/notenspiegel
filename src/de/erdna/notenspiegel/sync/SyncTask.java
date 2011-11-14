package de.erdna.notenspiegel.sync;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import de.erdna.notenspiegel.GradesListActivity;
import de.erdna.notenspiegel.MyApp;
import de.erdna.notenspiegel.db.DbAdapter;

import static de.erdna.notenspiegel.Constants.*;

public class SyncTask extends AsyncTask<Object, Object, Object> {

	private Context context;
	private DbAdapter dbAdapter;
	private MyApp app;
	private SharedPreferences preferences;

	public SyncTask(Context context, Application application) {
		this.context = context;
		this.app = (MyApp) application;
	}

	@Override
	protected Object doInBackground(Object... objects) {

		// Connect to DB
		dbAdapter = app.getDbAdapter();
		dbAdapter.deleteAll();

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
			connector.moveToMarksGrid(httpHandler);
			if (DEBUG) publishProgress("moved to marks grid");
			connector.saveMarksToDb(httpHandler, dbAdapter);
			if (DEBUG) publishProgress("parsed grid");
		} catch (Exception e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
		} finally {
			connector.logout(httpHandler);
			if (DEBUG) publishProgress("logout");
		}

		//dbAdapter.close();

		if (DEBUG) publishProgress("successfully synced");

		return "";
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

		// send new intent to MainActivity
		Intent intent = new Intent(context, GradesListActivity.class);
		intent.putExtra(GradesListActivity.EXTRA_ERROR_MSG, (String) result);
		context.startActivity(intent);

	}

}