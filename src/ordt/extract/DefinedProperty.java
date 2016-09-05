package ordt.extract;

import java.util.HashMap;
import java.util.List;

public class DefinedProperty {
	private String name;
	private DefinedPropertyType type;
	private String defaultValue;
	private int usage;   // components allowed to use this property
	private boolean hidden = false;
	private boolean userDefined = false;
	
	public enum DefinedPropertyType { NUMBER, STRING, BOOLEAN, ADDRMAP, REG, REGSET, FIELD, FIELDSET, REF, SPECIAL };
	
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

	DefinedProperty(String name, DefinedPropertyType type, String defaultValue, int usage, boolean hidden, boolean userDefined) {
		super();
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
		this.usage = usage;
		this.hidden = hidden;
		this.userDefined = userDefined;
	}

	// ----------
	
	/** initialize the component encoding map 
	 * 	 'signal' | 'addrmap' | 'reg' | 'regfile' | 'field' | 'all'
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
		// ref types (disabled for now)
		//newList.put("ref", DefinedPropertyType.REF);
		//newList.put("addrmap", DefinedPropertyType.ADDRMAP);  
		//newList.put("reg", DefinedPropertyType.REG);
		//newList.put("regfile", DefinedPropertyType.REGSET);
		//newList.put("regset", DefinedPropertyType.REGSET);
		//newList.put("field", DefinedPropertyType.FIELD);
		//newList.put("fieldset", DefinedPropertyType.FIELDSET);
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

	/** generate usage encoding from a list of strings */
	public static int getUsageEncoding(String name, List<String> components) {
		int usage = 0;
		for (String comp : components) {
			if (!compEncodings.containsKey(comp)) Ordt.errorMessage("invalid usage type " + comp + " specified in definition of user-defined property" + name);
			else usage += compEncodings.get(comp);
		}
		return usage;
	}

	/** get property type encoding from  string */
	public static DefinedPropertyType getTypeEncoding(String name, String typeStr) {
		if (!typeEncodings.containsKey(typeStr)) 
			Ordt.errorExit("invalid type " + typeStr + " specified in definition of user-defined property " + name);
		return typeEncodings.get(typeStr);
	}

}
