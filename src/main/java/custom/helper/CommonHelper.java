package custom.helper;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import custom.object.MoreConfig;
import custom.object.TenantDetails;
import custom.object.TokenDetails;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CommonHelper {
	public static String AUTHORIZATION_HEADER = "Authorization";
	public static String BEARER_TOKEN_PREFIX = "Bearer ";
	public static String CAPABILITY_STATEMENT_FILE_NAME = "capabilitystatement.json";
	public static String MORE_CONFIG_FILE_NAME = "moreconfig.json";
	public static String HAPI_PROPERTIES_FILE_NAME = "hapi.properties";
	public static String RESOURCE_Provenance = "Provenance";
	public static String OPERATION_TYPE_EXPORT = "$export";
	public static String OPERATION_TYPE_EXPORT_POLL_STATUS = "$export-poll-status";
	public static String URL_PARAMETER_INCLUDE = "_include";
	public static String URL_PARAMETER_GETPAGES = "_getpages";
	public static String URL_PARAMETER_CATEGORY = "category";

	public static String CONDITION_CATEGORY_PROBLEM_LIST_ITEM = "problem-list-item";
	public static String CONDITION_CATEGORY_ENCOUNTER_DIAGNOSIS = "encounter-diagnosis";
	public static String CONDITION_CATEGORY_HEALTH_CONCERN = "health-concern";

	public static String OBSERVATION_CATEGORY_LABORATORY = "laboratory";
	public static String OBSERVATION_CATEGORY_SOCIAL_HISTORY = "social-history";
	public static String OBSERVATION_CATEGORY_VITAL_SIGNS = "vital-signs";
	public static String OBSERVATION_CATEGORY_SURVEY = "survey";
	public static String OBSERVATION_CATEGORY_SDOH = "sdoh";

	public static final String CONFORMANCE_PATH_METADATA = "metadata";
	public static final String CONFORMANCE_PATH_WELLKNOWN_OPENID = ".well-known/openid-configuration";
	public static final String CONFORMANCE_PATH_WELLKNOWN_SMART = ".well-known/smart-configuration";

	public static final String TENANT_HEADER_NAME = "X-FHIR-TENANT-ID";
	public static final String TENANT_NAME_DEFAULT = "default";

	public static String GetTenantNameBasedOnHeader(String tenantname){
		// String tenantId = requestDetails.getHeader(CommonHelper.TENANT_HEADER_NAME);
		if (tenantname == null || tenantname.isEmpty()) {
			// throw new IllegalArgumentException("Tenant ID is missing");
			tenantname = CommonHelper.TENANT_NAME_DEFAULT;
		}

		return tenantname;
	}

	public static MoreConfig  GetMoreConfigFromConfig() {
		MoreConfig moreConfig = new MoreConfig();

		try {
			ClassPathResource resource = new ClassPathResource(CommonHelper.MORE_CONFIG_FILE_NAME);
			InputStream inputStream = resource.getInputStream();
			String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

			// JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

			Gson gson = new Gson();
			moreConfig = gson.fromJson(json, MoreConfig.class);
			Boolean bb = false;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return moreConfig;
	}

	public static TokenDetails GetTokenDetailsFromTokenString(RequestDetails requestDetails) throws JsonProcessingException {
		String bearerToken = requestDetails.getHeader(AUTHORIZATION_HEADER);

		if (bearerToken == null || bearerToken.isEmpty()) {
			// Throw an HTTP 401
			// throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
			throw new AuthenticationException("Authorization token is missing");
		}

		bearerToken = bearerToken.replaceFirst(BEARER_TOKEN_PREFIX, "");
		String DecodedToken = TokenHelper.DecodeToken(bearerToken);

		ObjectMapper objectMapper = new ObjectMapper();
		TokenDetails tokendets = objectMapper.readValue(DecodedToken, TokenDetails.class);

		return tokendets;
	}
}
