package com.acmetensortoys.android.teled;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Vector;

public class RemoteControlService extends Service {
    private boolean remoteControlEnabled = false;
    private Vector<Thread> ongoingRemoteBehaviors = new Vector<>(1);

    private void removeOngoing(Thread t) {
        synchronized(RemoteControlService.this) {
            ongoingRemoteBehaviors.remove(t);
        }
    }

    public interface OnCessation {
        void run();
    }

    // Assumes that t has been started already
    private void _addOngoing(final Thread t, final OnCessation l) {
        Thread nt = new Thread() {
            public void run() {
                while(t.isAlive()) {
                    //noinspection EmptyCatchBlock
                    try { t.join(); } catch (InterruptedException e) { }
                }
                removeOngoing(t);
                l.run();
            }
        };
        nt.start();
        synchronized(this) { ongoingRemoteBehaviors.add(t); }
    }

    private boolean startAndAddOngoing(Thread t, OnCessation l) {
        if(!remoteControlEnabled) {
            return false;
        }
        t.start();
        _addOngoing(t, l);
        return true;
    }

    // TODO Should have a similar set of functionality for behaviors that should
    // resume at some later point.  Consider losing the IOIO connection but wanting
    // to restart when it comes back.

    class LocalBinder extends Binder {
        public void stopRemoteControl() {
            synchronized (this) {
                remoteControlEnabled = false;
                // Cause all outstanding behaviors to cease their activities
                for (Thread t : ongoingRemoteBehaviors) {
                    t.interrupt();
                }
            }
        }
    }
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d("RCS", "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent arg0) {
        Log.d("RCS", "onUnbind");
        return false;
    }
}
