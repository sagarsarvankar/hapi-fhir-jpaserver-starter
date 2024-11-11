package ca.uhn.fhir.wellknown;

import com.fasterxml.jackson.databind.ObjectMapper;
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

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		String path = req.getPathInfo();
		resp.setContentType("application/json");

		if ("/smart-configuration".equals(path)) {
			Map<String, Object> smartConfig = Map.of(
				"issuer", "https://your.fhir.server",
				"authorization_endpoint", "https://your.fhir.server/auth/authorize",
				"token_endpoint", "https://your.fhir.server/auth/token",
				"jwks_uri", "https://your.fhir.server/.well-known/jwks.json",
				"response_types_supported", List.of("code"),
				"scopes_supported", List.of("openid", "profile", "fhirUser"),
				"grant_types_supported", List.of("authorization_code")
			);
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
