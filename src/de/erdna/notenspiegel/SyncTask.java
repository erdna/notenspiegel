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

		// build htw specific handler
		HttpHandler parser = new HtwHttpHandler();

		// give over username and password
		parser.username = preferences.getString("username", "");
		parser.password = preferences.getString("password", "");

		// start syncing
		SslConnector connector = new SslConnector();
		connector.sync(parser, dbAdapter);

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		Intent intent = new Intent(context, MainActivity.class);
		context.startActivity(intent);
		super.onPostExecute(result);
	}

}