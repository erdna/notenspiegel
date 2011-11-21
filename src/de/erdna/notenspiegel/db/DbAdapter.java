package de.erdna.notenspiegel.db;

import static de.erdna.notenspiegel.Constants.*;

import de.erdna.notenspiegel.Grade;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
	public static final String KEY_GRADES_TRY = "try";
	public static final String KEY_GRADES_DATE = "date";

	// create tables
	private static final String CREATE_TABLE_GRADES = "CREATE TABLE " + TABLE_GRADES + " (" + KEY_GRADES_ID
			+ " integer primary key autoincrement, " + KEY_GRADES_NR + " text, " + KEY_GRADES_TEXT + " text, "
			+ KEY_GRADES_SEM + " text, " + KEY_GRADES_GRADE + " text, " + KEY_GRADES_TRY + " text, " + KEY_GRADES_DATE
			+ " text);";

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
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
				+ ", which will destroy all old data!");
		db.execSQL(DROP_TABLE_GRADES);
		onCreate(db);
	}

	private boolean existsGrade(Grade grade) {
		// this is my interpretation of an unique grade identifier
		// be open to help me find a better solution!
		Cursor cursor = getReadableDatabase().query(TABLE_GRADES, null,
				KEY_GRADES_NR + " = '" + grade.mNr + "' AND " + KEY_GRADES_TRY + " = '" + grade.mTry + "'", null, null,
				null, null);
		int count = cursor.getCount();
		cursor.close();
		return count > 0;
	}

	public long createIfNotExitsGrade(Grade grade) {

		if (existsGrade(grade)) {
			if (DEBUG) Log.i(TAG, "existsGrade() nr: " + grade.mNr + " try: " + grade.mTry);
		} else {
			if (DEBUG) Log.e(TAG, "NOT existsGrade() nr: " + grade.mNr + " try: " + grade.mTry);

			createGrade(grade);

			// send action broadcast to receivers
			Intent intent = new Intent(ACTION_NEW_GRADE);
			context.sendBroadcast(intent);

		}
		return 0;
	}

	private long createGrade(Grade grade) {
		ContentValues values = new ContentValues();
		values.put(KEY_GRADES_NR, grade.mNr);
		values.put(KEY_GRADES_TEXT, grade.mText);
		values.put(KEY_GRADES_SEM, grade.mSem);
		values.put(KEY_GRADES_GRADE, grade.mGrade);
		values.put(KEY_GRADES_TRY, grade.mTry);
		values.put(KEY_GRADES_DATE, grade.mDate);
		return getWritableDatabase().insert(TABLE_GRADES, null, values);
	}

	public Cursor fetchAllMarks() {
		Cursor cursor = getReadableDatabase().query(TABLE_GRADES, null, null, null, null, null,
				KEY_GRADES_NR + " DESC, " + KEY_GRADES_TRY + " ASC");
		if (cursor != null) cursor.moveToFirst();
		return cursor;
	}

	public int deleteGrade(long id) {
		return getReadableDatabase().delete(TABLE_GRADES, "_id = " + id, null);
	}

	public long deleteAll() {
		return getWritableDatabase().delete(TABLE_GRADES, null, null);
	}

}
