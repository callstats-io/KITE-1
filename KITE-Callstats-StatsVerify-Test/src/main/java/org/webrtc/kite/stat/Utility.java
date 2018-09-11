/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.webrtc.kite.stat;

import org.openqa.selenium.Capabilities;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class holding various static methods.
 */
public class Utility {


    public static String getUniqueKeys(List<String> lst) {
        Set<String> st = new HashSet<>();
        for (String val : lst) {
            st.add(val);
        }
        return st.toString();
    }

    /**
     * Create a JsonObjectBuilder Object to eventually build a Json object
     * from data obtained via tests.
     *
     * @param clientStatsBoth of data sent back from test
     * @return JsonObjectBuilder.
     */
    public static JsonObjectBuilder buildClientObject(Object clientStatsBoth, Object clientStatsAudioOnly, Object clientStatsVideoOnly, Object capabilities) {
        JsonObjectBuilder tmpJsonObjectBuilderBoth = Json.createObjectBuilder();

        Map<String, Object> treeMapBoth = (Map<String, Object>) clientStatsBoth;
        for (Map.Entry<String, Object> entry : treeMapBoth.entrySet()) {
            String key = entry.getKey();
            String keyValues = getUniqueKeys((ArrayList) entry.getValue());
            tmpJsonObjectBuilderBoth.add(key, keyValues);
        }

        JsonObjectBuilder tmpJsonObjectBuilderAudioOnly = Json.createObjectBuilder();
        Map<String, Object> treeMapAudioOnly = (Map<String, Object>) clientStatsAudioOnly;
        for (Map.Entry<String, Object> entry : treeMapAudioOnly.entrySet()) {
            String key = entry.getKey();
            String keyValues = getUniqueKeys((ArrayList) entry.getValue());
            tmpJsonObjectBuilderAudioOnly.add(key, keyValues);
        }

        JsonObjectBuilder tmpJsonObjectBuilderVideoOnly = Json.createObjectBuilder();
        Map<String, Object> treeMapVideoOnly = (Map<String, Object>) clientStatsVideoOnly;
        for (Map.Entry<String, Object> entry : treeMapVideoOnly.entrySet()) {
            String key = entry.getKey();
            String keyValues = getUniqueKeys((ArrayList) entry.getValue());
            tmpJsonObjectBuilderVideoOnly.add(key, keyValues);
        }



        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("browserStatsBoth",
                tmpJsonObjectBuilderBoth);

        jsonObjectBuilder.add("browserStatsAudioOnly",
                tmpJsonObjectBuilderAudioOnly);

        jsonObjectBuilder.add("browserStatsVideoOnly",
                tmpJsonObjectBuilderVideoOnly);

        jsonObjectBuilder.add("standardStatsBoth",
                StandardRTCStats.getStandardGetStats(true, 11));
        jsonObjectBuilder.add("standardStatsAudioOnly",
                StandardRTCStats.getStandardGetStats(false, 01));

        jsonObjectBuilder.add("standardStatsVideoOnly",
                StandardRTCStats.getStandardGetStats(false, 10));

        JsonObjectBuilder retval = Json.createObjectBuilder();
        if (capabilities != null && capabilities instanceof Capabilities) {
            Capabilities cap = (Capabilities)capabilities;
            JsonObjectBuilder jbuild = Json.createObjectBuilder();
            jbuild.add("browserName", cap.getBrowserName());
            jbuild.add("version", cap.getVersion());
            jbuild.add("platform", cap.getPlatform().toString());
            retval.add("browser", jbuild);
        }
        retval.add("csioGetStatsDiff", jsonObjectBuilder);
        return retval;
    }

    /**
     * Create a JsonObject to send back to KITE
     *
     * @param audioVideoBoth result map of the test
     * @param audioOnly      result map of the test
     * @param videoOnly      result map of the test
     * @return JsonObject.
     */
    public static JsonObject developResult(Map<String, Object> audioVideoBoth, Map<String, Object> audioOnly, Map<String, Object> videoOnly, int tupleSize) {

        JsonObjectBuilder tmp = Json.createObjectBuilder();
        for (int i = 1; i <= tupleSize; i++) {
            String name = "client_" + i;
            String browser = "browser_" + i;
            if (audioVideoBoth.get(name) != null && audioOnly.get(name) != null) {
                tmp.add(
                    name,
                    Utility.buildClientObject(
                        audioVideoBoth.get(name), audioOnly.get(name), videoOnly.get(name), (Capabilities)audioVideoBoth.get(browser)));
                        }
        }

        return Json.createObjectBuilder()
                .add("result", (String) audioVideoBoth.get("result"))
                .add("stats", tmp).build();
    }

    /**
     * Saves a JSON object into a file, with line breaks and indents.
     *
     * @param testName the name of the test, which will be inlcuded in the file name
     * @param jsonStr the json object as a String.
     * @param path the file path where to save the file.
     */
    public static void printJsonTofile(String testName, String jsonStr, String path) {
        try {
            Map<String, Object> properties = new LinkedHashMap<>(1);
            properties.put(JsonGenerator.PRETTY_PRINTING, true);
            String jsonFilename =
                    testName.replace(" ", "")
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
            e.printStackTrace();
        }
    }


}
