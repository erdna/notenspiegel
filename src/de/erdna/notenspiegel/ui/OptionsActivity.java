package de.erdna.notenspiegel.ui;

import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.ui.actionbar.ActionBarPreferenceActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class OptionsActivity extends ActionBarPreferenceActivity implements OnSharedPreferenceChangeListener {

	SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);

		// set title to options
		setTitle(getString(R.string.options));

		// get shared preferences
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

	}

	@Override
	protected void onResume() {
		super.onResume();

		// register change listener
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	protected void onPause() {
		super.onPause();

		// register change listener
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if ("username".equals(key)) {
			// TODO ask about deletion of content
			((GradesApp) getApplication()).getDbAdapter().deleteAll();
		}

	}
}
