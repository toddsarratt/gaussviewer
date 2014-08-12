package com.toddsarratt.gaussviewer;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;
import net.toddsarratt.GaussTrader.Portfolio;
import net.toddsarratt.GaussTrader.Position;
import org.postgresql.ds.PGSimpleDataSource;

public class PositionsOpenToday extends HttpServlet {

    static Connection dbConnection;
    private static String portfolioName = "shortStrat2014Aug07";
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();
    private static final ZoneOffset NEW_YORK_TZ = LocalDateTime.now().atZone(ZoneId.of("America/New_York")).getOffset();
    private static final DateTimeFormatter MONTH_DAY_YEAR_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
/*		portfolioName = request.getParameter("portfolioName");			*/
		JsonObject positionJson;
		JsonArray positionsOpenAllJsonArray = new JsonArray();
		PrintWriter responseWriter = response.getWriter();
		for(Position positionToConvert : generatePositionsOpenTodayList()) {
			positionJson = new JsonObject();	
			positionJson.addProperty("positionId", Long.toString(positionToConvert.getPositionId()));
			positionJson.addProperty("ticker", positionToConvert.getTicker());
			positionJson.addProperty("secType", positionToConvert.getSecType());
			positionJson.addProperty("expiry", positionToConvert.isStock() ? "n/a" : positionToConvert.getExpiry().toString());
			positionJson.addProperty("underlyingTicker", positionToConvert.getUnderlyingTicker());
			positionJson.addProperty("strikePrice", CURRENCY_FORMAT.format(positionToConvert.getStrikePrice()));
			positionJson.addProperty("epochOpened", LocalDateTime.ofEpochSecond(positionToConvert.getEpochOpened() / 1000, 0, NEW_YORK_TZ).format(MONTH_DAY_YEAR_FORMATTER));
			positionJson.addProperty("longPosition", positionToConvert.isLong() ? "long" : "short");
			positionJson.addProperty("numberTransacted", positionToConvert.getNumberTransacted());
			positionJson.addProperty("priceAtOpen", CURRENCY_FORMAT.format(positionToConvert.getPriceAtOpen()));
			positionJson.addProperty("costBasis", CURRENCY_FORMAT.format(positionToConvert.getCostBasis()));
			positionJson.addProperty("claimAgainstCash", CURRENCY_FORMAT.format(positionToConvert.getClaimAgainstCash()));
			positionJson.addProperty("lastTick", positionToConvert.getLastTick());
			positionJson.addProperty("nav", CURRENCY_FORMAT.format(positionToConvert.calculateNetAssetValue()));
			positionJson.addProperty("profit", CURRENCY_FORMAT.format(positionToConvert.getProfit()));
			positionsOpenAllJsonArray.add(positionJson);
		}
		response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
		responseWriter.print(positionsOpenAllJsonArray);
	}

	private List<Position> generatePositionsOpenTodayList() {
		List<Position> positionsOpenTodayList = Collections.emptyList();
		Position portfolioPositionEntry;
        ZonedDateTime todayMidnight = ZonedDateTime.of(LocalDate.now(), LocalTime.parse("00:00"), NEW_YORK_TZ);
	    try {
	    	positionsOpenTodayList = new ArrayList<>();
		    PreparedStatement positionsOpenAllStatement = dbConnection.prepareStatement("SELECT * FROM positions " +
                    "WHERE portfolio = ? " +
                    "AND open = true " +
                    "AND epoch_opened > ?" +
                    "ORDER BY epoch_opened DESC");
		    /* Fix this damn shit
		    portfolioSummaryStatement.setString(1, portfolioName);
		    */
		    positionsOpenAllStatement.setString(1, portfolioName);
            positionsOpenAllStatement.setLong(2, todayMidnight.toEpochSecond() * 1000);
            ResultSet positionsOpenAllResultSet = positionsOpenAllStatement.executeQuery();
		    while(positionsOpenAllResultSet.next()) {
				portfolioPositionEntry = Portfolio.dbToPortfolioPosition(positionsOpenAllResultSet);
				positionsOpenTodayList.add(portfolioPositionEntry);
		    }
		} catch(SQLException sqle) {
			sqle.printStackTrace();
		}
		return positionsOpenTodayList;
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