package ordt.extract;

import java.util.HashMap;
import java.util.List;

/** class containing the set of defined model properties (extension of rdl property set) */
public class DefinedProperties {
	public enum DefinedPropertyType { NUMBER, STRING, BOOLEAN, ADDRMAP, REG, REGSET, FIELD, FIELDSET, REF };
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

	private static HashMap<String, DefinedProperty> propertySet = initDefinedProperties();  // set of defined properties
	
	// ---------
	
	/** initialize the list of defined properties */
	private static HashMap<String, DefinedProperty> initDefinedProperties() {
		HashMap<String, DefinedProperty> newList = new HashMap<String, DefinedProperty>();
		
		return newList;
	}
		
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
	newList.put("ref", DefinedPropertyType.REF);
	newList.put("addrmap", DefinedPropertyType.ADDRMAP);  // specific ref types
	newList.put("reg", DefinedPropertyType.REG);
	newList.put("regfile", DefinedPropertyType.REGSET);
	newList.put("regset", DefinedPropertyType.REGSET);
	newList.put("field", DefinedPropertyType.FIELD);
	newList.put("fieldset", DefinedPropertyType.FIELDSET);
	return newList;
}
	
	/** add a new property to the list of defined properties */
	private static void addProperty(String name, DefinedPropertyType type, String defaultValue, int usage, boolean hidden) {
		System.out.println("DefinedProperties addProperty: name=" + name + ", type=" + type + ", default=" + defaultValue + ", usage=" + usage + ", hidden=" + hidden);
		propertySet.put(name, new DefinedProperty(name, type, defaultValue, usage, hidden));
	}
	
	/** add a new non-hidden property to the list of defined properties */
	public static void addProperty(String name, String typeStr, String defaultValue, List<String> components) {
        // generate usage encoding
		int usage = 0;
		for (String comp : components) {
			if (!compEncodings.containsKey(comp)) Ordt.errorMessage("invalid usage type " + comp + " specified in definition of user-defined property " + name);
			else usage += compEncodings.get(comp);
		}
		// get property type
		if (!typeEncodings.containsKey(typeStr)) 
			Ordt.errorExit("invalid type " + typeStr + " specified in definition of user-defined property " + name);
		DefinedPropertyType type = typeEncodings.get(typeStr);
		addProperty(name, type, defaultValue, usage, false);
	}
	

}
