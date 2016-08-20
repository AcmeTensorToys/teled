package com.acmetensortoys.android.teled.Service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 * Used within TeleD to handle things like SMS dispatch via PendingIntent.
 * What a pain in the ass.
 */
public class EphemeralTeleDService extends IntentService {
    private static final String ACTION_SEND_LOCATION_SMS
            = "com.acmetensortoys.android.teled.action.SEND_LOCATION_SMS";
    private static final String ACTION_TEST
            = "com.acmetensortoys.android.teled.action.TEST";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.acmetensortoys.android.teled.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.acmetensortoys.android.teled.extra.PARAM2";

    public EphemeralTeleDService() {
        super("EphemeralTeleDService");
    }

    public static PendingIntent pendingSendLocationSMS(Context context, Uri data) {
        Intent intent = new Intent(ACTION_SEND_LOCATION_SMS,
                data, context, EphemeralTeleDService.class);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    public static PendingIntent pendingTest(Context context, Uri data) {
        Intent intent = new Intent(ACTION_TEST, data, context, EphemeralTeleDService.class);
        return PendingIntent.getService(context, 0, intent, 0);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_TEST.equals(action)) {
                Log.d("EphemeralTeleDService", "Test action: " + intent.toString() + " eb=" + intent.getExtras().toString());
            } else if (ACTION_SEND_LOCATION_SMS.equals(action)) {
                // final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                handleActionSendLocationSMS(intent);
            } /* else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            } */
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionSendLocationSMS(Intent i) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
