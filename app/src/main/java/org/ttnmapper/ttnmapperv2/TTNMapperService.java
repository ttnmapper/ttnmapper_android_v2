package org.ttnmapper.ttnmapperv2;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jpmeijers on 30-1-17.
 */

public class TTNMapperService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static String TAG = "LoggingService";
    private final IBinder mBinder = new LocalBinder();
    private final MqttConnectOptions connOpts = new MqttConnectOptions();
    private int startId;
    private MqttClient mqttClient;
    private MqttClientPersistence persistence = new MemoryPersistence();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        mGoogleApiClient.disconnect();

        mqtt_disconnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart");

        this.startId = startId;

        Intent notificationIntent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_connected))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.service_connected));

        Notification notification;
        if (Build.VERSION.SDK_INT < 16) {
            notification = notificationBuilder.getNotification();
        } else {
            notification = notificationBuilder.build();
        }

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        //clear old coordinates
        MyApplication mApplication = (MyApplication) getApplicationContext();
        mApplication.setLatestLat(0.0);
        mApplication.setLatestLon(0.0);
        mApplication.setLatestAlt(0.0);
        mApplication.setLatestAcc(0.0);
        mApplication.setLatestProvider("");

        //start mqtt
        mqtt_connect();

        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location update received");
        MyApplication mApplication = (MyApplication) getApplicationContext();
        mApplication.setLatestLat(location.getLatitude());
        mApplication.setLatestLon(location.getLongitude());
        mApplication.setLatestAlt(location.getAltitude());
        mApplication.setLatestAcc(location.getAccuracy());
        mApplication.setLatestProvider(location.getProvider());
//        Log.d(TAG, "Provider=" + location.getProvider());
//        Log.d(TAG, "Accuracy=" + location.getAccuracy());

        //notify activity for auto center and zoom
        Intent intent = new Intent("ttn-mapper-service-event");
        intent.putExtra("message", "locationupdate");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendNotification(String message) {
        Intent intent = new Intent("ttn-mapper-service-event");
        // add data
        intent.putExtra("message", "notification");
        intent.putExtra("payload", message);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    void stopThisService(String reason) {
        mqtt_disconnect();
        stopSelf();

        Intent intent = new Intent("ttn-mapper-service-event");
        // add data
        intent.putExtra("message", "selfstop");
        intent.putExtra("payload", reason);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    void mqtt_connect() {
        MyApplication mApplication = (MyApplication) getApplicationContext();
        try {
            mqttClient = new MqttClient("tcp://" + mApplication.getTtnBroker(), MqttClient.generateClientId(), persistence);

            connOpts.setUserName(mApplication.getTtnApplicationId());
            connOpts.setPassword(mApplication.getTtnAccessKey().toCharArray());

            mqttClient.connect(connOpts);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(final Throwable cause) {
                    Log.d(TAG, "mqtt connection lost");
                    stopThisService("mqtt connection lost");
                }

                @Override
                public void messageArrived(String topic, final MqttMessage message) throws Exception {
//                    Log.d(TAG, "mqtt message arrived");
//                    Log.d(TAG, "Topic: "+topic);
//                    Log.d(TAG, "Payload: "+message.toString());

                    MyApplication mApplication = (MyApplication) getApplicationContext();

                    if (mApplication.getLatestAcc() > 10) {
                        Log.d(TAG, "Packet received, GPS not accurate enough " + message.toString());
                        Log.d(TAG, message.isDuplicate() + "");
                        sendNotification("Packet received, but location of phone is not accurate enough. Try going outside.\nCurrent accuracy: " +
                                (Math.round(mApplication.getLatestAcc() * 100) / 100) + " metres\n" +
                                (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                    } else if (mApplication.getLatestLat() != 0 && mApplication.getLatestLon() != 0) {
                        Log.d(TAG, "Packet received, logging");
                        mApplication.logPacket(topic, message.toString());

                        // after logging the packet, let the activity know to refresh
                        Intent intent = new Intent("ttn-mapper-service-event");
                        intent.putExtra("message", "rxmessage");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                        sendNotification("Last packet received at:\n" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                    } else {
                        Log.d(TAG, "Packet received, GPS location unknown");
                        sendNotification("Packet received, but location of phone is still unknown.");
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "mqtt delivery complete");
                }
            });

            // jpm_testing/devices/arduino_uno_rn2483/up
            String mqttTopic = mApplication.getTtnApplicationId() + "/devices/" + mApplication.getTtnDeviceId() + "/up";
            mqttClient.subscribe(mqttTopic);
            Log.d(TAG, "MQTT subscribed to topic: " + mqttTopic);

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void mqtt_disconnect() {
        if (mqttClient != null) {
            if (mqttClient.isConnected()) {
                try {
                    Log.d(TAG, "Disconnecting MQTT");
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "MQTT disconnected");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google API client connected");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We should have permission as we ask for it at startup.
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Google API connection failed");
        stopThisService("Google play services outdated. Can not obtain a GPS location.");
    }

    public class LocalBinder extends Binder {
        TTNMapperService getService() {
            Log.d(TAG, "getService");
            // Return this instance of LocalService so clients can call public methods
            return TTNMapperService.this;
        }
    }
}