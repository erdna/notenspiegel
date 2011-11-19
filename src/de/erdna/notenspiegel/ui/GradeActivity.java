package de.erdna.notenspiegel.ui;

import de.erdna.notenspiegel.R;
import android.app.Activity;
import android.os.Bundle;

public class GradeActivity extends Activity {

	public static final String EXTRA_GRADE_ID = "grade_id";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grade);
	}

}
