package de.erdna.notenspiegel.ui;

import static de.erdna.notenspiegel.Constants.*;

import de.erdna.notenspiegel.Grade;
import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.db.DbAdapter;
import de.erdna.notenspiegel.sync.SyncTask;
import de.erdna.notenspiegel.ui.actionbar.ActionBarActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
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
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class GradesListActivity extends ActionBarActivity implements OnClickListener, OnItemClickListener {

	private static final int DIALOG_ERROR = 2;

	private DbAdapter dbAdapter;
	private SimpleCursorAdapter listAdapter;
	private SharedPreferences preferences;

	private String errorMsg;

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (ACTION_SYNC_ERROR.equals(action)) {

				getActionBarHelper().setRefreshActionItemState(((GradesApp) getApplication()).isSyncing());

				Bundle extras = intent.getExtras();
				if (extras != null) {
					errorMsg = extras.getString(EXTRA_ERROR_MSG);
					showDialog(DIALOG_ERROR);
				}

			} else if (ACTION_NEW_GRADE.equals(action)) {

				// if action is refresh
				refreshGradeList();

			} else if (ACTION_SYNC_DONE.equals(action)) {

				getActionBarHelper().setRefreshActionItemState(((GradesApp) getApplication()).isSyncing());

			}

		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_list);

		// if nothing is configured start PreferencePage
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getString("username", "").equals("")) {
			Intent intent = new Intent(this, OptionsActivity.class);
			startActivity(intent);
		}

		// Connect to DataSevice and DB
		dbAdapter = ((GradesApp) getApplication()).getDbAdapter();

		// Get Cursor
		Cursor cursor = dbAdapter.fetchAllMarks();
		startManagingCursor(cursor);

		// Simple Cursor Adapter
		String[] from = { Grade.KEY_TRY, Grade.KEY_TEXT, Grade.KEY_GRADE, Grade.KEY_STATUS, Grade.KEY_NOTATION };
		int[] to = { R.id.textViewGradeTry, R.id.textViewGradeText, R.id.textViewGradeGrade, R.id.textViewGradeStatus,
				R.id.textViewGradeNotation };
		listAdapter = new SimpleCursorAdapter(this, R.layout.list_item_grades, cursor, from, to);

		// create and assign adapter
		ListView listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);

		// activates context menu on list
		registerForContextMenu(listView);

	}

	@Override
	protected void onStart() {

		// set title and activate progress indicator
		setTitle(R.string.app_name);
		getActionBarHelper().setRefreshActionItemState(((GradesApp) getApplication()).isSyncing());
		super.onStart();

	}

	@Override
	protected void onResume() {
		super.onResume();

		// register broadcast receiver and actions
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_NEW_GRADE);
		filter.addAction(ACTION_SYNC_ERROR);
		filter.addAction(ACTION_SYNC_DONE);
		registerReceiver(broadcastReceiver, new IntentFilter(filter));

		refreshGradeList();

		getActionBarHelper().setRefreshActionItemState(((GradesApp) getApplication()).isSyncing());

	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(broadcastReceiver);
	}

	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		Intent intent = new Intent(this, GradeActivity.class);
		intent.putExtra(GradeActivity.EXTRA_GRADE_ID, id);
		startActivity(intent);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grade_context_menu, menu);

		// TODO show correct header
		menu.setHeaderTitle("Context füllen");

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.menuItemContextDelete:
			dbAdapter.deleteGrade(info.id);
			listAdapter.changeCursor(dbAdapter.fetchAllMarks());
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_preferences:
			Intent intent = new Intent(this, OptionsActivity.class);
			startActivity(intent);
			break;
		case R.id.menu_refresh:
			getActionBarHelper().setRefreshActionItemState(true);
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
			builder.setIcon(android.R.drawable.ic_dialog_alert);
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

	public void onClick(DialogInterface dialog, int which) {
		// TODO Auto-generated method stub

	}

	private void refreshGradeList() {
		listAdapter.getCursor().requery();
		// mListAdapter.notifyDataSetChanged();
	}

}