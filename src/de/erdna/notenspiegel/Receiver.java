package de.erdna.notenspiegel;

import static de.erdna.notenspiegel.Constants.*;
import de.erdna.notenspiegel.ui.GradesListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class Receiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (ACTION_NEW_GRADE.equals(action)) {

			Bundle extras = intent.getExtras();
			if (extras != null) {
				String text = extras.getString(EXTRA_GRADE_TEXT);
				int count = extras.getInt(EXTRA_GRADE_COUNT);
				updateNotification(context, count, text);

			}
		}

	}

	private void updateNotification(Context context, int notificationCount, String text) {

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		// singular
		String message = context.getString(R.string.notification_new_grade_msg);

		// plural
		if (notificationCount != 1) {
			text = context.getString(R.string.notification_new_grades);
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			message = sharedPreferences.getString(PREF_FULL_NAME, "Mister Nobody");
			message = message.concat("(" + notificationCount + ")");
		}

		Notification notification = new Notification(R.drawable.ic_stat_launcher, text, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context,
				GradesListActivity.class), 0);

		notification.setLatestEventInfo(context, text, message, contentIntent);
		manager.notify(NOTIFICATION, notification);
	}

}
