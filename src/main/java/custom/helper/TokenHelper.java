package custom.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class TokenHelper {
	public static List<String> getScopesListByScopeString(String scopesString) {

		if (scopesString == null) {
			return new ArrayList<>();
		}

		String[] scopesArray = scopesString.trim().split("\\s+");

		return Arrays.stream(scopesArray).filter(scope -> !"".equals(scope))
			.collect(Collectors.toList());
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
