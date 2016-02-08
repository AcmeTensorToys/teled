package com.acmetensortoys.android.teled;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.Bundle;
import android.renderscript.RSInvalidStateException;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        Intent i = new Intent(this, IOIOService.class);
        startService(i);                                    // start the IOIO service
        bindService(i, tdisSC, Context.BIND_AUTO_CREATE);   // and get a handle to it, eventually
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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

    private Thread blinker;
    private void makeBlinker() {
        blinker = tdis.makeBehaviorThread(IOIOBehaviors.makeBlinkLEDMany(500));
        if(blinker != null) {
            blinker.start();
        } else {
            Log.d("Main", "onReady unable to start blinker");
        }
    }

    private IOIOService.LocalBinder tdis = null;
    private IOIOService.OnReady tdisOnReady = new IOIOService.OnReady() {
        public void onReady() {
            Log.d("Main", "TDIS onReady");
            if(blinker != null) {
                throw new RSInvalidStateException("onReady with blinker");
            }
            makeBlinker();
        }
        public void onUnready() {
            Log.d("Main", "TDIS onUnready");
            blinker = null;
        }
    };
    private final ServiceConnection tdisSC = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName cn, IBinder service) {
            Log.d("Main", "TDIS conn");
            tdis = ((IOIOService.LocalBinder) service);
            tdis.addOnReady(tdisOnReady);
        }
        @Override
        public void onServiceDisconnected(ComponentName cn) {
            // Because we're binding to a local service, this should never happen to us
            Log.d("Main", "TDIS discon");
            tdis = null;
            tdisOnReady.onUnready();
        }
    };

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {
            if(tdis != null) {
                Log.d("Main", "Share w/ non-null...");
                if(blinker == null) {
                    makeBlinker();
                } else {
                    blinker.interrupt();
                    blinker = null;
                }
            }
        } else if (id == R.id.nav_send) {
            Log.d("Main", "Send...");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDestroy(){
        unbindService(tdisSC);
        super.onDestroy();
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
