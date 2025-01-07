package custom.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import custom.helper.CommonHelper;
import custom.helper.HapiPropertiesConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

//@Interceptor
public class IncomingRequestPreProcessInterceptor {

	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
	public boolean interceptPreProcessedRequest(HttpServletRequest requestDetails,
														  HttpServletResponse theResponse) throws ServletException, IOException {

		boolean returnValue = true;

		String pathInfo = requestDetails.getPathInfo();
		if (pathInfo == null) {
			return returnValue;
		}
		// Since there is no support of _outputFormat other than
		// application/fhir+ndjson, hence changing the request parameter
		// to application/fhir+ndjson if the format is any other
		if (Enablesupport_default_to_application_fhir_ndjson_bulk_export()) {

			if (pathInfo.endsWith("/" + ProviderConstants.OPERATION_EXPORT)) {

				String outputFormat = requestDetails.getParameter("_outputFormat"); //.get("_outputFormat")[0];

				if ("application/ndjson".equals(outputFormat) || "ndjson".equals(outputFormat)) {
					String oldFormatEnc = URLEncoder.encode(outputFormat, StandardCharsets.UTF_8);
					String newFormatEnc = URLEncoder.encode("application/fhir+ndjson", StandardCharsets.UTF_8);
					String queryStr = requestDetails.getQueryString().replace(oldFormatEnc, newFormatEnc);
					String newPath = requestDetails.getServletPath() + requestDetails.getPathInfo() + "?" + queryStr;
					RequestDispatcher requestDispatcher = requestDetails.getRequestDispatcher(newPath);
					requestDispatcher.forward(requestDetails, theResponse);

					returnValue = false;
				}
			}
		}

		return returnValue;
		//
		//
	}

	private static boolean Enablesupport_default_to_application_fhir_ndjson_bulk_export() {
		boolean returnValue = false;

		try
		{
			HapiPropertiesConfig hapiConfig = new HapiPropertiesConfig();
			String getSecurityValue = hapiConfig.getdefault_to_application_fhir_ndjson_bulk_export();

			if (getSecurityValue.toLowerCase().equals("true")){
				returnValue = true;
			}
		}
		catch (Exception e){
		}

		return returnValue;
	}

	private static void TakeActionIfOutputFormatForBulkExportOtherThanfhirndjson(RequestDetails requestDetails){
		try{
			String operationType = requestDetails.getOperation();

			if (operationType != null &&
				operationType.toLowerCase().equals(CommonHelper.OPERATION_TYPE_EXPORT))
			{
				try
				{
					String outputFormat = requestDetails.getParameters().get("_outputFormat")[0];
					if (outputFormat == null || !outputFormat.toLowerCase().equals("application/fhir+ndjson")) {
						// Set the default outputFormat
						requestDetails.addParameter("_outputFormat", new String[] { "application/fhir+ndjson" });
					}
				} catch (Exception e) {
				}
			}
		}
		catch (Exception e){

		}
	}
}
