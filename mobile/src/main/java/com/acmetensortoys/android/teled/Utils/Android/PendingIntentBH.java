package com.acmetensortoys.android.teled.Utils.Android;

import android.app.PendingIntent;

import com.acmetensortoys.android.teled.Utils.Subscribee;
import com.acmetensortoys.android.teled.Utils.SubscribeeImpl;
import com.acmetensortoys.android.teled.Utils.SuspendableBH;

import fj.Void;
import fj.function.Effect1;

public abstract class PendingIntentBH extends SuspendableBH<Void> {

    // or restart; true to start, false to indicate a desire to stay suspended
    abstract protected boolean onStart(PendingIntent pi);

    // or stop; must be idempotent
    abstract protected void onPause(PendingIntent pi);

    public PendingIntentBH(PendingIntent pi) {
        ln = new SubscribeeImpl<>(BehaviorState.NEW);
        this.pi = pi;
    }

    //
    // You can stop reading here if all you want to know is what to extend.
    //

    final private SubscribeeImpl<BehaviorState> ln;
    final private PendingIntent pi;

    private void die() {
        synchronized(this) {
            ln.publish(BehaviorState.DEAD);
            this.notifyAll();
        }
    }

    @Override
    public final void start() {
        BehaviorState bs = ln.getLast();
        if(bs == BehaviorState.NEW || bs == BehaviorState.SUSPENDED) {
            if(this.onStart(pi)) {
                ln.publish(BehaviorState.RUNNING);
            } else if (bs == BehaviorState.NEW) {
                ln.publish(BehaviorState.SUSPENDED);
            }
        }
    }

    @Override
    public void suspend() {
        BehaviorState bs = ln.getLast();
        switch(ln.getLast()) {
            case RUNNING:
                this.onPause(pi);
                // fallthru
            case NEW:
                ln.publish(BehaviorState.SUSPENDED);
                break;
            case SUSPENDED:
            case DEAD:
                break;
        }
    }

    @Override
    public final void interrupt() {
        switch(ln.getLast()) {
            case NEW:
            case RUNNING:
                this.onPause(pi);
            case SUSPENDED:
                die();
                break;
            case DEAD:
                break;
        }
        pi.cancel();
    }

    @Override
    public final void join() throws InterruptedException {
        synchronized (this) {
            while (ln.getLast() != BehaviorState.DEAD) {
                this.wait();
            }
        }
    }

    @Override
    public final Subscribee<BehaviorState> getStateSubscribee() {
        return ln;
    }

    @Override
    public final Effect1<Void> getUpdateIn() {
        return null;
    }
}
