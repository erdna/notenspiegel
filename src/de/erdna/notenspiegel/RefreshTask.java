package de.erdna.notenspiegel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import de.erdna.notenspiegel.db.DbAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.util.Xml;

public class RefreshTask extends AsyncTask<Object, Void, Void> {

	private final static String HTW_HISQIS_URL = "https://wwwqis.htw-dresden.de/qisserver/rds?state=user&type=1&"
			+ "category=auth.login&startpage=portal.vm";

	private Context context;

	@Override
	protected Void doInBackground(Object... objects) {

		// get context and url from MainActivity
		context = (Context) objects[0];
		String url = HTW_HISQIS_URL;

		// Connect to DataSevice and DB
		DbAdapter dbAdapter = new DbAdapter(context);
		dbAdapter.open();
		dbAdapter.deleteAll();

		try {
			readMarksFromHisQis(url, dbAdapter);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		Intent intent = new Intent(context, MainActivity.class);
		context.startActivity(intent);
		super.onPostExecute(result);
	}

	void readMarksFromHisQis(String url, DbAdapter dbAdapter) throws ClientProtocolException, IOException, IllegalStateException,
			XmlPullParserException {

		HttpClient client = getNewHttpClient();

		HttpPost request = new HttpPost(Html.fromHtml(url).toString());

		// get username and password from SharedPreferences
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		String username = preferences.getString("username", "");
		String password = preferences.getString("password", "");

		// send password and user name
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
		nameValuePairs.add(new BasicNameValuePair("password", password));
		nameValuePairs.add(new BasicNameValuePair("submit", " Ok "));
		nameValuePairs.add(new BasicNameValuePair("username", username));
		request.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

		HttpResponse response = client.execute(request);

		printResponseHeader(response);
		printResponseContent(response);

		String urlNoten = parseMenu(response.getEntity().getContent());
		Log.v("urlNoten", urlNoten);

		HttpGet getRequest = new HttpGet(Html.fromHtml(urlNoten).toString());
		response = client.execute(getRequest);

		printResponseHeader(response);

		String[] kontenUrls = parseNoten(response.getEntity().getContent());

		// for (String kontoUrl : kontenUrls) {

		String kontoUrl = kontenUrls[1];

		Log.d("-> url", kontoUrl);

		getRequest = new HttpGet(Html.fromHtml(kontoUrl).toString());
		response = client.execute(getRequest);

		printResponseHeader(response);

		parseNotenTab(response.getEntity().getContent(), dbAdapter);

		// String kontoUrl2 = kontenUrls[0];
		//
		// Log.d("-> url", kontoUrl2);
		//
		// getRequest = new HttpGet(Html.fromHtml(kontoUrl2).toString());
		// response = client.execute(getRequest);
		//
		// printResponseHeader(response);
		//
		// parseNotenTab(response.getEntity().getContent(), dbAdapter);

		// }

	}

	private void printResponseContent(HttpResponse response) throws IOException {
		if (response != null) {
			String ret = EntityUtils.toString(response.getEntity());
			Log.v("content", ret);
		}
	}

	private void printResponseHeader(HttpResponse response) {
		System.out.println("----------------------------------------");
		System.out.println(response.getStatusLine());
		for (Header header : response.getAllHeaders()) {
			System.out.println(header.toString());
		}
		System.out.println("----------------------------------------");
	}

	private void parseNotenTab(InputStream htmlPage, DbAdapter dbAdapter) throws XmlPullParserException, IOException {

		final String TAB_HEADING = "N o t e n s p i e g e l";

		boolean foundTab = false;
		boolean foundRow = false;

		String name = "";
		String mark = "";

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setValidating(false);
		factory.setFeature(Xml.FEATURE_RELAXED, true);
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(new InputStreamReader(htmlPage));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			// search for tab with explicit heading
			if (eventType == XmlPullParser.TEXT) {
				if (xpp.getText().equals(TAB_HEADING)) {
					Log.v("parseNotenTab()", TAB_HEADING);
					foundTab = true;
				}
			}

			// search for row which is not heading
			if (foundTab && !foundRow && eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equals("tr")) {
					Log.v("parseNotenTab()", xpp.getName());
					foundRow = true;
				}
			}

			// search for first element in row
			if (foundRow && eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equals("td")) {
					Log.v("parseNotenTab()", xpp.getName());
					eventType = xpp.next();
					if (eventType == XmlPullParser.TEXT) {
						Log.i("Prüfungsnummer", xpp.getText());
						// Log.w("Position", xpp.getPositionDescription());
					}

					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();

					Log.w("Position", xpp.getPositionDescription());
					if (eventType == XmlPullParser.TEXT) {
						name = xpp.getText();
						Log.i("Prüfungstext", name);
						Log.w("Position", xpp.getPositionDescription());
					}

					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();

					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();

					Log.w("Position", xpp.getPositionDescription());
					if (eventType == XmlPullParser.TEXT) {
						String text = xpp.getText();
						mark = Html.fromHtml(text).subSequence(18, 21).toString();
						Log.i("Note", mark);
						Log.w("Position", xpp.getPositionDescription());
					}

					dbAdapter.createMark(name, mark);

					foundRow = false;
				}
			}

			// search for tale end tag and abort
			if (foundTab && eventType == XmlPullParser.END_TAG) {
				if (xpp.getName().equals("table")) {
					Log.w("parseNotenTab", "table end tag found");
					break;
				}
			}

			eventType = xpp.next();

		}
	}

	public class MySSLSocketFactory extends SSLSocketFactory {
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

	public HttpClient getNewHttpClient() {
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

	private String parseMenu(InputStream htmlPage) throws XmlPullParserException, IOException {

		final int ASSUMED_HREF_INDEX = 1;

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setValidating(false);
		factory.setFeature(Xml.FEATURE_RELAXED, true);
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(new InputStreamReader(htmlPage));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equalsIgnoreCase("a") && xpp.getAttributeCount() > 1) {
					String attributeName = xpp.getAttributeName(ASSUMED_HREF_INDEX);
					if (attributeName.equalsIgnoreCase("href")) {
						String link = xpp.getAttributeValue("", "href");
						if (link.contains("=notenspiegel")) {
							Log.v("Link", link);
							return link;
						}
					}
				}
			}

			eventType = xpp.next();

		}

		return "";
	}

	private String[] parseNoten(InputStream htmlPage) throws XmlPullParserException, IOException {

		final int ASSUMED_HREF_INDEX = 1;
		String link[] = { "", "" };

		int c = 0;

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setValidating(false);
		factory.setFeature(Xml.FEATURE_RELAXED, true);
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();

		xpp.setInput(new InputStreamReader(htmlPage));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equalsIgnoreCase("a") && xpp.getAttributeCount() > 1) {
					String attributeName0 = xpp.getAttributeName(0);
					// Log.v("AttributeName0", attributeName0);
					String attributeName1 = xpp.getAttributeName(ASSUMED_HREF_INDEX);
					// Log.v("AttributeName1", attributeName1);
					if (attributeName0.equalsIgnoreCase("class") && attributeName1.equalsIgnoreCase("href")) {
						String attributeValue0 = xpp.getAttributeValue(0);
						// Log.v("AttributeValue1", attributeValue1);
						if (attributeValue0.equals("Konto")) {
							link[c++] = xpp.getAttributeValue("", "href");
							Log.v("Link", link[c - 1]);
						}
					}
				}
			}

			eventType = xpp.next();

		}

		return link;
	}

}