package org.webrtc.kite.apprtc;

import org.apache.log4j.Logger;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * StatsUtils is a Singleton class that collects and save KITE load testing stats into a CSV file.
 */
public class StatsUtils {

  private static HashMap<String, StatsUtils> instance = new HashMap<String, StatsUtils>();
  private static final Logger logger = Logger.getLogger(StatsUtils.class.getName());

  private static Map<String, String> keyValMap = new LinkedHashMap<String, String>();

  private final String filename;
  private FileOutputStream fout = null;
  private PrintWriter pw = null;

  private int testID = 1; // start count at 1
  private boolean initialized = false;

  private StatsUtils(String prefix) {
    filename = prefix + "report_" + new SimpleDateFormat("yyyyMMdd_hhmmss").format(new Date()) + ".csv";
  }

  /**
   * @return and instance of StatsUtils
   */
  public static StatsUtils getInstance(String prefix) {
    try {
      if (!instance.containsKey(prefix)) {
        instance.put(prefix,new StatsUtils(prefix));
      }
    } catch (Exception e) {
      logger.error("\r\n" + Utility.getStackTrace(e));
    }
    return instance.get(prefix);
  }

  /**
   * Print the test statistic line.
   *
   * @param o Object object containing the test results. Either a JsonObject or any Object with a
   *     toString() method
   * @param path the file path where to save the file.
   */
  public synchronized void println(Object o, String path) {
    try {
      if (!initialized) {
        File dir = new File(path);
        if (!dir.isDirectory()) {
          dir.mkdirs();
        }
        fout = new FileOutputStream(path + filename);
        pw = new PrintWriter(fout, true);
      }
      if (o instanceof JsonObject) {
        logger.info("StatsUtils.println(JsonObject) " + o.toString());
        JsonObject jsonObject = (JsonObject) o;
        Map<String, String> map = StatsUtils.jsonToHashMap(jsonObject);
        if (!initialized) {
          pw.println(keysLine(map));
        }
        pw.println(valuesLine(map));
      } else {
        logger.info("StatsUtils.println(String) " + o.toString());
        pw.println(o.toString());
      }
    } catch (Exception e) {
      logger.error("\r\n" + Utility.getStackTrace(e));
    }
    initialized = true;
  }

  /**
   * Saves a JSON object into a file, with line breaks and indents.
   *
   * @param testName the name of the test, which will be inlcuded in the file name
   * @param jsonStr the json object as a String.
   * @param path the file path where to save the file.
   */
  public void printJsonTofile(String testName, String jsonStr, String path) {
    try {
      Map<String, Object> properties = new LinkedHashMap<>(1);
      properties.put(JsonGenerator.PRETTY_PRINTING, true);
      String jsonFilename =
          testName.replace(" ", "").replaceAll("[^A-Za-z0-9()\\[\\]]", "")
              + "_"
              + new SimpleDateFormat("yyyyMMdd_hhmmss").format(new Date())
              + ".json";
      File dir = new File(path);
      if (!dir.isDirectory()) {
        dir.mkdirs();
      }
      FileOutputStream fo = new FileOutputStream(path + jsonFilename);
      PrintWriter pw = new PrintWriter(fo, true);
      JsonWriterFactory writerFactory = Json.createWriterFactory(properties);
      JsonWriter jsonWriter = writerFactory.createWriter(pw);
      JsonObject obj = Json.createReader(new StringReader(jsonStr)).readObject();
      jsonWriter.writeObject(obj);
      jsonWriter.close();
      pw.close();
      fo.close();
    } catch (Exception e) {
      logger.error("\r\n" + Utility.getStackTrace(e));
    }
  }

  /**
   * Convert the JSON Object into a line of values that can be printed in the CSV file.
   *
   * @param map
   * @return line String to be printed in the CSV file
   */
  private String valuesLine(Map<String, String> map) {
    String line = "";
    int i = 0;
    for (String key : map.keySet()) {
      line += map.get(key) + (i++ < map.size() ? "," : "");
    }
    return line;
  }

  /**
   * Convert the JSON Object into a line of keys that can be printed as the header of the CSV file.
   *
   * @param map
   * @return line String to be printed in the CSV file
   */
  private String keysLine(Map<String, String> map) {
    String line = "";
    int i = 0;
    for (String key : map.keySet()) {
      line += key + (i++ < map.size() ? "," : "");
    }
    return line;
  }

  /**
   * Print the CSV file header. Make sure to call this right after calling getInstance() for the
   * first time, and before calling print(Stat s).
   *
   * @param header a String for the CSV header
   */
  private void printHeader(String header) {
    pw.println(header);
  }

  /** Close the printWriter object. It must be called once the test is over. */
  public void close() {
    try {
      if (pw != null) {
        logger.debug("Closing " + filename);
        pw.close();
        fout.close();
      }
    } catch (Exception e) {
      logger.error("\r\n" + Utility.getStackTrace(e));
    }
  }

  /**
   * Translate the JsonObject into a Map<String,Object> where Object is either the json value or
   * another Map<String, Object>. String is always the json key.
   *
   * @param json the JsonObject
   * @return Map<String key, Object: either json value or another Map<String, Object>
   * @throws JsonException
   */
  private static Map<String, Object> jsonToMap(JsonObject json) throws JsonException {
    Map<String, Object> retMap = new LinkedHashMap<String, Object>();
    keyValMap = new LinkedHashMap<String, String>(); // re-initialise it in case.
    if (json != JsonObject.NULL) {
      retMap = toMap(json, "");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("jsonToMap() dump");
      for (String key : keyValMap.keySet()) {
        logger.debug("keyList[" + key + "] = " + keyValMap.get(key));
      }
    }
    return retMap;
  }

  /**
   * Translate the JsonObject into a flat Map<String,String> of key - value pairs For nested
   * objects, the key becomes parentkey.key, to achieve the flat Map.
   *
   * @param json the JsonObject
   * @return Map<String key, Object: either json value or another Map<String, Object>
   * @throws JsonException
   */
  private static Map<String, String> jsonToHashMap(JsonObject json) throws JsonException {
    Map<String, Object> retMap = new LinkedHashMap<String, Object>();
    keyValMap = new LinkedHashMap<String, String>(); // re-initialise it in case.
    StringBuilder keyBuilder = new StringBuilder("");
    if (json != JsonObject.NULL) {
      retMap = toMap(json, "");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("jsonToHashMap() dump");
      for (String key : keyValMap.keySet()) {
        logger.debug("keyList[" + key + "] = " + keyValMap.get(key));
      }
    }
    return keyValMap;
  }

  /**
   * Recursively browse the jsonObject and returns a Map<String key, Object: either json value or
   * another map
   *
   * @param object JsonObject
   * @param parent json key of the parent json node.
   * @return Map<String key, Object: either json value or another Map<String, Object>
   * @throws JsonException
   */
  private static Map<String, Object> toMap(JsonObject object, String parent) throws JsonException {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    Iterator<String> keysItr = object.keySet().iterator();
    while (keysItr.hasNext()) {
      String key = keysItr.next();
      Object value = object.get(key);
      if (value instanceof JsonArray) {
        value = toList((JsonArray) value, key);
      } else if (value instanceof JsonObject) {
        value = toMap((JsonObject) value, key);
      } else {
        String keyFull = parent + (parent.length() > 0 ? "." : "") + key;
        keyValMap.put(keyFull, value.toString());
      }
      map.put(key, value);
    }
    return map;
  }

  /**
   * Recursively browse the jsonObject and returns a List<Object1> where Object is either a
   * List<Object> or another Map<String, Object> (see toMap)
   *
   * @param array JsonArray
   * @param parent json key of the parent json node.
   * @return List<Object1> where Object is either a List<Object> or another Map<String, Object> (see
   *     toMap)
   * @throws JsonException
   */
  private static List<Object> toList(JsonArray array, String parent) throws JsonException {
    List<Object> list = new ArrayList<Object>();
    for (int i = 0; i < array.size(); i++) {
      Object value = array.get(i);
      parent = parent + "[" + i + "]";
      if (value instanceof JsonArray) {
        value = toList((JsonArray) value, parent);
      } else if (value instanceof JsonObject) {
        value = toMap((JsonObject) value, parent);
      }
      list.add(value);
    }
    return list;
  }


  /**
   * Gets the successful candidate pair (state = succeed)
   *
   * @param jsonObject of the successful candidate pair
   * @return
   */
  private static JsonObject getSuccessfulCandidate(JsonObject jsonObject) {
    JsonObject candObj = jsonObject.getJsonObject("candidate-pair");
    for (String key: candObj.keySet()) {
      JsonObject o = candObj.getJsonObject(key);
      if ("succeeded".equals(o.getString("state"))) {
        return o;
      }
    }
    for (String key: candObj.keySet()) {
      //sometimes there are no "succeeded" pair, but the "in-progress" with
      //a valid currentRoundTripTime value looks just fine.
      JsonObject o = candObj.getJsonObject(key);
      if ("in-progress".equals(o.getString("state")) &&
              !"NA".equals(o.getString("currentRoundTripTime"))) {
        return o;
      }
    }
    return null;
  }


  /**
   * Gets the successful
   *
   * @param jsonObject of the stats
   * @return
   */
  private static JsonObject getRTCStats(JsonObject jsonObject, String stats, String mediaType) {
    JsonObject myObj = jsonObject.getJsonObject(stats);
    for (String key: myObj.keySet()) {
      JsonObject o = myObj.getJsonObject(key);
      if (mediaType.equals(o.getString("mediaType"))) {
        return o;
      }
    }
    return null;
  }


  private static final String[] candidatePairStats = {"bytesSent", "bytesReceived", "currentRoundTripTime", "totalRoundTripTime", "timestamp"};
  private static final String[] inboundStats = {"bytesReceived", "packetsReceived", "packetsLost", "jitter", "timestamp"};
  private static final String[] outboundStats = {"bytesSent", "timestamp"};

  /**
   * Build a simple JsonObject of selected stats meant to test NW Instrumentation.
   * Stats includes bitrate, packetLoss, Jitter and RTT
   *
   * @param obj
   * @return
   */
  public static JsonObjectBuilder extractStats(JsonObject obj) {
    JsonObjectBuilder mainBuilder = Json.createObjectBuilder();
    JsonArray jsonArray = obj.getJsonObject("stats").getJsonArray("statsArray");
    int noStats = 0;
    if (jsonArray != null) {
      noStats = jsonArray.size();
      for (int i = 0; i < noStats; i++) {
        mainBuilder.add("candidate-pair_" + i, getStatsJsonBuilder(jsonArray.getJsonObject(i), candidatePairStats, "candidate-pair", ""));
        mainBuilder.add("inbound-audio_" + i, getStatsJsonBuilder(jsonArray.getJsonObject(i), inboundStats, "inbound-rtp", "audio"));
        mainBuilder.add("inbound-video_" + i, getStatsJsonBuilder(jsonArray.getJsonObject(i), inboundStats, "inbound-rtp", "video"));
        mainBuilder.add("outbound-audio_" + i, getStatsJsonBuilder(jsonArray.getJsonObject(i), outboundStats, "outbound-rtp", "audio"));
        mainBuilder.add("outbound-video_" + i, getStatsJsonBuilder(jsonArray.getJsonObject(i), outboundStats, "outbound-rtp", "video"));
      }
    } else {
      logger.error(
          "statsArray is null \r\n ---------------\r\n"
              + obj.toString()
              + "\r\n ---------------\r\n");
    }
    JsonObject result = mainBuilder.build();
    JsonObjectBuilder csvBuilder = Json.createObjectBuilder();
    csvBuilder.add("currentRoundTripTime (ms)", computeRoundTripTime(result, noStats, "current"));
    csvBuilder.add("totalRoundTripTime (ms)", computeRoundTripTime(result, noStats, "total"));
    csvBuilder.add("totalBytesReceived (Bytes)", totalBytes(result, noStats, "Received"));
    csvBuilder.add("totalBytesSent (Bytes)", totalBytes(result, noStats, "Sent"));
    csvBuilder.add("avgSentBitrate (bps)", computeBitrate(result, noStats, "Sent", "candidate-pair"));
    csvBuilder.add("avgReceivedBitrate (bps)", computeBitrate(result, noStats, "Received", "candidate-pair"));
    csvBuilder.add("inboundAudioBitrate (bps)", computeBitrate(result, noStats, "in", "audio"));
    csvBuilder.add("inboundVideoBitrate (bps)", computeBitrate(result, noStats, "in", "video"));
    csvBuilder.add("outboundAudioBitrate (bps)", computeBitrate(result, noStats, "out", "audio"));
    csvBuilder.add("outboundVideoBitrate (bps)", computeBitrate(result, noStats, "out", "video"));
    csvBuilder.add("audioJitter (ms)", computeAudioJitter(result, noStats));
    csvBuilder.add("audioPacketsLoss (%)", computePacketsLoss(result, noStats, "audio"));
    csvBuilder.add("videoPacketsLoss (%)", computePacketsLoss(result, noStats, "video"));
    //uncomment the following line to add the detailed stats to the CSV
//    csvBuilder.add("stats", result);
    return csvBuilder;
  }




  /**
   * Computes the average bitrate.
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @param direction "in" or "out" or "Sent" or "Received"
   * @param mediaType "audio", "video" or "candidate-pair"
   * @return totalNumberBytes sent or received
   */
  private static String computeBitrate(JsonObject jsonObject, int noStats, String direction, String mediaType) {
    long bytesStart = 0;
    long bytesEnd = 0;
    long tsStart = 0;
    long tsEnd = 0;
    long avgBitrate = 0;
    try {
      if (noStats < 2) {
        return "Error: less than 2 stats";
      }
      String jsonObjName = getJsonObjectName(direction, mediaType);
      String jsonKey = getJsonKey(direction);
      boolean debug = false;
      if (debug) {
        logger.info("-----------------------------");
        logger.info(" jsonKey:     \" + jsonKey);");
      }
      for (int i = 0; i < noStats; i++) {
        String s = jsonObject.getJsonObject(jsonObjName + i).getString(jsonKey);
        if (s != null && !"NA".equals(s) && isLong(s)) {
          long b = Long.parseLong(s);
          bytesStart = (bytesStart == 0 || b < bytesStart) ? b : bytesStart;
          bytesEnd = (bytesEnd == 0 || b > bytesEnd) ? b : bytesEnd;
        }
        String ts = jsonObject.getJsonObject(jsonObjName + i).getString("timestamp");
        if (ts != null && !"NA".equals(ts) && isLong(ts)) {
          long b = Long.parseLong(ts);
          if (i == 0) {
            tsStart = b;
          }
          if (i == noStats - 1) {
            tsEnd = b;
          }
        }
        if (debug) {
          logger.info("jsonObjName: " + jsonObjName + i);
          logger.info("jsonKey:     " + jsonKey);
          logger.info("bytesEnd:   " + bytesEnd);
          logger.info("bytesStart: " + bytesStart);
          logger.info("tsEnd:   " + tsEnd);
          logger.info("tsStart: " + tsStart);
        }
      }

      if (tsEnd != tsStart) {
        long timediff = (tsEnd - tsStart);
        avgBitrate = (8000 * (bytesEnd - bytesStart)) / timediff;
        avgBitrate = (avgBitrate < 0) ? avgBitrate * -1 : avgBitrate;
        if (debug) {
          logger.info(
              "computeBitrate()(8000 * ( " + bytesEnd + " - " + bytesStart + " )) /" + timediff);
        }
        return "" + (avgBitrate);
      } else {
        logger.error("computeBitrate() tsEnd == tsStart : " + tsEnd + " , " + tsStart);
      }
    } catch (NullPointerException npe) {
      logger.error("NullPointerException in computeBitrate");
      logger.error("" + Utility.getStackTrace(npe));
    }
    return "";
  }


  /**
   *
   * @param direction "in" or "out" or "Sent" or "Received"
   * @param mediaType "audio", "video" or "candidate-pair"
   * @return "candidate-pair_" or "inbound-audio_" or "inbound-video_" or "outbound-audio_" or "outbound-video_"
   */
  private static String getJsonObjectName(String direction, String mediaType) {
    if ("candidate-pair".equals(mediaType)) {
      return "candidate-pair_";
    }
    //else  "inbound-audio_"
    return direction + "bound-" + mediaType + "_";
  }


  /**
   *
   * @param direction "in" or "out" or "Sent" or "Received"
   * @return bytesSent or bytesReceived
   */
  private static String getJsonKey(String direction) {
    if ("Sent".equals(direction) || "out".equals(direction)) {
      return "bytesSent";
    }
    if ("Received".equals(direction) || "in".equals(direction)) {
      return "bytesReceived";
    }
    return null;
  }

  /**
   *  Computes the average roundTripTime
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @return the average of valid (> 0) "totalRoundTripTime"
   */
  private static String computeRoundTripTime(JsonObject jsonObject, int noStats, String prefix) {
    double rtt = 0;
    int ct = 0;
    try {
      for (int i = 0; i < noStats; i++) {
        String s = jsonObject.getJsonObject("candidate-pair_" + i).getString(prefix + "RoundTripTime");
        if (s != null && !"NA".equals(s) && !"0".equals(s) && isDouble(s)) {
          rtt += 1000 * Double.parseDouble(s);
          ct++;
        }
      }
    } catch (NullPointerException npe) {
      logger.error("Unable to find " + prefix + "RoundTripTime in the stats. ");
      logger.error("" + Utility.getStackTrace(npe));
    }
    if (ct > 0) {
      return "" + ((int)rtt/ct);
    }
    return "";
  }

  /**
   *  Computes the average audioJitter
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @return the average "audioJitter"
   */
  private static String computeAudioJitter(JsonObject jsonObject, int noStats) {
    double jitter = 0;
    int ct = 0;
    if (noStats < 2) return ""; //min two stats
    try {
      for (int i = 0; i < noStats; i++) {
        JsonObject myObject = jsonObject.getJsonObject("inbound-audio_" + i);
        if (myObject != null) {
          String s = myObject.getString("jitter");
          if (s != null && !"NA".equals(s) && isDouble(s)) {
            jitter += (1000 * Double.parseDouble(s));
            ct++;
          }
        }
      }
      if (ct > 0) {
        return "" + (jitter/ct);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * Computes the packet losses as a % packetLost/total packets
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @param mediaType "audio" or "video"
   * @return the packet losses (% packetLost/total packets)
   */
  private static String computePacketsLoss(JsonObject jsonObject, int noStats, String mediaType) {
    if (noStats < 1) return ""; // min one stats
    try {
      JsonObject myObject = jsonObject.getJsonObject("inbound-" + mediaType + "_" + (noStats - 1));
      if (myObject != null) {
        String s = myObject.getString("packetsReceived");
        String l = myObject.getString("packetsLost");
        if (s != null && !"NA".equals(s) && isLong(s)
                && l != null && !"NA".equals(l) && isLong(l)) {
          long packetsLost = Long.parseLong(l);
          long totalPackets = Long.parseLong(s) + packetsLost;
          if (totalPackets > 0) {
            double packetLoss = packetsLost / totalPackets;
            return "" + (new DecimalFormat("#0.000").format(packetLoss));
          }
        } else {
            logger.error(
                    "computePacketsLoss  \r\n ---------------\r\n"
                            + myObject.toString()
                            + "\r\n ---------------\r\n");
        }
      } else {
          logger.error(
                  "computePacketsLoss  my object is null " + ("inbound-" + mediaType + "_" + (noStats - 1)));

      }
    } catch (Exception e) {
      logger.error("" + Utility.getStackTrace(e));
    }
    return "";
  }

  /**
   * Computes the total bytes sent or received by the candidate
   *
   * @param jsonObject object containing the list getStats result.
   * @param noStats how many stats in jsonObject
   * @param direction Sent or Received
   * @return totalNumberBytes sent or received
   */
  private static String totalBytes(JsonObject jsonObject, int noStats, String direction) {
    long bytes = 0;
    try {
      for (int i = 0; i < noStats; i++) {
        String s = jsonObject.getJsonObject("candidate-pair_" + i).getString("bytes" + direction);
        if (s != null && !"NA".equals(s) && isLong(s)) {
          long b = Long.parseLong(s);
          bytes = Math.max(b, bytes);
        }
      }
    } catch (NullPointerException npe) {
      logger.error("Unable to find \"bytes" + direction + "\" in the stats. ");
      logger.error("" + Utility.getStackTrace(npe));
    }
    return "" + bytes;
  }


  private static JsonObjectBuilder getStatsJsonBuilder(JsonObject jsonObject, String[] stringArray, String stats, String mediaType) {
    JsonObjectBuilder subBuilder = Json.createObjectBuilder();
    if ("candidate-pair".equals(stats)) {
      JsonObject successfulCandidate = getSuccessfulCandidate(jsonObject);
      if (successfulCandidate != null) {
        for (int j = 0; j < stringArray.length; j++) {
          subBuilder.add(stringArray[j], successfulCandidate.getString(stringArray[j]));
        }
      }
    } else {
      JsonObject myObj = getRTCStats(jsonObject, stats, mediaType);
      if (myObj != null) {
        for (int j = 0; j < stringArray.length; j++) {
          subBuilder.add(stringArray[j], myObj.getString(stringArray[j]));
        }
      }
    }
    return subBuilder;
  }


  /**
   *  Checks if a String is a double
   *
   * @param s the String to check
   * @return true if the String is a double
   */
  private static boolean isDouble(String s) {
    try {
      Double.parseDouble(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   *  Checks if a String is a long
   *
   * @param s the String to check
   * @return true if the String is a long
   */
  private static boolean isLong(String s) {
    try {
      Long.parseLong(s);
      return true;
    } catch (Exception e) {
      return false;
    }
  }



}
