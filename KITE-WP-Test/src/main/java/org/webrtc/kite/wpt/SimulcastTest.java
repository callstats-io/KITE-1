/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.webrtc.kite.wpt;

import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.webrtc.kite.KiteTest;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.Arrays;
import java.util.List;

import static org.webrtc.kite.wpt.Utility.alertHandling;
import static org.webrtc.kite.wpt.Utility.checkVideoDisplay;

/**
 * SimulcastTest implementation of KiteTest.
 *
 * <p>The testScript() implementation does the following in sequential manner on the provided
 * WebDriver:
 *
 * <ul>
 *   <li>1) <li>1) Opens all the browsers with the url specified in config file.
 *   <li>3) Do the following every 1 second for 1 minute:
 *       <ul>
 *         <li>a) Executes the JavaScripts on the browser given
 *         <li>b) Checks whether the peer connection was created
 *         <li>c) Checks whether the sdp offer is correct
 *         <li>d) Checks whether the sdp answer is correct
 *         <li>e) Checks whether the video stream echoed from peer connection is actually being
 *             displayed
 *         <li>f) Checks whether the original video stream received back from sfu is actually being
 *             displayed
 *         <li>g) Checks whether the half video stream received back from sfu is actually being
 *             displayed
 *         <li>h) Checks whether the quarter video stream received back from sfu is actually being
 *             displayed
 *       </ul>
 *   <li>4) The test is considered as successful if all the verification return ok
 * </ul>
 */
public class SimulcastTest extends KiteTest {

  private static final Logger logger = Logger.getLogger(SimulcastTest.class.getName());
  private static final String RESULT_SUCCESSFUL = "SUCCESSFUL";
  private static final String RESULT_FAILED = "FAILED";
  private static final int TIMEOUT = 30000;
  private static final int INTERVAL = 1000;

  private static String url = null;
  private static String IP = "localhost";
  private static int port = 8443;
  private static WebDriver webDriver;
  private static String browser;
  private static String alertMsg = null;

  /**
   * Returns the test's getSDPOfferScript to retrieve simulcast.pc.localDescription.sdp or
   * simulcast.pc.remoteDescription.sdp. If it doesn't exist then the method returns 'unknown'.
   *
   * @param local boolean
   * @return the getSDPOfferScript as string.
   */
  private static final String getSDPOfferScript(boolean local) {
    if (local) {
      return "var SDP;"
          + "try {SDP = simulcast.pc.localDescription.sdp;} catch (exception) {} "
          + "if (SDP) {return SDP;} else {return 'unknown';}";
    } else {
      return "var SDP;"
          + "try {SDP = simulcast.pc.remoteDescription.sdp;} catch (exception) {} "
          + "if (SDP) {return SDP;} else {return 'unknown';}";
    }
  }

  /**
   * Returns the test's checkPeerConnectionExistScript
   *
   * @return the string format of a boolean value returned from the JS console.
   */
  private static final String checkPeerConnectionExistScript() {
    return "var pc;"
        + "try {pc = simulcast.pc} catch (exception) {} "
        + "if (pc) {return true;} else {return false;}";
  }

  /**
   * Restructuring the test according to options given in payload object from config file. This
   * function will not be the same for every test.
   */
  private void payloadHandling() {
    if (this.getPayload() != null) {
      JsonValue jsonValue = this.getPayload();
      JsonObject payload = (JsonObject) jsonValue;
      url = payload.getString("url", null);
      if (url == null) {
        IP = payload.getString("ip", "localhost");
        port = payload.getInt("port", 8083);
      }
    }
  }

  /**
   * Validate whether the sdp message is in the right format for simulcast
   *
   * @param sdp message to validate
   * @return RESULT_SUCCESSFUL or error message of the validation.
   */
  private String getAndValidateSDP(String sdp) {
    String res = RESULT_SUCCESSFUL;
    int simucastLineCount = 0;

    String source = null;
    List<String> lines = Arrays.asList(sdp.split("\n"));
    for (String line : lines) {
      if (line.startsWith("a=simulcast")) {
        simucastLineCount += 1;
        if (simucastLineCount > 1) {
          // More than 1 simulcast declaration line
          res = "More than 1 simulcast declaration";
          break;
        }
        if (line.contains("send")) {
          if (Arrays.asList(line.split("send")).size() > 2) {
            // Checks if "send" appears more than once
            res = "send direction appears more than once";
            break;
          }
        }
        if (line.contains("recv")) {
          if (Arrays.asList(line.split("recv")).size() > 2) {
            // Checks if "recv" appears more than once
            res = "recv direction appears more than once";
            break;
          }
        }
      }
      if (line.startsWith("a=ssrc")) {
        if (line.contains("cname")) {
          if (source == null) source = line.split("cname:")[1];
          if (!line.split("cname:")[1].equalsIgnoreCase(source)) {
            // More than 1 source
            res = "There are more than one source";
            break;
          }
        }
      }
      if (line.startsWith("a=rid")) {
        if (line.contains("send")) {
          if (Arrays.asList(line.split("send")).size() > 2) {
            // Checks if "send" appears more than once
            res = "send direction appears more than once";
            break;
          }
        }
        if (line.contains("recv")) {
          if (Arrays.asList(line.split("recv")).size() > 2) {
            // Checks if "recv" appears more than once
            res = "recv direction appears more than once";
            break;
          }
        }
      }
    }
    return res;
  }

  /**
   * @return whether the peer connection exists
   * @throws InterruptedException
   */
  private boolean checkPeerConnectionObject() throws InterruptedException {
    for (int i = 0; i < TIMEOUT; i += INTERVAL) {
      boolean pcExist =
          (boolean)
              ((JavascriptExecutor) this.webDriver).executeScript(checkPeerConnectionExistScript());
      if (!pcExist) {
        logger.info(browser + ": looking for pc");
        Thread.sleep(INTERVAL);
      } else {
        logger.info(browser + ": found pc");
        return true;
      }
    }
    return false;
  }

  /**
   * Gets SDP from peer connection and verifies whether it's simulcast
   *
   * @param local indicates local or remote description.
   * @return JsonObject with result.
   * @throws InterruptedException
   */
  private JsonObject getAndValidateSDP(boolean local) throws InterruptedException {
    JsonObjectBuilder res = Json.createObjectBuilder();
    String validation;
    if (local) {
      validation = "Could not get local SDP from peer connection";
    } else {
      validation = "Could not get remote SDP from peer connection";
    }
    for (int i = 0; i < TIMEOUT; i += INTERVAL) {
      String SDP = (String) ((JavascriptExecutor) webDriver).executeScript(getSDPOfferScript(true));
      if (SDP.equalsIgnoreCase("unknown")) {
        Thread.sleep(INTERVAL);
      } else {
        validation = getAndValidateSDP(SDP);
        if (validation.equalsIgnoreCase(RESULT_SUCCESSFUL)) {
          res.add("validation", true);
        } else {
          res.add("validation", false);
          res.add("message", validation);
        }
        return res.build();
      }
    }
    res.add("validation", false);
    res.add("message", validation);
    return res.build();
  }

  /**
   * Develops a result that conforms to dashboard result format.
   *
   * @param result test result
   * @param log more detail on test result.
   * @return JsonObject
   */
  private String developResult(String result, String log) {
    JsonObjectBuilder res = Json.createObjectBuilder();
    JsonObjectBuilder stats = Json.createObjectBuilder();
    stats.add("log", log);
    res.add("result", result);
    res.add("stats", stats);
    return res.build().toString();
  }

  /**
   * Opens the URL, fills channelId input and clicks startButton
   *
   * @return true if all browsers are ready for calls, false otherwise
   */
  private void takeAction() throws Exception {
    payloadHandling();
    if (this.getWebDriverList().size() > 1) {
      throw new Exception("This test is limited to 1 browser only");
    }
    webDriver = getWebDriverList().get(0);
    Capabilities capabilities = ((RemoteWebDriver) webDriver).getCapabilities();
    browser =
        capabilities.getBrowserName()
            + "_"
            + capabilities.getVersion()
            + "_"
            + capabilities.getPlatform();
    if (url == null) {
      url =
          new StringBuilder("https://").append(IP).append(":").append(port).append("/").toString();
    }
    webDriver.get(url);
    alertMsg = alertHandling(webDriver);
  }

  @Override
  public Object testScript() throws Exception {
    this.takeAction();
    JsonObject validation;
    boolean everythingOK;

    // Looking for peer connection object

    everythingOK = checkPeerConnectionObject();
    if (!everythingOK) {
      return developResult(RESULT_FAILED, "Peer connection was not created.");
    } else {
      validation = getAndValidateSDP(true);
      everythingOK = validation.getBoolean("validation", false);
    }

    if (!everythingOK) {
      return developResult(RESULT_FAILED, validation.getString("message"));
    } else {
      validation = getAndValidateSDP(false);
      everythingOK = validation.getBoolean("validation", false);
    }

    if (!everythingOK) {
      return developResult(RESULT_FAILED, validation.getString("message"));
    } else {
      // Checking echo video from pc
      everythingOK = checkVideoDisplay(webDriver, 1, TIMEOUT, INTERVAL);
    }

    if (!everythingOK) {
      return developResult(RESULT_FAILED, "Echo stream not playing");
    } else {
      // Checking return streams from pc
      everythingOK = checkVideoDisplay(webDriver, 2, TIMEOUT, INTERVAL);
    }

    if (!everythingOK) {
      return developResult(RESULT_FAILED, "No return streams from sfu found");
    } else {
      // Checking Half RID video from pc
      everythingOK = checkVideoDisplay(webDriver, 3, TIMEOUT, INTERVAL);
    }

    if (!everythingOK) {
      return developResult(RESULT_FAILED, "Half RID failed to display");
    } else {
      // Checking Quarter RID video from pc
      everythingOK = checkVideoDisplay(webDriver, 4, TIMEOUT, INTERVAL);
    }

    if (!everythingOK) {
      return developResult(RESULT_FAILED, "Quarter RID failed to display");
    }

    return developResult(RESULT_SUCCESSFUL, RESULT_SUCCESSFUL).toString();
  }
}
