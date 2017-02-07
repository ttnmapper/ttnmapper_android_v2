package org.ttnmapper.ttnmapperv2;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by jpmeijers on 28-1-17.
 */

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication singleton;
    public ArrayList<TTNApplication> ttnApplications = new ArrayList<>();
    public TTNApplication chosenTtnApplication = null;
    public ArrayList<Measurement> measurements = new ArrayList<>();
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

    public static MyApplication getInstance(){
        return singleton;
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
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmm"); // Quoted "Z" to indicate UTC, no timezone offset
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

    public void logPacket(String topic, String payload) {
        Log.d(TAG, "Logging rx packet");

        /*
        {
          "app_id":"jpm_testing",
          "dev_id":"arduino_uno_rn2483",
          "hardware_serial":"00999B8A917DBB71",
          "port":1,
          "counter":123,
          "payload_raw":"IQ==",
          "metadata":{
            "time":"2017-02-07T11:03:29.086549185Z",
            "frequency":868.1,
            "modulation":"LORA",
            "data_rate":"SF7BW125",
            "coding_rate":"4/5",
            "gateways":
              [
                {
                  "gtw_id":"eui-1dee039aac75c307",
                  "timestamp":1401010363,
                  "time":"",
                  "channel":0,
                  "rssi":-108,
                  "snr":-5,
                  "rf_chain":1,
                  "latitude":52.2388,
                  "longitude":6.8551,
                  "altitude":6
                }
              ]
            }
          }
         */
        try {
            JSONObject packetData = new JSONObject(payload);
            JSONObject metadata = packetData.getJSONObject("metadata");
            JSONArray gateways = metadata.getJSONArray("gateways");

            for (int i = 0; i < gateways.length(); i++) {
                JSONObject gateway = gateways.getJSONObject(i);

                Measurement measurement = new Measurement();
                measurement.setTime(metadata.getString("time"));
                measurement.setNodeaddr(packetData.getString("dev_id"));
                measurement.setGwaddr(gateway.getString("gtw_id"));
                measurement.setSnr(gateway.getDouble("snr"));
                measurement.setRssi(gateway.getDouble("rssi"));
                measurement.setFreq(metadata.getDouble("frequency"));
                measurement.setLat(latestLat);
                measurement.setLon(latestLon);
                measurement.setDatarate(metadata.getString("data_rate"));
                measurement.setAppeui(packetData.getString("app_id"));
                measurement.setAlt(latestAlt);
                measurement.setAccuracy(latestAcc);
                measurement.setProvider(latestProvider);
                measurement.setMqtt_topic(topic);

                measurements.add(measurement);

                if (shouldUpload) {
                    uploadMeasurement(measurement);
                }
                if (saveToFile) {
                    saveMeasurementToFIle(measurement);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "Parsing packet payload failed with a json error");
        }
    }

    private void saveMeasurementToFIle(Measurement measurement) {
        Log.d(TAG, "Saving to file");

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-storage.html#filesExternal
        File root = android.os.Environment.getExternalStorageDirectory();

        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder
        File dir = new File(root.getAbsolutePath() + "/ttnmapper_logs");
        dir.mkdirs();
        File file = new File(dir, fileName);

        try {
            final FileOutputStream f = new FileOutputStream(file, true);
            final PrintWriter pw = new PrintWriter(f);
            pw.println(measurement.getJSON().toString().trim());
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadMeasurement(Measurement measurement) {
        Log.d(TAG, "Uploading: " + measurement.getJSON().toString());
    }


}
