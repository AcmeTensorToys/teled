package com.acmetensortoys.android.teled.Utils;

import java.util.LinkedList;
import java.util.List;
import fj.function.Effect1;

public class SubscribeeImpl<V> implements Subscribee<V> {
    private V last;
    private final List<Effect1<V>> subscribers = new LinkedList<>();

    public SubscribeeImpl(V init) { last = init; }

    public void publish(V b) {
        synchronized (this) {
            last = b;
            for (Effect1<V> l : subscribers) {
                l.f(b);
            }
        }
    }
    public V getLast() {
        return last;
    }
    public void subscribe(Effect1<V> l) {
        synchronized (this) {
            subscribers.add(l);
        }
    }
    public void unsubscribe(Effect1<V> l) {
        synchronized (this) {
            subscribers.remove(l);
        }
    }
}