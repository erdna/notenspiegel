package de.erdna.notenspiegel;

import de.erdna.notenspiegel.db.DbAdapter;
import de.erdna.notenspiegel.sync.SyncTask;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

public class GradesListActivity extends ListActivity {

	private static final int DIALOG_ACCEPT_CERT = 1;

	public static final String EXTRA_ERROR_MSG = "EXTRA_ERROR_MSG";

	private DbAdapter dbAdapter;
	private SimpleCursorAdapter adapter;
	private Context context;
	private SharedPreferences preferences;
	private Bundle extras;

	// private Cursor cursor;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// save own context at example for dialogs
		context = this;

		// activate progress indicator
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(((MyApp) getApplication()).isSyncing());

		// Connect to DataSevice and DB
		dbAdapter = ((MyApp) getApplication()).getDbAdapter();

		// Get Cursor
		Cursor cursor = dbAdapter.fetchAllMarks();
		startManagingCursor(cursor);

		// Simple Cursor Adapter
		String[] from = { DbAdapter.KEY_MARK_NAME, DbAdapter.KEY_MARK_MARK };
		int[] to = { R.id.textViewMarkName, R.id.textViewMarkMark };
		adapter = new SimpleCursorAdapter(this, R.layout.layout_mark_row, cursor, from, to);
		setListAdapter(adapter);

		// if nothing is configured start PreferencePage
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getString("username", "").equals("")) {
			Intent intent = new Intent(this, Options.class);
			startActivity(intent);
		}

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// get extras if necessary
		extras = intent.getExtras();
	}

	@Override
	protected void onResume() {

		if (extras != null) {
			String errorMsg = extras.getString(EXTRA_ERROR_MSG);
			if (errorMsg != null) Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
			extras.clear();
		}

		setProgressBarIndeterminateVisibility(((MyApp) getApplication()).isSyncing());

		// refresh list of grades
		adapter.getCursor().close();
		adapter.changeCursor(dbAdapter.fetchAllMarks());

		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_mark, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.menu_item_refresh);
		item.setEnabled(!((MyApp) getApplication()).isSyncing());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_preferences:
			Intent intent = new Intent(this, Options.class);
			startActivity(intent);
			break;
		case R.id.menu_item_refresh:
			// TODO react on decission in options
			// showDialog(DIALOG_ACCEPT_CERT);
			setProgressBarIndeterminateVisibility(true);
			new SyncTask(this, getApplication()).execute();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {

		case DIALOG_ACCEPT_CERT:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.warning_certificate, "HTW Dresden"));
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.btn_ignore, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					setProgressBarIndeterminateVisibility(true);
					new SyncTask(context, getApplication()).execute();
				}
			});
			builder.setNegativeButton(R.string.btn_abort, new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			dialog = builder.create();
			break;

		default:
			dialog = null;
			break;
		}
		return dialog;
	}
}