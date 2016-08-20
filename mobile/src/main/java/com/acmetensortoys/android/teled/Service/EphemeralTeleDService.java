package com.acmetensortoys.android.teled.Service;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.telephony.SmsManager;
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
                Log.d("EphemeralTeleDService", "Test action: " + intent.toString());
                if(intent.getExtras() != null) {
                    Log.d("EphemeralTeleDService", " ... has extras=" + intent.getExtras().toString());
                }
            } else if (ACTION_SEND_LOCATION_SMS.equals(action)) {
                Uri to = intent.getData();

                LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                this.enforceCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION,
                    "Missing location permission");
                Location l = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                SmsManager sm = SmsManager.getDefault();
                // XXX Hook send and delivery messages
                sm.sendTextMessage(to.getSchemeSpecificPart(), null, "locn rep : " + l.toString(), null, null);

            } /* else if (ACTION_BAZ.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBaz(param1, param2);
            } */
        }
    }
}
