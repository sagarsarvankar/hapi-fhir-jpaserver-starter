package custom.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import custom.helper.CommonHelper;
import custom.multitenancy.TenantContext;

public class TenantIdentificationInterceptor {

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void identifyTenant(RequestDetails requestDetails) {
		String tenantId = requestDetails.getHeader(CommonHelper.TENANT_HEADER_NAME);
		if (tenantId == null || tenantId.isEmpty()) {
			// throw new IllegalArgumentException("Tenant ID is missing");
			tenantId = CommonHelper.TENANT_NAME_DEFAULT;
		}

		//requestDetails.setTenantId(tenantId);
		TenantContext.setCurrentTenant(tenantId);
	}
}