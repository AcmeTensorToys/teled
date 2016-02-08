package com.acmetensortoys.android.teled;

import java.util.Collection;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.Closeable;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;

public class IOIOBehaviors {
    static abstract class Factory {
        abstract class Behavior {
            @SuppressWarnings("EmptyMethod")
            void shutdown() { }
            abstract void run() throws Exception;
        }
        /*
        The provided set of Closeable things is initially empty but can be
        added to to simplify resource management inside the create() method and
        in the returned Behavior: if create() raises an exception, the contents
        of cc will be iterated and closed.  When it comes time to shut down the
        behavior, again, cc will be iterated and closed.  Hooray mutation, I
        guess; the cause of, and solution to, all of mutations' problems.
        */
        abstract Behavior create(IOIO i, Collection<Closeable> cc) throws Exception;
    }

    static private Factory makeBlinkOnce(final int pinId, final int dur) {
        return new Factory() {
            public Behavior create(IOIO ioio_, Collection<Closeable> cc)
                    throws Exception {
                final DigitalOutput pin = ioio_.openDigitalOutput(pinId);
                cc.add(pin);
                return new Behavior() {
                    public void run() throws Exception {
                        pin.write(false);
                        Thread.sleep(dur);
                        pin.write(true);
                    }
                };
            }
        };
    }
    static public Factory makeBlinkLEDOnce(int dur) {
        return makeBlinkOnce(IOIO.LED_PIN, dur);
    }

    static private Factory makeBlinkMany(final int pinId, final int dur) {
        return new Factory() {
            public Behavior create(IOIO ioio_, Collection<Closeable> cc)
                    throws Exception {
                final DigitalOutput pin = ioio_.openDigitalOutput(pinId);
                cc.add(pin);
                return new Behavior() {
                    public void run() throws Exception {
                        while(true) {
                            pin.write(false);
                            Thread.sleep(dur);
                            pin.write(true);
                            Thread.sleep(dur/2);
                        }
                    }
                };
            }
        };
    }
    static public Factory makeBlinkLEDMany(int dur) {
        return makeBlinkMany(IOIO.LED_PIN, dur);
    }

    public interface ForceFeedbackCallback {
        void run(float v);
    }
    static public Factory makeForceFeedback(final ForceFeedbackCallback cb) {
        return new Factory() {
            public Behavior create(IOIO ioio_, Collection<Closeable> cc) throws Exception {
                final AnalogInput sensor = ioio_.openAnalogInput(31);
                cc.add(sensor);
                sensor.setBuffer(50);
                final DigitalOutput motorEnable = ioio_.openDigitalOutput(1, false); // active low
                cc.add(motorEnable);
                final PwmOutput motorPwm = ioio_.openPwmOutput(2, 100);
                cc.add(motorPwm);
                return new Behavior() {
                    public void run() throws Exception {
                        float setv = 0.0f;
                        while (!Thread.interrupted()) {
                            float v = sensor.getVoltageBuffered();
                            // Try to save some bandwidth by only updating if it matters.
                            if (Math.abs(v - setv) > 0.1) {
                                motorPwm.setDutyCycle(v / 3.3f);
                                setv = v;
                            }
                            // Hooray for JITs; I hope they can code-motion this test out.
                            if (cb != null) {
                                cb.run(v);
                            }
                        }
                    }
                };
            }
        };
    }
}
