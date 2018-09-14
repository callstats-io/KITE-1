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

package org.webrtc.kite.apprtc.network;

import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.webrtc.kite.KiteTest;
import org.webrtc.kite.apprtc.StatsUtils;
import org.webrtc.kite.apprtc.Utility;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static org.webrtc.kite.apprtc.Utility.alertHandling;

/**
 * IceConnectionTest implementation of KiteTest.
 * <p>
 * The testScript() implementation does the following in sequential manner on the provided array of
 * WebDriver:
 * <ul>
 * <li>1) Opens all the browsers with the url specified in APPRTC_URL.</li>
 * <li>2) Clicks 'confirm-join-button'.</li>
 * <li>3) Do the following every 1 second for 1 minute:</li>
 * <ul>
 * <li>a) Executes the JavaScript on all browsers given via getIceConnectionStateScript() which returns
 * iceConnectionState.</li>
 * <li>b) Checks whether all the browsers have returned either 'completed' or 'connected'.</li>
 * </ul>
 * <li>4) The test is considered as successful if all the browsers either returns 'completed' or
 * 'connected' within 1 minute.</li>
 * <li>5) A successful test returns a boolean 'true' while the unsuccessful test returns a boolean
 * 'false'.</li>
 * </ul>
 * </p>
 */
public class IceConnectionTest extends KiteTest {

  private static final Logger logger = Logger.getLogger(IceConnectionTest.class.getName());

  private final static String APPRTC_URL = "https://appr.tc/r/";
  private final static String RESULT_TIMEOUT = "TIME OUT";
  private final static String RESULT_SUCCESSFUL = "SUCCESSFUL";
  private final static String RESULT_FAILED = "FAILED";

  private boolean printToCSV = false;
  private boolean printToJson = false;

  private int timeout = 60;
  private int statsCollectionTime = 10;
  private int interval = 1;
  private int statsCollectionInterval = 1;
  private JsonArray selectedStats = null;
  private String commandName = "";

  /**
   * Process the payload from the json config file.
   */
  private void payloadHandling() {
    JsonObject jsonPayload = (JsonObject) this.getPayload();
    if (jsonPayload != null) {
      printToJson = jsonPayload.getBoolean("printToJson", printToJson);
      printToCSV = jsonPayload.getBoolean("printToCSV", printToCSV);
      timeout = jsonPayload.getInt("timeout", timeout);
      interval = jsonPayload.getInt("interval", interval);
      statsCollectionTime = jsonPayload.getInt("statsCollectionTime", statsCollectionTime);
      statsCollectionInterval = jsonPayload.getInt("statsCollectionInterval", statsCollectionInterval);
      selectedStats = jsonPayload.getJsonArray("selectedStats");
    }
  }

  public void setPrintToCSV(boolean printToCSV) {
    this.printToCSV = printToCSV;
  }

  public void setPrintToJson(boolean printToJson) {
    this.printToJson = printToJson;
  }

  /**
   * Opens the APPRTC_URL and clicks 'confirm-join-button'.
   */
  private JsonObject takeAction() throws InterruptedException {
    JsonObjectBuilder res = Json.createObjectBuilder();
    JsonObjectBuilder stats = Json.createObjectBuilder();
    List<JsonObject> browserResults = new ArrayList<>();
    List<String> resultList = new ArrayList<>();
    this.commandName = this.getCommandName() != null ? this.getCommandName() : "";

    List<WebDriver> webDriverList = this.getWebDriverList();

    Random rand = new Random(System.currentTimeMillis());
    long channel = Math.abs(rand.nextLong());


    if (commandName != null) {
      logger.error("NW Instrumentation command: " + this.commandName);
    }
    List<Tester> testerList = new ArrayList<>();
    for (WebDriver webDriver : webDriverList) {
      webDriver.get(APPRTC_URL + channel);
      testerList.add(new Tester(webDriver));
    }
    try {
      ExecutorService executorService = Executors.newFixedThreadPool(webDriverList.size());
      List<Future<JsonObject>> futureList = executorService.invokeAll(testerList, 2, TimeUnit.MINUTES);
      executorService.shutdown();

      for (Future<JsonObject> future : futureList) {
        try {
          JsonObject jsonObject = future.get();
          browserResults.add(jsonObject);
        } catch (Exception e) {
          browserResults.add(null);
          logger.error("Exception in Tester: " + e.getLocalizedMessage() + "\r\n" + Utility.getStackTrace(e));
        }
      }
    } catch(TimeoutException e) {
      res.add("result", RESULT_TIMEOUT).add("log", Json.createArrayBuilder());
    } catch (InterruptedException e) {
      logger.error("Error while running Tester threads.");
    }
    for (int i = 0; i < browserResults.size() ; i++){
      JsonObject browserResult = browserResults.get(i);
      if (browserResult == null){
        resultList.add(RESULT_FAILED);
      } else {
        resultList.add(browserResult.getString("result", RESULT_FAILED));
        String browser =  browserResult.getString("browser", "client_"+(i+1));
        browser = (i+1) + "_" + browser;
        stats.add(browser, browserResult.getJsonObject("stats"));
      }
    }
    if (resultList.contains(RESULT_FAILED)){
      res.add("result", RESULT_FAILED);
    } else {
      res.add("result", RESULT_SUCCESSFUL);
    }
    res.add("getStats", stats);
    return res.build();
  }


  @Override
  public Object testScript() throws Exception {
    payloadHandling();
    JsonObject res = this.takeAction();
    return res.toString();
  }




  private class Tester implements Callable<JsonObject> {
    private final WebDriver webDriver;
    private String alertMsg, browser, result;
    private String message = "No problem detected.";
    JsonObjectBuilder stats = Json.createObjectBuilder();

    public Tester(WebDriver webDriver){
      this.webDriver = webDriver;
      Capabilities capabilities = ((RemoteWebDriver) this.webDriver).getCapabilities();
      this.browser = capabilities.getBrowserName() + "_" + capabilities.getVersion() +"_" + capabilities.getPlatform();
    }

    @Override
    public JsonObject call() throws Exception {
      JsonObjectBuilder res = Json.createObjectBuilder();
      res.add("browser", browser);
      boolean everythingOK;
      alertMsg = alertHandling(webDriver);
      webDriver.findElement(By.id("confirm-join-button")).click();

      everythingOK = Utility.checkIceConnectionState(webDriver, timeout, interval);

      if (everythingOK){
        everythingOK = Utility.checkVideoDisplay(webDriver, timeout, interval);
        if (!everythingOK){
          message = "Video not playing";
          result = RESULT_FAILED;
        } else {
          result = RESULT_SUCCESSFUL;
        }
      } else {
        message = "Failed to establish ICE connection";
        result = RESULT_FAILED;
      }
      //wait 2s before getting stats
      stats = Utility.getStatOvertime(webDriver, statsCollectionTime, statsCollectionInterval, selectedStats);


      JsonObject jsonObj =
              Utility.developResult(browser, result, stats, message, alertMsg, Utility.getLog(webDriver));
      String jsonStr = jsonObj.toString();
      StatsUtils ts = StatsUtils.getInstance("");
      if (printToJson) ts.printJsonTofile("verify", jsonStr, "results/");
      if (true || printToCSV) {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("browser", browser);
        jsonBuilder.add("commandName", commandName);
        jsonBuilder.addAll(StatsUtils.extractStats(jsonObj));
        ts.println(jsonBuilder.build(), "results/");
      }
      return jsonObj;


    }
  }

}
