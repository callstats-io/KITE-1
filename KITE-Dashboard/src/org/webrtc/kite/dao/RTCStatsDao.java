package org.webrtc.kite.dao;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.webrtc.kite.BrowserMapping;
import org.webrtc.kite.Utility;
import org.webrtc.kite.pojo.Browser;
import org.webrtc.kite.pojo.RTCStatsDiff;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class in charged of all data gesture concerning the RTCStats verification data
 */
public class RTCStatsDao {
    private static final Log log = LogFactory.getLog(BrowserDao.class);

    private Connection connection;

    /**
     * Constructs a new RTCStatsDao object associated with a connection to the database.
     *
     * @param connection a JDBC connection to the database.
     */
    public RTCStatsDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Get list of browsers that we are supporting currently
     * @return return list of browsers
     * @throws SQLException
     */
    private List<Browser> getBrowserList() throws SQLException {
        String query = "SELECT * FROM BROWSERS ORDER BY NAME DESC, VERSION DESC, PLATFORM DESC;";

        List<Browser> listOfBrowsers = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = this.connection.prepareStatement(query);
            if (log.isDebugEnabled())
                log.debug("Executing: " + query);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (log.isDebugEnabled()) {
                    final StringBuilder rsLog = new StringBuilder();
                    for (int c = 1; c <= rs.getMetaData().getColumnCount(); c++) {
                        rsLog.append(rs.getMetaData().getColumnName(c)).append(":").append(rs.getString(c))
                                .append("-");
                    }
                    log.debug(rsLog.toString());
                }
                int id = rs.getInt("BROWSER_ID");
                String name = rs.getString("NAME");
                String version = rs.getString("VERSION");
                String platform = rs.getString("PLATFORM");
                if (!BrowserMapping.IrrelevantList.contains(version) && !BrowserMapping.IrrelevantList.contains(platform))
                    listOfBrowsers.add(new Browser(id, name, version, platform));
            }

        } finally {
            Utility.closeDBResources(ps, rs);
        }
        return listOfBrowsers;
    }
    private String getBrowserStats(String tableName, int browserId) throws SQLException {
        final String query = "select * from "+ tableName+
                " where RESULT      = 'SUCCESSFUL' " +
                " and   BROWSER_1   = "+browserId +
                " limit 1";
        PreparedStatement ps = null;
        ResultSet rs = null;
        String stats = null;
        try {
            ps = this.connection.prepareStatement(query);
            rs = ps.executeQuery();
            while (rs.next()) {
                stats = rs.getString("STATS");
            }
        }finally {
            Utility.closeDBResources(ps,rs);
        }
        return stats;
    }

    public String getResultTable() throws SQLException{
        // todo Change hardcoded test name
        final String query = "select * from TESTS " +
                "where TEST_NAME = 'StatsVerifyTest' " +
                "and STATUS = 'DONE' " +
                "order by TEST_ID desc " +
                "limit 1;";

        PreparedStatement ps = null;
        ResultSet rs = null;
        String resultTable = null;
        try {
            ps = this.connection.prepareStatement(query);
            rs = ps.executeQuery();
            while (rs.next()) {
                resultTable = rs.getString("RESULT_TABLE");
                break;
            }
        }finally {
            Utility.closeDBResources(ps,rs);
        }
        return resultTable;
    }

    public List<RTCStatsDiff> getLatestResult(String tableName) throws SQLException{
        List<Browser> browserList = getBrowserList();
        List<RTCStatsDiff> rtcStatsDiffList = new ArrayList<>();
        for(Browser curBrowser : browserList){
            String browserStats = getBrowserStats(tableName, curBrowser.getId());
            if(browserStats != null) {
                RTCStatsDiff rtcStatsDiff = new RTCStatsDiff(curBrowser, browserStats);
                rtcStatsDiffList.add(rtcStatsDiff);
            }
        }
        return rtcStatsDiffList;
    }

}
