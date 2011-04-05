package de.erdna.notenspiegel;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PreferencePage extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference_mark);
	}

}
