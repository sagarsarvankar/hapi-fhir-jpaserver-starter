package custom.helper;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import custom.object.TokenDetails;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.h2.util.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class TokenHelper {

	public static boolean IsTokenActiveAfterIntrospection(String token) {

		boolean returnval = true;
		HapiPropertiesConfig hapiPropertiesConfig = new HapiPropertiesConfig();

		String introspectUrl = hapiPropertiesConfig.getintrospection_endpoint();
		String introspectclientid = hapiPropertiesConfig.getclientid_for_introspection_endpoint();
		String introspectclientsecret = hapiPropertiesConfig.getclientsecret_for_introspection_endpoint();

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost postRequest = new HttpPost(introspectUrl);

			// Set Basic Auth for client credentials
			String auth = introspectclientid + ":" + introspectclientsecret;
			String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
			postRequest.setHeader("Authorization", "Basic " + encodedAuth);
			postRequest.setHeader("Content-Type", "application/x-www-form-urlencoded");

			// Add token in the body
			String requestBody = "token=" + token + "&token_type=access_token";
			StringEntity entity = new StringEntity(requestBody);
			postRequest.setEntity(entity);

			// Execute request
			try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
				if (response.getStatusLine().getStatusCode() == 200) {
					String responseBody = EntityUtils.toString(response.getEntity());
					Gson gsonResponse = new Gson();
					JsonObject parsedresponse = gsonResponse.fromJson(responseBody, JsonObject.class);
					String activeflag = parsedresponse.get("active").getAsString();
					if (activeflag.toLowerCase().equals("false")) {
						returnval = false;
					}
				}
			}
		} catch (Exception e) {
			returnval = true;
		}

		return returnval;
	}

	public static List<String> getScopesListByScopeString(String scopesString) {

		if (scopesString == null) {
			return new ArrayList<>();
		}

		String[] scopesArray = scopesString.trim().split("\\s+");

		return Arrays.stream(scopesArray).filter(scope -> !"".equals(scope))
			.collect(Collectors.toList());
	}

	public static boolean isTokenExpired(TokenDetails tokenDetails) {
		Long expDate = Long.parseLong(tokenDetails.exp);
		// Date expirationDate = new Date(expDate * 1000);

		Instant expirationInstant = Instant.ofEpochSecond(expDate);

		// Get the current time as an Instant in UTC
		Instant currentInstant = Instant.now();

		// Compare the instants
		boolean isExpired = currentInstant.isAfter(expirationInstant);
		return isExpired;
	}

	public static String DecodeToken(String token) {
		String TokenString = "";

		String[] split_string = token.split("\\.");
		String base64EncodedHeader = split_string[0];
		String base64EncodedBody = split_string[1];
		String base64EncodedSignature = split_string[2];

		Base64.Decoder decoder = Base64.getUrlDecoder();
		TokenString = new String(decoder.decode(base64EncodedBody));

		return TokenString;
	}

	public static String getScopesStringFromScopesList(List<String> scopesList) {
		if (scopesList == null) {
			return "";
		}

		return String.join(" ", scopesList);
	}
}
