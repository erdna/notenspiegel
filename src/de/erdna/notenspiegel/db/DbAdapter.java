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
	private static final int DATABASE_VERSION = 2;

	// table marks
	public static final String TABLE_GRADES = "grades";
	public static final String KEY_GRADES_ID = "_id";
	public static final String KEY_GRADES_NR = "nr";
	public static final String KEY_GRADES_SEM = "sem";
	public static final String KEY_GRADES_TEXT = "text";
	public static final String KEY_GRADES_GRADE = "grade";
	public static final String KEY_GRADES_DATE = "date";

	// create tables
	private static final String CREATE_TABLE_GRADES = "CREATE TABLE " + TABLE_GRADES + " (" + KEY_GRADES_ID
			+ " integer primary key autoincrement, " + KEY_GRADES_NR + " text, " + KEY_GRADES_SEM + " text, " + KEY_GRADES_TEXT
			+ " text not null, " + KEY_GRADES_GRADE + " text, " + KEY_GRADES_DATE + " text);";

	// drop tables
	private static final String DROP_TABLE_GRADES = "DROP TABLE IF EXISTS " + TABLE_GRADES + ";";

	protected final Context context;

	public DbAdapter(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_GRADES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data!");
		db.execSQL(DROP_TABLE_GRADES);
		onCreate(db);
	}

	public long createMark(String text, String grade) {
		ContentValues values = new ContentValues();
		values.put(KEY_GRADES_TEXT, text);
		values.put(KEY_GRADES_GRADE, grade);
		return getWritableDatabase().insert(TABLE_GRADES, null, values);
	}

	public Cursor fetchAllMarks() {
		Cursor cursor = getReadableDatabase().query(TABLE_GRADES, null, null, null, null, null, null);
		if (cursor != null) cursor.moveToFirst();
		return cursor;
	}

	public long deleteAll() {
		return getWritableDatabase().delete(TABLE_GRADES, null, null);
	}

}
