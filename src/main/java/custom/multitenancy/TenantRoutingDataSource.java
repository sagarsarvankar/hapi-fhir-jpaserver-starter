package custom.multitenancy;

import ca.uhn.fhir.jpa.starter.common.FhirServerConfigCommon;
import custom.batchjob.BatchJobTenantRegistry;
import custom.batchjob.CurrentBatchJobTracker;
import custom.helper.CommonHelper;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

	@Override
	protected Object determineCurrentLookupKey() {
		// Use the TenantContext to get the current tenant ID
		try
		{
			if (CommonHelper.EnableDebugLog()) {
				String tenant =
					TenantContext.getCurrentTenant();

				org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirServerConfigCommon.class);
				ourLog.info("Thread={} tenant={}",
					Thread.currentThread().getName(),
					tenant);
			}
		}
		catch (Exception e){}

		return TenantContext.getCurrentTenant();

		/*
		String tenantId = TenantContext.getCurrentTenant();
		if (tenantId != null) {
			return tenantId;
		}
		*/

		/*
		String threadName = Thread.currentThread().getName();
		if (threadName.contains("hapi-batch2") || threadName.contains("Batch2") || threadName.contains("scheduled")) {

			// We use the ThreadLocal or a fallback check to resolve the active job context
			//String batchTenant = resolveTenantFromActiveBatchContext();
			String activeJobId = CurrentBatchJobTracker.getActiveJobId();
			if (activeJobId != null) {
				String mappedTenant = BatchJobTenantRegistry.getTenantForJob(activeJobId);
				if (mappedTenant != null) {
					return mappedTenant;
				}
			}
		}

		// 3. Fallback to default DB if no tenant context can be found
		return "default";
		*/
	}

	private String resolveTenantFromActiveBatchContext() {
		// Since HAPI keeps trying to look up the instance ID in the DB,
		// we map the active Job ID being processed by your maintenance poller loop
		// to its corresponding tenant.

		String activeJobId = CurrentBatchJobTracker.getActiveJobId();
		if (activeJobId != null) {
			return BatchJobTenantRegistry.getTenantForJob(activeJobId);
		}

		return null;
	}
}