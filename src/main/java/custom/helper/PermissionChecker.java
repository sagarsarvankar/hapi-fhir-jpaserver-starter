package custom.helper;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import custom.object.TokenDetails;

import java.util.List;

public class PermissionChecker {
	private static boolean checkSystemScopes(String resourceType, Scope.Permission requiredPermission,
												 List<String> grantedscopes) {
		boolean bApproved = true;

		if (!isApprovedByScopes(resourceType, requiredPermission, grantedscopes)) {
			bApproved = false;
		}

		return  bApproved;
	}

	private static boolean isApprovedByScopes(String resourceType, Scope.Permission requiredPermission, List<String> grantedScopes) {
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

						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @param grantedPermission
	 * @param requiredPermission
	 * @return true if the grantedPermission includes the requiredPermission; otherwise false
	 */
	private static boolean hasPermission(Scope.Permission grantedPermission, Scope.Permission requiredPermission) {
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

	public static boolean AllowAccess(TokenDetails tokenDetails, Scope.Permission neededPermission, String resourceType) {
		boolean returnValue = false;

		try {
			//Scope.Permission neededPermission = Scope.Permission.READ;
			//Set<String> resourceTypes;
			List<String> ScopesFromToken = TokenHelper.getScopesListByScopeString(tokenDetails.scope);
			returnValue = checkSystemScopes(resourceType, neededPermission, ScopesFromToken);
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
