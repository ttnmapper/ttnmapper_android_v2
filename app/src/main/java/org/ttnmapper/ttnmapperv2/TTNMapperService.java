package org.ttnmapper.ttnmapperv2;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.Locale;

/**
 * Created by jpmeijers on 30-1-17.
 */

public class TTNMapperService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static String TAG = "LoggingService";
    final Handler handler = new Handler();
    private final IBinder mBinder = new LocalBinder();
    private final MqttConnectOptions connOpts = new MqttConnectOptions();
    private int startId;
    private MqttClient mqttClient;
    private MqttClientPersistence persistence = new MemoryPersistence();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private int reconnectCounter = 0;
    private boolean reconnectPending = false;
    private boolean shouldExit = false;

    private Ringtone ringtone;

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

        shouldExit = true;

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
                .setSmallIcon(R.drawable.ic_silhouette)
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

        new mqttConnectThread().execute();

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

        if (mqttClient != null) {
            if (!mqttClient.isConnected() && !reconnectPending && !shouldExit) {
                new mqttConnectThread().execute();
            }
        }
    }

    private void sendNotification(String message) {
        Intent intent = new Intent("ttn-mapper-service-event");
        // add data
        intent.putExtra("message", "notification");
        intent.putExtra("payload", message);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    void stopThisService(String reason) {

        shouldExit = true;

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
            connOpts.setAutomaticReconnect(true);
            connOpts.setConnectionTimeout(5);
            connOpts.setKeepAliveInterval(30);

            mqttClient.connect(connOpts);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(final Throwable cause) {
                    Log.d(TAG, "mqtt connection lost");
                    // should reconnect automatically
//                    logErrorToFile("mqtt conenction lost");

                    if (reconnectCounter < 10) {
                        if (!reconnectPending && !shouldExit) {
                            reconnectCounter++;
                            sendNotification("MQTT reconnect retry " + reconnectCounter + "/10");
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //Do something after 100ms
                                    Log.d(TAG, "Should restart MQTT now");
                                    reconnectPending = false;
                                    new mqttConnectThread().execute();
                                }
                            }, 10000);
                            reconnectPending = true;
                        }
                    } else {
                        stopThisService("MQTT connection failed");
                    }
                }

                @Override
                public void messageArrived(String topic, final MqttMessage message) throws Exception {
                    reconnectCounter = 0;

                    MyApplication mApplication = (MyApplication) getApplicationContext();
                    mApplication.logPacket(topic, message.toString());

                    if (mApplication.getLatestAcc() > 20) {
                        Log.d(TAG, "Packet received, GPS not accurate enough " + message.toString());
                        Log.d(TAG, message.isDuplicate() + "");
                        sendNotification("Packet received but not logged. Location is not accurate enough (>20m). Try going outside.\nCurrent accuracy: " +
                                (Math.round(mApplication.getLatestAcc() * 100) / 100) + " metres\n" +
                                (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date())));
                    } else if (mApplication.getLatestLat() != 0 && mApplication.getLatestLon() != 0) {
                        Log.d(TAG, "Packet received, logging");

                        // after logging the packet, let the activity know to refresh
                        Intent intent = new Intent("ttn-mapper-service-event");
                        intent.putExtra("message", "rxmessage");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                        if (mApplication.lastPacket.getGateways().size() > 1) {
                            sendNotification("Lastest packet:\n" +
                                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date())) + "\n" +
                                    "Gateways: " + mApplication.lastPacket.getGateways().size() + "\n" +
                                    "RSSI: " + mApplication.lastPacket.getMaxRssi() + "dBm (max)\n" +
                                    "SNR: " + mApplication.lastPacket.getMaxSnr() + "dB (max)\n" +
                                    "Distance: " + Math.round(mApplication.lastPacket.getMaxDistance() * 100) / 100 + "m (max)"
                            );
                        } else if (mApplication.lastPacket.getGateways().size() == 1) {
                            sendNotification("Lastest packet:\n" +
                                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date())) + "\n" +
                                    "Received by: " + mApplication.lastPacket.getGateways().get(0).gatewayID + "\n" +
                                    "RSSI: " + mApplication.lastPacket.getMaxRssi() + "dBm\n" +
                                    "SNR: " + mApplication.lastPacket.getMaxSnr() + "dB\n" +
                                    "Distance: " + Math.round(mApplication.lastPacket.getMaxDistance() * 100) / 100 + "m"
                            );
                        } else {
                            sendNotification("Lastest packet:\n" +
                                    (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date())) + "\n" +
                                    "Received by unknown gateway!\n" +
                                    "RSSI: " + mApplication.lastPacket.getMaxRssi() + "dBm\n" +
                                    "SNR: " + mApplication.lastPacket.getMaxSnr() + "dB\n" +
                                    "Distance: " + Math.round(mApplication.lastPacket.getMaxDistance() * 100) / 100 + "m"
                            );
                        }
                    } else {
                        Log.d(TAG, "Packet received, GPS location unknown");
                        sendNotification("Packet received, but location of phone is still unknown.\n" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(new Date())));
                    }

                    playSound();
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
            Log.d(TAG, "MQTT Exception in mqtt connect");
//            logErrorToFile("MQTT Exception in mqtt_connect");
            e.printStackTrace();

            if (reconnectCounter < 10) {
                if (!reconnectPending && !shouldExit) {
                    reconnectCounter++;
                    sendNotification("MQTT reconnect retry " + reconnectCounter + "/10");
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 100ms
                            Log.d(TAG, "Should restart MQTT now");
                            reconnectPending = false;
                            new mqttConnectThread().execute();
                        }
                    }, 10000);
                    reconnectPending = true;
                }
            } else {
                stopThisService("MQTT connection failed");
            }
        }
    }

    public void playSound() {
        //sound
        try {
            SharedPreferences myPrefs = getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
            if (myPrefs.getBoolean(SettingConstants.SOUNDON, SettingConstants.SOUNDON_DEFAULT)) {
                MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(getApplicationContext(), Uri.parse(myPrefs.getString(SettingConstants.SOUNDFILE, SettingConstants.SOUNDFILE_DEFAULT)));
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (Exception e) {
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

//    public void logErrorToFile(String error)
//    {
//        try
//        {
//            TimeZone tz = TimeZone.getTimeZone("UTC");
//            DateFormat df = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH); // Quoted "Z" to indicate UTC, no timezone offset
//            df.setTimeZone(tz);
//            String nowAsISO = df.format(new Date());
//
//            // Find the root of the external storage.
//            // See http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
//            File root = android.os.Environment.getExternalStorageDirectory();
//
//            // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder
//            File dir = new File(root.getAbsolutePath() + "/ttnmapper_logs");
//            dir.mkdirs();
//            File file = new File(dir, "error_log");
//
//            final FileOutputStream f = new FileOutputStream(file, true);
//            final PrintWriter pw = new PrintWriter(f);
//            pw.println(nowAsISO+" "+error);
//
//            pw.flush();
//            pw.close();
//            f.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google API client connected");
//        String locationProvider = LocationManager.GPS_PROVIDER;
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

    private class mqttConnectThread extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {
            mqtt_connect();
            return null;
        }
    }

    public class LocalBinder extends Binder {
        TTNMapperService getService() {
            Log.d(TAG, "getService");
            // Return this instance of LocalService so clients can call public methods
            return TTNMapperService.this;
        }
    }
}