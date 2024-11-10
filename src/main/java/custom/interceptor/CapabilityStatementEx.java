package custom.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Extension;
import org.springframework.stereotype.Component;

// @Component
@Interceptor
public class CapabilityStatementEx {
	@Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
	//public void customizeCapabilityStatement(RequestDetails theRequestDetails, CapabilityStatement capabilityStatement) {
	public void customize(IBaseConformance theCapabilityStatement){
		// Example: Set a custom publisher name

		CapabilityStatement  capabilityStatement = (CapabilityStatement) theCapabilityStatement;

		if (capabilityStatement != null) {
			capabilityStatement.setPublisher("Custom Publisher");

			// Example: Add a custom description
			capabilityStatement.setDescription("Custom description for our FHIR server");

			// Example: Add a custom REST operation
			CapabilityStatement.CapabilityStatementRestComponent rest = capabilityStatement.getRestFirstRep();
			CapabilityStatement.CapabilityStatementRestResourceOperationComponent customOperation =
				new CapabilityStatement.CapabilityStatementRestResourceOperationComponent();
			customOperation.setName("customOperation");
			customOperation.setDefinition("http://example.org/fhir/OperationDefinition/customOperation");

			rest.addOperation(customOperation);

			// Example: Add a security extension
			CapabilityStatement.CapabilityStatementRestSecurityComponent security = new CapabilityStatement.CapabilityStatementRestSecurityComponent();
			security.setCors(true);
			security.addService().setText("OAuth2");

			// Add a custom extension to the security component
			Extension customSecurityExtension = new Extension("http://example.org/fhir/StructureDefinition/custom-security-extension");
			customSecurityExtension.setValue(new org.hl7.fhir.r4.model.StringType("Custom Security Value"));
			security.addExtension(customSecurityExtension);

			rest.setSecurity(security);
		}
	}
}
