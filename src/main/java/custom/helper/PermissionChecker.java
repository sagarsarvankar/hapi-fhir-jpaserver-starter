package custom.helper;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import custom.object.SubScope;
import custom.object.TokenDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PermissionChecker {

	public ArrayList<Scope> approvedScopesList;
	public boolean m_allowAccess;

	public PermissionChecker()
	{
		approvedScopesList = new ArrayList<Scope>();
		m_allowAccess = false;
	}

	private boolean checkSystemScopes(String resourceType, Scope.Permission requiredPermission,
												 List<String> grantedscopes, RequestDetails requestDetails) {
		boolean bApproved = true;

		if (!isApprovedByScopes(resourceType, requiredPermission, grantedscopes, requestDetails)) {
			bApproved = false;
		}

		return  bApproved;
	}

	private boolean isApprovedByScopes(String resourceType, Scope.Permission requiredPermission,
												  List<String> grantedScopes,RequestDetails requestDetails) {
		if (grantedScopes == null) {
			return false;
		}

		for (String scope : grantedScopes) {
			Scope sc = new Scope(scope);
			// "Resource" is used for "*" which applies to all resource types

			if (sc.getResourceType().isEmpty() || sc.getResourceType().isBlank())
			{
			}
			else {
				if (sc.getResourceType().equals("*")
					|| sc.getResourceType().equals(resourceType)) {
					if (hasPermission(sc.getPermission(), requiredPermission)) {

						boolean bAllowAccess = true;
						//
						/*
						if (sc.hasSubscope())
						{
							SubScope subScTemp = sc.getSubscope();
							Map<String, String[]> urlParameters = requestDetails.getParameters();
							for (Map.Entry<String, String[]> entry : urlParameters.entrySet()) {
								String key = entry.getKey(); // Get the parameter name
								String value = entry.getValue()[0];


								String queryparamvalue = value;
								try {
									String[] split3 = queryparamvalue.split("\\|");
									value = split3[1];
								}
								catch (Exception e) {}

								if (
									key.equals(subScTemp.getName())
										&& value.equals(subScTemp.getValue())
								) {
									bAllowAccess = true;
									break;
								}
							}
						}
						//
						else
						{
							bAllowAccess = true;
						}
						*/

						if (bAllowAccess)
						{
							approvedScopesList.add(sc);
							m_allowAccess = true;
						}
					}
				}
			}
		}
		return m_allowAccess;
	}

	/**
	 * @param grantedPermission
	 * @param requiredPermission
	 * @return true if the grantedPermission includes the requiredPermission; otherwise false
	 */
	private boolean hasPermission(Scope.Permission grantedPermission, Scope.Permission requiredPermission) {
		boolean permittedValue = false;

		if (grantedPermission == Scope.Permission.ALL) {
			return true;
		} else {
			switch(grantedPermission.value()){
				case "cruds":
					if (requiredPermission.value().equals("read") || requiredPermission.value().equals("write")){
						permittedValue = true;
					}
					break;
				case "rs":
					if (requiredPermission.value().equals("read")){
						permittedValue = true;
					}
					break;
				case "read":
					if (requiredPermission.value().equals("read")){
						permittedValue = true;
					}
					break;
				case "write":
					if (requiredPermission.value().equals("read") || requiredPermission.value().equals("write")){
						permittedValue = true;
					}
					break;
			}

			if (permittedValue){
				return permittedValue;
			}
			else{
				return grantedPermission == requiredPermission;
			}
		}
	}

	public boolean AllowAccess(TokenDetails tokenDetails, Scope.Permission neededPermission,
										String resourceType, RequestDetails requestDetails) {
		boolean returnValue = false;

		try {
			//Scope.Permission neededPermission = Scope.Permission.READ;
			//Set<String> resourceTypes;
			List<String> ScopesFromToken = TokenHelper.getScopesListByScopeString(tokenDetails.scope);
			returnValue = checkSystemScopes(resourceType, neededPermission, ScopesFromToken, requestDetails);
		}
		catch (Exception e){

		}

		return returnValue;
	}


	public static Scope.Permission GetNeededPermission(RequestDetails requestDetails) {
		Scope.Permission neededPermission = Scope.Permission.READ;

		RequestTypeEnum requestType = requestDetails.getRequestType();

		switch (requestType)
		{
			case GET -> neededPermission = Scope.Permission.READ;
			case POST -> neededPermission = Scope.Permission.ALL;
			case PUT -> neededPermission = Scope.Permission.WRITE;
			case DELETE -> neededPermission = Scope.Permission.WRITE;
			default -> neededPermission = Scope.Permission.READ;
		}

		return neededPermission;
	}
}
