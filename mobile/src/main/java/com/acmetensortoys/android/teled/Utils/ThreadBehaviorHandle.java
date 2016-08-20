package com.acmetensortoys.android.teled.Utils;

import android.util.Log;

import fj.Void;
import fj.function.Effect1;

public class ThreadBehaviorHandle<In> extends BehaviorHandle<In> {
    private final Thread thread;
    private final Effect1<In> updateIn;
    /* Actual liveness transitions are expected to be managed by the thread t */
    final private Subscribee<BehaviorState> ln;

    public ThreadBehaviorHandle(Thread t, Effect1<In> u, Subscribee<BehaviorState> ln) {
        this.thread = t;
        this.updateIn = u;
        this.ln = ln;
    }

    /* Given a runnable and an updater, make a behavior handle */
    public static <In> ThreadBehaviorHandle<In> create(final Runnable r, Effect1<In> u) {
        final SubscribeeImpl<BehaviorHandle.BehaviorState> lni
                = new SubscribeeImpl<>(BehaviorHandle.BehaviorState.NEW);
        Thread t = new Thread() {
            public void run() {
                lni.publish(BehaviorState.RUNNING);
                r.run();
                lni.publish(BehaviorState.DEAD);
            }
        };
        return new ThreadBehaviorHandle<In>(t, u, lni);
    }

    /* Sometimes we don't even need an updater */
    public static ThreadBehaviorHandle<Void> create(final Runnable r) {
        return create(r, new Effect1<Void>() { public void f(Void v) { v.absurd(); } });
    }

    @Override
    public void join() throws InterruptedException { thread.join(); }
    @Override
    public void interrupt() { thread.interrupt(); }
    @Override
    public void start() { thread.start(); }

    @Override
    public Effect1<In> getUpdateIn() { return updateIn; }
    @Override
    public Subscribee<BehaviorState> getStateSubscribee() { return ln; }
}