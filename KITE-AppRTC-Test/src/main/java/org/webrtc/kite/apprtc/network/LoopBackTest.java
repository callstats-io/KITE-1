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
import org.openqa.selenium.remote.RemoteWebDriver;
import org.webrtc.kite.KiteTest;
import org.webrtc.kite.apprtc.Utility;

import javax.json.Json;
import java.util.Random;

/**
 * LoopBackTest implementation of KiteTest.
 * <p>
 * The testScript() implementation does the following in sequential manner on the provided array of
 * WebDriver:
 * <ul>
 * <li>1) Opens all the browsers with the url specified in APPRTC_URL.</li>
 * <li>2) Clicks 'confirm-join-button'.</li>
 * <li>3) Do the following every 1 second for 1 minute:</li>
 * <ul>
 * <li>a) Executes the JavaScript on the browser given via getIceConnectionScript() which returns
 * iceConnectionState.</li>
 * <li>b) Checks whether the browser has returned either 'completed' or 'connected'.</li>
 * <li>c) Executes the JavaScript on the given via stashStatsScript() which store stats
 * in a global variable to fetch later.</li>
 * stashed earlier.</li>
 * </ul>
 * <li>4) The test is considered as successful if all the browsers either returns 'completed' or
 * 'connected' within 1 minute.</li>
 * <li>5) A successful test returns a boolean 'true' while the unsuccessful test returns a boolean
 * 'false'.</li>
 * </ul>
 * </p>
 */
public class LoopBackTest extends KiteTest {

  private final static Logger logger = Logger.getLogger(LoopBackTest.class.getName());

  private final static String APPRTC_URL = "https://appr.tc/r/";
  private final String value = "loopback";
  private final String option = "debug=" + value;
  private final static int TIMEOUT = 60000;
  private final static int INTERVAL = 1000;
  private final static String RESULT_SUCCESSFUL = "SUCCESSFUL";
  private final static String RESULT_FAILED = "FAILED";
  private WebDriver webDriver;
  private String alertMsg, browser, result;
  private String message = "No problem detected.";

   /**
   * Opens the APPRTC_URL and clicks 'confirm-join-button'.
   */
  private void takeAction() throws Exception {
    Random rand = new Random(System.currentTimeMillis());
    long channel = Math.abs(rand.nextLong());
    if (this.getWebDriverList().size() > 1) {
      throw new Exception("This test is limited to 1 browser only");
    }
    webDriver = this.getWebDriverList().get(0);
    Capabilities capabilities = ((RemoteWebDriver) webDriver).getCapabilities();
    browser = capabilities.getBrowserName() + "_" + capabilities.getVersion() +"_" + capabilities.getPlatform();
    webDriver.get(APPRTC_URL + channel + "?" + option);
    try {
      Alert alert = webDriver.switchTo().alert();
      alertMsg = alert.getText();
      if (alertMsg != null) {
        alertMsg =
                ((RemoteWebDriver) webDriver).getCapabilities().getBrowserName()
                        + " alert: "
                        + alertMsg;
        alert.accept();
      }
    } catch (NoAlertPresentException e) {
      alertMsg = null;
    } catch (ClassCastException e) {
      alertMsg = " Cannot retrieve alert message due to alert.getText() class cast problem";
      webDriver.switchTo().alert().accept();
    }
    webDriver.findElement(By.id("confirm-join-button")).click();
  }

  @Override
  public Object testScript() throws Exception {
    this.takeAction();
    boolean everythingOK;
    everythingOK = Utility.checkIceConnectionState(webDriver, TIMEOUT, INTERVAL);

    if (everythingOK){
      everythingOK = Utility.checkVideoDisplay(webDriver, TIMEOUT, INTERVAL);
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

    return Utility.developResult(browser, result, Json.createObjectBuilder(), message, alertMsg, Json.createArrayBuilder()).toString();
  }

}