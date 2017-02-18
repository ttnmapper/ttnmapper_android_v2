package org.ttnmapper.ttnmapperv2;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
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

import io.fabric.sdk.android.Fabric;
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
    public String lastStatusMessage = "";
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
    private String latestProvider = "none";
    private OkHttpClient httpClient = new OkHttpClient();

    public static MyApplication getInstance(){
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        singleton = this;

        SharedPreferences myPrefs = getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        ttnApplicationId = myPrefs.getString(SettingConstants.TTN_APPLICATION_ID, "");
        ttnDeviceId = myPrefs.getString(SettingConstants.TTN_DEVICE_ID, "");
        ttnAccessKey = myPrefs.getString(SettingConstants.TTN_ACCESS_KEY, "");
        ttnBroker = myPrefs.getString(SettingConstants.TTN_BROKER, "");

        shouldUpload = myPrefs.getBoolean(SettingConstants.SHOULD_UPLOAD, SettingConstants.SHOULD_UPLOAD_DEFAULT);
        isExperiment = myPrefs.getBoolean(SettingConstants.IS_EXPERIMENT, SettingConstants.IS_EXPERIMENT_DEFAULT);
        saveToFile = myPrefs.getBoolean(SettingConstants.SAVE_TO_FILE, SettingConstants.SAVE_TO_FILE_DEFAULT);

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        experimentName = myPrefs.getString(SettingConstants.EXPERIMENT_NAME, "experiment_" + nowAsISO);
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
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString(SettingConstants.TTN_APPLICATION_ID, ttnApplicationId);
        prefsEditor.apply();
    }

    public String getTtnDeviceId() {
        return ttnDeviceId;
    }

    public void setTtnDeviceId(String ttnDeviceId) {
        this.ttnDeviceId = ttnDeviceId;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString(SettingConstants.TTN_DEVICE_ID, ttnDeviceId);
        prefsEditor.apply();
    }

    public String getTtnAccessKey() {
        return ttnAccessKey;
    }

    public void setTtnAccessKey(String ttnAccessKey) {
        this.ttnAccessKey = ttnAccessKey;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString(SettingConstants.TTN_ACCESS_KEY, ttnAccessKey);
        prefsEditor.apply();
    }

    public String getTtnBroker() {
        return ttnBroker;
    }

    public void setTtnBroker(String ttnBroker) {
        this.ttnBroker = ttnBroker;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString(SettingConstants.TTN_BROKER, ttnBroker);
        prefsEditor.apply();
    }

    public boolean isShouldUpload() {
        return shouldUpload;
    }

    public void setShouldUpload(boolean shouldUpload) {
        this.shouldUpload = shouldUpload;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(SettingConstants.SHOULD_UPLOAD, shouldUpload);
        prefsEditor.apply();
    }

    public boolean isExperiment() {
        return isExperiment;
    }

    public void setExperiment(boolean experiment) {
        isExperiment = experiment;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(SettingConstants.IS_EXPERIMENT, isExperiment);
        prefsEditor.apply();
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putString(SettingConstants.EXPERIMENT_NAME, experimentName);
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
        SharedPreferences myPrefs = this.getSharedPreferences(SettingConstants.PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(SettingConstants.SAVE_TO_FILE, saveToFile);
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

        if (saveToFile) {
            saveMeasurementToFile(topic, payload);
        }

        if (latestLat == 0 || latestLon == 0 || latestAcc > 20) {
            //we do not know our location yet
            return;
        }

        try {
            JSONObject packetData = new JSONObject(payload);
            JSONObject metadata = packetData.getJSONObject(APIJsonFields.TTNPacket.METADATA);
            JSONArray gateways = metadata.getJSONArray(APIJsonFields.TTNMetadata.GATEWAYS);

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
                //without an id it is useless, so we will error out
                gatewayToSave.setGatewayID(gatewayFromJson.getString(APIJsonFields.TTNGateway.ID));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.TIMESTAMP))
                    gatewayToSave.setTimestamp(gatewayFromJson.getString(APIJsonFields.TTNGateway.TIMESTAMP));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.TIME))
                    gatewayToSave.setTime(gatewayFromJson.getString(APIJsonFields.TTNGateway.TIME));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.CHANNEL))
                    gatewayToSave.setChannel(gatewayFromJson.getInt(APIJsonFields.TTNGateway.CHANNEL));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.RSSI))
                    gatewayToSave.setRssi(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.RSSI));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.SNR))
                    gatewayToSave.setSnr(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.SNR));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.RFCHAIN))
                    gatewayToSave.setRfChain(gatewayFromJson.getInt(APIJsonFields.TTNGateway.RFCHAIN));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.LATITUDE))
                    gatewayToSave.setLatitude(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.LATITUDE));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.LONGITUDE))
                    gatewayToSave.setLongitude(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.LONGITUDE));
                if (gatewayFromJson.has(APIJsonFields.TTNGateway.ALTITUDE))
                    gatewayToSave.setAltitude(gatewayFromJson.getDouble(APIJsonFields.TTNGateway.ALTITUDE));

                packet.addGateway(gatewayToSave);
            }
            packets.add(packet);
            lastPacket = packet;

            if (shouldUpload) {
                uploadMeasurement(packet);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Parsing packet payload failed with a json error");
        }
    }

    private void saveMeasurementToFile(String topic, String payload) {
        Log.d(TAG, "Saving to file");

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

            JSONObject payloadJson = new JSONObject(); //empty json object in case the parsing fails
            try {
                payloadJson = new JSONObject(payload);
                payloadJson.put("mqtt_topic", topic);

                payloadJson.put("phone_lat", latestLat);
                payloadJson.put("phone_lon", latestLon);
                payloadJson.put("phone_alt", latestAlt);
                payloadJson.put("phone_loc_acc", latestAcc);
                payloadJson.put("phone_loc_provider", latestProvider);

                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH); // Quoted "Z" to indicate UTC, no timezone offset
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                String nowAsISO = df.format(new Date());
                payloadJson.put("phone_time", nowAsISO);

                //set the app instance ID (https://developers.google.com/instance-id/)
                final PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                final String version = pInfo.versionName;
                int verCode = pInfo.versionCode;
                payloadJson.put(APIJsonFields.MapperPacket.USER_AGENT, "Android" + android.os.Build.VERSION.RELEASE + " App" + verCode + ":" + version);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            pw.println(payloadJson.toString());
            Log.d(TAG, "Line written: " + payloadJson.toString());

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
                Answers.getInstance().logCustom(new CustomEvent("Upload to experiment"));
            }

            //post packet
            try {
                postToServer(getString(R.string.ttnmapper_api_upload_packet), toPost.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "Error uploading");
                        e.printStackTrace();
                        Answers.getInstance().logCustom(new CustomEvent("Upload").putCustomAttribute("error", e.toString()));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            final String returnedString = response.body().string();
                            System.out.println("HTTP response: " + returnedString);
                            if (!returnedString.contains("New records created successfully")) {
                                // Request not successful
                                Log.d(TAG, "server error: " + returnedString);
                                Answers.getInstance().logCustom(new CustomEvent("Upload").putCustomAttribute("error", returnedString));
                            } else {
                                Answers.getInstance().logCustom(new CustomEvent("Upload").putCustomAttribute("error", "success"));
                            }
                            // Do what you want to do with the response.
                        } else {
                            // Request not successful
                            Log.d(TAG, "server error");
                            Answers.getInstance().logCustom(new CustomEvent("Upload").putCustomAttribute("error", "server error"));
                        }
                    }
                });
            } catch (IOException e) {
                Log.d(TAG, "HTTP call IO exception");
                e.printStackTrace();
                Answers.getInstance().logCustom(new CustomEvent("Upload").putCustomAttribute("error", e.toString()));
            }
        }
    }
}
