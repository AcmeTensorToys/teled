// Based, in long past, on IOIOService example from Ytai's repository

package com.acmetensortoys.android.teled;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.Vector;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.Closeable;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;

public class IOIOService extends ioio.lib.util.android.IOIOService {
    private static final int NOTIFY_ID = 1;
    private IOIO ioio = null; // If we have one connected, here 'tis.
    // TODO We should have some state that lets us identify which IOIO we mean
    // which should be configurable through the UI.

    public interface OnReady {
        void onReady();
        void onUnready();
    }
    private Vector<OnReady> subscribers = new Vector<>(5);

    @Override
    public IOIOLooper createIOIOLooper(final String type, Object info) {
        if (type.equals("ioio.lib.android.bluetooth.BluetoothIOIOConnection")) {
            Object[] info_ = (Object[])info;
            Log.d("TDIS", "Looper creating for " + info_[0].toString() + ":" + info_[1].toString());
        }

        // TODO Check to see if it is the right IOIO.

        return new BaseIOIOLooper() {
            @Override
            protected void setup() throws ConnectionLostException,
                    InterruptedException {
                synchronized(IOIOService.this) {
                    if(ioio != null) {
                        Log.e("TDIS", "Aaaaaaa! Setup with existing IOIO?", new Exception());
                    }
                    ioio = ioio_;
                    for (OnReady l : subscribers) { l.onReady(); }
                }
            }

            @Override
            public void loop() throws ConnectionLostException,
                    InterruptedException {
                ioio.waitForDisconnect();
            }

            @Override
            public void disconnected() {
                synchronized(IOIOService.this) {
                    for (OnReady l : subscribers) { l.onUnready(); }
                    IOIOService.this.ioio = null;
                }
            }
        };
    }

    final class LocalBinder extends Binder {
        // XXX for development only
        // IOIOService getService() { return IOIOService.this; }

        // Subscribe or unsubscribe to the IOIO being connected;
        // when we are ready, the behaviors offered below will work; when
        // not, they won't.  On transition from ready to unready, all the
        // threads will die of their own accord as their closed-over
        // objects which reach into the IOIO issue exceptions.
        public void addOnReady(OnReady l) {
            subscribers.add(l);
        }
        public void removeOnReady(OnReady l) {
            subscribers.remove(l);
        }

        @Nullable
        public Thread makeBehaviorThread(IOIOBehaviors.Factory bf) {
            IOIO ioio_;
            // XXX? synchronized(IOIOService.this) {
                ioio_ = ioio;
            // }
            if(ioio_ == null) {
                return null;
            }
            final IOIOBehaviors.Factory.Behavior b;
            final Vector<Closeable> cc = new Vector<>(5);
            try {
                b = bf.create(ioio_, cc);
            } catch (Exception e) {
                for(Closeable c : cc) { c.close(); }
                return null;
            }
            return new Thread() {
                public void run() {
                    try {
                        b.run();
                    } catch (Exception e) {
                        // ignored; the thread is allowed to just die;
                        // anyone curious can use .isAlive() or .join().
                        // They're also welcome to .interrupt()
                    } finally {
                        b.shutdown();
                        for(Closeable c : cc) { c.close(); }
                    }
                }
            };
        }

    }
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d("TDIS", "onBind");
        Notification.Builder nb = new Notification.Builder(this);
        Notification n = nb.setSmallIcon(R.drawable.ic_menu_send)
                .setContentTitle("TeleD IOIO service running")
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .build();
        startForeground(NOTIFY_ID, n);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent arg0) {
        stopForeground(true);
        stopSelf();
        return false;
    }
}