package ordt.extract;

import java.util.HashMap;
import java.util.List;

import ordt.output.common.MsgUtils;

public class DefinedProperty {
	private String name;
	private DefinedPropertyType type;
	private String defaultValue;
	private int usage;   // encoding of components allowed to use this property
	private boolean hidden = false;
	private boolean userDefined = false;
	private boolean jsPassthru = false;  // indication that property is a jspec passthru
	
	public enum DefinedPropertyType { NUMBER, STRING, BOOLEAN, CONSTANT, 
		ADDRMAP, REG, REGSET, FIELD, FIELDSET, REF, SPECIAL };  // CONSTANT is not an allowed rdl type, but is used for jspec const assigns
	
	private static HashMap<String, DefinedPropertyType> typeEncodings = initTypeEncodings();
	
	// define property usage component encodings
	// 'signal' | 'addrmap' | 'reg' | 'regfile' | 'field' | 'all'
	public static final Integer SIGNAL = 1;
	public static final Integer ADDRMAP = 2;
	public static final Integer REG = 4;
	public static final Integer REGSET = 8;
	public static final Integer FIELD = 16;
	public static final Integer FIELDSET = 32;
	public static final Integer ANY = 63;  // any is AND of all comps
	private static HashMap<String, Integer> compEncodings = initCompEncodings();

	DefinedProperty(String name, DefinedPropertyType type, String defaultValue, int usage, boolean hidden, boolean userDefined, boolean jsPassthru) {
		super();
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
		this.usage = usage;
		this.hidden = hidden;
		this.userDefined = userDefined;
		this.jsPassthru = jsPassthru;
	}

	// ----------
	
	/** initialize the component encoding map 
	 * 	 'signal' | 'addrmap' | 'reg' | 'regfile' | 'field' | 'fieldstruct' | 'all'
    */
	private static HashMap<String, Integer> initCompEncodings() {
		HashMap<String, Integer> newList = new HashMap<String, Integer>();
		newList.put("signal", SIGNAL);
		newList.put("addrmap", ADDRMAP);
		newList.put("reg", REG);
		newList.put("regfile", REGSET);
		newList.put("regset", REGSET);
		newList.put("field", FIELD);
		newList.put("fieldset", FIELDSET);
		newList.put("fieldstruct", FIELDSET);
		newList.put("all", ANY);
		return newList;
	}

	/** initialize the property encoding map 
	 * 	  'boolean', 'number', ref', 'string', or a comp type
	 */
	private static HashMap<String, DefinedPropertyType> initTypeEncodings() {
		HashMap<String, DefinedPropertyType> newList = new HashMap<String, DefinedPropertyType>();
		newList.put("boolean", DefinedPropertyType.BOOLEAN);
		newList.put("string", DefinedPropertyType.STRING);
		newList.put("number", DefinedPropertyType.NUMBER);
		newList.put("integer", DefinedPropertyType.NUMBER); // jspec param type
		// ref types (disabled for now)
		//newList.put("ref", DefinedPropertyType.REF);
		//newList.put("addrmap", DefinedPropertyType.ADDRMAP);  
		//newList.put("reg", DefinedPropertyType.REG);
		//newList.put("regfile", DefinedPropertyType.REGSET);
		//newList.put("regset", DefinedPropertyType.REGSET);
		//newList.put("field", DefinedPropertyType.FIELD);
		//newList.put("fieldset", DefinedPropertyType.FIELDSET);
		//newList.put("fieldstruct", DefinedPropertyType.FIELDSET);
		return newList;
	}
	
	// ----------
	
	public String getName() {
		return name;
	}

	public DefinedPropertyType getType() {
		return type;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	/** return encoded usage value */
	public int getUsage() {
		return usage;
	}
	
	public boolean isAddrmapProperty() {
		return (usage & ADDRMAP) > 0;
	}
	
	public boolean isRegsetProperty() {
		return (usage & REGSET) > 0;
	}
	
	public boolean isRegProperty() {
		return (usage & REG) > 0;
	}
	
	public boolean isFieldsetProperty() {
		return (usage & FIELDSET) > 0;
	}
	
	public boolean isFieldProperty() {
		return (usage & FIELD) > 0;
	}

	public boolean isHidden() {
		return hidden;
	}

    public boolean isUserDefined() {
		return userDefined;
	}

    public boolean isJsPassthru() {
		return jsPassthru;
	}

	/** generate usage encoding from a list of strings */
	public static int getUsageEncoding(String name, List<String> components) {
		int usage = 0;
		for (String comp : components) {
			if (!compEncodings.containsKey(comp)) MsgUtils.errorMessage("invalid usage type " + comp + " specified in definition of user-defined property" + name);
			else usage += compEncodings.get(comp);
		}
		return usage;
	}

	/** get property type encoding from  string */
	public static DefinedPropertyType getTypeEncoding(String name, String typeStr) {
		if (!typeEncodings.containsKey(typeStr)) 
			MsgUtils.errorExit("invalid type " + typeStr + " specified in definition of user-defined property " + name);
		return typeEncodings.get(typeStr);
	}

	// -------------------- rdl define methods
	
	/** return an rdl define string for this property */
	public String getRdlDefineString() {
		return "property " + name + " {" + getRdlComponentClause() + getRdlTypeClause() + getRdlDefaultClause() + "};";
	}

	private String getRdlDefaultClause() {
		if ((defaultValue==null) || defaultValue.isEmpty()) return "";
		return "default=" + getRdlValue(defaultValue) + ";";
	}

	/** return rdl value with appropriate quoting based on type */
	public String getRdlValue(String value) {
		String cleanValue = value.replace("\"", "");
		if ((type==DefinedPropertyType.STRING) || (type==DefinedPropertyType.CONSTANT)) return "\"" + cleanValue + "\"";  // js CONSTANT are treated as string in rdl
		return cleanValue;
	}

	private String getRdlTypeClause() {
		String typeName="string";
		switch(type) {
		case STRING:
		case CONSTANT:
			typeName="string";
			break;
		case NUMBER:
			typeName="number";
			break;
		case BOOLEAN:
			typeName="boolean";
			break;
		default:
			MsgUtils.errorMessage("User defined rdl property " + name + " has an unsupported type.  Default string type will be used.");
		}
		return "type=" + typeName + ";";
	}

	private String getRdlComponentClause() {
		String compString = "all";
		if (usage==ANY) compString = "all";
		else {
			if ((usage & FIELD) > 0) compString = compString.isEmpty()? "field" : "|field";
			if ((usage & FIELDSET) > 0) compString = compString.isEmpty()? "fieldstruct" : compString + "|fieldstruct";
			if ((usage & REG) > 0) compString = compString.isEmpty()? "reg" : compString + "|reg";
			if ((usage & REGSET) > 0) compString = compString.isEmpty()? "regfile" : compString + "|regfile";
			if ((usage & ADDRMAP) > 0) compString = compString.isEmpty()? "addrmap" : compString + "|addrmap";
			if ((usage & SIGNAL) > 0) compString = compString.isEmpty()? "signal" : compString + "|signal";
		}
		return "component=" + compString + ";";
	}

	// -------------------- jspec define methods

	// typedef param root_cause = string;
	public String getJsDefineString() {
		return "typedef param " + name.substring(3) + " = " + getJsType() + ";";  // strip js_ prefix
	}

	private String getJsType() {
		String typeName="string";
		switch(type) {
		case STRING:
		case CONSTANT:
			typeName="string";
			break;
		case NUMBER:
			typeName="integer";
			break;
		case BOOLEAN:
			typeName="boolean";
			break;
		default:
			MsgUtils.errorMessage("User defined jspec parameter " + name + " has an unsupported type.  Default string type will be used.");
		}
		return typeName;
	}

	/** return jspec value with appropriate quoting based on type */
	public String getJsValue(String value) {
		String cleanValue = value.replace("\"", "");
		if (type==DefinedPropertyType.STRING) return "\"" + cleanValue + "\"";
		return cleanValue;
	}

}
