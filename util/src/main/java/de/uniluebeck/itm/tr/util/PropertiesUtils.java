package de.uniluebeck.itm.tr.util;

import java.util.Map;
import java.util.Properties;


public class PropertiesUtils {

    /**
     * Copies key/value pairs from the map to a properties instance.
     *
     * @return a {@link java.util.Properties} instance containing the key/value-pairs of the map given.
     */
    public static Properties copyToProperties(Map<String, String> map) {
        Properties props = new Properties();
        for (Object key : map.keySet()) {
            props.setProperty((String) key, map.get(key));
        }
        return props;
    }

}
