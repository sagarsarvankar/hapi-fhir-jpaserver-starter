package custom.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import custom.multitenancy.TenantContext;

public class TenantContextCleanupInterceptor {
	@Hook(Pointcut.SERVER_PROCESSING_COMPLETED_NORMALLY)
	public void clearTenantContext(RequestDetails requestDetails) {
		TenantContext.clear();
	}
}