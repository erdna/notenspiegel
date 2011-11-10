package de.erdna.notenspiegel;

import de.erdna.notenspiegel.db.DbAdapter;
import android.app.Application;

public class MyApp extends Application {

	private DbAdapter dbAdapter;

	public synchronized DbAdapter getDbAdapter() {

		if (dbAdapter == null) {
			dbAdapter = new DbAdapter(getApplicationContext());
		}

		return dbAdapter;
	}

}
