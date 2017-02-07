package org.ttnmapper.ttnmapperv2;

/**
 * Variables specifying how the API fields are called.
 * <p>
 * Should the API rename some fields in the future,
 * they can be changed here in a single place.
 */
public final class ApiFields {

    public final static class Metadata {
        public final static String TIME = "time";
        public final static String FREQUENCY = "frequency";
        public final static String MODULATION = "modulation";
        public final static String DATA_RATE = "data_rate";
        public final static String CODING_RATE = "coding_rate";
    }

    public final static class Gateway {
        public final static String ID = "gtw_id";
        public final static String TIMESTAMP = "timestamp";
        public final static String TIME = "time";
        public final static String CHANNEL = "channel";
        public final static String RSSI = "rssi";
        public final static String SNR = "snr";
        public final static String LATITUDE = "latitude";
        public final static String LONGITUDE = "longitude";
        public final static String ALTITUDE = "altitude";
    }

}
