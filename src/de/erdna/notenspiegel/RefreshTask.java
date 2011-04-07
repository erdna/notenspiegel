package de.erdna.notenspiegel;

import de.erdna.notenspiegel.db.DbAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

public class RefreshTask extends AsyncTask<Object, Void, Void> {

	private Context context;

	@Override
	protected Void doInBackground(Object... objects) {
		
		// get context from MainActivity
		context = (Context) objects[0];
		
		// Connect to DataSevice and DB
		DbAdapter dbAdapter = new DbAdapter(context);
		dbAdapter.open();

		dbAdapter.createMark("Info", "1.7");

		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		Intent intent = new Intent(context,MainActivity.class);
		context.startActivity(intent);
		super.onPostExecute(result);
	}

}
