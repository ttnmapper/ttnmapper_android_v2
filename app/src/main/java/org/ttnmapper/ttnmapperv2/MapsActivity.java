package org.ttnmapper.ttnmapperv2;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, CompoundButton.OnCheckedChangeListener {

    private final String TAG = "MapsActivity";
    //service handles
    TTNMapperService mService;
    boolean mBound = false;
    BitmapDescriptor circleBlack = null;
    BitmapDescriptor circleBlue = null;
    BitmapDescriptor circleCyan = null;
    BitmapDescriptor circleGreen = null;
    BitmapDescriptor circleYellow = null;
    BitmapDescriptor circleOrange = null;
    BitmapDescriptor circleRed = null;
    Bitmap bmBlack;
    Bitmap bmBlue;
    Bitmap bmCyan;
    Bitmap bmGreen;
    Bitmap bmYellow;
    Bitmap bmOrange;
    Bitmap bmRed;
    ArrayList<String> gatewaysWithMarkers = new ArrayList<>();
    private GoogleMap mMap;
    private boolean startUpComplete = false;
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

            //disable toggle button until service is bound
            ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
            toggleButton.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d(TAG, "onServiceDisconnected");
            mBound = false;

            //disable toggle button until service is bound
            ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
            toggleButton.setEnabled(true);
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
                    addLastMeasurementToMap();
                    break;
                case "locationupdate":
                    //TODO: center and zoom map
                    break;
                case "selfstop":
                    Log.d(TAG, "Received selfstop from service.");
                    stopLoggingService();
                    if (payloadData == null) {
                        setStatusMessage("Logging stopped unexpectedly");
                    } else {
                        setStatusMessage("Logging stopped - " + payloadData);
                    }
                    ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
                    toggleButton.setChecked(false);
                    break;
                case "notification":
                    setStatusMessage(payloadData);
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

        // hide top bar
        getSupportActionBar().hide();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        //logging button
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
        //first change the button state, then add the listener
        if (isMyServiceRunning(TTNMapperService.class)) {
            toggleButton.setChecked(true);
            Intent startServiceIntent = new Intent(this, TTNMapperService.class);
            bindService(startServiceIntent, mConnection, Context.BIND_AUTO_CREATE);

        }

        //second the listener
        toggleButton.setOnCheckedChangeListener(this);

        //toggle button states
        setFABcolors();

        MyApplication mApplication = (MyApplication)getApplicationContext();

        /*
         * First check if google play services are available for location and maps
         * Then check if a device has been configured
         * Lastly check if we have permission to do what we want. - maybe we need to do this earlier for location, but the location onChange will just never fire.
         */
        if (!isPlayServicesAvailable())
        {
            setStatusMessage("Google Play services needed.");
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
            builder1.setMessage("This app requires Google Play Services. Please update or install Google Play services.");
            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "Install",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("market://details?id=com.google.android.gms"));
                            startActivity(intent);
                            finish();
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
        } else if (!mApplication.isConfigured()) {
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
        MyApplication mApplication = (MyApplication)getApplicationContext();
        if(!mApplication.isConfigured()){
            setStatusMessage("You have to link a device before mapping coverage.");
        }
        else
        {
            setStatusMessage("Ready to start logging.");
        }

        if (isMyServiceRunning(TTNMapperService.class)) {
            //restartLogging();
            setStatusMessage("Logging in progress.");
        }

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("ttn-mapper-service-event"));

        startUpComplete = true;

        //TODO: refresh view with new data from application class
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        startUpComplete = false;
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        // Unbind from the service
        if (mBound) {
            Log.d(TAG, "Unbinding");
            unbindService(mConnection);
            mBound = false;
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        // Unbind from the service
        if (mBound) {
            Log.d(TAG, "Unbinding");
            unbindService(mConnection);
            mBound = false;
        }
    }

    //toggle button to start/stop logging
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.toggleButtonStartLogging) {
            if (isChecked) {
                MyApplication mApplication = (MyApplication) getApplicationContext();
                if (mApplication.isConfigured()) {
                    Log.d(TAG, "Starting logging");
                    setStatusMessage("Logging started.");

                    //only start the service if we are not already bound to it
                    if (!mBound && !isMyServiceRunning(TTNMapperService.class)) {
                        startLoggingService();
                    } else if (isMyServiceRunning(TTNMapperService.class)) {
                        Log.d(TAG, "Trying to start a service that is already running.");
                    } else {
                        Log.d(TAG, "Trying to start a service that is already bound.");
                    }
                } else {
                    setStatusMessage("You need to link a device before you can start logging!");
                    Toast.makeText(this, "You need to link a device before you can start logging!", Toast.LENGTH_SHORT).show();
                    buttonView.setChecked(false);
                }
            } else {
                Log.d(TAG, "Stopping logging");
                stopLoggingService();
                setStatusMessage("Logging stopped.");
            }
        }
    }

    public void setFABcolors() {
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);

        com.github.clans.fab.FloatingActionButton floatingActionButton = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabItemScreenOn);
        if (myPrefs.getBoolean(SettingConstants.KEEP_SCREEN_ON, SettingConstants.KEEP_SCREEN_ON_DEFAULT)) {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);

            //Set on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);

            //Set off
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }


        floatingActionButton = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabItemAutoCenter);
        if (myPrefs.getBoolean(SettingConstants.AUTO_CENTER, SettingConstants.AUTO_CENTER_DEFAULT)) {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);
            //Can be applied in runtime
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);
            //Can be applied in runtime
        }


        floatingActionButton = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabItemAutoZoom);
        if (myPrefs.getBoolean(SettingConstants.AUTO_ZOOM, SettingConstants.AUTO_ZOOM_DEFAULT)) {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);
            //Initial zoom in onMapReady
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);
            //Just do not zoom anymore. Runtime.
        }


        floatingActionButton = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabItemLordrive);
        if (myPrefs.getBoolean(SettingConstants.LORDRIVE, SettingConstants.LORDRIVE_DEFAULT)) {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);
            //Will be done at first packet received
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);
            //Will be done in runtime
        }


        floatingActionButton = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fabItemCoverage);
        if (myPrefs.getBoolean(SettingConstants.COVERAGE, SettingConstants.COVERAGE_DEFAULT)) {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);

            //rile layer will be added in onMapReady
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);

            //Will be done in runtime

        }
    }

    public void onToggleScreen(View v) {
        com.github.clans.fab.FloatingActionButton floatingActionButton = (com.github.clans.fab.FloatingActionButton) v;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        boolean previousState = myPrefs.getBoolean(SettingConstants.KEEP_SCREEN_ON, SettingConstants.KEEP_SCREEN_ON_DEFAULT);

        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(SettingConstants.KEEP_SCREEN_ON, !previousState);
        prefsEditor.apply();

        if (previousState) {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);

            //It was on, now off
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);

            //It was off, now on
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    public void onToggleAutoCenter(View v) {
        com.github.clans.fab.FloatingActionButton floatingActionButton = (com.github.clans.fab.FloatingActionButton) v;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        boolean previousState = myPrefs.getBoolean(SettingConstants.AUTO_CENTER, SettingConstants.AUTO_CENTER_DEFAULT);

        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(SettingConstants.AUTO_CENTER, !previousState);
        prefsEditor.apply();

        if (previousState) {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);

            //It's off now, do nothing
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);

            //TODO: center map now
        }
    }

    public void onToggleAutoZoom(View v) {
        com.github.clans.fab.FloatingActionButton floatingActionButton = (com.github.clans.fab.FloatingActionButton) v;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        boolean previousState = myPrefs.getBoolean(SettingConstants.AUTO_ZOOM, SettingConstants.AUTO_ZOOM_DEFAULT);

        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(SettingConstants.AUTO_ZOOM, !previousState);
        prefsEditor.apply();

        if (previousState) {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);

            //It's off now, do nothing
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);

            //TODO: autozoom now
        }
    }

    public void onToggleLordrive(View v) {
        com.github.clans.fab.FloatingActionButton floatingActionButton = (com.github.clans.fab.FloatingActionButton) v;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        boolean previousState = myPrefs.getBoolean(SettingConstants.LORDRIVE, SettingConstants.LORDRIVE_DEFAULT);

        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(SettingConstants.LORDRIVE, !previousState);
        prefsEditor.apply();

        if (previousState) {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);

            clearAndReaddAllToMap();
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);

            clearAndReaddAllToMap();
        }
    }

    public void onToggleCoverage(View v) {
        com.github.clans.fab.FloatingActionButton floatingActionButton = (com.github.clans.fab.FloatingActionButton) v;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        boolean previousState = myPrefs.getBoolean(SettingConstants.COVERAGE, SettingConstants.COVERAGE_DEFAULT);

        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(SettingConstants.COVERAGE, !previousState);
        prefsEditor.apply();

        if (previousState) {
            floatingActionButton.setColorNormalResId(R.color.fab_red_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_red_light);

            clearAndReaddAllToMap();
        } else {
            floatingActionButton.setColorNormalResId(R.color.fab_green_dark);
            floatingActionButton.setColorPressedResId(R.color.fab_green_light);

            CoverageTileProvider mTileProvider = new CoverageTileProvider(256, 256, getString(R.string.ttnmapper_tms_url));
            TileOverlayOptions options = new TileOverlayOptions();
            options.tileProvider(mTileProvider);
            options.transparency((float) 0.8);
            mMap.addTileOverlay(options);
        }
    }

    public void onSettingsClicked(View v) {
        Intent intentSettings = new Intent(this, SettingsActivity.class);
        startActivity(intentSettings);
    }

    public void restartLogging() {
        Log.d(TAG, "Restarting logging");

        stopLoggingService();

        MyApplication mApplication = (MyApplication) getApplicationContext();
        if (mApplication.isConfigured()) {
            Log.d(TAG, "Starting logging");
            startLoggingService();
            setStatusMessage("Logging restarted.");
        } else {
            setStatusMessage("You need to link a device before you can start logging!");
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
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);

        if (myPrefs.getBoolean(SettingConstants.COVERAGE, SettingConstants.COVERAGE_DEFAULT)) {
            CoverageTileProvider mTileProvider = new CoverageTileProvider(256, 256, getString(R.string.ttnmapper_tms_url));
            TileOverlayOptions options = new TileOverlayOptions();
            options.tileProvider(mTileProvider);
            options.transparency((float) 0.8);

            mMap.addTileOverlay(options);
        }

        if (myPrefs.getBoolean(SettingConstants.AUTO_ZOOM, SettingConstants.AUTO_ZOOM_DEFAULT)) {
            //TODO: We should save the previous viewport location and zoom to that on startup
        }
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
        //check for permissions
        MyApplication mApplication = (MyApplication) getApplicationContext();
        if (!mApplication.checkPermissions()) {
            ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
            toggleButton.setChecked(false);

            AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
            builder1.setMessage("To obtain a GPS location and to log packets to a file, we need to have special permissions. Click below to configure the permissions now.");
            builder1.setCancelable(true);

            builder1.setPositiveButton(
                    "Configure",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            Intent intent = new Intent(getApplicationContext(), CheckPermissions.class);
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
        } else {
            //disable toggle button until service is bound
            ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonStartLogging);
            toggleButton.setEnabled(false);

            //begin service
            Intent startServiceIntent = new Intent(this, TTNMapperService.class);
            startService(startServiceIntent);
            bindService(startServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        }
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

    public boolean isPlayServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        return code == ConnectionResult.SUCCESS;
    }

    public void clearAndReaddAllToMap() {
        MyApplication mApplication = (MyApplication) getApplicationContext();
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);

        mMap.clear();
        gatewaysWithMarkers.clear();

        for (Packet packet : mApplication.packets) {
            addMeasurementMarker(packet);

            if (myPrefs.getBoolean(SettingConstants.LORDRIVE, SettingConstants.LORDRIVE_DEFAULT)) {
                addMeasurementLine(packet);
                addGateway(packet);
            }
        }

        if (myPrefs.getBoolean(SettingConstants.COVERAGE, SettingConstants.COVERAGE_DEFAULT)) {
            CoverageTileProvider mTileProvider = new CoverageTileProvider(256, 256, getString(R.string.ttnmapper_tms_url));
            TileOverlayOptions options = new TileOverlayOptions();
            options.tileProvider(mTileProvider);
            options.transparency((float) 0.8);
            mMap.addTileOverlay(options);
        }
    }

    public void addLastMeasurementToMap() {
        if (mMap == null) return;

        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        MyApplication mApplication = (MyApplication) getApplicationContext();
        if (mApplication.lastPacket == null) return;

        Packet packet = mApplication.lastPacket;
        addMeasurementMarker(packet);

        if (myPrefs.getBoolean(SettingConstants.LORDRIVE, SettingConstants.LORDRIVE_DEFAULT)) {
            addMeasurementLine(packet);
            addGateway(packet);
        }
    }

    public void addMeasurementMarker(Packet packet) {
        createMarkerBitmaps(); //create markers if they do not exist

        MarkerOptions options = new MarkerOptions();
        double rssi = packet.getMaxRssi();
        if (rssi == 0) {
            options.icon(circleBlack);
        } else if (rssi < -120) {
            options.icon(circleBlue);
        } else if (rssi < -115) {
            options.icon(circleCyan);
        } else if (rssi < -110) {
            options.icon(circleGreen);
        } else if (rssi < -105) {
            options.icon(circleYellow);
        } else if (rssi < -100) {
            options.icon(circleOrange);
        } else {
            options.icon(circleRed);
        }
        options.position(new LatLng(packet.getLatitude(), packet.getLongitude()));
        options.anchor((float) 0.5, (float) 0.5);

        mMap.addMarker(options);
    }

    public void addMeasurementLine(Packet packet) {
        for (Gateway gateway : packet.getGateways()) {
            double gwLat = gateway.getLatitude();
            double gwLon = gateway.getLongitude();
            double rssi = gateway.getRssi();
            if (gwLat != 0 && gwLon != 0) {
                PolylineOptions options = new PolylineOptions();
                options.add(new LatLng(packet.getLatitude(), packet.getLongitude()));
                options.add(new LatLng(gwLat, gwLon));
                if (rssi == 0) {
                    options.color(0x7f000000);
                } else if (rssi < -120) {
                    options.color(0x7f0000ff);
                } else if (rssi < -115) {
                    options.color(0x7f00ffff);
                } else if (rssi < -110) {
                    options.color(0x7f00ff00);
                } else if (rssi < -105) {
                    options.color(0x7fffff00);
                } else if (rssi < -100) {
                    options.color(0x7fff7f00);
                } else {
                    options.color(0x7fff0000);
                }
                options.width(2);
                mMap.addPolyline(options);
            }
        }
    }

    public void addGateway(Packet packet) {
        for (Gateway gateway : packet.getGateways()) {
            double gwLat = gateway.getLatitude();
            double gwLon = gateway.getLongitude();

            if (gwLat != 0 && gwLon != 0) {
                String gatewayId = gateway.getGatewayID();

                if (gatewaysWithMarkers.contains(gatewayId)) {
                    //already has a marker for this gateway
                } else {
                    MarkerOptions gwoptions = new MarkerOptions();
                    gwoptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.gateway_dot));
                    gwoptions.position(new LatLng(gwLat, gwLon));
                    gwoptions.title(gatewayId);
                    gwoptions.snippet(gatewayId);
                    gwoptions.anchor((float) 0.5, (float) 0.5);
                    mMap.addMarker(gwoptions);

                    gatewaysWithMarkers.add(gatewayId);
                }
            }
        }
    }

    public void setStatusMessage(String message) {
        TextView tv = (TextView) findViewById(R.id.textViewStatus);
        tv.setText(message);
    }

    void createMarkerBitmaps() {
        int d = 30; // diameter
        if (circleBlack == null) {
            bmBlack = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmBlack);
            Paint p = new Paint();
            p.setColor(0x7f000000);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleBlack = BitmapDescriptorFactory.fromBitmap(bmBlack);
        }
        if (circleBlue == null) {
            bmBlue = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmBlue);
            Paint p = new Paint();
            p.setColor(0x7f0000ff);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleBlue = BitmapDescriptorFactory.fromBitmap(bmBlue);
        }
        if (circleCyan == null) {
            bmCyan = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmCyan);
            Paint p = new Paint();
            p.setColor(0x7f00ffff);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleCyan = BitmapDescriptorFactory.fromBitmap(bmCyan);
        }
        if (circleGreen == null) {
            bmGreen = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmGreen);
            Paint p = new Paint();
            p.setColor(0x7f00ff00);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleGreen = BitmapDescriptorFactory.fromBitmap(bmGreen);
        }
        if (circleYellow == null) {
            bmYellow = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmYellow);
            Paint p = new Paint();
            p.setColor(0x7fffff00);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleYellow = BitmapDescriptorFactory.fromBitmap(bmYellow);
        }
        if (circleOrange == null) {
            bmOrange = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmOrange);
            Paint p = new Paint();
            p.setColor(0x7fff7f00);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleOrange = BitmapDescriptorFactory.fromBitmap(bmOrange);
        }
        if (circleRed == null) {
            bmRed = Bitmap.createBitmap(d, d, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmRed);
            Paint p = new Paint();
            p.setColor(0x7fff0000);
            c.drawCircle(d / 2, d / 2, d / 2, p);
            circleRed = BitmapDescriptorFactory.fromBitmap(bmRed);
        }
    }
}
