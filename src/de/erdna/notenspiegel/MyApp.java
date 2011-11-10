package de.erdna.notenspiegel;

import de.erdna.notenspiegel.db.DbAdapter;
import android.app.Application;

public class MyApp extends Application {

	private DbAdapter dbAdapter;
	private boolean isSyncing;

	public synchronized DbAdapter getDbAdapter() {

		if (dbAdapter == null) {
			dbAdapter = new DbAdapter(getApplicationContext());
		}

		return dbAdapter;
	}

	public synchronized boolean isSyncing() {
		return isSyncing;
	}

	public synchronized void setSyncing(boolean isSyncing) {
		this.isSyncing = isSyncing;
	}

}
