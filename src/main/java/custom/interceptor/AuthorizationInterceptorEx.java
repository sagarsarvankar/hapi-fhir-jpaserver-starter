package custom.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.searchparam.matcher.AuthorizationSearchParamMatcher;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AdditionalCompartmentSearchParameters;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import custom.helper.CommonHelper;
import custom.helper.PermissionChecker;
import custom.helper.Scope;
import custom.helper.TokenHelper;
import custom.object.SubScope;
import custom.object.TokenDetails;
import jakarta.annotation.PostConstruct;
import org.hl7.elm.r1.IsNull;
import org.hl7.fhir.Observation;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
			CommonHelper.CONFORMANCE_PATH_WELLKNOWN_SMART.equals(requestDetails.getRequestPath())) {
		}
		else
		{
			TokenDetails tokendets = GetTokenDetailsFromTokenString(requestDetails);

			// Validate the token using your custom method
			if (validateToken(tokendets)) {
				// Throw an HTTP 401
				//throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
				throw new AuthenticationException("Authorization token is invalid");
			}
		}
		// Check if the token is missing

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
