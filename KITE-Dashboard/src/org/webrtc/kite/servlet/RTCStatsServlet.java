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

package org.webrtc.kite.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.webrtc.kite.Utility;
import org.webrtc.kite.dao.RTCStatsDao;
import org.webrtc.kite.exception.KiteSQLException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Servlet implementation class RTCStatsServlet for verify page
 */
@WebServlet("/verify")
public class RTCStatsServlet extends HttpServlet {

  private static final long serialVersionUID = -6712086996275183628L;
  private static final Log log = LogFactory.getLog(RTCStatsServlet.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public RTCStatsServlet() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    try {
      final String resultTable = new RTCStatsDao(Utility.getDBConnection(this.getServletContext())).getResultTable();
      if(resultTable!=null) {
        String requestString = "rtcstatsjson?name=" + resultTable;
        request.setAttribute("rtcstatsRequest", requestString);
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new KiteSQLException(e.getLocalizedMessage());
    }
    // todo remove this - not necessary if we don't need hosting csio verify page, and other library in seperate host
    response.addHeader("Access-Control-Allow-Origin", "*");
    // get UI
    if (log.isDebugEnabled())
      log.debug("Displaying: rtcstats.vm");
    RequestDispatcher requestDispatcher = request.getRequestDispatcher("rtcstats.vm");
    requestDispatcher.forward(request, response);
  }
}
