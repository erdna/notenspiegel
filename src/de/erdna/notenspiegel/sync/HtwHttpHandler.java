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

import de.erdna.notenspiegel.Grade;
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
	public void login(HttpClient client) throws Exception {

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

		if (loginFailed(content)) {
			Log.e(TAG, "login was NOT successful");
			throw new Exception("login was NOT successful");
		}

		url = parseMenu(content);
		if (DEBUG) Log.v(TAG, url);

	}

	@Override
	public String getFullUserName(HttpClient httpClient) {
		// TODO implement search for correct name
		return "Mister Nobody";
	}

	@Override
	public void moveToMarksGrid(HttpClient client) throws Exception {
		HttpGet request = new HttpGet(Html.fromHtml(url).toString());
		HttpResponse response = client.execute(request);

		printResponseHeader(response);

		urls = parseNoten(response.getEntity().getContent());
	}

	@Override
	public void saveMarksToDb(HttpClient client, DbAdapter dbAdapter) throws Exception {

		if (urls == null || urls.isEmpty()) {
			if (DEBUG) Log.e(TAG, "saveMarksToDb() no urls were in moveToMarksGrid() found");
			throw new Exception("no urls were found");
		}

		for (String url : urls) {
			try {

				if (DEBUG) Log.d(TAG, url);

				HttpGet request = new HttpGet(Html.fromHtml(url).toString());
				HttpResponse response;
				response = client.execute(request);

				printResponseHeader(response);

				parseNotenTab(response.getEntity().getContent(), dbAdapter);

			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	private boolean loginFailed(String content) throws Exception {
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
					if (DEBUG) Log.v(TAG, xpp.getText());
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
							if (DEBUG) Log.v(TAG, attributeValue1);
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

		Grade grade = new Grade();

		xpp.setInput(new InputStreamReader(htmlPage));
		int eventType = xpp.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {

			// search for tab with explicit heading
			if (eventType == XmlPullParser.TEXT) {
				if (xpp.getText().equals(TAB_HEADING)) {
					if (DEBUG) Log.v(TAG, TAB_HEADING);
					foundTab = true;
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
				if (xpp.getName().equals("td")) {

					// parse "Prüfungsnummer"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mNr = xpp.nextText();
					if (DEBUG) Log.i(TAG, grade.mNr);
					eventType = xpp.next();

					// parse "Prüfungstext"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mText = xpp.nextText();
					if (DEBUG) Log.i(TAG, grade.mText);
					eventType = xpp.next();

					// parse "Semester"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mSem = Html.fromHtml(xpp.nextText()).toString().trim();
					if (DEBUG) Log.i(TAG, grade.mSem);
					eventType = xpp.next();

					// parse "Note"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					String temp = Html.fromHtml(xpp.nextText()).toString();
					if (temp != null && temp.length() != 0) {
						grade.mGrade = temp.subSequence(18, 21).toString();
						if (DEBUG) Log.i(TAG, grade.mGrade);
					}
					eventType = xpp.next();

					// parse "Status"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mStatus = xpp.nextText();
					if (DEBUG) Log.i(TAG, grade.mStatus);
					eventType = xpp.next();

					// parse "Credits"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mCredits = xpp.nextText();
					if (DEBUG) Log.i(TAG, grade.mCredits);
					eventType = xpp.next();

					// parse "ECTS"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mEcts = xpp.nextText();
					if (DEBUG) Log.i(TAG, grade.mEcts);
					eventType = xpp.next();

					// parse "Vermerk" alias Notation
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mNotation = xpp.nextText();
					if (DEBUG) Log.i(TAG, grade.mNotation);
					eventType = xpp.next();

					// parse "Versuch"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mTry = xpp.nextText();
					if (DEBUG) Log.i(TAG, grade.mTry);
					eventType = xpp.next();

					// parse "Prüfungsdatum"
					while (eventType != XmlPullParser.START_TAG)
						eventType = xpp.next();
					grade.mDate = Html.fromHtml(xpp.nextText()).toString();
					if (DEBUG) Log.i(TAG, grade.mDate);
					eventType = xpp.next();

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
