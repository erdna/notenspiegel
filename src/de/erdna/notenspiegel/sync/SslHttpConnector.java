package de.erdna.notenspiegel.sync;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

public class SslHttpConnector extends HttpConnector {

	private class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException,
				UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	// private HttpClient client;

	public SslHttpConnector() {
		// create client for ssl connection
		client = getNewHttpClient();
	}

	// @Deprecated
	// public void sync(HttpHandler handler, DbAdapter dbAdapter) {
	//
	// try {
	//
	// // login into ssl page
	// login(handler);
	//
	// // move to page with marks
	// moveToMarksGrid(handler);
	//
	// // save marks in db
	// saveMarksToDb(handler, dbAdapter);
	//
	// // logout correctly and kill client
	// logout(handler);
	//
	// } catch (Exception e) {
	// e.printStackTrace();
	// } finally {
	// client.getConnectionManager().shutdown();
	// }
	//
	// }
	//
	// public void login(HttpHandler handler) throws Exception {
	// if (!handler.login(client)) {
	// Log.e(TAG, "login was NOT successful");
	// // TODO inform user via Intent or anything
	// throw new Exception("login was NOT successful");
	// }
	// if (DEBUG) Log.i(TAG, "login was successful");
	// }
	//
	// public void moveToMarksGrid(HttpHandler handler) throws Exception {
	// if (!handler.moveToMarksGrid(client)) {
	// Log.e(TAG, "move to marks grid was NOT successful");
	// // TODO inform user via Intent or anything
	// throw new Exception("move to marks grid was NOT successful");
	// }
	// }
	//
	// public void saveMarksToDb(HttpHandler handler, DbAdapter dbAdapter)
	// throws Exception {
	// if (!handler.saveMarksToDb(client, dbAdapter)) {
	// Log.e(TAG, "move to marks grid was NOT successful");
	// throw new Exception("move to marks grid was NOT successful");
	// }
	// }
	//
	// public void logout(HttpHandler handler) {
	//
	// client.getConnectionManager().shutdown();
	//
	// }

	private HttpClient getNewHttpClient() {
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			return new DefaultHttpClient(ccm, params);
		} catch (Exception e) {
			return new DefaultHttpClient();
		}
	}

}
