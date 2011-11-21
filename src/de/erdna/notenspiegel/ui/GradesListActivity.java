package de.erdna.notenspiegel.ui;

import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.db.DbAdapter;
import de.erdna.notenspiegel.sync.SyncTask;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class GradesListActivity extends ListActivity {

	private static final int DIALOG_ERROR = 2;

	public static final String EXTRA_ERROR_MSG = "EXTRA_ERROR_MSG";
	public static final String EXTRA_REFRESH = "EXTRA_REFRESH";

	private DbAdapter dbAdapter;
	private SimpleCursorAdapter adapter;
	private SharedPreferences preferences;
	private Bundle extras;

	private String errorMsg;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// activate progress indicator
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminateVisibility(((GradesApp) getApplication()).isSyncing());

		// Connect to DataSevice and DB
		dbAdapter = ((GradesApp) getApplication()).getDbAdapter();

		// activates context menu on list
		registerForContextMenu(getListView());

		// Get Cursor
		Cursor cursor = dbAdapter.fetchAllMarks();
		startManagingCursor(cursor);

		// Simple Cursor Adapter
		String[] from = { DbAdapter.KEY_GRADES_TEXT, DbAdapter.KEY_GRADES_GRADE };
		int[] to = { R.id.textViewGradeText, R.id.textViewGradeGrade };
		adapter = new SimpleCursorAdapter(this, R.layout.list_item_grades, cursor, from, to);
		setListAdapter(adapter);

		// if nothing is configured start PreferencePage
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getString("username", "").equals("")) {
			Intent intent = new Intent(this, OptionsActivity.class);
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

			errorMsg = extras.getString(EXTRA_ERROR_MSG);
			if (errorMsg != null && errorMsg.length() > 0) {
				// Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
				showDialog(DIALOG_ERROR);
				extras.clear();
			}

		}

		setProgressBarIndeterminateVisibility(((GradesApp) getApplication()).isSyncing());

		// refresh list of grades
		adapter.changeCursor(dbAdapter.fetchAllMarks());

		super.onResume();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(this, GradeActivity.class);
		intent.putExtra(GradeActivity.EXTRA_GRADE_ID, id);
		startActivity(intent);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grade_context_menu, menu);
		menu.setHeaderTitle("Context füllen");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.menuItemContextDelete:
			dbAdapter.deleteGrade(info.id);
			adapter.changeCursor(dbAdapter.fetchAllMarks());
			break;

		case R.id.menuItemContextInfo:
			Intent intent = new Intent(this, GradeActivity.class);
			intent.putExtra(GradeActivity.EXTRA_GRADE_ID, info.id);
			startActivity(intent);

		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grade_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(R.id.menu_item_refresh);
		item.setEnabled(!((GradesApp) getApplication()).isSyncing());
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_preferences:
			Intent intent = new Intent(this, OptionsActivity.class);
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

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {

		case DIALOG_ERROR:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(android.R.string.dialog_alert_title);
			builder.setMessage("Wenn man diesen Text sieht, ist was schief gegangen!");
			builder.setCancelable(true);
			builder.setIcon(android.R.drawable.ic_dialog_info);
			builder.setNeutralButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

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

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {

		case DIALOG_ERROR:
			((AlertDialog) dialog).setMessage(errorMsg);
			break;

		default:
			break;
		}

		super.onPrepareDialog(id, dialog);

	}
}