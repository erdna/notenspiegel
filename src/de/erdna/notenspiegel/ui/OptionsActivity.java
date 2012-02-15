package de.erdna.notenspiegel.ui;

import static de.erdna.notenspiegel.Constants.*;

import java.util.Date;

import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.ui.actionbar.ActionBarPreferenceActivity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
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
		setSummaryOfAutoSync(sharedPreferences, PREF_INTERVAL);

		// set version name to legal information
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String legal = getString(R.string.legal_information_summary, packageInfo.versionName);
			findPreference(PREF_LEGAL).setSummary(legal);
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Could not get versionName, package name not found!");
			if (DEBUG) e.printStackTrace();
		}

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

		if (DEBUG) Toast.makeText(this, "key " + key + " changed", Toast.LENGTH_SHORT).show();

		if (PREF_USERNAME.equals(key) || PREF_UNIVERSITIES.equals(key)) {

			// if username or university changed delete list of grades
			((GradesApp) getApplication()).getDbAdapter().deleteAll();

		}

		if (PREF_USERNAME.equals(key)) {
			// delete full name and password
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(PREF_FULL_NAME, "");
			editor.commit();
		}

		if (PREF_UNIVERSITIES.equals(key)) {
			// set summary
			setSummaryOfListPreference(key);
		}

		if (PREF_INTERVAL.equals(key)) {

			setSummaryOfAutoSync(sharedPreferences, key);

			long interval = Long.valueOf(sharedPreferences.getString(PREF_INTERVAL, "0"));

			setAutoSync(interval);

		}

	}

	/**
	 * Set interval of auto synchronization with AlarmManager.
	 * 
	 * @param interval
	 *            in milliseconds: 604800000(7 days), 259200000(3 days),
	 *            86400000(24 hours), 21600000(6 hours) 3600000(1 hours)
	 *            1800000(30 minutes) 0(disable)
	 */
	private void setAutoSync(long interval) {
		Intent intent = new Intent(ACTION_START_SYNCSERVICE);
		PendingIntent operation = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		if (interval <= 0) {
			alarmManager.cancel(operation);
		} else if (interval <= 3600000) {
			long triggerAtTime = System.currentTimeMillis() + interval;
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtTime, interval, operation);
		} else {
			long triggerAtTime = System.currentTimeMillis() + interval;
			alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTime, interval, operation);
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

	private void setSummaryOfAutoSync(SharedPreferences sharedPreferences, String key) {

		Preference pref = findPreference(key);
		if (pref instanceof ListPreference) {

			// get last update time stamp
			String lastSyncText = "";
			long lastSync = sharedPreferences.getLong(PREF_LAST_SYNC, 0);
			Date d = new Date(lastSync);
			if (lastSync > 0) lastSyncText = "\n" + getString(R.string.last_sync) + " " + d.toLocaleString();

			// set summary
			ListPreference listPref = (ListPreference) pref;
			pref.setSummary(getString(R.string.sync_interval) + " " + listPref.getEntry() + lastSyncText);

		}
	}
}
