package custom.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import custom.helper.Scope;
import custom.helper.TokenHelper;
import custom.object.TokenDetails;

import java.util.List;

public class AuthorizationInterceptorEx extends AuthorizationInterceptor {
	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String CONFORMANCE_PATH_METADATA = "metadata";
	private static final String CONFORMANCE_PATH_WELLKNOWN_OPENID = ".well-known/openid-configuration";
	private static final String CONFORMANCE_PATH_WELLKNOWN_SMART = ".well-known/smart-configuration";

	private static final String BEARER_TOKEN_PREFIX = "Bearer ";

	private TokenDetails GetTokenDetailsFromTokenString(String bearerToken) throws JsonProcessingException {
		String DecodedToken = TokenHelper.DecodeToken(bearerToken);

		ObjectMapper objectMapper = new ObjectMapper();
		TokenDetails tokendets = objectMapper.readValue(DecodedToken, TokenDetails.class);

		return tokendets;
	}

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void authorizeRequest(RequestDetails requestDetails) throws JsonProcessingException {

		if (CONFORMANCE_PATH_METADATA.equals(requestDetails.getRequestPath()) ||
			CONFORMANCE_PATH_WELLKNOWN_OPENID.equals(requestDetails.getRequestPath()) ||
			CONFORMANCE_PATH_WELLKNOWN_SMART.equals(requestDetails.getRequestPath())) {
		}
		else
		{
			String bearerToken = requestDetails.getHeader(AUTHORIZATION_HEADER);

			if (bearerToken == null || bearerToken.isEmpty()) {
				// Throw an HTTP 401
				// throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
				throw new AuthenticationException("Authorization token is missing");
			}

			bearerToken = bearerToken.replaceFirst(BEARER_TOKEN_PREFIX, "");

			TokenDetails tokendets = GetTokenDetailsFromTokenString(bearerToken);

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

	private boolean checkSystemScopes(String resourceType, Scope.Permission requiredPermission,
												 List<String> grantedscopes) {
		boolean bApproved = true;

		if (!isApprovedByScopes(resourceType, requiredPermission, grantedscopes)) {
			bApproved = false;
		}

		return  bApproved;
	}

	private boolean isApprovedByScopes(String resourceType, Scope.Permission requiredPermission, List<String> grantedScopes) {
		if (grantedScopes == null) {
			return false;
		}

		for (String scope : grantedScopes) {
			Scope sc = new Scope(scope);
			// "Resource" is used for "*" which applies to all resource types

			if (sc.getResourceType().isEmpty() || sc.getResourceType().isBlank())
			{
			}
			else {
				if (sc.getResourceType() == "*"
					|| sc.getResourceType().equals(resourceType)) {
					if (hasPermission(sc.getPermission(), requiredPermission)) {

						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @param grantedPermission
	 * @param requiredPermission
	 * @return true if the grantedPermission includes the requiredPermission; otherwise false
	 */
	private boolean hasPermission(Scope.Permission grantedPermission, Scope.Permission requiredPermission) {
		boolean permittedValue = false;

		if (grantedPermission == Scope.Permission.ALL) {
			return true;
		} else {
			switch(grantedPermission.value()){
				case "cruds":
					if (requiredPermission.value().equals("read") || requiredPermission.value().equals("write")){
						permittedValue = true;
					}
					break;
				case "rs":
					if (requiredPermission.value().equals("read")){
						permittedValue = true;
					}
					break;
				case "read":
					if (requiredPermission.value().equals("read")){
						permittedValue = true;
					}
					break;
				case "write":
					if (requiredPermission.value().equals("read") || requiredPermission.value().equals("write")){
						permittedValue = true;
					}
					break;
			}

			if (permittedValue){
				return permittedValue;
			}
			else{
				return grantedPermission == requiredPermission;
			}
		}
	}

	private boolean AllowAccess(TokenDetails tokenDetails, Scope.Permission neededPermission, String resourceType) {
		boolean returnValue = false;

		try {
			//Scope.Permission neededPermission = Scope.Permission.READ;
			//Set<String> resourceTypes;
			List<String> ScopesFromToken = TokenHelper.getScopesListByScopeString(tokenDetails.scope);
			returnValue = checkSystemScopes(resourceType, neededPermission, ScopesFromToken);
		}
		catch (Exception e){

		}

		return returnValue;
	}


	@Override
	public List<IAuthRule> buildRuleList(RequestDetails requestDetails) {
		// Define authorization rules based on validated token or other criteria
		// For example, you might want to allow certain operations based on the user's token

		if (CONFORMANCE_PATH_METADATA.equals(requestDetails.getRequestPath()) ||
			CONFORMANCE_PATH_WELLKNOWN_OPENID.equals(requestDetails.getRequestPath()) ||
			CONFORMANCE_PATH_WELLKNOWN_SMART.equals(requestDetails.getRequestPath())) {

			return new RuleBuilder()

				// 1. Allow all users to access the /metadata endpoint without restriction
				.allowAll("Allow all requests without restriction")
				.build();
		}

		else {

			String bearerToken = requestDetails.getHeader(AUTHORIZATION_HEADER);
			bearerToken = bearerToken.replaceFirst(BEARER_TOKEN_PREFIX, "");

			TokenDetails tokendets = new TokenDetails();
			try{
				tokendets = GetTokenDetailsFromTokenString(bearerToken);
			}
			catch (Exception e){	}

			boolean allowAccess = false;
			allowAccess = AllowAccess(tokendets, Scope.Permission.READ, requestDetails.getRequestPath());

			return new RuleBuilder()

				// 1. Allow all users to access the /metadata endpoint without restriction
				//.allow().operation().named("/metadata").onServer().andAllowAllResponses().andThen()

				// 2. Allow read access to Patient resources for all authenticated users
				.allow().read().resourcesOfType("Patient").withAnyId().andThen()

				// 3. Allow read access to Observation resources for all authenticated users
				.allow().read().resourcesOfType("Observation").withAnyId().andThen()

				// 4. Allow read access to Encounter resources for all authenticated users
				.allow().read().resourcesOfType("Encounter").withAnyId().andThen()

				// 5. Optional: Allow conditional write access to Observation and Patient resources for testing
				.allow().write().resourcesOfType("Observation").withAnyId().andThen()
				.allow().write().resourcesOfType("Patient").withAnyId().andThen()

				// 6. Deny all other access (default deny-all policy for security)
				.denyAll("Deny all other requests")
				.build();
		}
	}
}
