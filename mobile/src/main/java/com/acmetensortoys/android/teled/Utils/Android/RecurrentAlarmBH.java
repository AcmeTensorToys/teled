package com.acmetensortoys.android.teled.Utils.Android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

public class RecurrentAlarmBH extends PendingIntentBH {
    private final Context c;
    private final int type;
    private final long tam;
    private final long ri;

    RecurrentAlarmBH(Context c, int type, long triggerAtMillis, long repeatInterval, PendingIntent pi){
        super(pi);
        this.c = c;
        this.type = type;
        this.tam = triggerAtMillis;
        this.ri = repeatInterval;
    }

    @Override
    protected boolean onStart(PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(type, tam, ri, pi);
        return true;
    }

    @Override
    protected void onPause(PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
    }
}
