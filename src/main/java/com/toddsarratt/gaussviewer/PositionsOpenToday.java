import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.*;
import javax.servlet.http.*;
import net.toddsarratt.GaussTrader.Portfolio;
import net.toddsarratt.GaussTrader.Position;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.postgresql.ds.PGSimpleDataSource;

public class PositionsOpenAll extends HttpServlet {

    static Connection dbConnection;
    private static String portfolioName;
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance();
    private static final ZoneId NEW_YORK_TZ = ZoneId.of("America/New_York");
    private static final DateTimeFormatter MONTH_DAY_YEAR_FORMATTER = DateTimeFormat.forPattern("MM/dd/yyyy");

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
/*		portfolioName = request.getParameter("portfolioName");			*/
		JsonObject positionJson = new JsonObject();
		JsonArray positionsOpenAllJsonArray = new JsonArray();
		PrintWriter responseWriter = response.getWriter();
		for(Position positionToConvert : generatePositionsOpenAllList()) {
			positionJson = new JsonObject();	
			positionJson.addProperty("positionId", Long.toString(positionToConvert.getPositionId()));
			positionJson.addProperty("ticker", positionToConvert.getTicker());
			positionJson.addProperty("secType", positionToConvert.getSecType());
			positionJson.addProperty("expiry", positionToConvert.isStock() ? "n/a" : MONTH_DAY_YEAR_FORMATTER.print(positionToConvert.getExpiry()));
			positionJson.addProperty("underlyingTicker", positionToConvert.getUnderlyingTicker());
			positionJson.addProperty("strikePrice", CURRENCY_FORMAT.format(positionToConvert.getStrikePrice()));
			positionJson.addProperty("epochOpened", MONTH_DAY_YEAR_FORMATTER.print(positionToConvert.getEpochOpened()));
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

	private List<Position> generatePositionsOpenAllList() {
		List<Position> positionsOpenAllList = null;
		Position portfolioPositionEntry;
	    try {
	    	positionsOpenAllList = new ArrayList<>();
		    PreparedStatement positionsOpenAllStatement = dbConnection.prepareStatement("SELECT * FROM positions WHERE portfolio = ? AND open = true ORDER BY epoch_opened DESC");
		    /* Fix this damn shit
		    portfolioSummaryStatement.setString(1, portfolioName);
		    */
		    positionsOpenAllStatement.setString(1, "shortStrat2014Feb");
		    ResultSet positionsOpenAllResultSet = positionsOpenAllStatement.executeQuery();
		    while(positionsOpenAllResultSet.next()) {
				portfolioPositionEntry = Portfolio.dbToPortfolioPosition(positionsOpenAllResultSet);
				positionsOpenAllList.add(portfolioPositionEntry);
		    }
		} catch(SQLException sqle) {
			sqle.printStackTrace();
		}
		return positionsOpenAllList;
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