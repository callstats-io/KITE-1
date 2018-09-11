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

package org.webrtc.kite.wpt;

import junit.framework.TestCase;
import org.openqa.selenium.WebDriver;
import org.webrtc.kite.KiteTest;
import org.webrtc.kite.WebDriverFactory;
import org.webrtc.kite.config.Browser;
import org.webrtc.kite.config.Mobile;

import java.util.ArrayList;
import java.util.List;

public class WPTestTest extends TestCase {

  private static final String SELENIUM_SERVER_URL = "http://localhost:4444/wd/hub";
  private static final String TEST_NAME = "WPTestTest";

  private List<WebDriver> webDriverList = new ArrayList<WebDriver>();

  public void setUp() throws Exception {
    super.setUp();

    Mobile mobile = new Mobile();
    mobile.setDeviceName("iPhone");
    mobile.setPlatformName("iOS");
    mobile.setPlatformVersion("11");

    Mobile mobile1 = new Mobile();
    mobile1.setDeviceName("AndroidPhone");
    mobile1.setPlatformName("Android");
    mobile1.setPlatformVersion("6");

    final Browser browser = new Browser("firefox");
    browser.setPlatform("WINDOWS");
    browser.setVersion("62");
    //browser.setMobile(mobile);
    //browser.setTechnologyPreview(true);
    browser.setRemoteAddress(SELENIUM_SERVER_URL);
    final Browser browser1 = new Browser("chrome");
    browser1.setPlatform("ANDROID");
    browser1.setVersion("ANDROID 67");
    browser1.setMobile(mobile1);
    browser1.setRemoteAddress(SELENIUM_SERVER_URL);

    webDriverList.add(WebDriverFactory.createWebDriver(browser1, TEST_NAME));
    webDriverList.add(WebDriverFactory.createWebDriver(browser, TEST_NAME));
  }

  public void tearDown() throws Exception {
    // Close all the browsers
    for (WebDriver webDriver : this.webDriverList)
      try {
        webDriver.quit();
      } catch (Exception e) {
        e.printStackTrace();
      }
  }

  public void testTestScript() throws Exception {
    KiteTest test = new OneCodecOnlyTest();
    test.setWebDriverList(this.webDriverList);
    System.out.println("Skipping Web Platform Test Unit Test. Uncomment line 79 in WPTestTest.java to enable this unit test.");
    //System.out.println(test.testScript());
  }
}
