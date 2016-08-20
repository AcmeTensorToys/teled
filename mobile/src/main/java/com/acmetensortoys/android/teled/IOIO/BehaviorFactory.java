package com.acmetensortoys.android.teled.IOIO;

import java.util.Collection;

import ioio.lib.api.Closeable;
import ioio.lib.api.IOIO;

public interface BehaviorFactory<In> {
    abstract class Behavior<In> {
        @SuppressWarnings("EmptyMethod")
        public void shutdown() { }
        abstract public void updateIn(In i);
        abstract public void run() throws Exception;
    }

    /*
    The provided set of Closeable things is initially empty but can be
    added to to simplify resource management inside the create() method and
    in the returned Behavior: if create() raises an exception, the contents
    of cc will be iterated and closed.  When it comes time to shut down the
    behavior, again, cc will be iterated and closed.  Hooray mutation, I
    guess; the cause of, and solution to, all of mutations' problems.
    */
    Behavior<In> create(IOIO i, Collection<Closeable> cc) throws Exception;
}