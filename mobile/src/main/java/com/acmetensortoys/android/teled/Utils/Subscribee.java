package com.acmetensortoys.android.teled.Utils;

import fj.function.Effect1;

public interface Subscribee<V> {
    V getLast();
    void subscribe(Effect1<V> cb);
    void unsubscribe(Effect1<V> cb);
}