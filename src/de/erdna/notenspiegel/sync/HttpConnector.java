package de.erdna.notenspiegel.sync;

import static de.erdna.notenspiegel.Constants.*;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import de.erdna.notenspiegel.db.DbAdapter;

public class HttpConnector {

	protected final String TAG = this.getClass().getSimpleName();

	protected HttpClient client;

	public HttpConnector() {
		client = new DefaultHttpClient();
	}

	public void login(HttpHandler handler, Context context) throws Exception {
		handler.login(client);
		if (DEBUG) Log.i(TAG, "login() was successful");

		String text = handler.fullname;

		// save full user name to SharedPreferences
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(PREF_FULL_NAME, text);
		editor.commit();
	}

	public void moveToGradesGrid(HttpHandler handler) throws Exception {
		handler.moveToGradesGrid(client);
		if (DEBUG) Log.i(TAG, "moveToGradesGrid() was successful");
	}

	public void saveGradesToDb(HttpHandler handler, DbAdapter dbAdapter) throws Exception {
		handler.saveGradesToDb(client, dbAdapter);
		if (DEBUG) Log.i(TAG, "saveGradesToDb() was succesful");
	}

	public void logout(HttpHandler handler) {
		try {

			// TODO logout correctly by logout URL

			// close connection
			client.getConnectionManager().shutdown();

		} catch (Exception e) {
			// not necessary to know just throw away all connections
		}

	}
}
