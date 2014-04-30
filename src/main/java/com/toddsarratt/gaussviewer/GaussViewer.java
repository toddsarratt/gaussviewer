package com.toddsarratt.gaussviewer;

import com.google.gson.JsonObject;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.*;
import javax.servlet.http.*;
import org.postgresql.ds.PGSimpleDataSource;

public class GaussViewer extends HttpServlet {

    static Connection dbConnection;
/*    private static String portfolioName;          */
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();
    private static final ZoneId NEW_YORK_TZ = ZoneId.of("America/New_York");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
/*		portfolioName = request.getParameter("portfolioName");		*/
		PrintWriter responseWriter = response.getWriter();
		Map <String, Double> summaryMap = generateSummaryMap();
		JsonObject portfolioJson = new JsonObject();	
		portfolioJson.addProperty("nav", CURRENCY_FORMAT.format(summaryMap.get("nav")));
		portfolioJson.addProperty("freeCash", CURRENCY_FORMAT.format(summaryMap.get("freeCash")));
		portfolioJson.addProperty("reservedCash", CURRENCY_FORMAT.format(summaryMap.get("reservedCash")));
		portfolioJson.addProperty("totalCash", CURRENCY_FORMAT.format(summaryMap.get("totalCash")));
		portfolioJson.addProperty("positionsToday", positionsOpenedToday());
		portfolioJson.addProperty("positionCount", openPositions());
		portfolioJson.addProperty("orderCount", openOrders());
		response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
		responseWriter.print(portfolioJson);
	}

	private Map<String, Double> generateSummaryMap() {
		Map<String, Double> dbMap = null;
	    try {
	    	dbMap = new HashMap<>();
		    PreparedStatement portfolioSummaryStatement = dbConnection.prepareStatement("SELECT * FROM portfolios WHERE name = ?");
		    /* Fix this damn shit
		    portfolioSummaryStatement.setString(1, portfolioName);
		    */
		    portfolioSummaryStatement.setString(1, "shortStrat2014Feb");
		    ResultSet portfolioSummaryResultSet = portfolioSummaryStatement.executeQuery();
		    if(portfolioSummaryResultSet.next()) {
		        double netAssetValue = portfolioSummaryResultSet.getDouble("net_asset_value");
		        double freeCash = portfolioSummaryResultSet.getDouble("free_cash");
		        double reservedCash = portfolioSummaryResultSet.getDouble("reserved_cash");
		        double totalCash = portfolioSummaryResultSet.getDouble("total_cash");
		        dbMap.put("nav", netAssetValue);
		        dbMap.put("freeCash", freeCash);
		        dbMap.put("reservedCash", reservedCash);
		        dbMap.put("totalCash", totalCash);
			}
		} catch(SQLException sqle) {
		    sqle.printStackTrace();
		}
		return dbMap;
	}
	private int positionsOpenedToday() {
	    try {
			ZonedDateTime todayMidnight = ZonedDateTime.of(LocalDate.now(), LocalTime.parse("00:00"), NEW_YORK_TZ);
		    PreparedStatement countStatement = dbConnection.prepareStatement("SELECT count(*) FROM positions WHERE portfolio = ? AND epoch_opened >= ?");
		    countStatement.setString(1, "shortStrat2014Feb");
		    countStatement.setLong(2, (todayMidnight.toEpochSecond() * 1000));
		    ResultSet countResultSet = countStatement.executeQuery();
		    return countResultSet.next() ? countResultSet.getInt("count") : 0;
		} catch(SQLException sqle) {
		    sqle.printStackTrace();
   		    return 0;
		}
	}
	private int openPositions() {
	    try {
		    PreparedStatement portfolioPositionStatement = dbConnection.prepareStatement("SELECT count(*) FROM positions WHERE portfolio = ? AND open = true");
		    portfolioPositionStatement.setString(1, "shortStrat2014Feb");
		    ResultSet portfolioPositionResultSet = portfolioPositionStatement.executeQuery();
		    return portfolioPositionResultSet.next() ? portfolioPositionResultSet.getInt("count") : 0;
		} catch(SQLException sqle) {
		    sqle.printStackTrace();
   		    return 0;
		}
	}
	private int openOrders() {
		try{
		    PreparedStatement portfolioOrderStatement = dbConnection.prepareStatement("SELECT count(*) FROM orders WHERE portfolio = ? AND open = true");
		    portfolioOrderStatement.setString(1, "shortStrat2014Feb");
		    ResultSet portfolioOrderResultSet = portfolioOrderStatement.executeQuery();		
		    return portfolioOrderResultSet.next() ? portfolioOrderResultSet.getInt("count") : 0;
		} catch(SQLException sqle) {
		    sqle.printStackTrace();
   		    return 0;
		}
	}
	@Override
	public void init() throws ServletException {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerName("localhost");
        dataSource.setDatabaseName("postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("b3llcurv38");
        try {
            dbConnection = dataSource.getConnection();
        } catch(SQLException sqle) {
            sqle.printStackTrace();
        }

	}
}