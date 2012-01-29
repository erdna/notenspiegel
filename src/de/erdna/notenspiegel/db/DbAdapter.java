package de.erdna.notenspiegel.db;

import static de.erdna.notenspiegel.Constants.*;
import static de.erdna.notenspiegel.db.Grade.*;

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

	// create tables
	private static final String CREATE_TABLE_GRADES = "CREATE TABLE " + TABLE_GRADES + " (" + KEY_ID
			+ " integer primary key autoincrement, " + KEY_NR + " text, " + KEY_TEXT + " text, " + KEY_SEM + " text, "
			+ KEY_GRADE + " text, " + KEY_STATUS + " text, " + KEY_CREDITS + " text, " + KEY_NOTATION + " text, "
			+ KEY_TRY + " text, " + KEY_DATE + " text);";

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
		Cursor cursor = getReadableDatabase().query(
				TABLE_GRADES,
				null,
				KEY_NR + " = '" + grade.mNr + "' AND " + KEY_TRY + " = '" + grade.mTry + "' AND " + KEY_DATE + " = '"
						+ grade.mDate + "'", null, null, null, null);
		int count = cursor.getCount();
		cursor.close();
		return count > 0;
	}

	public long createIfNotExitsGrade(Grade grade) {
		long ret = -1;

		if (existsGrade(grade)) {
			if (DEBUG) Log.i(TAG, "grade exists already (nr,try,date) " + grade.mNr + "," + grade.mTry + ","
					+ grade.mDate);
		} else {
			if (DEBUG) Log.i(TAG, "create grade (nr,try,date) " + grade.mNr + "," + grade.mTry + "," + grade.mDate);

			ret = createGrade(grade);

			// send action broadcast to receivers
			Intent intent = new Intent(ACTION_NEW_GRADE);
			intent.putExtra(EXTRA_GRADE_TEXT, grade.mText + " " + grade.mGrade);
			context.sendBroadcast(intent);

		}
		return ret;
	}

	private long createGrade(Grade grade) {
		ContentValues values = new ContentValues();
		values.put(KEY_NR, grade.mNr);
		values.put(KEY_TEXT, grade.mText);
		values.put(KEY_SEM, grade.mSem);
		values.put(KEY_GRADE, grade.mGrade);
		values.put(KEY_STATUS, grade.mStatus);
		values.put(KEY_CREDITS, grade.mCredits);
		values.put(KEY_NOTATION, grade.mNotation);
		values.put(KEY_TRY, grade.mTry);
		values.put(KEY_DATE, grade.mDate);
		return getWritableDatabase().insert(TABLE_GRADES, null, values);
	}

	public Cursor fetchAllGrades() {
		Cursor cursor = getReadableDatabase().query(TABLE_GRADES, null, null, null, null, null,
				KEY_NR + " DESC, " + KEY_TRY + " ASC");
		if (cursor != null) cursor.moveToFirst();
		return cursor;
	}

	public int deleteGrade(long id) {
		return getReadableDatabase().delete(TABLE_GRADES, "_id = " + id, null);
	}

	public long deleteAll() {
		return getWritableDatabase().delete(TABLE_GRADES, null, null);
	}

	public Grade fetchGrade(long mId) {
		Grade grade = new Grade();

		String[] columns = new String[] { Grade.KEY_NR, Grade.KEY_TEXT, Grade.KEY_SEM, Grade.KEY_GRADE,
				Grade.KEY_STATUS, Grade.KEY_CREDITS, Grade.KEY_NOTATION, Grade.KEY_TRY, Grade.KEY_DATE };

		Cursor cursor = getReadableDatabase().query(TABLE_GRADES, columns, "_id = ?",
				new String[] { Long.toString(mId) }, null, null, null, "1");
		cursor.moveToFirst();

		if (cursor.getCount() > 0) {
			grade.mNr = cursor.getString(0);
			grade.mText = cursor.getString(1);
			grade.mSem = cursor.getString(2);
			grade.mGrade = cursor.getString(3);
			grade.mStatus = cursor.getString(4);
			grade.mCredits = cursor.getString(5);
			grade.mNotation = cursor.getString(6);
			grade.mTry = cursor.getString(7);
			grade.mDate = cursor.getString(8);
		}
		return grade;
	}

	public boolean isTableGradesEmpty() {
		Cursor cursor = getReadableDatabase().query(TABLE_GRADES, null, null, null, null, null, null);
		int count = cursor.getCount();
		cursor.close();
		return count == 0;
	}

	public Cursor searchGrades(String query) {

		// search text contains
		String s = '%' + query.trim() + '%';
		Cursor cursor = getReadableDatabase().query(TABLE_GRADES, null,
				KEY_TEXT + " LIKE ? OR " + KEY_NOTATION + " LIKE ?", new String[] { s, s }, null, null,
				KEY_NR + " DESC, " + KEY_TRY + " ASC");
		if (cursor != null) cursor.moveToFirst();
		return cursor;
	}

	/**
	 * calc average (alpha)
	 * 
	 * Just for HTW students with new regulations affected to the Bologna Process.
	 * @return Number as String
	 */
	public String getGardeAverage() {
		final String SQL_AVERAGE = "SELECT " + KEY_CREDITS + " ," + KEY_GRADE + " FROM " + TABLE_GRADES;
		Cursor cursor = getReadableDatabase().rawQuery(SQL_AVERAGE, null);

		int credits = 0;
		float tmp = 0;
		while (cursor.moveToNext()) {
			int credit = cursor.getInt(0);
			credits += credit;
			String string = cursor.getString(1).replaceAll("[^0-9,]", "").replace(",", ".");
			float grade = Float.parseFloat(string);
			tmp += (grade * credit);
		}
		String average = String.format("%.2f", (tmp / credits));
		return "count:\t\t" + cursor.getCount() + "\ncredits:\t" + credits + "\naverage:\t" + average;
	}
}
