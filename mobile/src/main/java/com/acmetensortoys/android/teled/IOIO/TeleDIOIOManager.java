// Based, in long past, on IOIOService example from Ytai's repository

package com.acmetensortoys.android.teled.IOIO;

import android.support.annotation.Nullable;
import android.util.Log;

import com.acmetensortoys.android.teled.Utils.BehaviorHandle;
import com.acmetensortoys.android.teled.Utils.ThreadBehaviorHandle;
import com.acmetensortoys.android.teled.Utils.Subscribee;
import com.acmetensortoys.android.teled.Utils.SubscribeeImpl;

import java.util.ArrayList;

import fj.function.Effect1;

import ioio.lib.api.Closeable;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;

public class TeleDIOIOManager
        implements ioio.lib.util.IOIOLooperProvider {
    private IOIO ioio = null; // If we have one connected, here 'tis.

    // TODO We should have some state that lets us identify which IOIO we mean
    // which should be configurable through the UI.

    // Subscribe or unsubscribe to the IOIO being connected;
    // when we are ready, the behaviors offered below will work; when
    // not, they won't.  On transition from ready to unready, all the
    // threads will die of their own accord as their closed-over
    // objects which reach into the IOIO issue exceptions.
    private SubscribeeImpl<Boolean> ioioStatus = new SubscribeeImpl<>(false);
    public Subscribee<Boolean> getStatus() { return ioioStatus; }

    @Override
    public IOIOLooper createIOIOLooper(final String type, Object info) {
        if (type.equals("ioio.lib.android.bluetooth.BluetoothIOIOConnection")) {
            Object[] info_ = (Object[]) info;
            Log.d("TDIS", "Looper creating for " + info_[0].toString() + ":" + info_[1].toString());
        }

        // TODO Check to see if it is the right IOIO.

        return new BaseIOIOLooper() {
            @Override
            protected void setup() throws ConnectionLostException,
                    InterruptedException {
                synchronized (TeleDIOIOManager.this) {
                    if (ioio != null) {
                        throw new RuntimeException("Aaaaaaa! Setup with existing IOIO?");
                    }
                    ioio = ioio_;
                    ioioStatus.publish(true);
                }
            }

            // All action takes place in the worker threads which are spawned below.
            // So here we just park ourselves in a waiter and hope for the best.
            @Override
            public void loop() throws ConnectionLostException,
                    InterruptedException {
                ioio.waitForDisconnect();
            }

            @Override
            public void disconnected() {
                synchronized (TeleDIOIOManager.this) {
                    ioioStatus.publish(false);
                    TeleDIOIOManager.this.ioio = null;
                }
            }
        };
    }

    @Nullable
    public <In> BehaviorHandle<In> makeBehaviorThread(BehaviorFactory<In> bf) {
        IOIO ioio_;
        synchronized (TeleDIOIOManager.this) {
            ioio_ = ioio;
        }
        if (ioio_ == null) {
            return null;
        }
        final BehaviorFactory.Behavior<In> b;
        final ArrayList<Closeable> cc = new ArrayList<>(5);
        try {
            b = bf.create(ioio_, cc);
        } catch (Exception e) {
            for (Closeable c : cc) { c.close(); }
            return null;
        }
        final SubscribeeImpl<BehaviorHandle.BehaviorState> blni
                = new SubscribeeImpl<>(BehaviorHandle.BehaviorState.NEW);
        return new ThreadBehaviorHandle<>(new Thread() {
            public void run() {
                try {
                    blni.publish(BehaviorHandle.BehaviorState.RUNNING);
                    b.run();
                } catch (Exception e) {
                    // ignored; the thread is allowed to just die;
                    // anyone curious can use .isAlive() or .join().
                    // They're also welcome to .interrupt()
                    Log.e("IOIOBehavior", "Exiting from exception", e);
                } finally {
                    b.shutdown();
                    for (Closeable c : cc) { c.close(); }
                    // Publish DEAD state after all resources have been released,
                    // in case one of our subscribers wants to claim them.
                    blni.publish(BehaviorHandle.BehaviorState.DEAD);
                }
            }
        }, new Effect1<In>() {
            public void f(In x) {
                b.updateIn(x);
            }
        }, blni);
    }
}