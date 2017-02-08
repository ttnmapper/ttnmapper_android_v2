package org.ttnmapper.ttnmapperv2;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jpmeijers on 7-2-17.
 */

public class Measurement {
    String time;
    String nodeaddr;
    String gwaddr;
    double snr;
    double rssi;
    double freq;
    double lat;
    double lon;
    String datarate;
    String appeui;
    double alt;
    double accuracy;
    String provider;
    String mqtt_topic;
    double maxRssi; //if this packet was received by multiple gateways, save the rssi of the best one -> used for plotting on map
    double gwlat;
    double gwlon;

    public double getGwlon() {
        return gwlon;
    }

    public void setGwlon(double gwlon) {
        this.gwlon = gwlon;
    }

    public double getGwlat() {
        return gwlat;
    }

    public void setGwlat(double gwlat) {
        this.gwlat = gwlat;
    }

    public double getMaxRssi() {
        return maxRssi;
    }

    public void setMaxRssi(double maxRssi) {
        this.maxRssi = maxRssi;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNodeaddr() {
        return nodeaddr;
    }

    public void setNodeaddr(String nodeaddr) {
        this.nodeaddr = nodeaddr;
    }

    public String getGwaddr() {
        return gwaddr;
    }

    public void setGwaddr(String gwaddr) {
        this.gwaddr = gwaddr;
    }

    public double getSnr() {
        return snr;
    }

    public void setSnr(double snr) {
        this.snr = snr;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public double getFreq() {
        return freq;
    }

    public void setFreq(double freq) {
        this.freq = freq;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getDatarate() {
        return datarate;
    }

    public void setDatarate(String datarate) {
        this.datarate = datarate;
    }

    public String getAppeui() {
        return appeui;
    }

    public void setAppeui(String appeui) {
        this.appeui = appeui;
    }

    public double getAlt() {
        return alt;
    }

    public void setAlt(double alt) {
        this.alt = alt;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getMqtt_topic() {
        return mqtt_topic;
    }

    public void setMqtt_topic(String mqtt_topic) {
        this.mqtt_topic = mqtt_topic;
    }


    public JSONObject getJSON() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("time", time);
            jsonObject.put("nodeaddr", nodeaddr);
            jsonObject.put("gwaddr", gwaddr);
            jsonObject.put("snr", snr);
            jsonObject.put("rssi", rssi);
            jsonObject.put("freq", freq);
            jsonObject.put("lat", lat);
            jsonObject.put("lon", lon);
            jsonObject.put("datarate", datarate);
            jsonObject.put("appeui", appeui);
            jsonObject.put("alt", alt);
            jsonObject.put("accuracy", accuracy);
            jsonObject.put("mqtt_topic", mqtt_topic);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObject;
    }

}
