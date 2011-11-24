package de.erdna.notenspiegel.ui;

import de.erdna.notenspiegel.GradesApp;
import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.db.DbAdapter;
import de.erdna.notenspiegel.db.Grade;
import de.erdna.notenspiegel.ui.actionbar.ActionBarActivity;
import android.os.Bundle;
import android.view.View;

public class GradeActivity extends ActionBarActivity {

	public static final String EXTRA_GRADE_ID = "grade_id";
	private DbAdapter mDbAdapter;
	private long mId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grade);

		// get id from intent
		mId = getIntent().getLongExtra(EXTRA_GRADE_ID, -1);
		if (mId == -1) finish();

		// get DbAdapter
		mDbAdapter = ((GradesApp) getApplication()).getDbAdapter();

	}

	@Override
	protected void onStart() {
		super.onStart();

		// set content from db by id
		Grade grade = mDbAdapter.fetchGrade(mId);
		setTitle(grade.mText);
		setText(R.id.textViewDetailsNr, grade.mNr);
		setText(R.id.textViewDetailsText, grade.mText);
		setText(R.id.textViewDetailsSem, grade.mSem);
		setText(R.id.textViewDetailsGrade, grade.mGrade);
		setText(R.id.textViewDetailsStatus, grade.mStatus);
		setText(R.id.textViewDetailsCredits, grade.mCredits);
		setText(R.id.textViewDetailsEcts, grade.mEcts);
		setText(R.id.textViewDetailsNotation, grade.mNotation);
		setText(R.id.textViewDetailsDate, grade.mDate);

	}

	@Override
	protected void setText(int viewId, String text) {
		super.setText(viewId, text);
		if (text.trim().length() == 0) {
			findViewById(viewId).setVisibility(View.GONE);
		}
	}

}
