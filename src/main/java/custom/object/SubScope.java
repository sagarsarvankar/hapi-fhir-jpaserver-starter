package custom.object;

import custom.helper.Scope;

public class SubScope {
	public String m_name;
	public String m_value;

	public SubScope(String name, String value) {
		m_name = name;
		m_value = value;
	}

	public String getName() {
		return m_name;
	}

	public String getValue() {
		return m_value;
	}
}
