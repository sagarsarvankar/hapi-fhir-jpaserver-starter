package custom.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.BaseResponseTerminologyInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import custom.helper.CommonHelper;
import custom.helper.Scope;
import custom.helper.TokenHelper;
import custom.object.SubScope;
import custom.object.TokenDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static custom.helper.CommonHelper.GetTokenDetailsFromTokenString;

@Interceptor
public class GranularScopePostResponseInterceptor {

	private static final String SMART_SCOPE_KEY = "scope"; // Assume scope is stored here

	@Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
	public void filterResponse(RequestDetails requestDetails, IBaseResource responseResource) throws JsonProcessingException {

		String operationType = requestDetails.getOperation();

		if (CommonHelper.CONFORMANCE_PATH_METADATA.equals(requestDetails.getRequestPath()) ||
			CommonHelper.CONFORMANCE_PATH_WELLKNOWN_OPENID.equals(requestDetails.getRequestPath()) ||
			CommonHelper.CONFORMANCE_PATH_WELLKNOWN_SMART.equals(requestDetails.getRequestPath())) {

			return;
		}

		// This is for bulk related requests
		if (operationType != null) {
			return;
		}
		//

		//
		String resourceTypeTemp = requestDetails.getResourceName();

		boolean AllowWrite = false;
		boolean AllowRead = false;

		// below if is because, during pagination _getpages query parameter appears
		// with no resource in the URL.
		// Hence checking resourcetype is null and followed by getpages query parameter
		/*
		if (resourceTypeTemp == null
			|| resourceTypeTemp.isEmpty()
			|| resourceTypeTemp.isBlank()
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

			if (bGetPagesPresent) {
				return;
			}
		}
		*/
		//

		TokenDetails tokendets = GetTokenDetailsFromTokenString(requestDetails);

		String scope = tokendets.scope; //(String) requestDetails.getUserData();//(SMART_SCOPE_KEY);
		String resourceName = "";

		try
		{
			resourceName = requestDetails.getResourceName().toLowerCase();
		} catch (Exception e) {
			// return;
		}


		// Only process if the response is a Bundle or a resource we need to filter
		if (responseResource instanceof Bundle) {
			Bundle bundle = (Bundle) responseResource;

			if (resourceName.equals("condition")
				|| resourceName.equals("observation")
				|| resourceName.equals("")
			) {
				// Filter the resources within the Bundle

				List<Bundle.BundleEntryComponent> filteredEntries = GetFilteredEntries(scope, bundle);

				// Replace the Bundle's entries with the filtered list
				if (filteredEntries != null) {
					bundle.setEntry(filteredEntries);
				}
			}
		} else if (responseResource instanceof Resource) {
			// Handle individual resources (e.g., Patient, Condition)
			Resource OutPutResource = (Resource) responseResource;
			if (!EvaluateIfResourceValid(scope, (Resource) responseResource)) {
				throw new SecurityException("Access denied for the requested resource based on the provided scope.");
				//throw new AuthenticationException("Access denied for the requested resource based on the provided scope.");
			}
		}

	}

	private List<Bundle.BundleEntryComponent> GetFilteredEntries(String scope, Bundle bundle)
	{
		List<Bundle.BundleEntryComponent> filteredEntries = new ArrayList<>();

		for (int i = 0; i < bundle.getEntry().size(); i++) {
			Bundle.BundleEntryComponent SingleEntry = bundle.getEntry().get(i);
			Resource SingleResource = SingleEntry.getResource();

			//
			if (EvaluateIfResourceValid(scope, SingleResource))
			{
				filteredEntries.add(SingleEntry);
			}
			//
		}

		return filteredEntries;
	}


	private boolean EvaluateIfResourceValid(String scope, Resource SingleResource)
	{
		boolean ValidEntry = false;

		try
		{
			List<String> ScopesFromToken = TokenHelper.getScopesListByScopeString(scope);

			if (SingleResource instanceof org.hl7.fhir.r4.model.Condition
				|| SingleResource instanceof org.hl7.fhir.r4.model.Observation
			)
			{
				for (String scopeTemp : ScopesFromToken) {
					Scope sc = new Scope(scopeTemp);

					// "Resource" is used for "*" which applies to all resource types
					if (sc.getResourceType().isEmpty()
						|| sc.getResourceType().isBlank()
						|| !sc.hasSubscope()
					) {
						//continue;
					} else {
						SubScope subScopeTemp = sc.getSubscope();
						String resourceTypeString = SingleResource.fhirType();

						if (sc.getResourceType().equals("*")
							|| sc.getResourceType().equals(resourceTypeString)) {

							boolean isMatch = false;

							if (SingleResource instanceof org.hl7.fhir.r4.model.Condition) {
								org.hl7.fhir.r4.model.Condition conditionResource = (org.hl7.fhir.r4.model.Condition) SingleResource;

								if (conditionResource.getCategory().size() > 0)
								{
									isMatch = conditionResource.getCategory().stream()
										.anyMatch(category -> category.getCoding().stream()
											.anyMatch(
												coding -> subScopeTemp.getValue().toLowerCase().equals(coding.getCode().toLowerCase())
											)
										);
								}
								else {
									isMatch = true;
								}


							} else if (SingleResource instanceof org.hl7.fhir.r4.model.Observation)
							{
								// Example: Checking Observation with specific category
								org.hl7.fhir.r4.model.Observation observation = (org.hl7.fhir.r4.model.Observation) SingleResource;

								if (observation.getCategory().size() > 0)
								{
									// Example: Enforce "category=problem-list-item" filter from scope
									isMatch = observation.getCategory().stream()
										.anyMatch(category -> category.getCoding().stream()
											.anyMatch(
												coding -> subScopeTemp.getValue().toLowerCase().equals(coding.getCode().toLowerCase())
											)
										);
								}
								else {
									isMatch = true;
								}
							}

							if (isMatch) {
								// filteredEntries.add(SingleEntry);
								ValidEntry = isMatch;
								break;
							}
						}
					}

				}
			}
		}
		catch (Exception e)
		{
			ValidEntry = true;
		}

		return ValidEntry;
	}


	/**
	 * Checks if a resource is allowed based on the user's scope.
	 * @param scope The user's scope (e.g., Patient.read?gender=female).
	 * @param resource The FHIR resource to check.
	 * @return True if allowed, false otherwise.
	 */
	private boolean isResourceAllowedByScope(String scope, Resource resource) {
		// Parse scope for resource type and filters
		List<String> ScopesFromToken = TokenHelper.getScopesListByScopeString(scope);
		for (String scopeTemp : ScopesFromToken) {
			Scope sc = new Scope(scopeTemp);

			// "Resource" is used for "*" which applies to all resource types

			if (sc.getResourceType().isEmpty()
				|| sc.getResourceType().isBlank()
				|| !sc.hasSubscope()
			)
			{
				//continue;
			}
			else {
				SubScope subScopeTemp = sc.getSubscope();
				String resourceTypeString = resource.fhirType();

				if (sc.getResourceType().equals("*")
					|| sc.getResourceType().equals(resourceTypeString)) {

					if (resource instanceof org.hl7.fhir.r4.model.Condition)
					// Example: Checking Condition with specific category
					{
						org.hl7.fhir.r4.model.Condition condition = (org.hl7.fhir.r4.model.Condition) resource;

						// Example: Enforce "category=problem-list-item" filter from scope
						return condition.getCategory().stream()
							.anyMatch(category -> category.getCoding().stream()
								.anyMatch(
									coding -> subScopeTemp.getValue().toLowerCase().equals(coding.getCode().toLowerCase())
									/*
									coding -> CommonHelper.CONDITION_CATEGORY_PROBLEM_LIST_ITEM.equals(coding.getCode())
										|| CommonHelper.CONDITION_CATEGORY_ENCOUNTER_DIAGNOSIS.equals(coding.getCode())
										|| CommonHelper.CONDITION_CATEGORY_HEALTH_CONCERN.equals(coding.getCode())
									*/
								)
							);
					}
					else if (resource instanceof org.hl7.fhir.r4.model.Observation)
					// Example: Checking Observation with specific category
					{
						org.hl7.fhir.r4.model.Observation observation = (org.hl7.fhir.r4.model.Observation) resource;

						// Example: Enforce "category=problem-list-item" filter from scope
						return observation.getCategory().stream()
							.anyMatch(category -> category.getCoding().stream()
								.anyMatch(
									coding -> CommonHelper.OBSERVATION_CATEGORY_LABORATORY.equals(coding.getCode())
										|| CommonHelper.OBSERVATION_CATEGORY_SOCIAL_HISTORY.equals(coding.getCode())
										|| CommonHelper.OBSERVATION_CATEGORY_VITAL_SIGNS.equals(coding.getCode())
										|| CommonHelper.OBSERVATION_CATEGORY_SURVEY.equals(coding.getCode())
										|| CommonHelper.OBSERVATION_CATEGORY_SDOH.equals(coding.getCode())
								)
							);
					}
				}
			}
		}

		/*
		// Example: Checking for Patient resource with gender=female
		if (scope.startsWith("Patient.read") && resource instanceof org.hl7.fhir.r4.model.Patient) {
			org.hl7.fhir.r4.model.Patient patient = (org.hl7.fhir.r4.model.Patient) resource;

			// Example: Enforce "gender=female" filter from scope
			return patient.getGender() != null && patient.getGender().toCode().equals("female");
		}
		*/



		// Deny access by default
		return false;
	}
}