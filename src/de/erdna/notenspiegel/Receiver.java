package de.erdna.notenspiegel;

import static de.erdna.notenspiegel.Constants.*;
import de.erdna.notenspiegel.sync.SyncService;
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

	private static int mSyncCount = 0;

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		if (ACTION_START_SYNCSERVICE.equals(action)) {
			// called if alarm manager encountered sync action
			// or someone clicked on refresh in ui

			Intent service = new Intent(context, SyncService.class);
			context.startService(service);

		} else if (ACTION_SYNC_STARTED.equals(action)) {
			// called when sync task has began to sync

		} else if (ACTION_DB_NEWGRADE.equals(action)) {
			// called after each new grade synchronized

			Bundle extras = intent.getExtras();
			if (extras != null) {
				String text = extras.getString(EXTRA_GRADE_TEXT);
				updateNotification(context, ++mSyncCount, text);

			}
		} else if (ACTION_SYNC_DONE.equals(action)) {
			// called when sync was successful

			mSyncCount = 0;

		} else if (ACTION_SYNC_ERROR.equals(action)) {
			// called when sync received an http, ssl or parser error

			mSyncCount = 0;

		}

	}

	private void updateNotification(Context context, int notificationCount, String text) {

		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		// singular
		String message = context.getString(R.string.notification_new_grade);

		// plural
		if (notificationCount != 1) {
			text = context.getString(R.string.notification_new_grades);
			message = sharedPreferences.getString(PREF_FULL_NAME, "");
			message = message.concat("(" + notificationCount + ")");
		}

		Notification notification = new Notification(R.drawable.ic_stat_launcher, text, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		// set LED lights
		notification.defaults |= Notification.DEFAULT_LIGHTS;

		// set vibration
		if (sharedPreferences.getBoolean(PREF_VIBRATE, false)) notification.defaults |= Notification.DEFAULT_VIBRATE;

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context,
				GradesListActivity.class), 0);

		notification.setLatestEventInfo(context, text, message, contentIntent);
		manager.notify(NOTIFICATION, notification);
	}

}
