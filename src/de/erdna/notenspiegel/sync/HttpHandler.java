package de.erdna.notenspiegel.sync;

import org.apache.http.client.HttpClient;

import de.erdna.notenspiegel.db.DbAdapter;

public abstract class HttpHandler {

	protected final String TAG = this.getClass().getSimpleName();

	public String username;
	public String password;
	public String url;

	public abstract boolean login(HttpClient httpClient);

	public abstract String getFullUserName(HttpClient httpClient);

	public abstract boolean moveToMarksGrid(HttpClient httpClient);

	public abstract boolean saveMarksToDb(HttpClient httpClient, DbAdapter dbAdapter);

}
