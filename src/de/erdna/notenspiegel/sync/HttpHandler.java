package de.erdna.notenspiegel.sync;

import org.apache.http.client.HttpClient;

import de.erdna.notenspiegel.db.DbAdapter;

public abstract class HttpHandler {

	protected final String TAG = this.getClass().getSimpleName();

	public String username;
	public String password;
	public String url;
	public String fullname;

	public abstract void login(HttpClient httpClient) throws Exception;

	public abstract void moveToGradesGrid(HttpClient httpClient) throws Exception;

	public abstract void saveGradesToDb(HttpClient httpClient, DbAdapter dbAdapter) throws Exception;

}
