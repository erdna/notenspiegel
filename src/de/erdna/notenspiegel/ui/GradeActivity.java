package de.erdna.notenspiegel.ui;

import de.erdna.notenspiegel.R;
import de.erdna.notenspiegel.ui.actionbar.ActionBarActivity;
import android.os.Bundle;

public class GradeActivity extends ActionBarActivity {

	public static final String EXTRA_GRADE_ID = "grade_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grade);
		
		// set title
		setTitle(getString(R.string.title_grade));
	}

}
