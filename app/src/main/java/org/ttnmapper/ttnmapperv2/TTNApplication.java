package org.ttnmapper.ttnmapperv2;

import java.util.ArrayList;

/**
 * Created by jpmeijers on 29-1-17.
 */

public class TTNApplication {
    private String id;
    private String name;
    private String accessKey;
    private String devicesKey;
    private String handler;
    private String mqttAddress;
    private String apiAddress;
    private String netAddress;
    private ArrayList<String> devices;



    public TTNApplication(String id)
    {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public ArrayList<String> getDevices() {
        if(devices == null)
        {
            devices = new ArrayList<>();
        }
        return devices;
    }

    public void setDevices(ArrayList<String> devices) {
        this.devices = devices;
    }

    public void addDevice(String deviceToAdd)
    {
        if(devices == null)
        {
            devices = new ArrayList<>();
        }
        if(!devices.contains(deviceToAdd))
            devices.add(deviceToAdd);
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getMqttAddress() {
        return mqttAddress;
    }

    public void setMqttAddress(String mqttAddress) {
        this.mqttAddress = mqttAddress;
    }

    public String getApiAddress() {
        return apiAddress;
    }

    public void setApiAddress(String apiAddress) {
        this.apiAddress = apiAddress;
    }

    public String getDevicesKey() {
        return devicesKey;
    }

    public void setDevicesKey(String devicesKey) {
        this.devicesKey = devicesKey;
    }

    public String getNetAddress() {
        return netAddress;
    }

    public void setNetAddress(String netAddress) {
        this.netAddress = netAddress;
    }
}
