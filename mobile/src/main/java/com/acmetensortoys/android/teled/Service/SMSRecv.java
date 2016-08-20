package com.acmetensortoys.android.teled.Service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.acmetensortoys.android.teled.Utils.BehaviorHandleSet;

// TODO: The constructor of this class should bind the Service for us
// so we're not binding and un-binding at every message?  What a pain in the ass.

public class SMSRecv extends BroadcastReceiver {
    public SMSRecv() {
        Log.d("SMSRecv","Constructor");
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("TeleD/SMSRecv", intent.toString());

        // TODO: Check that we should be doing SMS, authenticate incoming message,
        // figure out what behaviors need to be spawned, ...

        context.bindService(
                new Intent(context, TeleDService.class),
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        TeleDService.LocalBinder tds = ((TeleDService.LocalBinder) service);

                        // Grab list of current behaviors
                        StringBuilder sb = new StringBuilder();
                        BehaviorHandleSet.showMetaMap(sb, tds.getActiveBehaviors());

                        // TODO: Send response message

                        context.unbindService(this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) { ; }
                },
                0);

        /* NWF planning aloud here.
         *
         * We should check that we should even be paying attention to SMS;
         * they may be disabled.  That's now handled by dynamic registration of
         * the receiver in TeleDService.
         *
         * We're going to check some authentication present in the incoming
         * message -- source number, signed message hash, etc. -- and are then
         * going to decode it in full.
         *
         * The message should contain a sequence number that we echo back,
         * Since SMS's may reorder on the wire, let's just not support
         * multiple outstanding messages; simpler on us this way...
         */
    }
}
