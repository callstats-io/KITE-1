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
 * Callstats.io Twilio audio only SHIM test of KiteTest.
 * <p>
 * The testScript() implementation does the following in sequential manner on the provided array of
 * WebDriver:
 * <ul>
 * <li>1) Opens all the browsers with the url specified in CSIO_TWILIO_AUDIO_ONLY_DEMO_URL.</li>
 * <li>2) Join Twilio audio only call with a namespace, and clicks 'call button' .</li>
 * <li>3) Do the following every 1 second for 1 minute: </li>
 * <ul>
 * <li>a) Executes the JavaScript on all browsers given via testJavaScript() which returns
 * callstats.io stats callback result</li>
 * <li>b) Checks whether all the browsers have returned either
 * "connectionState":, // one of: online, offline
 * "fabricState":, // one of: initialising, established, disrupted.
 * also may be streams if necessary
 * </li>
 * </ul>
 * <li>4) The test is considered as successful if all the browsers either returns
 * connectionState : 'online', and fabricState : 'established'
 * <li>5) A successful test returns a boolean 'true' while the unsuccessful test returns a boolean
 * 'false'.</li>
 * </ul>
 * </p>
 */
public class StatsVerifyTest extends KiteTest {

    private final static Logger logger = Logger.getLogger(StatsVerifyTest.class.getName());

    private final static Map<String, String> expectedResultMap = new HashMap<String, String>();
    private final static String APPRTC_URL = "https://appr.tc/r/";
    private final static int TIMEOUT = 60000;
    private final static int OVERTIME = 3 * 1000;
    private final static int INTERVAL = 1000;
    private final static String RESULT_TIMEOUT = "TIME OUT";
    private final static String RESULT_SUCCESSFUL = "SUCCESSFUL";
    private final static String RESULT_FAILED = "FAILED";
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
    private final static String testJavaScript() {
        return "var retValue;"
                + "try {retValue = appController.call_.pcClient_.pc_.iceConnectionState;} catch (exception) {} "
                + "if (retValue) {return retValue;} else {return 'unknown';}";
    }

    /**
     * @return the JavaScript as string.
     */
    private final static String stashStatsScript() {
        return "function getAllStats() {\n" +
                "    return new Promise( (resolve, reject) => {\n" +
                "        try{\n" +
                "            appController.call_.pcClient_.pc_.getStats().then((report) => {\n" +
                "                let statTypes = new Set();\n" +
                "                // type -> stat1, stat2, stat3\n" +
                "                let statTree = new Map();\n" +
                "                for (let stat of report.values()) {\n" +
                "                    const curType = stat.type;\n" +
                "                    const prvStat = statTree.get(curType);\n" +
                "                    if(prvStat) {\n" +
                "                        const _tmp = [...prvStat, stat];\n" +
                "                        statTree.set(curType, _tmp)\n" +
                "                    }else{\n" +
                "                        const _tmp = [stat];\n" +
                "                        statTree.set(curType, _tmp)\n" +
                "                    }\n" +
                "                }\n" +
                "                let retval = {};\n" +
                "                for (const [key, statsArr] of statTree) {\n" +
                "                    let keysArr = [];\n" +
                "                    for(const curStat of statsArr){\n" +
                "                        const keys = Object.keys(curStat);\n" +
                "                        keysArr = [ ...keysArr, ...keys ];\n" +
                "                    }\n" +
                "                    retval[key] = keysArr;\n" +
                "                }\n" +
                "                resolve(retval);\n" +
                "        });\n" +
                "        } catch(err) {\n" +
                "            reject(err);\n" +
                "        }\n" +
                "    });\n" +
                "}\n" +
                "function stashStats() {\n" +
                "    getAllStats().then( (data)=> {\n" +
                "        window.KITEStatsDiff = data;\n" +
                "    }, err => {\n" +
                "        console.log('error',err);\n" +
                "    });\n" +
                "}\n" +
                "stashStats()\n";
    }

    /**
     * @return the JavaScript as string.
     */
    private final static String getStatsScript() {
//        return "return window.KITEStats;";
        return "return window.KITEStatsDiff;";

    }

    /**
     * Opens the APPRTC_URL and clicks 'confirm-join-button'.
     */
    // audio only = 01
    // video only = 10
    // audio + video = 11
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
                    alertMsg = ((RemoteWebDriver) webDriver).getCapabilities().getBrowserName() + " alert: " + alertMsg;
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
        for (String result : resultList)
            if (expectedResultMap.get(result) == null)
                return false;
        return true;
    }

    /**
     * Checks whether atleast one the result string matches 'failed'.
     *
     * @param resultList ice connection states for the browsers as List<String>
     * @return true if atleast one of the result string matches 'failed'.
     */
    private boolean checkForFailed(List<String> resultList) {
        for (String result : resultList)
            if (result.equalsIgnoreCase("failed"))
                return true;
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
                if (logger.isInfoEnabled())
                    logger.info(webDriver + ": " + resultOfScript);
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

        return Utility.developResult(bothTest, audioOnly, videoOnly, this.getWebDriverList().size()).toString();
    }
}