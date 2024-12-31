package custom.multitenancy;

import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import custom.helper.CommonHelper;
import custom.object.MoreConfig;
import custom.object.TenantDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MultiTenantDataSourceConfig {

	public MultiTenantDataSourceConfig()
	{
	}

	@Bean
	@Primary
	public DataSource dataSource() {
		TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();

		MoreConfig moreConfig = CommonHelper.GetMoreConfigFromConfig();

		if (moreConfig != null) {
			if (moreConfig.tenants != null) {
				Map<Object, Object> dataSourceMap = new HashMap<>();

				for (TenantDetails singleTenant : moreConfig.tenants)
				{
					DataSource dataSource = getDataSourceForTenant(singleTenant);
					dataSourceMap.put(singleTenant.name, dataSource);

					try {
						if (singleTenant.defaulttenant.toLowerCase().equals("true"))
						{
							routingDataSource.setDefaultTargetDataSource(dataSource);
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

				routingDataSource.setTargetDataSources(dataSourceMap);
			}
		}

		return routingDataSource;
	}

	private DataSource getDataSourceForTenant(TenantDetails tenant) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();

		dataSource.setDriverClassName(tenant.driverClassName);
		dataSource.setUrl(tenant.url);
		dataSource.setUsername(tenant.username);
		dataSource.setPassword(tenant.password);

		return dataSource;
	}
}