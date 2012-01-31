package de.erdna.notenspiegel;

import static de.erdna.notenspiegel.Constants.*;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import android.text.Html;
import android.util.Log;

public final class Utilities {

	private final static String TAG = Utilities.class.getClass().getSimpleName();

	public static final void printResponseHeader(HttpResponse response) {
		if (DEBUG) {
			System.out.println("----------------------------------------");
			System.out.println(response.getStatusLine());
			for (Header header : response.getAllHeaders()) {
				System.out.println(header.toString());
			}
			System.out.println("----------------------------------------");
		}
	}

	public static final void printContent(String content) {
		if (DEBUG) {
			if (content != null) Log.v("content", content);
		}
	}

	public static final String cleanGrade(String dirtyText) {
		if (DEBUG) Log.v(TAG, "\tcleanGrade");
		if (DEBUG) Log.v(TAG, "\tdirtyText:\t" + dirtyText);
		String temp = Html.fromHtml(dirtyText).toString();
		if (temp != null && temp.length() != 0) {
			temp = temp.replace("ERR: unresolved:", "").replaceAll("[^0-9,]", "").trim();
		}
		if (DEBUG) Log.v(TAG, "\tcleanText:\t" + temp);
		return temp;
	}

}