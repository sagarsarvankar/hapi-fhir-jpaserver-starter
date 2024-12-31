package custom.multitenancy;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

	@Override
	protected Object determineCurrentLookupKey() {
		// Use the TenantContext to get the current tenant ID
		return TenantContext.getCurrentTenant();
	}
}