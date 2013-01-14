package latest;

import com.atlassian.jira.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author dmitry dementiev
 */

public class PluginUtils {

	public static Long getGroupIdByName(String groupName) throws DataAccessException, SQLException {
		Connection conn = SQLHelper.getConnection();
		String sqlQuery = SQLHelper.getSQLProperty("getGroupIdByName");
		PreparedStatement pstmt = conn.prepareStatement(sqlQuery);
        pstmt.setString(1, groupName);
		ResultSet rs = pstmt.executeQuery();
		Long idOfGroup = null;
		while (rs.next()) {
			idOfGroup = rs.getLong(1);
		}
		rs.close();
		pstmt.close();
		conn.close();
		return idOfGroup;
	}
}