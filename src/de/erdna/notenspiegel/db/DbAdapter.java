package de.erdna.notenspiegel.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter extends SQLiteOpenHelper {

	private final String TAG = this.getClass().getSimpleName();

	// database constraints
	private static final String DATABASE_NAME = "applicationdata";
	private static final int DATABASE_VERSION = 1;

	// create tables
	private static final String CREATE_TABLE_MARK = "CREATE TABLE " + DbAdapter.TABLE_MARK + " (" + DbAdapter.KEY_MARK_ID
			+ " integer primary key autoincrement, " + DbAdapter.KEY_MARK_NAME + " text not null, " + DbAdapter.KEY_MARK_MARK
			+ " text not null);";

	// drop tables
	private static final String DROP_TABLE_MARK = "DROP TABLE IF EXISTS " + DbAdapter.TABLE_MARK + ";";

	// table marks
	public static final String TABLE_MARK = "marks";
	public static final String KEY_MARK_ID = "_id";
	public static final String KEY_MARK_NAME = "name";
	public static final String KEY_MARK_MARK = "mark";

	protected final Context context;

	// private SQLiteDatabase db;

	public DbAdapter(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_MARK);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data!");
		db.execSQL(DROP_TABLE_MARK);
		onCreate(db);
	}

	public long createMark(String name, String mark) {
		ContentValues values = new ContentValues();
		values.put(KEY_MARK_NAME, name);
		values.put(KEY_MARK_MARK, mark);
		return getWritableDatabase().insert(TABLE_MARK, null, values);
	}

	public Cursor fetchAllMarks() {
		Cursor cursor = getReadableDatabase().query(TABLE_MARK, null, null, null, null, null, null);
		if (cursor != null) cursor.moveToFirst();
		return cursor;
	}

	public long deleteAll() {
		return getWritableDatabase().delete(TABLE_MARK, null, null);
	}

}
