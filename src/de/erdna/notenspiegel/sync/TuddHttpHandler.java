package de.erdna.notenspiegel.sync;

import static de.erdna.notenspiegel.Utilities.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.text.Html;
import android.util.Xml;

import de.erdna.notenspiegel.db.DbAdapter;

public class TuddHttpHandler extends HttpHandler {

	private final static String URL = "https://qis.dez.tu-dresden.de/qisserver/rds?state=user&type=0&category=menu.browse&startpage=portal.vm";

	private XmlPullParser xpp;
	private List<String> urls;

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
		nameValuePairs.add(new BasicNameValuePair("submit", " Login "));
		request.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
		
		HttpResponse response;
		response = client.execute(request);
		printResponseHeader(response);
		
		String content = EntityUtils.toString(response.getEntity());
		printContent(content);
		

	}

	@Override
	public void moveToGradesGrid(HttpClient httpClient) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveMarksToDb(HttpClient httpClient, DbAdapter dbAdapter) throws Exception {
		// TODO Auto-generated method stub

	}

}
