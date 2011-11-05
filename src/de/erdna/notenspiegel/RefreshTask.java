package de.erdna.notenspiegel;

import java.io.IOException;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import de.erdna.notenspiegel.db.DbAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

public class RefreshTask extends AsyncTask<Object, Void, Void> {

	private Context context;

	@Override
	protected Void doInBackground(Object... objects) {

		// get context from MainActivity
		context = (Context) objects[0];

		// Connect to DataSevice and DB
		DbAdapter dbAdapter = new DbAdapter(context);
		dbAdapter.open();

		try {
			readWebPage();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		dbAdapter.createMark("Info", "1.7");

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		Intent intent = new Intent(context, MainActivity.class);
		context.startActivity(intent);
		super.onPostExecute(result);
	}

	void readWebPage() throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet("http://www.google.com/");
		HttpResponse response = client.execute(request);
	}

	// EXAMPLE

	// public DefaultHttpClient getClient() {
	// DefaultHttpClient ret = null;
	//
	// // SETS UP PARAMETERS
	// HttpParams params = new BasicHttpParams();
	// HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	// HttpProtocolParams.setContentCharset(params, "utf-8");
	// params.setBooleanParameter("http.protocol.expect-continue", false);
	//
	// // REGISTERS SCHEMES FOR BOTH HTTP AND HTTPS
	// SchemeRegistry registry = new SchemeRegistry();
	// registry.register(new Scheme("http", PlainSocketFactory
	// .getSocketFactory(), 80));
	// final SSLSocketFactory sslSocketFactory =
	// SSLSocketFactory.getSocketFactory();
	// sslSocketFactory
	// .setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
	// registry.register(new Scheme("https", sslSocketFactory, 443));
	//
	// ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(
	// params, registry);
	// ret = new DefaultHttpClient(manager, params);
	// return ret;
	// }

}
