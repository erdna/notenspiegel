package de.erdna.notenspiegel;

import de.erdna.notenspiegel.db.DbAdapter;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.SimpleCursorAdapter;

public class MainActivity extends ListActivity {
	private DbAdapter dbAdapter;
	private SimpleCursorAdapter adapter;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(((MyApp) getApplication()).isSyncing());

		// Connect to DataSevice and DB
		dbAdapter = ((MyApp) getApplication()).getDbAdapter();
		dbAdapter.open(true);

		// Get Cursor
		Cursor cursor = dbAdapter.fetchAllMarks();
		startManagingCursor(cursor);

		// Simple Cursor Adapter
		String[] from = { DbAdapter.KEY_MARK_NAME, DbAdapter.KEY_MARK_MARK };
		int[] to = { R.id.textViewMarkName, R.id.textViewMarkMark };
		adapter = new SimpleCursorAdapter(this, R.layout.layout_mark_row, cursor, from, to);
		setListAdapter(adapter);

		// if nothing is configured start PreferencePage
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getString("username", "").equals("")) {
			Intent intent = new Intent(this, PreferencePage.class);
			startActivity(intent);
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		dbAdapter.open(true);
	}

	@Override
	protected void onResume() {
		setProgressBarIndeterminateVisibility(((MyApp) getApplication()).isSyncing());
		adapter.changeCursor(dbAdapter.fetchAllMarks());
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		dbAdapter.close();
	}

	@Override
	protected void onDestroy() {
		dbAdapter.close();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_mark, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_preferences:
			Intent intent = new Intent(this, PreferencePage.class);
			startActivity(intent);
			break;
		case R.id.menu_item_refresh:
			setProgressBarIndeterminateVisibility(true);
			new SyncTask(this, getApplication()).execute();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}