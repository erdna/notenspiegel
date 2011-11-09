package de.erdna.notenspiegel.sync;

import static de.erdna.notenspiegel.Constants.DEBUG;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.text.Html;
import android.util.Log;
import android.util.Xml;

import de.erdna.notenspiegel.db.DbAdapter;

public class HtwHttpHandler extends HttpHandler {

	// htw spedific constants and variables
	private final static String HTW_HISQIS_URL = "https://wwwqis.htw-dresden.de/qisserver/rds?state=user&type=1&"
			+ "category=auth.login&startpage=portal.vm";
	private XmlPullParser xpp;
	private List<String> urls;

	public HtwHttpHandler() {

		// build XML Pull Parser once
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setValidating(false);
			factory.setFeature(Xml.FEATURE_RELAXED, true);
			factory.setNamespaceAware(true);
			xpp = factory.newPullParser();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean login(HttpClient client) {
		try {

			// build post from specific htw url
			HttpPost request = new HttpPost(Html.fromHtml(HTW_HISQIS_URL).toString());

			// send password and user name
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
			nameValuePairs.add(new BasicNameValuePair("password", password));
			nameValuePairs.add(new BasicNameValuePair("submit", " Ok "));
			nameValuePairs.add(new BasicNameValuePair("username", username));
			request.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

			HttpResponse response;
			response = client.execute(request);
			printResponseHeader(response);

			String content = EntityUtils.toString(response.getEntity());

			if (loginFailed(content)) return false;

			url = parseMenu(content);
			if (DEBUG) Log.v(TAG, url);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}

	@Override
	public String getFullUserName(HttpClient httpClient) {
		// TODO implement search for correct name
		return "Mister Nobody";
	}

	@Override
	public boolean moveToMarksGrid(HttpClient client) {
		try {

			HttpGet request = new HttpGet(Html.fromHtml(url).toString());
			HttpResponse response = client.execute(request);

			printResponseHeader(response);

			urls = parseNoten(response.getEntity().getContent());

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	@Override
	public boolean saveMarksToDb(HttpClient client, DbAdapter dbAdapter) {

		if (urls == null || urls.isEmpty()) {
			if (DEBUG) Log.e(TAG, "no urls were found");
			return false;
		}

		for (String url : urls) {
			try {

				if (DEBUG) Log.d("url", url);

				HttpGet request = new HttpGet(Html.fromHtml(url).toString());
				HttpResponse response;
				response = client.execute(request);

				printResponseHeader(response);

				parseNotenTab(response.getEntity().getContent(), dbAdapter);

			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	private boolean loginFailed(String content) throws XmlPullParserException, IllegalStateException, IOException {
		// surely the status must be 401
		// but Bochmann is stupid
		// 401 - Not Authorised
		// The request needs user authentication

		final String ERROR_TEXT = "Anmeldung fehlgeschlagen";

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

	private String parseMenu(String htmlPage) throws XmlPullParserException, IOException {

		final int ASSUMED_HREF_INDEX = 1;

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

	private void parseNotenTab(InputStream htmlPage, DbAdapter dbAdapter) throws XmlPullParserException, IOException {

		final String TAB_HEADING = "N o t e n s p i e g e l";

		boolean foundTab = false;
		boolean foundRow = false;

		String name = "";
		String mark = "";

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
						String text = Html.fromHtml(xpp.getText()).toString();
						if (text != null && text.length() != 0) {
							mark = text.subSequence(18, 21).toString();
							if (DEBUG) Log.i("Note", mark);
						} else mark = "";
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

	@SuppressWarnings("unused")
	private void printContent(String content) {
		if (DEBUG && content != null) {
			Log.v("content", content);
		}
	}

}
