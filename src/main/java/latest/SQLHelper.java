package latest;

import com.atlassian.jira.appconsistency.db.OfbizConnectionFactory;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.upgrade.ConsistencyCheckImpl;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * @author dmitry dementiev
 */

public class SQLHelper {

	public final static ResourceBundle SQL = ResourceBundle.getBundle("dementiev.sql");

	public static Connection getConnection() throws DataAccessException, SQLException {
		String OFBIZ_DS_NAME = System.getProperty(ConsistencyCheckImpl.JIRA_DATASOURCE_PROPERTY, ConsistencyCheckImpl.CK_OFBIZ_DS_NAME);
		return new OfbizConnectionFactory(OFBIZ_DS_NAME).getConnection();
	}

	public static void closeConnection(Connection conn) throws SQLException {
		conn.close();
	}

	public static String getSQLProperty(String key) {
		return SQL.getString(key);
	}
}