package de.erdna.notenspiegel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.widget.Toast;

import de.erdna.notenspiegel.db.DbAdapter;
import de.erdna.notenspiegel.sync.HttpHandler;
import de.erdna.notenspiegel.sync.HtwHttpHandler;
import de.erdna.notenspiegel.sync.SslConnector;

import static de.erdna.notenspiegel.Constants.*;

public class SyncTask extends AsyncTask<Object, Object, Object> {

	private Context context;
	private MyApp app;
	private DbAdapter dbAdapter;

	public SyncTask(Context context) {
		this.context = context;
	}

	@Override
	protected Object doInBackground(Object... objects) {

		// get appplications
		app = (MyApp) objects[1];

		// Connect to DataSevice and DB
		dbAdapter = app.getDbAdapter();
		dbAdapter.open();
		dbAdapter.deleteAll();

		// get username and password from SharedPreferences
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

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

		SslConnector connector = new SslConnector();
		// connector.sync(httpHandler, dbAdapter);

		try {
			connector.login(httpHandler);
			if (DEBUG) publishProgress("login");
			connector.moveToMarksGrid(httpHandler);
			if (DEBUG) publishProgress("moved to marks grid");
			connector.saveMarksToDb(httpHandler, dbAdapter);
			if (DEBUG) publishProgress("parsed grid");
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		} finally {
			connector.logout(httpHandler);
			if (DEBUG) publishProgress("logout");
		}

		dbAdapter.close();

		return "successful synced";
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		super.onProgressUpdate(values);
		Toast.makeText(context, (String) values[0], Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected void onPostExecute(Object result) {
		super.onPostExecute(result);
		Intent intent = new Intent(context, MainActivity.class);
		context.startActivity(intent);
		Toast.makeText(context, (String) result, Toast.LENGTH_SHORT).show();
	}

}