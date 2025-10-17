package custom.interceptor;

import ca.uhn.fhir.batch2.model.JobInstance;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import custom.dbaccess.DatabaseHelper;
import custom.helper.*;
import custom.object.MoreConfig;
import custom.object.SubScope;
import custom.object.TenantDetails;
import custom.object.TokenDetails;
import javassist.NotFoundException;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static custom.helper.CommonHelper.GetTokenDetailsFromTokenString;

// import static custom.helper..GetNeededPermission;


public class AuthorizationInterceptorEx extends AuthorizationInterceptor {


	/*
	private TokenDetails GetTokenDetailsFromTokenString(String bearerToken) throws JsonProcessingException {
		String DecodedToken = TokenHelper.DecodeToken(bearerToken);

		ObjectMapper objectMapper = new ObjectMapper();
		TokenDetails tokendets = objectMapper.readValue(DecodedToken, TokenDetails.class);

		return tokendets;
	}
	*/



	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void authorizeRequest(RequestDetails requestDetails) throws JsonProcessingException {

		if (CommonHelper.CONFORMANCE_PATH_METADATA.equals(requestDetails.getRequestPath()) ||
			CommonHelper.CONFORMANCE_PATH_WELLKNOWN_OPENID.equals(requestDetails.getRequestPath()) ||
			CommonHelper.CONFORMANCE_PATH_WELLKNOWN_SMART.equals(requestDetails.getRequestPath()))
		{
			if (CommonHelper.CONFORMANCE_PATH_METADATA.equals(requestDetails.getRequestPath()))
			{
				requestDetails.addParameter("_format", new String[]{"json"});
			}
		}
		else {
			TokenDetails tokendets = GetTokenDetailsFromTokenString(requestDetails);

			// Check if token is revoked
			if (CommonHelper.AllowCheckingOfRevokedTokenDuringAuthorization() &&
				!TokenHelper.IsTokenActiveAfterIntrospection(tokendets.access_token)) {
				throw new AuthenticationException("Authorization token is revoked");
			}

			// Validate the token using your custom method
			if (validateToken(tokendets)) {
				// Throw an HTTP 401
				//throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
				throw new AuthenticationException("Authorization token is invalid");
			}

			// checking for valid tenantid
			String xfhirtenantid = "";
			try {
				xfhirtenantid = requestDetails.getHeader("X-FHIR-TENANT-ID");
			} catch (Exception e) {
			}
			if (xfhirtenantid != null && !xfhirtenantid.isEmpty()) {
				if (!IsTenantValid(xfhirtenantid)) {
					throw new AuthenticationException("Invalid tenant id sent in header X-FHIR-TENANT-ID");
				}
			}
			//

			//Checking if token is generated for this tenant
			if (CommonHelper.AllowCheck_token_generated_for_this_tenant()
				&& !IsTokenGeneratedForThisTenant(xfhirtenantid, tokendets)
			) {
				throw new AuthenticationException("Token is not valid for this tenant. ");
			}

			//

		}
		// Check if the token is missing


		// Since there is no support of _outputFormat other than
		// application/fhir+ndjson, hence changing the request parameter
		// to application/fhir+ndjson if the format is any other
		/*
		if (Enablesupport_default_to_application_fhir_ndjson_bulk_export())
		{
			TakeActionIfOutputFormatForBulkExportOtherThanfhirndjson(requestDetails);
		}
		*/
		//
		//


		// Here it will check if it is checking for $export-poll-status.
		// If the job is set for cancelled, it will return 401,
		// else the default behaviour will go ahead.
		if (Enablesupport_for_404_on_get_of_cancelled_job())
		{
			TakeActionIfBulkJobCancelled(requestDetails);
		}
		//
		//
	}

	private static boolean IsTokenGeneratedForThisTenant(String inputTenantId,
																		  TokenDetails tokendets) {
		boolean returnValue = false;

		try {
			String tenantidFromToken = tokendets.fhirtenant;

			if (inputTenantId == null || inputTenantId.isEmpty())
			{
				inputTenantId = "default";
			}
			if (tenantidFromToken == null || tenantidFromToken.isEmpty())
			{
				tenantidFromToken = "default";
			}

			if (inputTenantId.toLowerCase().equals(tenantidFromToken.toLowerCase())) {
				returnValue = true;
			}
		} catch (Exception e) {
			returnValue = true;
		}

		return returnValue;
	}

	private static boolean IsTenantValid(String inputTenantId) {
		boolean returnValue = false;

		try {
			if (inputTenantId != null && !inputTenantId.isEmpty()) {
				MoreConfig moreConfig = CommonHelper.GetMoreConfigFromConfig();

				for (TenantDetails singleTenant : moreConfig.tenants) {
					if (singleTenant.name.toLowerCase().equals(inputTenantId.toLowerCase())) {
						returnValue = true;
						break;
					}
				}
			}
		} catch (Exception e) {
			returnValue = true;
		}

		return returnValue;
	}

	private static boolean Enablesupport_default_to_application_fhir_ndjson_bulk_export() {
		boolean returnValue = false;

		try
		{
			HapiPropertiesConfig hapiConfig = new HapiPropertiesConfig();
			String getSecurityValue = hapiConfig.getdefault_to_application_fhir_ndjson_bulk_export();

			if (getSecurityValue.toLowerCase().equals("true")){
				returnValue = true;
			}
		}
		catch (Exception e){
		}

		return returnValue;
	}

	private static void TakeActionIfOutputFormatForBulkExportOtherThanfhirndjson(RequestDetails requestDetails){
		try{
			String operationType = requestDetails.getOperation();

			if (operationType != null &&
				operationType.toLowerCase().equals(CommonHelper.OPERATION_TYPE_EXPORT))
			{
				try
				{
					String outputFormat = requestDetails.getParameters().get("_outputFormat")[0];
					if (outputFormat == null || !outputFormat.toLowerCase().equals("application/fhir+ndjson")) {
						// Set the default outputFormat
						requestDetails.addParameter("_outputFormat", new String[] { "application/fhir+ndjson" });
					}
				} catch (Exception e) {
				}
			}
		}
		catch (Exception e){

		}
	}


	private static boolean Enablesupport_for_404_on_get_of_cancelled_job() {
		boolean returnValue = false;

		try
		{
			HapiPropertiesConfig hapiConfig = new HapiPropertiesConfig();
			String getSecurityValue = hapiConfig.getsupport_for_404_on_get_of_cancelled_job();

			if (getSecurityValue.toLowerCase().equals("true")){
				returnValue = true;
			}
		}
		catch (Exception e){
		}

		return returnValue;
	}

	private boolean IsJobCancelled(String jobid, String tenantname){
		boolean jobCancelled = false;

		try{
			JobInstance jobInstance = DatabaseHelper.GetJobInstanceByJobId(jobid, tenantname);

			if (jobInstance.isCancelled()){
				jobCancelled = true;
			}
		}
		catch(Exception e){}

		return jobCancelled;
	}

	private void TakeActionIfBulkJobCancelled(RequestDetails requestDetails) {

		String operationType = requestDetails.getOperation();

		if (operationType != null) {
			RequestTypeEnum requestTypeEnum = requestDetails.getRequestType();

			if (requestTypeEnum == RequestTypeEnum.GET &&
				operationType.toLowerCase().equals(CommonHelper.OPERATION_TYPE_EXPORT_POLL_STATUS)) {
				String jobId = "";
				String tenantname = "";

				try {
					jobId = requestDetails.getParameters().get("_jobId")[0];

					tenantname = requestDetails.getHeader(CommonHelper.TENANT_HEADER_NAME);
					tenantname = CommonHelper.GetTenantNameBasedOnHeader(tenantname);
				} catch (Exception e) {
				}

				if (IsJobCancelled(jobId, tenantname)) {
					throw new ResourceNotFoundException("Job with ID " + jobId + " has been canceled");
				}
			}
		}
	}

	private boolean validateToken(TokenDetails tokendets) throws JsonProcessingException {
		// Implement your token validation logic here
		return TokenHelper.isTokenExpired(tokendets);
	}

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails requestDetails) {
		// Define authorization rules based on validated token or other criteria
		// For example, you might want to allow certain operations based on the user's token

		if (CommonHelper.CONFORMANCE_PATH_METADATA.equals(requestDetails.getRequestPath()) ||
			CommonHelper.CONFORMANCE_PATH_WELLKNOWN_OPENID.equals(requestDetails.getRequestPath()) ||
			CommonHelper.CONFORMANCE_PATH_WELLKNOWN_SMART.equals(requestDetails.getRequestPath())) {

			return new RuleBuilder()

				// 1. Allow all users to access the /metadata endpoint without restriction
				.allowAll("Allow all requests without restriction")
				.build();
		}

		else {
			TokenDetails tokendets = new TokenDetails();
			try {
				tokendets = GetTokenDetailsFromTokenString(requestDetails);
			} catch (Exception e) {
			}

			// boolean m_allowAccess = false;

			//
			Scope.Permission neededPermission = PermissionChecker.GetNeededPermission(requestDetails);
			//String
			//
			List<IAuthRule> ruleList = new ArrayList<IAuthRule>();
			ruleList = new RuleBuilder()
				.denyAll("Deny requests")
				.build();

			String resourceType = requestDetails.getResourceName();
			String operationType = requestDetails.getOperation();

			boolean AllowWrite = false;
			boolean AllowRead = false;

			// below if is because, during pagination _getpages query parameter appears
			// with no resource in the URL.
			// Hence checking resourcetype is null and followed by getpages query parameter
			if (resourceType == null
				|| resourceType.isEmpty()
				|| resourceType.isBlank()
			) {
				boolean bGetPagesPresent = false;
				Map<String, String[]> urlParameters = requestDetails.getParameters();
				for (Map.Entry<String, String[]> entry : urlParameters.entrySet()) {
					String key = entry.getKey(); // Get the parameter name


					if (key.equals(CommonHelper.URL_PARAMETER_GETPAGES)) {
						bGetPagesPresent = true;
						break;
					}
				}

				if (bGetPagesPresent){
					ruleList = new RuleBuilder()
						.allow().read().allResources().withAnyId().andThen()
						//.allow().read().resourcesOfType("Medication").withAnyId().andThen()
						//.allow().read().resourcesOfType(CommonHelper.RESOURCE_Provenance).withAnyId().andThen()
						.build();

					return ruleList;
				}
				else {
					RequestTypeEnum requestTypeEnum = requestDetails.getRequestType();

					// Below handling is done when POSTing
					// bundle data to FHIR server
					if (requestTypeEnum == RequestTypeEnum.POST){
						ruleList = new RuleBuilder()
							.allowAll("Allow all requests without restriction")
							.build();

						return ruleList;
					}

				}
			}
			//

			PermissionChecker permissionCheckerObj = new PermissionChecker();

			if (neededPermission == Scope.Permission.READ) {
				AllowRead = permissionCheckerObj.AllowAccess(tokendets, neededPermission, resourceType, requestDetails);
			}
			else {
				AllowRead = permissionCheckerObj.AllowAccess(tokendets, Scope.Permission.READ, resourceType, requestDetails);
				AllowWrite = permissionCheckerObj.AllowAccess(tokendets, neededPermission, resourceType, requestDetails);
			}

			// This section is for $export and $export-poll-status, below checks if write or read operation
			if ( operationType != null) {
				if (AllowRead || AllowWrite) {

					if (operationType.toLowerCase().equals(CommonHelper.OPERATION_TYPE_EXPORT))
					{
						// Since there is no support of _outputFormat other than
						// application/fhir+ndjson, hence changing the request parameter
						// to application/fhir+ndjson if the format is any other
						/*
						try
						{
							String outputFormat = requestDetails.getParameters().get("_outputFormat")[0];
							if (outputFormat == null || !outputFormat.equals("application/fhir+ndjson")) {
								// Set the default outputFormat
								requestDetails.addParameter("_outputFormat", new String[] { "application/fhir+ndjson" });
							}
						} catch (Exception e) {
						}
						*/
						//
						//

						ruleList = new RuleBuilder()
							.allow().operation().named(CommonHelper.OPERATION_TYPE_EXPORT)
							.atAnyLevel().andAllowAllResponsesWithAllResourcesAccess()
							.build();
					}

					else if (operationType.toLowerCase().equals(CommonHelper.OPERATION_TYPE_EXPORT_POLL_STATUS)) {
						ruleList = new RuleBuilder()
							.allow().operation().named(CommonHelper.OPERATION_TYPE_EXPORT_POLL_STATUS)
							.atAnyLevel().andAllowAllResponsesWithAllResourcesAccess()
							.build();
					}
					else
					{
						ruleList = new RuleBuilder()
							.allow().read().resourcesOfType(resourceType).withAnyId().andThen()
							.allow().read().resourcesOfType(CommonHelper.RESOURCE_Provenance).withAnyId().andThen()
							.build();
					}
				}
			}

			// Below section is for regular resource fetching
			else {
				if (AllowWrite) {
					ruleList = new RuleBuilder()
						.allow().read().resourcesOfType(resourceType).withAnyId().andThen()
						.allow().read().resourcesOfType(CommonHelper.RESOURCE_Provenance).withAnyId().andThen()
						.allow().write().resourcesOfType(resourceType).withAnyId().andThen()
						.allow().delete().resourcesOfType(resourceType).withAnyId().andThen()
						.build();
				} else if (AllowRead) {
					ruleList = new RuleBuilder()
						.allow().read().resourcesOfType(resourceType).withAnyId().andThen()
						.allow().read().resourcesOfType(CommonHelper.RESOURCE_Provenance).withAnyId().andThen()
						.build();

					//ruleList = ConstructRuleBuilderWithGranularScopes(permissionCheckerObj);

					// Does parameter in URL contain _include, then allow all read
					boolean bIncludePresent = false;

					Map<String, String[]> urlParameters = requestDetails.getParameters();
					for (Map.Entry<String, String[]> entry : urlParameters.entrySet()) {
						String key = entry.getKey(); // Get the parameter name
						//String[] values = entry.getValue(); // Get the parameter values

						if (key.equals(CommonHelper.URL_PARAMETER_INCLUDE)) {
							bIncludePresent = true;
							break;
						}
					}

					if(bIncludePresent) {
						ruleList = new RuleBuilder()
							.allow().read().allResources().withAnyId().andThen()
							//.allow().read().resourcesOfType("Medication").withAnyId().andThen()
							//.allow().read().resourcesOfType(CommonHelper.RESOURCE_Provenance).withAnyId().andThen()
							.build();
					}
					// Does parameter in URL contain _include, then allow all read
				}
			}

			return ruleList;

		}
	}

	private List<IAuthRule> ConstructRuleBuilderWithGranularScopes(PermissionChecker permissionCheckerObj) {
		List<IAuthRule> ruleList = new ArrayList<>();

		ruleList.add(new RuleBuilder()
			.allow().read().resourcesOfType(CommonHelper.RESOURCE_Provenance).withAnyId()
			.build()
			.get(0)
		);

		try
		{
			for (int i = 0; i < permissionCheckerObj.approvedScopesList.size(); i++) {
				Scope scopeTemp = permissionCheckerObj.approvedScopesList.get(i);

				boolean bSubScopeExists = false;
				String subScopeFilter = "";
				String resourceType = scopeTemp.getResourceType();

				try {
					SubScope subScopeTemp = scopeTemp.getSubscope();
					subScopeFilter = subScopeTemp.getName() + "=" + subScopeTemp.getValue();
					// subScopeFilter = "category=http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item";

					bSubScopeExists = true;
				} catch (Exception e) {}

				//bSubScopeExists = false;

				if (bSubScopeExists)
				{

					ruleList.add(
						new RuleBuilder()
							.allow()
							.read()
							.resourcesOfType(Condition.class)
							.withFilter(subScopeFilter)
							//.withFilter("category=problem-list-item")
							.build()
							.get(0) // Add the first (and only) rule created by RuleBuilder
					);
				}
				else {
					ruleList.add(new RuleBuilder()
						.allow().read().resourcesOfType(resourceType).withAnyId().andThen()
						.build()
						.get(0)
					);
				}
			}
		}
		catch (Exception e)
		{

		}

		return ruleList;
	}
}
