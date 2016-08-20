package com.acmetensortoys.android.teled.Utils;

public abstract class SuspendableBH<In> extends BehaviorHandle<In> {
    abstract public void suspend(); // request transition from RUNNING to SUSPENDED
    // use start() to resume.
}