package de.erdna.notenspiegel.ui;

import de.erdna.notenspiegel.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class OptionsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);

		// set title to options
		setTitle(getString(R.string.options));
	}

}
