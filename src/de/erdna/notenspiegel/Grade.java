package de.erdna.notenspiegel;

public class Grade {

	public static final String KEY_ID = "_id";
	public static final String KEY_NR = "nr";
	public static final String KEY_SEM = "sem";
	public static final String KEY_TEXT = "text";
	public static final String KEY_GRADE = "grade";
	public static final String KEY_TRY = "try";
	public static final String KEY_DATE = "date";

	public String mNr;
	public String mText;
	public String mSem;
	public String mGrade;
	public String mStatus;
	public String mCredits;
	public String mEcts;
	public String mNotation;
	public String mTry;
	public String mDate;

	public Grade() {
		clear();
	}

	public void clear() {
		mNr = "";
		mText = "";
		mSem = "";
		mGrade = "";
		mStatus = "";
		mCredits = "";
		mEcts = "";
		mNotation = "";
		mTry = "";
		mDate = "";
	}

}
