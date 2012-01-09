package de.erdna.notenspiegel.sync;

import static de.erdna.notenspiegel.Constants.DEBUG;
import static de.erdna.notenspiegel.Utilities.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
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
import de.erdna.notenspiegel.db.Grade;

public class TuddHttpHandler extends HttpHandler {

	private final static String URL = "https://qis.dez.tu-dresden.de/qisserver/rds?state=user&type=1&category=auth.login&startpage=portal.vm";

	private XmlPullParser xpp;
	private Set<String> urls;

	public TuddHttpHandler() {
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
	public void login(HttpClient client) throws Exception {

		// build post from specific tu dresden url
		HttpPost request = new HttpPost(Html.fromHtml(URL).toString());

		// send password and user name
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
		nameValuePairs.add(new BasicNameValuePair("asdf", username));
		nameValuePairs.add(new BasicNameValuePair("fdsa", password));
		nameValuePairs.add(new BasicNameValuePair("submit", "Â AnmeldenÂ "));
		request.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));

		HttpResponse response = client.execute(request);
		printResponseHeader(response);

		String content = EntityUtils.toString(response.getEntity());

		if (loginFailed(content)) {
			Log.e(TAG, "login was NOT successful");
			throw new Exception("login failed, username or password not correct");
		}

		url = parseMenu(content);
		if (DEBUG) Log.v(TAG, url);

	}

	@Override
	public void moveToGradesGrid(HttpClient httpClient) throws Exception {
		HttpGet request = new HttpGet(Html.fromHtml(url).toString());
		HttpResponse response = httpClient.execute(request);

		printResponseHeader(response);

		urls = parseGradesTabUrls(response.getEntity().getContent());

	}

	@Override
	public void saveGradesToDb(HttpClient httpClient, DbAdapter dbAdapter) throws Exception {

		// check array of urls
		if (urls == null || urls.isEmpty()) {
			if (DEBUG) Log.e(TAG, "saveGradesToDb() no urls were in parseNoten() found");
			throw new Exception("no urls were found");
		}

		//
		for (String url : urls) {
			try {

				if (DEBUG) Log.d(TAG, url);

				HttpGet request = new HttpGet(Html.fromHtml(url).toString());
				HttpResponse response;
				response = httpClient.execute(request);

				printResponseHeader(response);

				parseGradeTab(response.getEntity().getContent(), dbAdapter);

			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}

	}

	private void parseGradeTab(InputStream content, DbAdapter dbAdapter) throws IOException, XmlPullParserException {

		final String TAB_NAME = "table";

		int countTab = 3;
		boolean foundTab = false;
		boolean foundRow = false;

		Grade grade = new Grade();

		xpp.setInput(new InputStreamReader(content));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			// search for tab
			if (eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equals(TAB_NAME)) {
					if (DEBUG) Log.v(TAG, TAB_NAME);
					--countTab;
					if (countTab <= 0) foundTab = true;
				}
			}

			// search for row which is not heading
			if (foundTab && !foundRow && eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equals("tr")) {
					foundRow = true;
				}
			}

			// search for first element in row
			if (foundRow && eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equals("tr")) {

					try {
						// parse "Prüfungsnummer"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isTextAndNotEmpty(eventType))
							eventType = xpp.next();
						grade.mNr = xpp.getText().trim();
						if (DEBUG) Log.v("Prüfungsnummer", grade.mNr);

						// parse "Prüfungstext"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isTextAndNotEmpty(eventType))
							eventType = xpp.next();
						grade.mText = xpp.getText().trim();
						if (DEBUG) Log.v("Prüfungstext", grade.mText);

						// parse "Semester"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isTextAndNotEmpty(eventType))
							eventType = xpp.next();
						grade.mSem = xpp.getText().trim();
						if (DEBUG) Log.v("Semester", grade.mSem);

						// parse "Note"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (eventType != XmlPullParser.TEXT)
							eventType = xpp.next();
						grade.mGrade = xpp.getText().trim();
						if (DEBUG) Log.v("Note", grade.mGrade);

						// parse "Punkte"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isTextAndNotEmpty(eventType))
							eventType = xpp.next();
						String points = xpp.getText().trim();
						if (DEBUG) Log.v("Punkte", points);

						// parse "Status"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isTextAndNotEmpty(eventType))
							eventType = xpp.next();
						grade.mStatus = xpp.getText().trim();
						if (DEBUG) Log.v("Status", grade.mStatus);

						// parse "SWS"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isTextAndNotEmpty(eventType))
							eventType = xpp.next();
						String sws = xpp.getText().trim();
						if (DEBUG) Log.v("SWS", sws);

						// parse "Bonus"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isTextAndNotEmpty(eventType))
							eventType = xpp.next();
						String bonus = xpp.getText().trim();
						if (DEBUG) Log.v("Bonus", bonus);

						// parse "Vermerk" alias Notation
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isText(eventType))
							eventType = xpp.next();
						grade.mNotation = xpp.getText().trim();
						if (DEBUG) Log.v("Vermerk", grade.mNotation);

						// parse "Versuch"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isTextAndNotEmpty(eventType))
							eventType = xpp.next();
						grade.mTry = xpp.getText().trim();
						if (DEBUG) Log.v("Versuch", grade.mTry);

						// parse "Prüfungsdatum"
						while (!isStartTagWithName(eventType, "td"))
							eventType = xpp.next();
						while (!isText(eventType))
							eventType = xpp.next();
						grade.mDate = xpp.getText().trim();
						if (DEBUG) Log.v("Prüfungsdatum", grade.mDate);

					} catch (XmlPullParserException e) {
						if (DEBUG) e.printStackTrace();
					}

					// write to DB
					dbAdapter.createIfNotExitsGrade(grade);

					grade.clear();

					foundRow = false;

				}
			}

			// search for table end tag and abort
			if (foundTab && eventType == XmlPullParser.END_TAG) {
				if (xpp.getName().equals("table")) {
					if (DEBUG) Log.w(TAG, "table end tag found");
					break;
				}
			}

			eventType = xpp.next();

		}

	}

	private boolean isText(int eventType) {
		return (eventType == XmlPullParser.TEXT);
	}

	private boolean isStartTagWithName(int eventType, String name) {
		return eventType == XmlPullParser.START_TAG && xpp.getName().equals(name);
	}

	private boolean isTextAndNotEmpty(int eventType) {
		if (eventType == XmlPullParser.TEXT) if (xpp.getText().trim().length() > 0) return true;
		return false;
	}

	private boolean loginFailed(String content) throws XmlPullParserException, IOException {

		final String LOGIN_FAILED_TEXT_1 = "Ihre Anmeldung ist leider fehlgeschlagen!";
		final String LOGIN_FAILED_TEXT_2 = "Passwort:";

		xpp.setInput(new StringReader(content));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.TEXT) {
				if (xpp.getText().contains(LOGIN_FAILED_TEXT_1)) {
					if (DEBUG) Log.e(TAG, xpp.getText());
					return true;
				} else if (xpp.getText().contains(LOGIN_FAILED_TEXT_2)) {
					if (DEBUG) Log.e(TAG, xpp.getText());
					return true;
				}
			}

			eventType = xpp.next();
		}

		return false;
	}

	private String parseMenu(String htmlPage) throws XmlPullParserException, IOException {

		final int ASSUMED_HREF_INDEX = 0;
		final int ASSUMED_CLASS_INDEX = 2;

		xpp.setInput(new StringReader(htmlPage));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equalsIgnoreCase("a") && xpp.getAttributeCount() >= 3) {
					String attributeNameHref = xpp.getAttributeName(ASSUMED_HREF_INDEX);
					String attributeNameClass = xpp.getAttributeName(ASSUMED_CLASS_INDEX);
					if (attributeNameHref.equalsIgnoreCase("href") && attributeNameClass.equalsIgnoreCase("class")) {
						String link = xpp.getAttributeValue("", "href");
						if (link.contains("=notenspiegel")) {
							if (DEBUG) Log.v(TAG, link);
							return link;
						}
					}
				}
			}

			eventType = xpp.next();

		}

		return "";
	}

	private Set<String> parseGradesTabUrls(InputStream htmlPage) throws XmlPullParserException, IOException {

		final int ASSUMED_HREF_INDEX = 0;
		final String ASSUMED_NAME = "a";
		final String ASSUMED_TEXT = "=notenspiegelStudent";
		Set<String> links = new HashSet<String>();

		xpp.setInput(new InputStreamReader(htmlPage));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				if (xpp.getName().equalsIgnoreCase(ASSUMED_NAME) && xpp.getAttributeCount() > 1) {
					String attributeName = xpp.getAttributeName(ASSUMED_HREF_INDEX);
					if (attributeName.equalsIgnoreCase("href")) {
						String attributeValue = xpp.getAttributeValue(0);
						if (attributeValue.contains(ASSUMED_TEXT)) {
							if (attributeValue != null) links.add(attributeValue);
							if (DEBUG) Log.v(TAG, attributeValue);
							if (DEBUG) Log.v(TAG, "url count: " + links.size());
						}
					}
				}
			}

			eventType = xpp.next();

		}

		return links;
	}

}
