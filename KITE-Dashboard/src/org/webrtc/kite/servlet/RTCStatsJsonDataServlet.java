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
import org.codehaus.jackson.map.ObjectMapper;
import org.webrtc.kite.Utility;
import org.webrtc.kite.dao.RTCStatsDao;
import org.webrtc.kite.exception.KiteNoKeyException;
import org.webrtc.kite.exception.KiteSQLException;
import org.webrtc.kite.pojo.RTCStatsDiff;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Servlet implementation class verification support
 * this endpoint will server json data for verification, and take table name as the param
 */
@WebServlet("/rtcstatsjson")
public class RTCStatsJsonDataServlet extends HttpServlet {

  private static final long serialVersionUID = -2L;
  private static final Log log = LogFactory.getLog(RTCStatsJsonDataServlet.class);

  /**
   * @see HttpServlet#HttpServlet()
   */
  public RTCStatsJsonDataServlet() {
    super();
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String tableName = request.getParameter("name");
    List<RTCStatsDiff> rtcStatsDiffList = new ArrayList<>();
    if (tableName == null)
      throw new KiteNoKeyException("table name");
    try {
      rtcStatsDiffList = new RTCStatsDao(Utility.getDBConnection(this.getServletContext())).getLatestResult(tableName);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new KiteSQLException(e.getLocalizedMessage());
    }

    final ObjectMapper mapper = new ObjectMapper();
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    mapper.writeValue(out, rtcStatsDiffList);
    final byte[] data = out.toByteArray();
    String resultString = new String(data);
    response.getWriter().print(resultString);
  }

}
