package com.acmetensortoys.android.teled.Utils.Android;

import android.app.PendingIntent;
import android.content.Context;
import android.location.LocationManager;

public class LocationUpdateBH extends PendingIntentBH {
    private final Context c;
    private final String prov;
    private final long mint;
    private final float mind;

    LocationUpdateBH(Context c, String provider, long minTime, float minDist, PendingIntent pi){
        super(pi);
        this.c = c;
        this.prov = provider;
        this.mint = minTime;
        this.mind = minDist;
    }

    @Override
    protected boolean onStart(PendingIntent pi) {
        LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        try {
            lm.requestLocationUpdates(prov, mint, mind, pi);
            return true;
        } catch (SecurityException se) {
            return false;
        }
    }

    @Override
    protected void onPause(PendingIntent pi) {
        LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        lm.removeUpdates(pi);
    }
}
