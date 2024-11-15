package custom.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class HapiPropertiesConfig {

	private Properties properties = new Properties();

	public HapiPropertiesConfig() {
		try (InputStream input = getClass().getClassLoader().getResourceAsStream("hapi.properties")) {
			if (input == null) {
				System.out.println("Sorry, unable to find hapi.properties");
				return;
			}
			properties.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public String getSmart_enabled() {
		return properties.getProperty("server.smart_enabled");
	}

	public String getAuthorization_endpoint() {
		return properties.getProperty("server.oauth.authorization_endpoint");
	}

	public String getEnablesecurity() {
		return properties.getProperty("server.enablesecurity");
	}

	public String getToken_endpoint_auth_signing_alg_values_supported() {
		return properties.getProperty("server.token_endpoint_auth_signing_alg_values_supported");
	}

	public String getSmart_scopes_supported() {
		return properties.getProperty("server.smart_scopes_supported");
	}

	public String getsmart_capabilities() {
		return properties.getProperty("server.smart_capabilities");
	}

	public String getCode_challenge_methods_supported() {
		return properties.getProperty("server.code_challenge_methods_supported");
	}

	public String getintrospection_endpoint() {
		return properties.getProperty("server.oauth.introspection_endpoint");
	}

	public String getgrant_types_supported() {
		return properties.getProperty("server.grant_types_supported");
	}

	public String getjwks_uri() {
		return properties.getProperty("server.oauth.jwks_uri");
	}

	public String getrevocation_endpoint() {
		return properties.getProperty("server.oauth.revocation_endpoint");
	}

	public String getsmart_token_endpoint_auth_methods_supported() {
		return properties.getProperty("server.smart_token_endpoint_auth_methods_supported");
	}

	public String getissuer() {
		return properties.getProperty("server.issuer");
	}

	public String gettoken_endpoint() {
		return properties.getProperty("server.oauth.token_endpoint");
	}

}
