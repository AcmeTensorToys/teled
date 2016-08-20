package com.acmetensortoys.android.teled.UI;

import com.acmetensortoys.android.teled.R;
import com.acmetensortoys.android.teled.Service.EphemeralTeleDService;
import com.acmetensortoys.android.teled.Service.SMSRecv;
import com.acmetensortoys.android.teled.Utils.BehaviorHandle;
import com.acmetensortoys.android.teled.IOIO.BehaviorFactories;
import com.acmetensortoys.android.teled.Service.TeleDService;
import com.acmetensortoys.android.teled.Utils.BehaviorHandleSet;
import com.acmetensortoys.android.teled.Utils.SubscribeeImpl;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.renderscript.RSInvalidStateException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

import fj.function.Effect1;

public class MainActivity extends Activity
        implements SharedPreferences.OnSharedPreferenceChangeListener
    /*
        implements NavigationView.OnNavigationItemSelectedListener
    */
{

    private final static int PERM_REQ_IX_RECV_SMS = 1;
    private final static int PERM_REQ_IX_FINE_LOC = 2;
    private final static int PERM_REQ_IX_EVERYTHING = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        setContentView(R.layout.newmain);

        getFragmentManager().beginTransaction()
                .add(R.id.main_fragment_container,new FragPrefs())
                .commit();

        /*
        setContentView(R.layout.fragment_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        */

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        */
    }

    @Override
    public void onDestroy(){
        if (tds != null) { unbindService(tdisSC); }

        PreferenceManager
                .getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        bindService(new Intent(this, TeleDService.class), tdisSC,
                Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);

        Log.d("Main", "Requesting permission?");
        ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.SEND_SMS},
                    PERM_REQ_IX_EVERYTHING);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private BehaviorHandle blinker;
    private void makeBlinker() {
        blinker = tds.addIOIOBehavior(
                new BehaviorHandleSet.Metadata("BlinkMany"),
                BehaviorFactories.makeBlinkLEDMany(500));
        if(blinker != null) {
            blinker.start();
        } else {
            Log.d("Main", "unable to start blinker");
        }
    }

    private TeleDService.LocalBinder tds = null;
    private final Effect1<Boolean> tdiOnReady = new Effect1<Boolean>() {
        public void f(Boolean b) {
            if(b) {
                Log.d("Main", "TDI onReady");
                if (blinker != null) {
                    throw new RSInvalidStateException("onReady with blinker");
                }
                makeBlinker();
            } else {
                Log.d("Main", "TDI onUnready");
                blinker.interrupt();
                try { blinker.join(); } catch (InterruptedException e) { ; }
                blinker = null;
            }
        }
    };

    private final ServiceConnection tdisSC = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName cn, IBinder service) {
            Log.d("Main", "TDIS conn");
            tds = ((TeleDService.LocalBinder) service);
            tds.getIOIOStatus().subscribe(tdiOnReady);
        }
        @Override
        public void onServiceDisconnected(ComponentName cn) {
            // Because we're binding to a local service, this should never happen to us
            Log.d("Main", "TDIS discon");
            tds = null;
            tdiOnReady.f(false);
        }
    };

    /*
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            Log.d("Main", "Camera...");

            if(tds != null) {
                Map<Integer,String> m = tds.getActiveBehaviors();
                for (Map.Entry<Integer,String> x : m.entrySet()) {
                    BehaviorHandle<?> b = tds.getActiveBehaviorById(x.getKey());
                    if(b != null) {
                        Log.d("Main", "Stopping behavior ID " + x.getKey().toString());
                        b.interrupt();
                        try { b.join(); } catch (Exception e) { break; }
                    }
                }
            }
        } else if (id == R.id.nav_gallery) {
            if(tds != null) {
                Log.d("Main", "Share w/ non-null...");
                if(blinker == null) {
                    makeBlinker();
                } else {
                    blinker.interrupt();
                    blinker = null;
                }
            }
        } else if (id == R.id.nav_slideshow) {
            if(tds != null) {
            }
        } else if (id == R.id.nav_manage) {
            if(tds != null) {
                // tds.enableIOIO(true);
                BehaviorHandle<Void> x = tds.addActiveBehavior("test",
                        ThreadBehaviorHandle.create(new Runnable() {
                            public void run() {
                                while(true) {
                                    try {
                                        Thread.sleep(5000);
                                    } catch (Exception e) {
                                        return;
                                    }
                                    Log.d("TestBehavior", "Tick");
                                }
                            }
                        }));
                x.start();
            }
        } else if (id == R.id.nav_share) {
            Log.d("Main", "Share...");

            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
            {
                Log.d("Main", "Requesting permission?");
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERM_REQ_IX_FINE_LOC);
            } else {
                Log.d("Main", "Have permission?");

                LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
                Log.d("Main", lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER).toString());
            }
        } else if (id == R.id.nav_send) {
            Log.d("Main", "Send...");

            if(checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED)
            {
                Log.d("Main", "Requesting permission?");
                requestPermissions(
                        new String[]{Manifest.permission.RECEIVE_SMS},
                        PERM_REQ_IX_RECV_SMS);
            } else {
                Log.d("Main", "Have permission?");
                if(tds != null) {
                    tds.enableSMS(true);
                }
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    */

    @Override
    public void onRequestPermissionsResult(int rq, @NonNull String[] perms, @NonNull int[] grs) {
        Log.d("Main", "ORPR:" + perms[0] + ":" + Integer.toString(grs[0]));
        //if(rq == PERM_REQ_IX_RECV_SMS && grs[0] == PackageManager.PERMISSION_GRANTED) {
        //}
    }

    // XXX Remove?
    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String k) {
        switch(k) {
            case "sms_ena": break;
            case "gps_ena": break;
            case "test":
                AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                PendingIntent pi = EphemeralTeleDService.pendingTest(this,Uri.EMPTY);
                if(PreferenceManager
                        .getDefaultSharedPreferences(this)
                        .getBoolean("test", false)) {
                    Log.d("Main", "test OSPC enabled");
                    am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            SystemClock.elapsedRealtime(), 1000 * 60, pi);
                    try { pi.send(); } catch (PendingIntent.CanceledException ce) {
                        Log.d("Main", "pi cancel?");
                    }
                } else {
                    Log.d("Main", "test OSPC disabled");
                    am.cancel(pi);
                    pi.cancel();
                }
            break;
            default:
                Log.d("MainActivity", "Unknown preference changed:" + k);
                break;
        }
    }
}

/* TODO:
 *
 *  * A settings activity with SMS configuration, including authentication
 *    information and an enable button.
 *
 *  * A settings activity with Bluetooth configuration and maybe USB?
 *
 *  * Spawn the IOIO service on startup if it isn't and see it work.
 */
