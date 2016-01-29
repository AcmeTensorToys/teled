// Based on IOIOService example from ytai's repository

package com.acmetensortoys.android.teled;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.util.Log;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

/**
 * An example IOIO service. While this service is alive, it will attempt to
 * connect to a IOIO and blink the LED. A notification will appear on the
 * notification bar, enabling the user to stop the service.
 */
public class TeleDIOIOService extends IOIOService {
    private static final int NOTIFY_ID = 0;


    /*
    public abstract class Command {
        abstract public void behave()
                throws ConnectionLostException, InterruptedException;
    };


    public class BlinkOnce extends Command {
        private int duration;
        public BlinkOnce(int dur) { duration = dur; }
        public void behave()
                throws ConnectionLostException, InterruptedException {
            led_.write(true);
            Thread.sleep(duration);
            led_.write(false);
        }
    }

    private LinkedBlockingQueue<Command> ioioq;
    */

    private boolean forceFeedbackBehavior = false;
    private synchronized void awaitAnyBehavior() {
        while(!forceFeedbackBehavior) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                // ignored
            }
        }
    }

    public void setForceFeedbackBehavior(boolean nv) {
        Log.d("TDIS", "sffb");
        if(forceFeedbackBehavior && !nv) {
            synchronized (this) {
                forceFeedbackBehavior = false;
            }
        } else if (!forceFeedbackBehavior && nv) {
            synchronized(this) {
                forceFeedbackBehavior = true;
                this.notifyAll();
            }
        }
    }

    @Override
    public IOIOLooper createIOIOLooper(String type, Object info) {
        return new BaseIOIOLooper() {
            private DigitalOutput boardLED;

            private AnalogInput forceSensor1;
            private long forceSensor1dix;

            // XXX Warmer controls?

            // XXX This is not how it will be in the final version
            private DigitalOutput motorEnable;
            private PwmOutput motorPwm;

            @Override
            protected void setup() throws ConnectionLostException,
                    InterruptedException {
                Log.d("TDIS", "Looper setup");

                boardLED = ioio_.openDigitalOutput(IOIO.LED_PIN, false);

                forceSensor1 = ioio_.openAnalogInput(31);
                forceSensor1.setBuffer(50);

                motorEnable = ioio_.openDigitalOutput(1,false); // active low
                motorPwm = ioio_.openPwmOutput(2,100);

                forceSensor1dix = 0;

                Log.d("TDIS", "Looper setup finish");
            }

            @Override
            public void loop() throws ConnectionLostException,
                    InterruptedException {

                awaitAnyBehavior();
                if (forceFeedbackBehavior) {
                    boolean did = forceSensor1.available() > 0;
                    float v = 0.0f;
                    while (forceSensor1.available() > 0) {
                        v = forceSensor1.getVoltageBuffered();
                        forceSensor1dix++;
                    }
                    if (did) {
                        motorPwm.setDutyCycle(v / 3.3f);
                    }
                }
            }

            @Override
            public void disconnected() { }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("TDIS", "onStartCommand");

        int result = super.onStartCommand(intent, flags, startId);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent != null && intent.getAction() != null
                && intent.getAction().equals("stop")) {
            // User clicked the notification. Need to stop the service.
            Log.d("TDIS", "stopping...");

            setForceFeedbackBehavior(false);

            // nm.cancel(0);
            // stopSelf();
        } else {
            Notification.Builder nb = new Notification.Builder(this);

            nm.notify(NOTIFY_ID,
                    nb.setContentIntent(PendingIntent.getService(this, 0, new Intent(
                            "stop", null, this, this.getClass()), 0))
                            .setSmallIcon(R.drawable.ic_menu_send)
                            .setContentTitle("TeleD IOIO service running")
                            .setWhen(System.currentTimeMillis())
                            .setOngoing(true)
                            .build());
        }
        return result;
    }

    protected final class LocalBinder extends Binder {
        TeleDIOIOService getService() { return TeleDIOIOService.this; }
    }
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d("TDIS", "onBind");
        return mBinder;
    }




    /*
    public void doNotify() {
        try {
            //ioioq.put(new BlinkOnce(500));
        } catch (InterruptedException e) {
        }
    }
    */
}