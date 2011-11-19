package de.erdna.notenspiegel;

public class Grade {

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
