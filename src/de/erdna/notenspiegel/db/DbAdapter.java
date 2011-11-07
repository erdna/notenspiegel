package de.erdna.notenspiegel.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DbAdapter {

	private Context context;
	private SQLiteDatabase db;
	private DbHelper dbHelper;

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
		if (cursor != null)
			cursor.moveToFirst();
		return cursor;
	}

	public long deleteAll() {
		return db.delete(TABLE_MARK, null, null);
	}

}
