package org.webrtc.kite.pojo;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that holds stats data for verify
 */
public class RTCStatsDiff {
    public Browser browser;
    public Object browserStats;
    public RTCStatsDiff(Browser browser, String _browserStats) {
        this.browser = browser;
        this.browserStats = this.clientStats(_browserStats);
    }

    private Object clientStats(String _browserStats){
        Object retval = null;
        try {
            Map<String, Object> response = new ObjectMapper().readValue(_browserStats, HashMap.class);
            retval = response.get("client_1");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retval;
    }

}
