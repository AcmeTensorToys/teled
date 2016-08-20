package com.acmetensortoys.android.teled.UI;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import com.acmetensortoys.android.teled.R;
import com.acmetensortoys.android.teled.Service.EphemeralTeleDService;

public class FragPrefs
        extends PreferenceFragment {

    private final int REQID_SMS_PICK = 1234;

    @Override
    public void onCreate(Bundle sis) {
        super.onCreate(sis);
        addPreferencesFromResource(R.xml.pref_frag_main);

        /* Customize preferences */
        Preference p;

        /*
         * Bahaha.  We can't create a custom Preference class for this because...
         * getPreferenceManager().getFragment() is package private.  Goddammit.
         */
        p = findPreference("sms_pick");
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference _p) {
                Intent conPik = new Intent(
                        Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(conPik, REQID_SMS_PICK);
                return true;
            }
        });

        /*
        p = findPreference("ioio_mac");
        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference _p) {
                ListPreference lp = (ListPreference) _p;
                lp.setEntries(new CharSequence[]{});
                lp.setEntryValues(new CharSequence[]{});
                return true;
            }
        });
        */
    }

    static final String[] phone_Uri_projection = { ContactsContract.CommonDataKinds.Phone.NUMBER };

    @Override
    public void onActivityResult(int req, int res, Intent i) {
        if(req == REQID_SMS_PICK && res == Activity.RESULT_OK) {
            Log.d("FragPrefs", "SMS_PICK " + i.toString());

            Cursor c = getActivity().getContentResolver()
                    .query(i.getData(), phone_Uri_projection, null, null, null );
            if (c != null) {
                c.moveToFirst();
                String number = c.getString(c.getColumnIndex(phone_Uri_projection[0]));
                c.close();

                Log.d("FragPrefs", " ... which is to say: " + number);

                Uri x = Uri.fromParts("smsto", number, "");
                PendingIntent pi = EphemeralTeleDService.pendingSendLocationSMS(getActivity(), x);
                try {
                    pi.send();
                } catch (PendingIntent.CanceledException ce) {
                    Log.d("FragPrefs", "... pi cancelled?");
                }
            } else {
                Log.d("FragPrefs", " ... null cursor?");
            }
        }
    }
}
