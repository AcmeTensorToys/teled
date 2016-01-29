package com.acmetensortoys.android.teled;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SMSRecv extends BroadcastReceiver {
    public SMSRecv() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        throw new UnsupportedOperationException("Not yet implemented");

        /* NWF planning aloud here.
         *
         * We should check that we should even be paying attention to SMS;
         * they may be disabled.
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
