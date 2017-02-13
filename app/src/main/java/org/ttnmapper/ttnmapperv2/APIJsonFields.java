package org.ttnmapper.ttnmapperv2;

/**
 * Variables specifying how the API fields are called.
 * <p>
 * Should the API rename some fields in the future,
 * they can be changed here in a single place.
 */
public final class APIJsonFields {
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

      Docs:
      "gateways": [
      {
        "id": "ttn-herengracht-ams",    // EUI of the gateway
        "timestamp": 12345,             // Timestamp when the gateway received the message
        "time": "1970-01-01T00:00:00Z", // Time when the gateway received the message - left out when gateway does not have synchronized time
        "channel": 0,                   // Channel where the gateway received the message
        "rssi": -25,                    // Signal strength of the received message
        "snr": 5,                       // Signal to noise ratio of the received message
        "rf_chain": 0,                  // RF chain where the gateway received the message
      },
      //...more if received by more gateways...
    ]
     */

    public final static class TTNPacket {
        public final static String APPID = "app_id";
        public final static String DEVID = "dev_id";
        public final static String SERIAL = "hardware_serial";
        public final static String PORT = "port";
        public final static String COUNTER = "counter";
        public final static String PAYLOAD = "payload_raw";
        public final static String METADATA = "metadata";
    }

    public final static class TTNMetadata {
        public final static String TIME = "time";
        public final static String FREQUENCY = "frequency";
        public final static String MODULATION = "modulation";
        public final static String DATA_RATE = "data_rate";
        public final static String CODING_RATE = "coding_rate";
        public final static String GATEWAYS = "gateways";
    }

    public final static class TTNGateway {
        public final static String ID = "gtw_id";
        public final static String TIMESTAMP = "timestamp";
        public final static String TIME = "time";
        public final static String CHANNEL = "channel";
        public final static String RSSI = "rssi";
        public final static String SNR = "snr";
        public final static String RFCHAIN = "rf_chain";
        public final static String LATITUDE = "latitude";
        public final static String LONGITUDE = "longitude";
        public final static String ALTITUDE = "altitude";
    }


    /*
    Packet definition for upload to TTN Mapper
     */
    public final static class MapperPacket {
        public final static String TIME = "time";
        public final static String DEVID = "nodeaddr";
        public final static String APPID = "appeui";
        public final static String GTWID = "gwaddr";
        public final static String RSSI = "rssi";
        public final static String SNR = "snr";
        public final static String MODULATION = "modulation";
        public final static String FREQUENCY = "freq";
        public final static String DATA_RATE = "datarate";
        public final static String CODING_RATE = "codingrate";
        public final static String LATITUDE = "lat";
        public final static String LONGITUDE = "lon";
        public final static String ALTITUDE = "alt";
        public final static String ACCURACY = "accuracy";
        public final static String PROVIDER = "provider";
        public final static String MQTT_TOPIC = "mqtt_topic";
        public final static String USER_AGENT = "user_agent";
        public final static String INSTANCE_ID = "iid";
        public final static String EXPERIMENT = "experiment";
    }

}
