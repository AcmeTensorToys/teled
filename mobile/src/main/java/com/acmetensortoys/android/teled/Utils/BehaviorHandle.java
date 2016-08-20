package com.acmetensortoys.android.teled.Utils;

import fj.function.Effect1;

public abstract class BehaviorHandle<In> {
    public enum BehaviorState { NEW, RUNNING, SUSPENDED, DEAD };

    abstract public void start(); // move from NEW to RUNNING
    abstract public void interrupt(); // request RUNNING to DEAD transition
    abstract public void join() throws InterruptedException; // await status DEAD
    abstract public Subscribee<BehaviorState> getStateSubscribee(); // to be notified on any state transition

    abstract public Effect1<In> getUpdateIn();
}