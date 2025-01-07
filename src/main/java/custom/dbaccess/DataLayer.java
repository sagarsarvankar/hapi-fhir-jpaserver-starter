package custom.dbaccess;

import custom.helper.CommonHelper;
import custom.object.MoreConfig;
import custom.object.TenantDetails;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataLayer {
	public Connection dbconnection;
	public String TenantName;

	public void CloseConnection() throws SQLException {
		dbconnection.close();
	}

	public void Connect(){
		MoreConfig moreConfig = CommonHelper.GetMoreConfigFromConfig();

		if (moreConfig.tenants != null) {
			Map<Object, Object> dataSourceMap = new HashMap<>();
			for (TenantDetails singleTenant : moreConfig.tenants)
			{
				try {
					if (singleTenant.name.toLowerCase().equals(TenantName.toLowerCase()))
					{
						String url = singleTenant.url;
						String username = singleTenant.username;
						String password = singleTenant.password;

						// Establish a connection based on tenant
						dbconnection = DriverManager.getConnection(url, username, password);
						break;
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public DataLayer(String tenantname){
		TenantName = tenantname;
	}

	public List<Map<String, Object>> execute(String query, Object[] params) throws SQLException {
		List<Map<String, Object>> result = new ArrayList<>();

		try (PreparedStatement stmt = dbconnection.prepareStatement(query)) {

			// Set the parameters in the PreparedStatement
			if (params != null) {
				for (int i = 0; i < params.length; i++) {
					stmt.setObject(i + 1, params[i]);
				}
			}

			// Execute the query
			try (ResultSet rs = stmt.executeQuery()) {
				ResultSetMetaData metaData = rs.getMetaData();
				int columnCount = metaData.getColumnCount();

				// Process the result set
				while (rs.next()) {
					Map<String, Object> row = new HashMap<>();
					for (int i = 1; i <= columnCount; i++) {
						String columnName = metaData.getColumnLabel(i); // Get the column name
						Object value = rs.getObject(i); // Get the column value
						row.put(columnName, value); // Store the name-value pair
					}
					result.add(row);
				}
			}
		} catch (SQLException e) {
			throw new SQLException("Error executing query", e);
		}

		return result;
	}
}
