package de.erdna.notenspiegel;

import static de.erdna.notenspiegel.Constants.*;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import android.util.Log;

public final class Utilities {
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
}