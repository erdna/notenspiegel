package de.erdna.notenspiegel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import de.erdna.notenspiegel.db.DbAdapter;
import de.erdna.notenspiegel.sync.HttpHandler;
import de.erdna.notenspiegel.sync.HtwHttpHandler;
import de.erdna.notenspiegel.sync.SslConnector;

public class SyncTask extends AsyncTask<Object, Void, Void> {

	private Context context;

	@Override
	protected Void doInBackground(Object... objects) {

		// get context and url from MainActivity
		context = (Context) objects[0];

		// Connect to DataSevice and DB
		DbAdapter dbAdapter = new DbAdapter(context);
		dbAdapter.open();
		dbAdapter.deleteAll();

		// get username and password from SharedPreferences
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		// get connector, htwdd or tudd
		String university = preferences.getString("listUniversities", "htwdd");

		HttpHandler handler = null;
		if ("htwdd".equals(university)) {
			// build HTW Dresden specific handler
			handler = new HtwHttpHandler();
		} else if ("tudd".equals(university)) {
			// TODO instantiate connector TU Dresden
		}

		// give over username and password
		handler.username = preferences.getString("username", "");
		handler.password = preferences.getString("password", "");

		// start syncing
		SslConnector connector = new SslConnector();
		connector.sync(handler, dbAdapter);

		dbAdapter.close();

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		Intent intent = new Intent(context, MainActivity.class);
		context.startActivity(intent);
		super.onPostExecute(result);
	}

}