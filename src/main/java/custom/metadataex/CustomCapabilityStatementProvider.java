package custom.metadataex;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import custom.helper.CommonHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ca.uhn.fhir.context.FhirContext;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;

@Component
public class CustomCapabilityStatementProvider extends ServerCapabilityStatementProvider {

	//public static String CAPABILITY_STATEMENT_FILE_NAME = "capabilitystatement.json";
	private final FhirContext fhirContext;

	public CustomCapabilityStatementProvider(RestfulServer restfulServer) {
		super(restfulServer);
		this.fhirContext = restfulServer.getFhirContext();
	}

	// Try using "getServerCapabilityStatement" if "getServerConformance" causes errors
	@Override
	public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails requestDetails) {
		// Load the custom CapabilityStatement from the JSON file
		CapabilityStatement customCapabilityStatement = loadCustomCapabilityStatement();
		return customCapabilityStatement;
		// return customCapabilityStatement != null ? customCapabilityStatement : super.getServerConformance(theRequest, requestDetails);
	}

	private CapabilityStatement loadCustomCapabilityStatement() {
		try {
			// Load JSON file from classpath
			ClassPathResource resource = new ClassPathResource(CommonHelper.CAPABILITY_STATEMENT_FILE_NAME);
			InputStream inputStream = resource.getInputStream();
			String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

			// Parse the JSON to create a CapabilityStatement
			return (CapabilityStatement) fhirContext.newJsonParser().parseResource(json);
		} catch (Exception e) {
			e.printStackTrace();
			return null; // Return null if the file cannot be loaded or parsed
		}
	}
}
