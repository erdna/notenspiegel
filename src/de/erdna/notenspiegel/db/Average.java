package de.erdna.notenspiegel.db;

public class Average {

	public Average() {
		clear();
	}

	public void clear() {
		mSumCredits = 0;
		mWeightedGarde = 0;
		mCountWithCredits = 0;
		mCountAll = 0;
	}

	public int mSumCredits;
	public float mWeightedGarde;
	public int mCountWithCredits;
	public int mCountAll;

	public String getAverage() {
		return String.format("%.2f", (mWeightedGarde / mSumCredits));
	}
}
