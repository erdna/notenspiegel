package de.erdna.notenspiegel;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Options extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
	}

}
