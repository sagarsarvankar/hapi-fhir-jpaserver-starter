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
import custom.helper.TokenHelper;
import custom.object.TokenDetails;

import java.util.List;

public class AuthorizationInterceptorEx extends AuthorizationInterceptor {
	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String CONFORMANCE_PATH_METADATA = "metadata";
	private static final String CONFORMANCE_PATH_WELLKNOWN_OPENID = ".well-known/openid-configuration";
	private static final String CONFORMANCE_PATH_WELLKNOWN_SMART = ".well-known/smart-configuration";

	private static final String BEARER_TOKEN_PREFIX = "Bearer ";

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void authorizeRequest(RequestDetails requestDetails) throws JsonProcessingException {
		// Extract token from the Authorization header
		String bearerToken = requestDetails.getHeader(AUTHORIZATION_HEADER);

		bearerToken = bearerToken.replaceFirst(BEARER_TOKEN_PREFIX, "");
		String DecodedToken = TokenHelper.DecodeToken(bearerToken);

		ObjectMapper objectMapper = new ObjectMapper();
		TokenDetails tokendets = objectMapper.readValue(DecodedToken, TokenDetails.class);

		if (CONFORMANCE_PATH_METADATA.equals(requestDetails.getRequestPath()) ||
			CONFORMANCE_PATH_WELLKNOWN_OPENID.equals(requestDetails.getRequestPath()) ||
			CONFORMANCE_PATH_WELLKNOWN_SMART.equals(requestDetails.getRequestPath())) {
		}

		else
		{
			bearerToken = bearerToken.replaceFirst(BEARER_TOKEN_PREFIX, "");

			if (bearerToken == null || bearerToken.isEmpty()) {
				// Throw an HTTP 401
				// throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
				throw new AuthenticationException("Authorization token is missing");
			}

			// Validate the token using your custom method
			if (!validateToken(bearerToken)) {
				// Throw an HTTP 401
				//throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
				throw new AuthenticationException("Authorization token is invalid");
			}
		}
		// Check if the token is missing

	}

	private boolean validateToken(String token) {
		// Implement your token validation logic here
		String DecodedToken = TokenHelper.DecodeToken(token);
		return "your-secure-token".equals(token); // Example check
	}

	@Override
	public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
		// Define authorization rules based on validated token or other criteria

		// For example, you might want to allow certain operations based on the user's token
		return new RuleBuilder()
			.allow().read().allResources().withAnyId().andThen()
			.allow().write().allResources().withAnyId().andThen()
			.denyAll() // Deny all other operations by default
			.build();
	}
}
