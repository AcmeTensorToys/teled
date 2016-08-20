package com.acmetensortoys.android.teled.IOIO;

import java.util.Collection;

import fj.function.Effect1;
import fj.Void;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.Closeable;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;

public class BehaviorFactories {
    static private BehaviorFactory<Void> makeBlinkOnce(final int pinId, final int dur) {
        return new BehaviorFactory<Void>() {
            public Behavior<Void> create(IOIO ioio_, Collection<Closeable> cc)
                    throws Exception {
                final DigitalOutput pin = ioio_.openDigitalOutput(pinId);
                cc.add(pin);
                return new Behavior<Void>() {
                    public void run() throws Exception {
                        pin.write(false);
                        Thread.sleep(dur);
                        pin.write(true);
                    }
                    public void updateIn(Void v) { v.absurd(); }
                };
            }
        };
    }
    static public BehaviorFactory<Void> makeBlinkLEDOnce(int dur) {
        return makeBlinkOnce(IOIO.LED_PIN, dur);
    }

    static private BehaviorFactory<Void> makeBlinkMany(final int pinId, final int dur) {
        return new BehaviorFactory<Void>() {
            public Behavior<Void> create(IOIO ioio_, Collection<Closeable> cc)
                    throws Exception {
                final DigitalOutput pin = ioio_.openDigitalOutput(pinId);
                cc.add(pin);
                return new Behavior<Void>() {
                    public void run() throws Exception {
                        while(!Thread.currentThread().isInterrupted()) {
                            pin.write(false);
                            Thread.sleep(dur);
                            pin.write(true);
                            Thread.sleep(dur/2);
                        }
                    }
                    public void updateIn(Void v) { v.absurd(); }
                };
            }
        };
    }
    static public BehaviorFactory<Void> makeBlinkLEDMany(int dur) {
        return makeBlinkMany(IOIO.LED_PIN, dur);
    }

    static public BehaviorFactory<Float> makePWMListener() {
        return new BehaviorFactory<Float>() {
            public Behavior<Float> create(IOIO ioio_, Collection<Closeable> cc) throws Exception {
                final DigitalOutput motorEnable = ioio_.openDigitalOutput(1, false); // active low
                cc.add(motorEnable);
                final PwmOutput motorPwm = ioio_.openPwmOutput(2, 100);
                cc.add(motorPwm);
                return new Behavior<Float>() {
                    private float v;

                    public void run() throws Exception {
                        while(!Thread.currentThread().isInterrupted()) {
                            this.wait();
                            motorPwm.setDutyCycle(v);
                        }
                    }

                    public void updateIn(Float v) {
                        this.v = v;
                        this.notify();
                    }
                };
            }
        };
    }

    static public BehaviorFactory<Void> makeForceStreamer(final Effect1<Double> cb) {
        return new BehaviorFactory<Void>() {
            public Behavior<Void> create(IOIO ioio_, Collection<Closeable> cc) throws Exception {
                final AnalogInput sensor = ioio_.openAnalogInput(31);
                cc.add(sensor);
                sensor.setBuffer(50);
                return new Behavior<Void>() {
                    public void run() throws Exception {
                        while (!Thread.currentThread().isInterrupted()) {
                            float v = sensor.getVoltageBuffered();
                            cb.f((double)v);
                        }
                    }
                    public void updateIn(Void v) { v.absurd(); }
                };
            }
        };
    }

    static public BehaviorFactory<Void> makeForceFeedback(final Effect1<Double> cb) {
        return new BehaviorFactory<Void>() {
            public Behavior<Void> create(IOIO ioio_, Collection<Closeable> cc) throws Exception {
                final AnalogInput sensor = ioio_.openAnalogInput(31);
                cc.add(sensor);
                sensor.setBuffer(50);
                final DigitalOutput motorEnable = ioio_.openDigitalOutput(1, false); // active low
                cc.add(motorEnable);
                final PwmOutput motorPwm = ioio_.openPwmOutput(2, 100);
                cc.add(motorPwm);
                return new Behavior<Void>() {
                    public void run() throws Exception {
                        float setv = 0.0f;
                        while (!Thread.currentThread().isInterrupted()) {
                            float v = sensor.getVoltageBuffered();
                            // Try to save some bandwidth by only updating if it matters.
                            if (Math.abs(v - setv) > 0.1) {
                                motorPwm.setDutyCycle(v / 3.3f);
                                setv = v;
                            }
                            cb.f((double)v);
                        }
                    }
                    public void updateIn(Void v) { v.absurd(); }
                };
            }
        };
    }
}