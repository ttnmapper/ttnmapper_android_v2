package org.ttnmapper.ttnmapperv2;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, CompoundButton.OnCheckedChangeListener {

    private final String TAG = "MapsActivity";
    //service handles
    TTNMapperService mService;
    boolean mBound = false;
    private GoogleMap mMap;
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(TAG, "onServiceConected");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            TTNMapperService.LocalBinder binder = (TTNMapperService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("message");
            String payloadData = intent.getStringExtra("payload");
            String topic = intent.getStringExtra("topic");
            Log.d(TAG, "Got message: " + message);

            switch (message) {
                case "rxmessage":
                    //TODO: refresh view with new data from application class
                    Log.d(TAG, "Will refresh the map now");
                    break;
                case "selfstop":
                    Log.d(TAG, "Received selfstop from service");
                    stopLoggingService();
                    if (payloadData == null) {
                        Toast.makeText(getApplicationContext(), "Logging stopped unexpectedly", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Logging stopped - " + payloadData, Toast.LENGTH_LONG).show();
                    }
                    ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
                    toggleButton.setChecked(false);
                    break;
                case "notification":
                    Toast.makeText(getApplicationContext(), payloadData, Toast.LENGTH_LONG).show();
                    break;
                case "test":
                    Log.d(TAG, "Test message received");
                    break;
                default:
                    Log.d(TAG, "Unknown message received");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
        toggleButton.setOnCheckedChangeListener(this);
        if (isMyServiceRunning(TTNMapperService.class)) {
            toggleButton.setChecked(true);
            Intent startServiceIntent = new Intent(this, TTNMapperService.class);
            bindService(startServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }

        MyApplication mApplication = (MyApplication)getApplicationContext();
        if(!mApplication.isConfigured())
        {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
            builder1.setMessage("To map coverage you need to link a device to this app. Click on Link device to log into your The Things Network account and choose a device.\n\nThis app subscribes to the linked device to receive packets from it. When a packet is received it is assumed that the linked device is relatively close to this phone. The phone's GPS location and metadata of the packet is used to draw a coverage map.\n\nYou can link a device at a later stage, or change the linked device from the options menu.");
            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "Link device",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Intent intent = new Intent(getApplicationContext(), LinkDevice.class);
                            startActivity(intent);
                        }
                    });

            builder1.setNegativeButton(
                    "Not now",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        TextView tv = (TextView) findViewById(R.id.textViewStatus);
        MyApplication mApplication = (MyApplication)getApplicationContext();
        if(!mApplication.isConfigured()){
            tv.setText("You have to link a device before mapping coverage.");
        }
        else
        {
            tv.setText("Ready to start logging.");
        }

        if (isMyServiceRunning(TTNMapperService.class)) {
            restartLogging();
        }

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("ttn-mapper-service-event"));

        //TODO: refresh view with new data from application class
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.device:
                Intent intent = new Intent(this, LinkDevice.class);
                startActivity(intent);
                return true;
            case R.id.settings:
//                Intent intent = new Intent(this, about.class);
//                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //toggle button to start/stop logging
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            MyApplication mApplication = (MyApplication) getApplicationContext();
            if (mApplication.isConfigured()) {
                Log.d(TAG, "Starting logging");
                startLoggingService();
            } else {
                Toast.makeText(this, "You need to link a device before you can start logging!", Toast.LENGTH_LONG).show();
                buttonView.setChecked(false);
            }
        } else {
            Log.d(TAG, "Stopping logging");
            stopLoggingService();
        }

    }

    public void restartLogging() {
        Log.d(TAG, "Restarting logging");

        stopLoggingService();

        MyApplication mApplication = (MyApplication) getApplicationContext();
        if (mApplication.isConfigured()) {
            Log.d(TAG, "Starting logging");
            startLoggingService();
            Toast.makeText(this, "Logging restarted", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "You need to link a device before you can start logging!", Toast.LENGTH_LONG).show();
            ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
            toggleButton.setChecked(false);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startLoggingService() {
        //begin service
        Intent startServiceIntent = new Intent(this, TTNMapperService.class);
        startService(startServiceIntent);
        bindService(startServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void stopLoggingService() {
        //unbind and stop service
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        Intent startServiceIntent = new Intent(this, TTNMapperService.class);
        stopService(startServiceIntent);
    }
}
