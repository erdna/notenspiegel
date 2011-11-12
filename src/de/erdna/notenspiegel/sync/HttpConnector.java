package de.erdna.notenspiegel.sync;

import static de.erdna.notenspiegel.Constants.DEBUG;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

import de.erdna.notenspiegel.db.DbAdapter;

public class HttpConnector {

	protected final String TAG = this.getClass().getSimpleName();

	protected HttpClient client;

	public HttpConnector() {
		client = new DefaultHttpClient();
	}

	@Deprecated
	public void sync(HttpHandler handler, DbAdapter dbAdapter) {

		try {

			// login into ssl page
			login(handler);

			// move to page with marks
			moveToMarksGrid(handler);

			// save marks in db
			saveMarksToDb(handler, dbAdapter);

			// logout correctly and kill client
			logout(handler);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();
		}

	}

	public void login(HttpHandler handler) throws Exception {
		handler.login(client);
		if (DEBUG) Log.i(TAG, "login() was successful");
	}

	public void moveToMarksGrid(HttpHandler handler) throws Exception {
		handler.moveToMarksGrid(client);
		if (DEBUG) Log.i(TAG, "moveToMarksGrid() was successful");
	}

	public void saveMarksToDb(HttpHandler handler, DbAdapter dbAdapter) throws Exception {
		handler.saveMarksToDb(client, dbAdapter);
		if (DEBUG) Log.i(TAG, "saveMarksToDb() was succesful");
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
