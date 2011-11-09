package de.erdna.notenspiegel.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter {

	private class DbHelper extends SQLiteOpenHelper {

		// database constraints
		private static final String DATABASE_NAME = "applicationdata";
		private static final int DATABASE_VERSION = 1;

		// create tables
		private static final String CREATE_TABLE_MARK = "CREATE TABLE " + DbAdapter.TABLE_MARK + " (" + DbAdapter.KEY_MARK_ID
				+ " integer primary key autoincrement, " + DbAdapter.KEY_MARK_NAME + " text not null, " + DbAdapter.KEY_MARK_MARK
				+ " text not null);";

		// drop tables
		private static final String DROP_TABLE_MARK = "DROP TABLE IF EXISTS " + DbAdapter.TABLE_MARK + ";";

		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE_MARK);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(DbHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data!");

			db.execSQL(DROP_TABLE_MARK);

			onCreate(db);

		}

	}

	private Context context;
	private DbHelper dbHelper;
	private SQLiteDatabase db;

	// table marks
	public static final String TABLE_MARK = "marks";
	public static final String KEY_MARK_ID = "_id";
	public static final String KEY_MARK_NAME = "name";
	public static final String KEY_MARK_MARK = "mark";

	public DbAdapter(Context context) {
		this.context = context;
	}

	public DbAdapter open() throws SQLException {
		dbHelper = new DbHelper(context);
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public long createMark(String name, String mark) {
		ContentValues values = new ContentValues();
		values.put(KEY_MARK_NAME, name);
		values.put(KEY_MARK_MARK, mark);
		return db.insert(TABLE_MARK, null, values);
	}

	public Cursor fetchAllMarks() {
		Cursor cursor = db.query(TABLE_MARK, null, null, null, null, null, null);
		if (cursor != null) cursor.moveToFirst();
		return cursor;
	}

	public long deleteAll() {
		return db.delete(TABLE_MARK, null, null);
	}

	public void close() {
		db.close();
		dbHelper.close();
	}

}
