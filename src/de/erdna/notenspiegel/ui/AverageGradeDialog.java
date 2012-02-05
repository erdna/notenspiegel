package de.erdna.notenspiegel.ui;

import de.erdna.notenspiegel.R;
import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

public class AverageGradeDialog extends Dialog {

	public AverageGradeDialog(Context context) {
		super(context);
		setContentView(R.layout.dialog_average_grad);
		setTitle(R.string.dialog_average_title);

		// TextView text = (TextView) findViewById(R.id.text);
		// text.setText("Hello, this is a custom dialog!");
		// ImageView image = (ImageView) findViewById(R.id.image);
		// image.setImageResource(android.R.drawable.ic_menu_myplaces);
	}

	public void setCountAll(String countAll) {
		TextView text = (TextView) findViewById(R.id.textDialogCountAll);
		text.setText(countAll);
	}

	public void setCountCredits(String countCredits) {
		TextView text = (TextView) findViewById(R.id.textDialogCountCredits);
		text.setText(countCredits);
	}

	public void setSumCredits(String sumCredits) {
		TextView text = (TextView) findViewById(R.id.textDialogSumCredits);
		text.setText(sumCredits);
	}
	
	public void setAverage(String average) {
		TextView text = (TextView) findViewById(R.id.textDialogAverage);
		text.setText(average);
	}

}
