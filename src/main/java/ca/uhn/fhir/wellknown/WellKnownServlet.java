package ca.uhn.fhir.wellknown;

import com.fasterxml.jackson.databind.ObjectMapper;
import custom.helper.HapiPropertiesConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = {"*"}, allowCredentials = "false")
public class WellKnownServlet extends HttpServlet {
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final String STRING_SPLIT_PARAM = ",";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		String path = req.getPathInfo();
		resp.setContentType("application/json");

		HapiPropertiesConfig hapiConfig = new HapiPropertiesConfig();

		//
		String[] token_endpoint_auth_signing_alg_values_supported = hapiConfig.getToken_endpoint_auth_signing_alg_values_supported().split(STRING_SPLIT_PARAM);
		String[] capabilities = hapiConfig.getsmart_capabilities().split(STRING_SPLIT_PARAM);
		String[] code_challenge_methods_supported = hapiConfig.getCode_challenge_methods_supported().split(STRING_SPLIT_PARAM);
		String introspection_endpoint = hapiConfig.getintrospection_endpoint();
		String[] grant_types_supported = hapiConfig.getgrant_types_supported().split(STRING_SPLIT_PARAM);
		String jwks_uri = hapiConfig.getjwks_uri();
		String revocation_endpoint = hapiConfig.getrevocation_endpoint();
		String[] token_endpoint_auth_methods_supported = hapiConfig.getsmart_token_endpoint_auth_methods_supported().split(STRING_SPLIT_PARAM);
		String issuer = hapiConfig.getissuer();
		String authorization_endpoint = hapiConfig.getAuthorization_endpoint();
		String token_endpoint = hapiConfig.gettoken_endpoint();
		String[] smart_capabilities = hapiConfig.getsmart_capabilities().split(STRING_SPLIT_PARAM);
		//

		if ("/smart-configuration".equals(path)) {
			Map<String, Object> smartConfig = Map.ofEntries(
				Map.entry("token_endpoint_auth_signing_alg_values_supported", token_endpoint_auth_signing_alg_values_supported),
				Map.entry("capabilities", capabilities),
				Map.entry("code_challenge_methods_supported", code_challenge_methods_supported),
				Map.entry("introspection_endpoint", introspection_endpoint),
				Map.entry("grant_types_supported", grant_types_supported),
				Map.entry("jwks_uri", jwks_uri),
				Map.entry("revocation_endpoint", revocation_endpoint),
				Map.entry("token_endpoint_auth_methods_supported", token_endpoint_auth_methods_supported),
				Map.entry("issuer", issuer),
				Map.entry("authorization_endpoint", authorization_endpoint),
				Map.entry("token_endpoint", token_endpoint)
			);
			/*
			Map<String, Object> smartConfig = Map.of(
				"issuer", "https://your.fhir.server",
				"authorization_endpoint", "https://your.fhir.server/auth/authorize",
				"token_endpoint", "https://your.fhir.server/auth/token",
				"jwks_uri", "https://your.fhir.server/.well-known/jwks.json",
				"response_types_supported", List.of("code"),
				"scopes_supported", List.of("openid", "profile", "fhirUser"),
				"grant_types_supported", List.of("authorization_code")
			);
			*/
			objectMapper.writeValue(resp.getOutputStream(), smartConfig);
		} else if ("/openid-configuration".equals(path)) {
			// Handle OpenID configuration if needed
			Map<String, Object> openIdConfig = Map.of(
				"issuer", "https://your.fhir.server",
				"authorization_endpoint", "https://your.fhir.server/auth/authorize",
				"token_endpoint", "https://your.fhir.server/auth/token",
				"userinfo_endpoint", "https://your.fhir.server/userinfo",
				"jwks_uri", "https://your.fhir.server/.well-known/jwks.json"
			);
			objectMapper.writeValue(resp.getOutputStream(), openIdConfig);
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
