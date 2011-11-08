package de.erdna.notenspiegel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.util.Xml;

import de.erdna.notenspiegel.db.DbAdapter;
import static de.erdna.notenspiegel.Constants.*;

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
			// TODO do error handling
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
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

		String content = EntityUtils.toString(response.getEntity());

		if (loginFailed(content)) {
			Log.e("login", "login was NOT successful");
			// TODO do something, maybe throw an exception
			return;
		} else {
			Log.i("login", "login was successful");
		}

		String urlNoten = parseMenu(content);
		Log.v("urlNoten", urlNoten);

		HttpGet getRequest = new HttpGet(Html.fromHtml(urlNoten).toString());
		response = client.execute(getRequest);

		printResponseHeader(response);

		List<String> kontoUrls = parseNoten(response.getEntity().getContent());

		for (String kontoUrl : kontoUrls) {

			if (DEBUG) Log.d("url", kontoUrl);

			getRequest = new HttpGet(Html.fromHtml(kontoUrl).toString());
			response = client.execute(getRequest);

			printResponseHeader(response);

			parseNotenTab(response.getEntity().getContent(), dbAdapter);

		}

	}

	private boolean loginFailed(String content) throws XmlPullParserException, IllegalStateException, IOException {
		// surely the status must be 401
		// but Bochmann is stupid
		// 401 - Not Authorised
		// The request needs user authentication

		final String ERROR_TEXT = "Anmeldung fehlgeschlagen";

		XmlPullParser xpp = getXmlPullParser();

		xpp.setInput(new StringReader(content));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			// search for tab with explicit heading
			if (eventType == XmlPullParser.TEXT) {
				if (xpp.getText().contains(ERROR_TEXT)) {
					if (DEBUG) Log.v("parseNotenTab()", xpp.getText());
					return true;
				}
			}

			eventType = xpp.next();

		}
		return false;
	}

	@SuppressWarnings("unused")
	private void printContent(String content) {
		if (DEBUG && content != null) {
			Log.v("content", content);
		}
	}

	private void printResponseHeader(HttpResponse response) {
		if (DEBUG) {
			System.out.println("----------------------------------------");
			System.out.println(response.getStatusLine());
			for (Header header : response.getAllHeaders()) {
				System.out.println(header.toString());
			}
			System.out.println("----------------------------------------");
		}
	}

	private void parseNotenTab(InputStream htmlPage, DbAdapter dbAdapter) throws XmlPullParserException, IOException {

		final String TAB_HEADING = "N o t e n s p i e g e l";

		boolean foundTab = false;
		boolean foundRow = false;

		String name = "";
		String mark = "";

		XmlPullParser xpp = getXmlPullParser();

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
					// if (DEBUG) Log.v("parseNotenTab()", xpp.getName());
					foundRow = true;
				}
			}

			// search for first element in row
			if (foundRow && eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equals("td")) {
					eventType = xpp.next();
					if (eventType == XmlPullParser.TEXT) {
						if (DEBUG) Log.i("Prüfungsnummer", xpp.getText());
						// Log.w("Position", xpp.getPositionDescription());
					}

					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();

					// Log.w("Position", xpp.getPositionDescription());
					if (eventType == XmlPullParser.TEXT) {
						name = xpp.getText();
						if (DEBUG) Log.i("Prüfungstext", name);
						// Log.w("Position", xpp.getPositionDescription());
					}

					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();

					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();
					eventType = xpp.next();

					// Log.w("Position", xpp.getPositionDescription());
					if (eventType == XmlPullParser.TEXT) {
						Log.w("befor getText()", xpp.getPositionDescription());
						String text = Html.fromHtml(xpp.getText()).toString();
						Log.w("after getText()", xpp.getPositionDescription());
						if (text != null && text.length() != 0) {
							mark = text.subSequence(18, 21).toString();
							if (DEBUG) Log.i("Note", mark);
						}
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

	private XmlPullParser getXmlPullParser() throws XmlPullParserException {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setValidating(false);
		factory.setFeature(Xml.FEATURE_RELAXED, true);
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		return xpp;
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

	private String parseMenu(String htmlPage) throws XmlPullParserException, IOException {

		final int ASSUMED_HREF_INDEX = 1;

		XmlPullParser xpp = getXmlPullParser();

		xpp.setInput(new StringReader(htmlPage));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equalsIgnoreCase("a") && xpp.getAttributeCount() > 1) {
					String attributeName = xpp.getAttributeName(ASSUMED_HREF_INDEX);
					if (attributeName.equalsIgnoreCase("href")) {
						String link = xpp.getAttributeValue("", "href");
						if (link.contains("=notenspiegel")) {
							if (DEBUG) Log.v("Link", link);
							return link;
						}
					}
				}
			}

			eventType = xpp.next();

		}

		return "";
	}

	private List<String> parseNoten(InputStream htmlPage) throws XmlPullParserException, IOException {

		final int ASSUMED_HREF_INDEX = 1;
		List<String> links = new ArrayList<String>();

		XmlPullParser xpp = getXmlPullParser();

		xpp.setInput(new InputStreamReader(htmlPage));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equalsIgnoreCase("a") && xpp.getAttributeCount() > 1) {
					String attributeName0 = xpp.getAttributeName(0);
					// if (DEBUG) Log.v("AttributeName0", attributeName0);
					String attributeName1 = xpp.getAttributeName(ASSUMED_HREF_INDEX);
					// if (DEBUG) Log.v("AttributeName1", attributeName1);
					if (attributeName0.equalsIgnoreCase("class") && attributeName1.equalsIgnoreCase("href")) {
						String attributeValue0 = xpp.getAttributeValue(0);
						// if (DEBUG) Log.v("AttributeValue1", attributeValue1);
						if (attributeValue0.equals("Konto")) {
							String attributeValue1 = xpp.getAttributeValue("", "href");
							if (attributeValue1 != null) links.add(attributeValue1);
							if (DEBUG) Log.v("added link", attributeValue1);
						}
					}
				}
			}

			eventType = xpp.next();

		}

		return links;
	}

}