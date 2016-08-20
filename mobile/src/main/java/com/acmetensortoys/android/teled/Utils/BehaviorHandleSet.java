package com.acmetensortoys.android.teled.Utils;

import android.graphics.drawable.Icon;

import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fj.function.Effect1;

// Manage a collection of active behavior threads; removal from the collection
// is handled by the behaviors themselves when they transition.
//
// Each behavior is also given a unique integer identifier for remote reference.
public class BehaviorHandleSet<BH extends BehaviorHandle<?>> {
    public static class Metadata {
        final public String name;
        final public Icon icon;

        public Metadata (String name) {
            this.name = name;
            this.icon = null;
        }
        public Metadata (String name, Icon icon) {
            this.name = name;
            this.icon = icon;
        }
    }

    private int nextId = 0;
    private final HashBiMap<BH,Integer> activeBehaviorIds = HashBiMap.create();
    private final HashMap<Integer,Metadata> activeBehaviorMeta = new HashMap<>();
    private final SubscribeeImpl<Map<Integer,Metadata>> abmSubee
            = new SubscribeeImpl<Map<Integer,Metadata>>(activeBehaviorMeta);

    public void
    addActiveBehavior(final Metadata meta, final BH bh)
    {
        synchronized(this) {
            while (activeBehaviorIds.containsValue(nextId)) { nextId++; }
            activeBehaviorIds.put(bh, nextId);
            activeBehaviorMeta.put(nextId, meta);
            abmSubee.publish(getActiveBehaviorMeta());
            bh.getStateSubscribee().subscribe(new Effect1<BehaviorHandle.BehaviorState>() {
                public void f(BehaviorHandle.BehaviorState x) {
                    if (x == BehaviorHandle.BehaviorState.DEAD) {
                        synchronized(this) {
                            int oldId = activeBehaviorIds.remove(bh);
                            activeBehaviorMeta.remove(oldId);
                            abmSubee.publish(getActiveBehaviorMeta());
                        }
                    }
                }
            });
        }
    }

    public BH
    getBehaviorById(int id) {
        synchronized(this) { return activeBehaviorIds.inverse().get(id); }
    }

    public Iterator<BH>
    getBehaviors() {
        return activeBehaviorIds.keySet().iterator();
    }

    // Clone the behavior meta map for consumption of the outside world.
    @SuppressWarnings("unchecked")
    public Map<Integer,Metadata>
    getActiveBehaviorMeta() {
        synchronized (this) {
            return (Map<Integer,Metadata>)(activeBehaviorMeta.clone());
        }
    }

    public Subscribee<Map<Integer,Metadata>>
    getActiveBehaviorSubee() { return abmSubee; }

    public static void showMetaMap(StringBuilder b, Map<Integer,Metadata> m) {
        for(Map.Entry<Integer,Metadata> x : m.entrySet()) {
            b.append(x.getKey());
            b.append(" ");
            b.append(x.getValue().name);
            b.append("\n");
        }
    }
}
