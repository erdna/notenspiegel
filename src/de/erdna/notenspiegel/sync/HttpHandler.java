package de.erdna.notenspiegel.sync;

import org.apache.http.client.HttpClient;

import de.erdna.notenspiegel.db.DbAdapter;

public abstract class HttpHandler {

	protected final String TAG = this.getClass().getSimpleName();

	public String username;
	public String password;
	public String url;

	public abstract void login(HttpClient httpClient) throws Exception;

	public abstract String getFullUserName(HttpClient httpClient);

	public abstract void moveToMarksGrid(HttpClient httpClient) throws Exception;

	public abstract void saveMarksToDb(HttpClient httpClient, DbAdapter dbAdapter) throws Exception;

}
