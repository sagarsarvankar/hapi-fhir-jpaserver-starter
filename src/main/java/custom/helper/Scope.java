package custom.helper;
import custom.object.SubScope;

import java.util.Objects;
public class Scope {
	public static final String SCOPE_STRING_REGEX = "(user|patient|system)/" + "([a-zA-Z]+|\\*)" + "\\." + "(cruds|rs|read|write|\\*)" + "(\\?.*)?";
	// public static final String SCOPE_STRING_REGEX = "(user|patient|system)/" + "([a-zA-Z]+|\\*)" + "\\." + "(cruds|rs|read|write|\\*)";

	private final ContextType contextType;
	private final String resourceType;
	private final Permission permission;
	private final SubScope subScope;


	private SubScope GetSubScopeIfPresent(String scopeString){
		SubScope scTemp = null;
		// Get sub scopes (granular level)
		// eg: patient/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis
		try
		{


			String[] splitBasedOnQuestionMark = scopeString.split("\\?");
			String SubScopeName = "";
			String SubScopeValue = "";
			if (splitBasedOnQuestionMark[1] != null && splitBasedOnQuestionMark[1].length() > 0)
			{
				String[] splitBasedOnEqual = splitBasedOnQuestionMark[1].split("=");

				if (splitBasedOnEqual[1] != null && splitBasedOnEqual[1].length() > 0){
					SubScopeName = splitBasedOnEqual[0];

					String[] splitBasedOnPipe = splitBasedOnEqual[1].split("\\|");
					if (splitBasedOnPipe[1] != null && splitBasedOnPipe[1].length() > 0){
						SubScopeValue = splitBasedOnPipe[1];

						scTemp = new SubScope(SubScopeName, SubScopeValue);
					}
				}
			}
		} catch (Exception e) {

		}

		return scTemp;
	}

	/**
	 * @param scopeString a string of the form {@code (user|patient|system)/:resourceType.(read|write|*)}
	 */
	public Scope(String scopeString) {
		Objects.requireNonNull(scopeString, "scope string");
		if (!scopeString.matches(SCOPE_STRING_REGEX)) {
			contextType = null;
			resourceType = "";
			permission = null;
			subScope = null;
		} else {


			subScope = GetSubScopeIfPresent(scopeString);

			String[] split1 = scopeString.split("/");
			this.contextType = ContextType.from(split1[0]);
			String[] split2 = split1[1].split("\\.");

			//this.resourceType = split2[0].charAt(0) == '*' ? String.RESOURCE : String.from(split2[0]);
			this.resourceType = split2[0];

			//
			String operationTempPermission = split2[1];
			try{
				String[] split3 = split2[1].split("\\?");
				operationTempPermission = split3[0];
			} catch (Exception e) {
			}
			//

			this.permission = Permission.from(operationTempPermission);
		}
	}

	/**
	 * @param contextType
	 * @param resourceType "Resource" for all resource types
	 * @param permission
	 */
	public Scope(ContextType contextType, String resourceType, Permission permission, SubScope subScope) {
		this.contextType = contextType;
		this.resourceType = resourceType;
		this.permission = permission;
		this.subScope = subScope;
	}

	/**
	 * @return the contextType
	 */
	public ContextType getContextType() {
		return contextType;
	}

	/**
	 * @return the resourceType; "Resource" for all resource types
	 */
	public String getResourceType() {
		return resourceType;
	}

	public SubScope getSubscope() {
		return subScope;
	}

	public boolean hasSubscope() {
		if (subScope != null) {
			return true;
		}
		else
			return false;
	}
	/**
	 * @return the permission
	 */
	public Permission getPermission() {
		return permission;
	}

	/**
	 * @return a scopeString of the form {@code (user|patient|system)/:resourceType.(read|write|*)}
	 */
	@Override
	public String toString() {
		String resourceTypeString = resourceType;
			// resourceType == String.RESOURCE ? "*" : String();
		return contextType.value + "/" + resourceTypeString + "." + permission.value;
	}

	public static enum ContextType {
		PATIENT("patient"),
		USER("user"),
		SYSTEM("system");

		private final String value;

		/**
		 * @param string
		 */
		ContextType(String value) {
			this.value = value;
		}

		public String value() {
			return value;
		}

		public static ContextType from(String value) {
			for (ContextType c : ContextType.values()) {
				if (c.value.equals(value)) {
					return c;
				}
			}
			throw new IllegalArgumentException(value);
		}
	}

	public static enum Permission {
		READ("read"),
		WRITE("write"),
		ALL("*"),
		RS("rs"),
		CRUDS("cruds");

		private final String value;

		/**
		 * @param string
		 */
		Permission(String value) {
			this.value = value;
		}

		public String value() {
			return value;
		}

		public static Permission from(String value) {
			for (Permission p : Permission.values()) {
				if (p.value.equals(value)) {
					return p;
				}
			}
			throw new IllegalArgumentException(value);
		}
	}
}
