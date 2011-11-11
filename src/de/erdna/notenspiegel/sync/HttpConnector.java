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
		if (!handler.login(client)) {
			Log.e(TAG, "login was NOT successful");
			// TODO inform user via Intent or anything
			throw new Exception("login was NOT successful");
		}
		if (DEBUG) Log.i(TAG, "login was successful");
	}

	public void moveToMarksGrid(HttpHandler handler) throws Exception {
		if (!handler.moveToMarksGrid(client)) {
			Log.e(TAG, "move to marks grid was NOT successful");
			// TODO inform user via Intent or anything
			throw new Exception("move to marks grid was NOT successful");
		}
	}

	public void saveMarksToDb(HttpHandler handler, DbAdapter dbAdapter) throws Exception {
		if (!handler.saveMarksToDb(client, dbAdapter)) {
			Log.e(TAG, "move to marks grid was NOT successful");
			throw new Exception("move to marks grid was NOT successful");
		}
	}

	public void logout(HttpHandler handler) {

		client.getConnectionManager().shutdown();

	}

}
