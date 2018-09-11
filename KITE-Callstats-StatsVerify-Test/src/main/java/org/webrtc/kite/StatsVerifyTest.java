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

package org.webrtc.kite;

import org.apache.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.webrtc.kite.stat.Utility;

import java.util.*;

/**
 * Callstats.io Stats verify test of KiteTest.
 *
 * <p>The testScript() implementation does the following in sequential manner on the provided array
 * of WebDriver:
 *
 * <ul>
 *   <li>1) Opens all the browsers with the url specified in APPRTC_URL.
 *   <li>2) Clicks 'confirm-join-button'.
 *   <li>3) Do the following every 1 second for 1 minute:
 *       <ul>
 *         <li>a) Executes the JavaScript on all browsers given via testJavaScript() which returns
 *             iceConnectionState.
 *         <li>b) Checks whether all the browsers have returned either 'completed' or 'connected'.
 *       </ul>
 *   <li>4) The test is considered as successful if all the browsers either returns 'completed' or
 *       'connected' within 1 minute.
 *   <li>5) Collect browser stats using getStats API .
 *   <li>6) A successful test returns a boolean 'true' while the unsuccessful test returns a boolean
 *       'false'.
 * </ul>
 */
public class StatsVerifyTest extends KiteTest {

  private static final Logger logger = Logger.getLogger(StatsVerifyTest.class.getName());

  private static final Map<String, String> expectedResultMap = new HashMap<String, String>();
  private static final String APPRTC_URL = "https://appr.tc/r/";
  private static final int TIMEOUT = 60000;
  private static final int OVERTIME = 3 * 1000;
  private static final int INTERVAL = 1000;
  private static final String RESULT_TIMEOUT = "TIME OUT";
  private static final String RESULT_SUCCESSFUL = "SUCCESSFUL";
  private static final String RESULT_FAILED = "FAILED";
  private static String alertMsg;

  static {
    expectedResultMap.put("completed", "completed");
    expectedResultMap.put("connected", "connected");
  }

  /**
   * Returns the test JavaScript to retrieve appController.call_.pcClient_.pc_.iceConnectionState.
   * If it doesn't exist then the method returns 'unknown'.
   *
   * @return the JavaScript as string.
   */
  private static final String testJavaScript() {
    return "var retValue;"
        + "try {retValue = appController.call_.pcClient_.pc_.iceConnectionState;} catch (exception) {} "
        + "if (retValue) {return retValue;} else {return 'unknown';}";
  }

  /**
   * Returns JavaScript to collect browser stats using getStats() API
   *
   * @return the JavaScript as string.
   */
  private static final String stashStatsScript() {
    return "function getAllStats() {\n"
        + "    return new Promise( (resolve, reject) => {\n"
        + "        try{\n"
        + "            appController.call_.pcClient_.pc_.getStats().then((report) => {\n"
        + "                let statTypes = new Set();\n"
        + "                // type -> stat1, stat2, stat3\n"
        + "                let statTree = new Map();\n"
        + "                for (let stat of report.values()) {\n"
        + "                    const curType = stat.type;\n"
        + "                    const prvStat = statTree.get(curType);\n"
        + "                    if(prvStat) {\n"
        + "                        const _tmp = [...prvStat, stat];\n"
        + "                        statTree.set(curType, _tmp)\n"
        + "                    }else{\n"
        + "                        const _tmp = [stat];\n"
        + "                        statTree.set(curType, _tmp)\n"
        + "                    }\n"
        + "                }\n"
        + "                let retval = {};\n"
        + "                for (const [key, statsArr] of statTree) {\n"
        + "                    let keysArr = [];\n"
        + "                    for(const curStat of statsArr){\n"
        + "                        const keys = Object.keys(curStat);\n"
        + "                        keysArr = [ ...keysArr, ...keys ];\n"
        + "                    }\n"
        + "                    retval[key] = keysArr;\n"
        + "                }\n"
        + "                resolve(retval);\n"
        + "        });\n"
        + "        } catch(err) {\n"
        + "            reject(err);\n"
        + "        }\n"
        + "    });\n"
        + "}\n"
        + "function stashStats() {\n"
        + "    getAllStats().then( (data)=> {\n"
        + "        window.KITEStatsDiff = data;\n"
        + "    }, err => {\n"
        + "        console.log('error',err);\n"
        + "    });\n"
        + "}\n"
        + "stashStats()\n";
  }

  /**
   * Return browser stats
   *
   * @return the JavaScript as string.
   */
  private static final String getStatsScript() {
    return "return window.KITEStatsDiff;";
  }

  /**
   * Opens the APPRTC_URL and clicks 'confirm-join-button'. It will cover three different test
   * scenario audio only 01 video only 10 audio, video 11
   */
  private void takeAction(int testType) {
    Random rand = new Random(System.currentTimeMillis());
    long channel = Math.abs(rand.nextLong());

    for (WebDriver webDriver : this.getWebDriverList()) {
      String testURL = APPRTC_URL + channel + "?";
      testURL += ((testType & (1 << 0)) > 0) ? "audio=true" : "audio=false";
      testURL += "&";
      testURL += ((testType & (1 << 1)) > 0) ? "video=true" : "video=false";

      webDriver.get(testURL);
      try {
        Alert alert = webDriver.switchTo().alert();
        alertMsg = alert.getText();
        if (alertMsg != null) {
          alertMsg =
              ((RemoteWebDriver) webDriver).getCapabilities().getBrowserName()
                  + " alert: "
                  + alertMsg;
          logger.warn(alertMsg);
          alert.accept();
        }
      } catch (NoAlertPresentException e) {
        alertMsg = null;
      }
      webDriver.findElement(By.id("confirm-join-button")).click();
    }
  }

  /**
   * Checks whether all of the result strings match at least one entry from the expectedResultMap.
   *
   * @param resultList ice connection states for the browsers as List<String>
   * @return true if all of the results strings match at least one entry from the expectedResultMap.
   */
  private boolean validateResults(List<String> resultList) {
    for (String result : resultList) if (expectedResultMap.get(result) == null) return false;
    return true;
  }

  /**
   * Checks whether atleast one the result string matches 'failed'.
   *
   * @param resultList ice connection states for the browsers as List<String>
   * @return true if atleast one of the result string matches 'failed'.
   */
  private boolean checkForFailed(List<String> resultList) {
    for (String result : resultList) if (result.equalsIgnoreCase("failed")) return true;
    return false;
  }

  public Map<String, Object> collectTestData() throws Exception {
    String result = RESULT_TIMEOUT;
    Map<String, Object> resultMap = new HashMap<String, Object>();
    for (int i = 0; i < TIMEOUT; i += INTERVAL) {
      List<String> resultList = new ArrayList<String>();
      for (WebDriver webDriver : this.getWebDriverList()) {
        String resultOfScript =
            (String) ((JavascriptExecutor) webDriver).executeScript(this.testJavaScript());
        if (logger.isInfoEnabled()) logger.info(webDriver + ": " + resultOfScript);
        resultList.add(resultOfScript);
      }
      if (this.checkForFailed(resultList)) {
        result = RESULT_FAILED;
        break;
      } else if (this.validateResults(resultList)) {
        result = RESULT_SUCCESSFUL;
        break;
      } else {
        Thread.sleep(INTERVAL);
      }
    }
    // collect stats
    for (int timer = 0; timer < OVERTIME; timer += INTERVAL) {
      int count = 1;
      for (WebDriver webDriver : this.getWebDriverList()) {
        ((JavascriptExecutor) webDriver).executeScript(this.stashStatsScript());
        Thread.sleep(INTERVAL);
        Object stats = ((JavascriptExecutor) webDriver).executeScript(this.getStatsScript());
        Capabilities cap = ((RemoteWebDriver)webDriver).getCapabilities();

        //comment out the following line to remove browser from the JSON object.
        resultMap.put("browser_" + count, cap);


        resultMap.put("client_" + count, stats);
        count += 1;
      }
    }
    resultMap.put("result", result);
    return resultMap;
  }

  @Override
  public Object testScript() throws Exception {
    this.takeAction(11);
    Map<String, Object> bothTest = collectTestData();

    this.takeAction(01);
    Map<String, Object> audioOnly = collectTestData();

    this.takeAction(10);
    Map<String, Object> videoOnly = collectTestData();

    String jsonStr =
        Utility.developResult(bothTest, audioOnly, videoOnly, this.getWebDriverList().size())
            .toString();
    Utility.printJsonTofile("verify", jsonStr, "results/");
    return jsonStr;
  }
}
