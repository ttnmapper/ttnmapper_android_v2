package org.ttnmapper.ttnmapperv2;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.iid.InstanceID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by jpmeijers on 28-1-17.
 */

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication singleton;
    public ArrayList<TTNApplication> ttnApplications = new ArrayList<>();
    public TTNApplication chosenTtnApplication = null;
    public ArrayList<Packet> packets = new ArrayList<>();
    public Packet lastPacket;
    private boolean shouldUpload;
    private boolean isExperiment;
    private boolean saveToFile;
    private String fileName;
    private String experimentName;
    private String ttnApplicationId = "";
    private String ttnDeviceId = "";
    private String ttnAccessKey = "";
    private String ttnBroker = "";
    private double latestLat = 0;
    private double latestLon = 0;
    private double latestAlt = 0;
    private double latestAcc = 0;
    private boolean lordriveMode = true;
    private boolean autoCenter = true;
    private boolean autoZoom = true;
    private String latestProvider = "none";
    private OkHttpClient httpClient = new OkHttpClient();

    public static MyApplication getInstance(){
        return singleton;
    }

    public boolean isLordriveMode() {
        return lordriveMode;
    }

    public void setLordriveMode(boolean lordriveMode) {
        this.lordriveMode = lordriveMode;
    }

    public boolean isAutoCenter() {
        return autoCenter;
    }

    public void setAutoCenter(boolean autoCenter) {
        this.autoCenter = autoCenter;
    }

    public boolean isAutoZoom() {
        return autoZoom;
    }

    public void setAutoZoom(boolean autoZoom) {
        this.autoZoom = autoZoom;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        SharedPreferences myPrefs = getSharedPreferences("myPrefs", MODE_PRIVATE);
        ttnApplicationId = myPrefs.getString("ttnApplicationId", "");
        ttnDeviceId = myPrefs.getString("ttnDeviceId", "");
        ttnAccessKey = myPrefs.getString("ttnAccessKey", "");
        ttnBroker = myPrefs.getString("ttnBroker", "");

        shouldUpload = myPrefs.getBoolean("shouldUpload", true);
        isExperiment = myPrefs.getBoolean("isExperiment", false);
        saveToFile = myPrefs.getBoolean("saveToFile", true);

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        experimentName = myPrefs.getString("experimentName", "experiment_" + nowAsISO);
        fileName = "ttnmapper-" + nowAsISO + ".log";
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public String getTtnApplicationId() {
        return ttnApplicationId;
    }

    public void setTtnApplicationId(String ttnApplicationId) {
        this.ttnApplicationId = ttnApplicationId;
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString("ttnApplicationId", ttnApplicationId);
        prefsEditor.apply();
    }

    public String getTtnDeviceId() {
        return ttnDeviceId;
    }

    public void setTtnDeviceId(String ttnDeviceId) {
        this.ttnDeviceId = ttnDeviceId;
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString("ttnDeviceId", ttnDeviceId);
        prefsEditor.apply();
    }

    public String getTtnAccessKey() {
        return ttnAccessKey;
    }

    public void setTtnAccessKey(String ttnAccessKey) {
        this.ttnAccessKey = ttnAccessKey;
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString("ttnAccessKey", ttnAccessKey);
        prefsEditor.apply();
    }

    public String getTtnBroker() {
        return ttnBroker;
    }

    public void setTtnBroker(String ttnBroker) {
        this.ttnBroker = ttnBroker;
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString("ttnBroker", ttnBroker);
        prefsEditor.apply();
    }

    public boolean isShouldUpload() {
        return shouldUpload;
    }

    public void setShouldUpload(boolean shouldUpload) {
        this.shouldUpload = shouldUpload;
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean("shouldUpload", shouldUpload);
        prefsEditor.apply();
    }

    public boolean isExperiment() {
        return isExperiment;
    }

    public void setExperiment(boolean experiment) {
        isExperiment = experiment;
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean("isExperiment", isExperiment);
        prefsEditor.apply();
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString("experimentName", experimentName);
        prefsEditor.apply();
    }

    public double getLatestAcc() {
        return latestAcc;
    }

    public void setLatestAcc(double latestAcc) {
        this.latestAcc = latestAcc;
    }

    public double getLatestAlt() {
        return latestAlt;
    }

    public void setLatestAlt(double latestAlt) {
        this.latestAlt = latestAlt;
    }

    public double getLatestLat() {
        return latestLat;
    }

    public void setLatestLat(double latestLat) {
        this.latestLat = latestLat;
    }

    public double getLatestLon() {
        return latestLon;
    }

    public void setLatestLon(double latestLon) {
        this.latestLon = latestLon;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isSaveToFile() {
        return saveToFile;
    }

    public void setSaveToFile(boolean saveToFile) {
        this.saveToFile = saveToFile;
        SharedPreferences myPrefs = this.getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean("saveToFile", saveToFile);
        prefsEditor.apply();
    }

    public String getLatestProvider() {
        return latestProvider;
    }

    public void setLatestProvider(String latestProvider) {
        this.latestProvider = latestProvider;
    }

    //is configured
    public boolean isConfigured()
    {
        return !(ttnApplicationId.equals("") || ttnDeviceId.equals("") || ttnAccessKey.equals("") || ttnBroker.equals(""));
    }

    //check if we have all the neccesary permissions
    public boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else
            return !(saveToFile && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);
    }

    public void logPacket(String topic, String payload) {
        Log.d(TAG, "Logging rx packet");

        if (latestLat == 0 || latestLon == 0) {
            //we do not know our location yet
            return;
        }

        try {
            JSONObject packetData = new JSONObject(payload);
            JSONObject metadata = packetData.getJSONObject("metadata");
            JSONArray gateways = metadata.getJSONArray("gateways");

            Packet packet = new Packet();
            packet.setAppID(packetData.getString(APIJsonFields.TTNPacket.APPID));
            packet.setDeviceID(packetData.getString(APIJsonFields.TTNPacket.DEVID));

            packet.setTime(metadata.getString(APIJsonFields.TTNMetadata.TIME));
            packet.setFrequency(metadata.getDouble(APIJsonFields.TTNMetadata.FREQUENCY));
            packet.setModulation(metadata.getString(APIJsonFields.TTNMetadata.MODULATION));
            packet.setDataRate(metadata.getString(APIJsonFields.TTNMetadata.DATA_RATE));
            packet.setCodingRate(metadata.getString(APIJsonFields.TTNMetadata.CODING_RATE));

            packet.setLatitude(latestLat);
            packet.setLongitude(latestLon);
            packet.setAltitude(latestAlt);
            packet.setAccuracy(latestAcc);
            packet.setProvider(latestProvider);

            packet.setMqttTopic(topic);

            for (int i = 0; i < gateways.length(); i++) {
                JSONObject gatewayFromJson = gateways.getJSONObject(i);

                Gateway gatewayToSave = new Gateway();
                gatewayToSave.setGatewayID(gatewayFromJson.getString(APIJsonFields.TTNGateway.ID));
                gatewayToSave.setTimestamp(gatewayFromJson.getString(APIJsonFields.TTNGateway.TIMESTAMP));
                gatewayToSave.setTime(gatewayFromJson.getString(APIJsonFields.TTNGateway.TIME));
                gatewayToSave.setChannel(gatewayFromJson.getInt(APIJsonFields.TTNGateway.CHANNEL));
                gatewayToSave.setRssi(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.RSSI));
                gatewayToSave.setSnr(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.SNR));
                gatewayToSave.setRfChain(gatewayFromJson.getInt(APIJsonFields.TTNGateway.RFCHAIN));
                gatewayToSave.setLatitude(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.LATITUDE));
                gatewayToSave.setLongitude(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.LONGITUDE));
                gatewayToSave.setAltitude(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.ALTITUDE));

                packet.addGateway(gatewayToSave);
            }
            packets.add(packet);
            lastPacket = packet;

            if (shouldUpload) {
                uploadMeasurement(packet);
            }
            if (saveToFile) {
                saveMeasurementToFile(packet);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Parsing packet payload failed with a json error");
        }
    }

    private void saveMeasurementToFile(Packet packet) {
        Log.d(TAG, "Saving to file");

        //TODO: Maybe we can serialize the packet object directly and save that to the file

        try {
            // Find the root of the external storage.
            // See http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
            File root = android.os.Environment.getExternalStorageDirectory();

            // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder
            File dir = new File(root.getAbsolutePath() + "/ttnmapper_logs");
            dir.mkdirs();
            File file = new File(dir, fileName);

            final FileOutputStream f = new FileOutputStream(file, true);
            final PrintWriter pw = new PrintWriter(f);

            for (Gateway gateway : packet.getGateways()) {

                JSONObject toPost = new JSONObject();

                try {
                    toPost.put(APIJsonFields.MapperPacket.TIME, packet.getTime());
                    toPost.put(APIJsonFields.MapperPacket.DEVID, packet.getDeviceID());
                    toPost.put(APIJsonFields.MapperPacket.APPID, packet.getAppID());
                    toPost.put(APIJsonFields.MapperPacket.GTWID, gateway.getGatewayID());
                    toPost.put(APIJsonFields.MapperPacket.RSSI, gateway.getRssi());
                    toPost.put(APIJsonFields.MapperPacket.SNR, gateway.getSnr());
                    toPost.put(APIJsonFields.MapperPacket.MODULATION, packet.getModulation());
                    toPost.put(APIJsonFields.MapperPacket.FREQUENCY, packet.getFrequency());
                    toPost.put(APIJsonFields.MapperPacket.DATA_RATE, packet.getDataRate());
                    toPost.put(APIJsonFields.MapperPacket.CODING_RATE, packet.getCodingRate());
                    toPost.put(APIJsonFields.MapperPacket.LATITUDE, packet.getLatitude());
                    toPost.put(APIJsonFields.MapperPacket.LONGITUDE, packet.getLongitude());
                    toPost.put(APIJsonFields.MapperPacket.ALTITUDE, packet.getAltitude());
                    toPost.put(APIJsonFields.MapperPacket.ACCURACY, packet.getAccuracy());
                    toPost.put(APIJsonFields.MapperPacket.PROVIDER, packet.getProvider());
                    toPost.put(APIJsonFields.MapperPacket.MQTT_TOPIC, packet.getMqttTopic());
                    toPost.put(APIJsonFields.MapperPacket.INSTANCE_ID, InstanceID.getInstance(getApplicationContext()).getId());

                    //set the app instance ID (https://developers.google.com/instance-id/)
                    final PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    final String version = pInfo.versionName;
                    int verCode = pInfo.versionCode;
                    try {
                        toPost.put(APIJsonFields.MapperPacket.USER_AGENT, "Android" + android.os.Build.VERSION.RELEASE + " App" + verCode + ":" + version);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } catch (JSONException | PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                pw.println(toPost.toString());
            }
            pw.flush();
            pw.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Call postToServer(String url, String json, Callback callback) throws IOException {
        RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = httpClient.newCall(request);
        call.enqueue(callback);
        return call;
    }

    private void uploadMeasurement(Packet packet) {
        for (Gateway gateway : packet.getGateways()) {

            JSONObject toPost = new JSONObject();

            try {
                toPost.put(APIJsonFields.MapperPacket.TIME, packet.getTime());
                toPost.put(APIJsonFields.MapperPacket.DEVID, packet.getDeviceID());
                toPost.put(APIJsonFields.MapperPacket.APPID, packet.getAppID());
                toPost.put(APIJsonFields.MapperPacket.GTWID, gateway.getGatewayID());
                toPost.put(APIJsonFields.MapperPacket.RSSI, gateway.getRssi());
                toPost.put(APIJsonFields.MapperPacket.SNR, gateway.getSnr());
                toPost.put(APIJsonFields.MapperPacket.MODULATION, packet.getModulation());
                toPost.put(APIJsonFields.MapperPacket.FREQUENCY, packet.getFrequency());
                toPost.put(APIJsonFields.MapperPacket.DATA_RATE, packet.getDataRate());
                toPost.put(APIJsonFields.MapperPacket.CODING_RATE, packet.getCodingRate());
                toPost.put(APIJsonFields.MapperPacket.LATITUDE, packet.getLatitude());
                toPost.put(APIJsonFields.MapperPacket.LONGITUDE, packet.getLongitude());
                toPost.put(APIJsonFields.MapperPacket.ALTITUDE, packet.getAltitude());
                toPost.put(APIJsonFields.MapperPacket.ACCURACY, packet.getAccuracy());
                toPost.put(APIJsonFields.MapperPacket.PROVIDER, packet.getProvider());
                toPost.put(APIJsonFields.MapperPacket.MQTT_TOPIC, packet.getMqttTopic());
                toPost.put(APIJsonFields.MapperPacket.INSTANCE_ID, InstanceID.getInstance(getApplicationContext()).getId());

                //set the app instance ID (https://developers.google.com/instance-id/)
                final PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                final String version = pInfo.versionName;
                int verCode = pInfo.versionCode;
                try {
                    toPost.put(APIJsonFields.MapperPacket.USER_AGENT, "Android" + android.os.Build.VERSION.RELEASE + " App" + verCode + ":" + version);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (JSONException | PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            //the only difference between a normal upload and an experiment is the experiment name parameter
            if (isExperiment) {
                try {
                    toPost.put(APIJsonFields.MapperPacket.EXPERIMENT, experimentName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //post packet
            try {
                postToServer(getString(R.string.ttnmapper_api_upload_packet), toPost.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "Error uploading");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            final String returnedString = response.body().string();
                            System.out.println("HTTP response: " + returnedString);
                            if (!returnedString.contains("New records created successfully")) {
                                // Request not successful
                                Log.d(TAG, "server error: " + returnedString);
                            }
                            // Do what you want to do with the response.
                        } else {
                            // Request not successful
                            Log.d(TAG, "server error");
                        }
                    }
                });
            } catch (IOException e) {
                Log.d(TAG, "HTTP call IO exception");
                e.printStackTrace();
            }
        }
    }
}
