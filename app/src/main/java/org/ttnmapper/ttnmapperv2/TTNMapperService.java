package org.ttnmapper.ttnmapperv2;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by jpmeijers on 30-1-17.
 */

public class TTNMapperService extends Service {

    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static String TAG = "LoggingService";
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart");

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

        return START_STICKY;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        TTNMapperService getService() {
            Log.d(TAG, "getService");
            // Return this instance of LocalService so clients can call public methods
            return TTNMapperService.this;
        }
    }
}