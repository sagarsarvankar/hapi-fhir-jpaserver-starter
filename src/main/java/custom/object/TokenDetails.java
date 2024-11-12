package custom.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenDetails {
	public String nbf;
	public String exp;
	public String iss;
	public String[] aud;
	public String client_id;
	public String sub;
	public String auth_time;
	public String idp;
	public String jti;
	public String sid;
	public String iat;
	public String patient;
	public String patient_id;
	public String userloggedin;
	public String encounter;
	public String upn;
	public String need_patient_banner;
	public String smart_style_url;
	public String scope;
	public String[] group;
	public String[] amr;
}
