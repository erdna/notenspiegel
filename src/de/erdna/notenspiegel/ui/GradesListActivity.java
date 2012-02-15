package de.erdna.notenspiegel.ui;

import static de.erdna.notenspiegel.Constants.*;

import de.erdna.notenspiegel.Constants;
import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.db.Average;
import de.erdna.notenspiegel.db.DbAdapter;
import de.erdna.notenspiegel.db.Grade;
import de.erdna.notenspiegel.ui.actionbar.ActionBarActivity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class GradesListActivity extends ActionBarActivity implements OnClickListener, OnItemClickListener {

	private static final int DIALOG_ERROR = 1;
	private static final int DIALOG_AVERAGE = 2;

	private DbAdapter dbAdapter;
	private ListView listView;
	private SimpleCursorAdapter listAdapter;
	private SharedPreferences preferences;
	private boolean isSearchResult = false;

	private String errorMsg;

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_SYNC_STARTED.equals(action)) {
				if (DEBUG) Toast.makeText(context, action, Toast.LENGTH_SHORT).show();

				getActionBarHelper().setRefreshActionItemState(true);
				final Button button = (Button) findViewById(R.id.button_refresh);
				if (button != null) button.setVisibility(View.INVISIBLE);

			} else if (ACTION_SYNC_ERROR.equals(action)) {
				if (DEBUG) Toast.makeText(context, action, Toast.LENGTH_SHORT).show();

				getActionBarHelper().setRefreshActionItemState(((GradesApp) getApplication()).isSyncing());

				Bundle extras = intent.getExtras();
				if (extras != null) {
					errorMsg = extras.getString(EXTRA_ERROR_MSG);
					showDialog(DIALOG_ERROR);
				}

				// refresh list
				refreshGradeList();

			} else if (ACTION_DB_NEWGRADE.equals(action)) {

				// refresh list
				refreshGradeList();

			} else if (ACTION_SYNC_DONE.equals(action)) {
				if (DEBUG) Toast.makeText(context, action, Toast.LENGTH_SHORT).show();

				getActionBarHelper().setRefreshActionItemState(((GradesApp) getApplication()).isSyncing());

				// refresh list
				refreshGradeList();
				
				// show successfully synchronized
				Toast.makeText(context, R.string.successfully_synced, Toast.LENGTH_LONG).show();

			}

		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_list);

		// if nothing is configured start PreferencePage
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getString(PREF_USERNAME, "").equals("")) {
			Intent intent = new Intent(this, OptionsActivity.class);
			startActivity(intent);
		}

		// Connect to DataSevice and DB
		dbAdapter = ((GradesApp) getApplication()).getDbAdapter();

		// Get the intent, verify the action and get the query
		Cursor cursor = null;
		Intent intent = getIntent();
		isSearchResult = Intent.ACTION_SEARCH.equals(intent.getAction());
		if (isSearchResult) {

			if (DEBUG) Toast.makeText(this, "ACTION_SEARCH", Toast.LENGTH_LONG).show();

			// search text
			String query = intent.getStringExtra(SearchManager.QUERY);

			// set title to search text
			setTitle(getString(R.string.search_result) + ": '" + query.trim() + "'");

			// react on search intent
			cursor = dbAdapter.searchGrades(query);
			startManagingCursor(cursor);

		} else {

			// set title
			setTitle(R.string.app_name);

			// normal behavior
			cursor = dbAdapter.fetchAllGrades();
			startManagingCursor(cursor);

		}

		// Simple Cursor Adapter
		String[] from = { Grade.KEY_TRY, Grade.KEY_TEXT, Grade.KEY_GRADE, Grade.KEY_STATUS, Grade.KEY_NOTATION };
		int[] to = { R.id.textViewGradeTry, R.id.textViewGradeText, R.id.textViewGradeGrade, R.id.textViewGradeStatus,
				R.id.textViewGradeNotation };
		listAdapter = new SimpleCursorAdapter(this, R.layout.list_item_grades, cursor, from, to);

		// create and assign adapter
		listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(this);

		// activates context menu on list
		registerForContextMenu(listView);

	}

	@Override
	protected void onStart() {

		// activate progress indicator
		getActionBarHelper().setRefreshActionItemState(((GradesApp) getApplication()).isSyncing());
		super.onStart();

	}

	@Override
	protected void onResume() {
		super.onResume();

		// register broadcast receiver and actions
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_SYNC_STARTED);
		filter.addAction(ACTION_SYNC_ERROR);
		filter.addAction(ACTION_DB_NEWGRADE);
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
		intent.putExtra(Constants.EXTRA_GRADE_ID, id);
		startActivity(intent);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.grade_context_menu, menu);

		// set title to context menu
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		View item = info.targetView;
		TextView textView = (TextView) item.findViewById(R.id.textViewGradeText);
		menu.setHeaderTitle(textView.getText());

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.menuItemContextDelete:
			dbAdapter.deleteGrade(info.id);
			listAdapter.changeCursor(dbAdapter.fetchAllGrades());
			break;

		case R.id.menuItemContextInfo:
			Intent intent = new Intent(this, GradeActivity.class);
			intent.putExtra(Constants.EXTRA_GRADE_ID, info.id);
			startActivity(intent);

		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!isSearchResult) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.grade_menu, menu);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (((GradesApp) getApplication()).isSyncing()) {
			return false;
		} else {
			return super.onPrepareOptionsMenu(menu);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_preferences:
			Intent intent = new Intent(this, OptionsActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_average:
			showDialog(DIALOG_AVERAGE);
			return true;
		case R.id.menu_refresh:
			syncGradeList(null);
			return true;
		case R.id.menu_search:
			onSearchRequested();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder;
		switch (id) {

		case DIALOG_ERROR:
			builder = new AlertDialog.Builder(this);
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

		case DIALOG_AVERAGE:
			dialog = new AverageGradeDialog(this);
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

		case DIALOG_AVERAGE:
			AverageGradeDialog averageGradeDialog = ((AverageGradeDialog) dialog);
			Average gardeAverage = dbAdapter.getGardeAverage();
			averageGradeDialog.setCountAll(gardeAverage.getCountAll());
			averageGradeDialog.setCountCredits(gardeAverage.getCountWithCredits());
			averageGradeDialog.setSumCredits(gardeAverage.getSumCredits());
			averageGradeDialog.setAverage(gardeAverage.getAverage());
			break;

		default:
			break;
		}

		super.onPrepareDialog(id, dialog);

	}

	public void syncGradeList(View view) {
		syncGradeList();
	}

	private void syncGradeList() {

		Intent intent = new Intent(ACTION_START_SYNCSERVICE);
		sendBroadcast(intent);

	}

	public void refreshGradeList() {
		listAdapter.getCursor().requery();

		// show refresh button in center when db is empty and is not syncing
		final Button button = (Button) findViewById(R.id.button_refresh);
		if (dbAdapter.isTableGradesEmpty() && !((GradesApp) getApplication()).isSyncing()) {

			button.setVisibility(View.VISIBLE);

		} else {

			button.setVisibility(View.INVISIBLE);

		}

	}

	public void onClick(DialogInterface dialog, int which) {
		// onClick from OnClickListner
		// at example to react on button clicked
	}

}
