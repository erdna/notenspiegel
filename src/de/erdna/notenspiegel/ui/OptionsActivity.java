package de.erdna.notenspiegel.ui;

import static de.erdna.notenspiegel.Constants.*;
import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.ui.actionbar.ActionBarPreferenceActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class OptionsActivity extends ActionBarPreferenceActivity implements OnSharedPreferenceChangeListener {

	SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);

		// set title to options
		setTitle(getString(R.string.title_options));

		// get shared preferences
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		setSummaryOfListPreference(PREF_UNIVERSITIES);

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

		if (PREF_USERNAME.equals(key) || PREF_UNIVERSITIES.equals(key)) {

			// if username or university changed delete list of grades
			((GradesApp) getApplication()).getDbAdapter().deleteAll();
		}

		if (PREF_USERNAME.equals(key)) {
			if (DEBUG) Toast.makeText(this, "key username changed", Toast.LENGTH_SHORT).show();

			// delete full name and password
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(PREF_FULL_NAME, "");
			editor.commit();
		}

		if (PREF_UNIVERSITIES.equals(key)) {
			if (DEBUG) Toast.makeText(this, "key listUniversities changed", Toast.LENGTH_SHORT).show();

			setSummaryOfListPreference(key);
		}

	}

	private void setSummaryOfListPreference(String key) {
		// set summary to value in list selected before
		Preference pref = findPreference(key);
		if (pref instanceof ListPreference) {
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(listPref.getEntry());
		}
	}
}
