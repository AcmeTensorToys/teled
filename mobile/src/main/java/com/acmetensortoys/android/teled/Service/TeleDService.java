package com.acmetensortoys.android.teled.Service;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.acmetensortoys.android.teled.IOIO.BehaviorFactory;
import com.acmetensortoys.android.teled.IOIO.TeleDIOIOManager;
import com.acmetensortoys.android.teled.R;
import com.acmetensortoys.android.teled.Utils.BehaviorHandle;
import com.acmetensortoys.android.teled.Utils.BehaviorHandleSet;
import com.acmetensortoys.android.teled.Utils.Subscribee;
import com.acmetensortoys.android.teled.Utils.SubscribeeImpl;
import com.acmetensortoys.android.teled.Utils.SuspendableBH;

import java.util.Map;

import fj.function.Effect1;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;

/*
 * This is the core persistent object of the system (or at least, that's the idea);
 * this service should know how to respond to messages from the UI,
 * any remote control channels, location sources, and the underlying
 * behavioral hardware.  Phfew!
 */
public class TeleDService extends Service {
    // Manage a collection of behaviors
    private final BehaviorHandleSet<BehaviorHandle<?>> bhs = new BehaviorHandleSet<>();

    // Some behaviors pause and unpause on SMS preference or GPS preference (or both!)
    private final BehaviorHandleSet<SuspendableBH<?>> smsbhs = new BehaviorHandleSet<>();
    private final BehaviorHandleSet<SuspendableBH<?>> gpsbhs = new BehaviorHandleSet<>();

    // Manage an IOIO
    private final TeleDIOIOManager ios = new TeleDIOIOManager();
    private final IOIOAndroidApplicationHelper ioh = new IOIOAndroidApplicationHelper(this, ios);
    private boolean ioactive;
    private void enableIOIO(Boolean on) {
        synchronized(ioh) {
            if (on && !ioactive) {
                ioh.restart();
            } else if (!on && ioactive) {
                ioh.stop();
            }
            ioactive = on;
        }
    }
    private void enableIOIOByPreference() {
        enableSMS(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("ioio_ena", false));
    }

    // Manage a Broadcast Receiver for SMSes
    //
    // XXX Should this really be here or should it be part of the main application?
    // it's moved back and forth a few times.
    private final SubscribeeImpl<Boolean> mSMSStatSubee = new SubscribeeImpl<>(false);
    private final SMSRecv mSMS = new SMSRecv();
    private void enableSMS(Boolean on) {
        synchronized(mSMS) {
            if (on && !mSMSStatSubee.getLast()) {
                mSMSStatSubee.publish(true);
                registerReceiver(mSMS, new IntentFilter(Manifest.permission.RECEIVE_SMS));
            } else if (!on && mSMSStatSubee.getLast()) {
                unregisterReceiver(mSMS);
                mSMSStatSubee.publish(false);
            }
        }
    }
    private void enableSMSByPreference() {
        enableSMS(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("sms_ena", false));
    }

    // Export an API
    public class LocalBinder extends Binder {
        public Subscribee<Boolean> getIOIOStatus() { return ios.getStatus(); }
        public <In> BehaviorHandle<In> addIOIOBehavior(
                BehaviorHandleSet.Metadata meta,
                BehaviorFactory<In> bf) {
            return addActiveBehavior(meta, ios.makeBehaviorThread(bf));
        }

        public <In> BehaviorHandle<In>
        addActiveBehavior(BehaviorHandleSet.Metadata meta, BehaviorHandle<In> bh) {
            bhs.addActiveBehavior(meta, bh);
            return bh;
        }

        public BehaviorHandle<?> getActiveBehaviorById(int id) {
            return bhs.getBehaviorById(id);
        }
        public Map<Integer,BehaviorHandleSet.Metadata>
        getActiveBehaviors() {
            return bhs.getActiveBehaviorMeta();
        }

        // are both SMS and GPS enabled?
        public boolean sendSMSPositionReports() {
            return mSMSStatSubee.getLast() /* && mGPSStatSubee.getLast() */ ;
        }

        // XXX Any need for this?
        public Subscribee<Boolean> getSMSStatus() { return mSMSStatSubee; }
    }
    private final LocalBinder mBinder = new LocalBinder();

    // Android preferences event glue
    private final SharedPreferences.OnSharedPreferenceChangeListener ospcl =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void
                onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
                    if (sharedPreferences != PreferenceManager
                            .getDefaultSharedPreferences(TeleDService.this)) {
                        // Some other collection of preferences changing?  How odd.
                        return;
                    }
                    switch(key) {
                        case "ioio_ena": enableIOIOByPreference(); break;
                        case "sms_ena": enableSMSByPreference(); break;
                    }
                }
            };

    // Android service-management glue
    private static final int NOTIFY_ID = 1;

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d("TDIS", "onBind");

        // This is a little hackish; to simplify the start/stop/restart state machine,
        // just always start and immediately disable if preferences indicate.
        ioh.start();
        ioactive = true;
        enableIOIOByPreference();

        enableSMSByPreference();

        PreferenceManager.
                getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(ospcl);

        Notification.Builder notif = new Notification.Builder(this);
        notif.setSmallIcon(R.drawable.ic_menu_send)
                .setContentTitle("TeleD service running")
                .setWhen(System.currentTimeMillis())
                .setOngoing(true);
        startForeground(NOTIFY_ID, notif.build());

        // TODO Hook a callback for displaying SMS enabled status?

        // TODO Hook a callback to update notification contents
        bhs.getActiveBehaviorSubee().subscribe(new Effect1<Map<Integer,BehaviorHandleSet.Metadata>>() {
            public void f(Map<Integer,BehaviorHandleSet.Metadata> x) {
                StringBuilder sb = new StringBuilder("Update to active behaviors:\n");
                BehaviorHandleSet.showMetaMap(sb, x);
                Log.d("TDIS", sb.toString());
            }
        });

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent arg0) {
        PreferenceManager.
                getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(ospcl);

        enableSMS(false);
        enableIOIO(false);

        stopForeground(true);
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ioh.create();
    }

    @Override
    public void onDestroy() {
        ioh.destroy();
        super.onDestroy();
    }

}