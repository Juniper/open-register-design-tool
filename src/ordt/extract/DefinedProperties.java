package ordt.extract;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ordt.extract.DefinedProperty.DefinedPropertyType;
import ordt.parameters.ExtParameters;

/** class containing the set of defined model properties (extension of rdl property set) */
public class DefinedProperties {

	private static HashMap<String, DefinedProperty> propertySet = initDefinedProperties();  // set of all defined properties
	public static Set<String> userFieldPropertySet = new HashSet<String>();  // set of all user defined field property names
	public static Set<String> userFieldSetPropertySet = new HashSet<String>();  // set of all user defined field set property names
	public static Set<String> userRegPropertySet = new HashSet<String>();  // set of all user defined reg property names
	public static Set<String> userRegSetPropertySet = new HashSet<String>();  // set of all user defined regset property names
	
	/** initialize the list of defined properties */
	private static HashMap<String, DefinedProperty> initDefinedProperties() {
		HashMap<String, DefinedProperty> newList = new HashMap<String, DefinedProperty>();
		// common properties
		addProperty(newList, "name", DefinedPropertyType.STRING, "", DefinedProperty.ANY, false, false);
		addProperty(newList, "desc", DefinedPropertyType.STRING, "", DefinedProperty.ANY, false, false);
		// implicit default properties
		addProperty(newList, "donttest", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.ANY, false, false);
		addProperty(newList, "dontcompare", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.ANY, false, false);
		// interface properties
		addProperty(newList, "use_interface", DefinedPropertyType.STRING, "", DefinedProperty.REGSET | DefinedProperty.REG |DefinedProperty.FIELD, false, false);
		addProperty(newList, "use_new_interface", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.REGSET | DefinedProperty.REG |DefinedProperty.FIELD, false, false);
		// reg + regset properties
		addProperty(newList, "js_superset_check", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.REGSET | DefinedProperty.REG, false, false);
		addProperty(newList, "external", DefinedPropertyType.SPECIAL, null, DefinedProperty.REGSET | DefinedProperty.REG, false, false);
		addProperty(newList, "repcount", DefinedPropertyType.NUMBER, "1", DefinedProperty.REGSET | DefinedProperty.REG, true, false); // hidden
		// regset only properties
		addProperty(newList, "js_macro_name", DefinedPropertyType.STRING, "", DefinedProperty.REGSET, false, false);
		addProperty(newList, "js_macro_mode", DefinedPropertyType.STRING, "STANDARD", DefinedProperty.REGSET, false, false);  // TODO - add constant type and use for output gen
		addProperty(newList, "js_namespace", DefinedPropertyType.STRING, "", DefinedProperty.REGSET, false, false);
		addProperty(newList, "js_typedef_name", DefinedPropertyType.STRING, "", DefinedProperty.REGSET, false, false);
		addProperty(newList, "js_instance_name", DefinedPropertyType.STRING, "", DefinedProperty.REGSET, false, false);
		addProperty(newList, "js_instance_repeat", DefinedPropertyType.NUMBER, "1", DefinedProperty.REGSET, false, false);
		// reg only properties
		addProperty(newList, "category", DefinedPropertyType.STRING, "", DefinedProperty.REG, false, false);
		addProperty(newList, "js_attributes", DefinedPropertyType.STRING, "false", DefinedProperty.REG, false, false);
		addProperty(newList, "aliasedId", DefinedPropertyType.STRING, "false", DefinedProperty.REG, true, false);  // hidden
		addProperty(newList, "regwidth", DefinedPropertyType.NUMBER, "32", DefinedProperty.REG, false, false);
		addProperty(newList, "uvmreg_is_mem", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.REG, false, false);
		addProperty(newList, "cppmod_prune", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.REG, false, false);
		addProperty(newList, "uvmreg_prune", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.REG, false, false);
		// signal properties
		addProperty(newList, "cpuif_reset", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.SIGNAL, false, false);
		addProperty(newList, "field_reset", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.SIGNAL, false, false);
		addProperty(newList, "activehigh", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.SIGNAL, false, false);
		addProperty(newList, "activelow", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.SIGNAL, false, false);
		addProperty(newList, "signalwidth", DefinedPropertyType.NUMBER, "1", DefinedProperty.SIGNAL, false, false);
		// fieldset only properties
		addProperty(newList, "fieldstructwidth", DefinedPropertyType.NUMBER, "1", DefinedProperty.FIELDSET, false, false);
		// field properties
		addProperty(newList, "rset", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "rclr", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "woclr", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "woset", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "we", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "wel", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "swwe", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "swwel", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "hwset", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "hwclr", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "swmod", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "swacc", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "sticky", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "stickybit", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "intr", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "anded", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "ored", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "xored", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "counter", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "overflow", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "reset", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "fieldwidth", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "singlepulse", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "underflow", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "incr", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "decr", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "incrwidth", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "decrwidth", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "incrvalue", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "decrvalue", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "saturate", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "incrsaturate", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "decrsaturate", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "threshold", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "incrthreshold", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "decrthreshold", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "sw", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "hw", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "precedence", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "encode", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "resetsignal", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "mask", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "enable", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "haltmask", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "haltenable", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "halt", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "next", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "nextposedge", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "nextnegedge", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "maskintrbits", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);  
		addProperty(newList, "satoutput", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "sub_category", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		addProperty(newList, "rtl_coverage", DefinedPropertyType.BOOLEAN, "false", DefinedProperty.FIELD, false, false);
		
		// override allowed property set if input type is jspec
		if (Ordt.hasInputType(Ordt.InputType.JSPEC)) {
			putProperty(newList, "sub_category", DefinedPropertyType.STRING, "", DefinedProperty.REGSET | DefinedProperty.REG | DefinedProperty.FIELDSET | DefinedProperty.FIELD, false, false);
			putProperty(newList, "category", DefinedPropertyType.STRING, "", DefinedProperty.REGSET | DefinedProperty.REG, false, false);
			putProperty(newList, "js_attributes", DefinedPropertyType.STRING, "", DefinedProperty.REGSET | DefinedProperty.REG, false, false);
			putProperty(newList, "regwidth", DefinedPropertyType.NUMBER, "32", DefinedProperty.REGSET | DefinedProperty.REG, false, false);
			putProperty(newList, "address", DefinedPropertyType.NUMBER, null, DefinedProperty.REGSET | DefinedProperty.REG, false, false);
			putProperty(newList, "arrayidx1", DefinedPropertyType.NUMBER, null, DefinedProperty.REGSET | DefinedProperty.REG, true, false);  // hidden
			putProperty(newList, "addrinc", DefinedPropertyType.NUMBER, null, DefinedProperty.REGSET | DefinedProperty.REG, true, false);  // hidden
		}		

		return newList;
	}	

	// -------
	
	public static DefinedPropertyType getType(String name) {
		if (!propertySet.containsKey(name)) return null;
		return propertySet.get(name).getType();
	}

	public static String getDefaultValue(String name) {
		if (!propertySet.containsKey(name)) return null;
		return propertySet.get(name).getDefaultValue();
	}
	
	public static boolean isAddrmapProperty(String name) {
		if (!propertySet.containsKey(name)) return false;
		return propertySet.get(name).isAddrmapProperty();
	}
	
	public static boolean isRegsetProperty(String name) {
		if (!propertySet.containsKey(name)) return false;
		return propertySet.get(name).isRegsetProperty();
	}
	
	public static boolean isRegProperty(String name) {
		if (!propertySet.containsKey(name)) return false;
		return propertySet.get(name).isRegProperty();
	}
	
	public static boolean isFieldsetProperty(String name) {
		if (!propertySet.containsKey(name)) return false;
		return propertySet.get(name).isFieldsetProperty();
	}
	
	public static boolean isFieldProperty(String name) {
		if (!propertySet.containsKey(name)) return false;
		return propertySet.get(name).isFieldProperty();
	}

	public static Boolean isHidden(String name) {
		if (!propertySet.containsKey(name)) return null;
		return propertySet.get(name).isHidden();
	}

    public static Boolean isUserDefined(String name) {
		if (!propertySet.containsKey(name)) return null;
		return propertySet.get(name).isUserDefined();
	}

	// -------
	
	/** update or add a new property to the specified set of defined properties 
	 * 
	 * @param propSet - HashMap of DefinedProperties that will be updated with the new property 
	 * @param name - name of this property
	 * @param type -type of this property (DefinedPropertyType)
	 * @param defaultValue - string default value
	 * @param usage - encoded component types where this property is allowed
	 * @param hidden - true if this property is hidden and not referenced by user
	 * @param userDefined - true if this property is user-defined and not in base set
	 * */
	private static void putProperty(HashMap<String, DefinedProperty> propSet, String name, DefinedPropertyType type, String defaultValue, int usage, boolean hidden, boolean userDefined) {
		//System.out.println("DefinedProperties putProperty: name=" + name + ", type=" + type + ", default=" + defaultValue + ", usage=" + usage + ", hidden=" + hidden + ", userDefined=" + userDefined);
        propSet.put(name, new DefinedProperty(name, type, defaultValue, usage, hidden, userDefined));
	}
	
	/** add a new property to the specified set of defined properties, issue error if a duplicate
	 * 
	 * @param propSet - HashMap of DefinedProperties that will be updated with the new property 
	 * @param name - name of this property
	 * @param type -type of this property (DefinedPropertyType)
	 * @param defaultValue - string default value
	 * @param usage - encoded component types where this property is allowed
	 * @param hidden - true if this property is hidden and not referenced by user
	 * @param userDefined - true if this property is user-defined and not in base set
	 */
	private static void addProperty(HashMap<String, DefinedProperty> propSet, String name, DefinedPropertyType type, String defaultValue, int usage, boolean hidden, boolean userDefined) {
        if (propSet.containsKey(name))
        	Ordt.errorMessage("invalid user-defined property name " + name + " specified");
        else putProperty(propSet, name, type, defaultValue, usage, hidden, userDefined);
	}
	
	/** add a new non-hidden property to the list of defined properties */
	public static void addUserProperty(String name, String typeStr, String defaultValue, List<String> components) {
		// if restricted names are specified, then compare and exit if not compliant
		if (ExtParameters.rdlRestrictDefinedPropertyNames() && !name.startsWith("p_")) {
			Ordt.errorMessage("user-defined property " + name + " must begin with 'p_' when restrict_defined_property_names is set");
			return;
		}
        // generate usage encoding
		int usage = DefinedProperty.getUsageEncoding(name, components);
		// get property type
		DefinedPropertyType type = DefinedProperty.getTypeEncoding(name, typeStr);
		addProperty(propertySet, name, type, defaultValue, usage, false, true);
		//System.out.println("DefinedProperties addUserProperty: name=" + name + ", type=" + type + ", default=" + defaultValue + ", usage=" + usage);
		// save set of new user properties by component
		DefinedProperty userProp = propertySet.get(name);
		if (userProp.isFieldProperty()) userFieldPropertySet.add(name);
		if (userProp.isFieldsetProperty()) userFieldSetPropertySet.add(name);
		if (userProp.isRegProperty()) userRegPropertySet.add(name);
		if (userProp.isRegsetProperty()) userRegSetPropertySet.add(name);
	}

}
