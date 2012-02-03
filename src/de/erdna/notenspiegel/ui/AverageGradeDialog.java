package de.erdna.notenspiegel.ui;

import de.erdna.notenspiegel.R;
import android.app.Dialog;
import android.content.Context;

public class AverageGradeDialog extends Dialog {

	public AverageGradeDialog(Context context) {
		super(context);
		setContentView(R.layout.dialog_average_grad);
		setTitle("Average Grade Dialog");

//		TextView text = (TextView) findViewById(R.id.text);
//		text.setText("Hello, this is a custom dialog!");
//		ImageView image = (ImageView) findViewById(R.id.image);
//		image.setImageResource(android.R.drawable.ic_menu_myplaces);
	}

}
