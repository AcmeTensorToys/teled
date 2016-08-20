package com.acmetensortoys.android.teled.Service;

import android.Manifest;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;

public class PositionReporting
{
    public PositionReporting(Context ctx, LocationListener ll) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);

        ctx.enforceCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION,
                "Missing location permission");

        lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000, 10, ll);
    }
}