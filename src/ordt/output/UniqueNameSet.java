package ordt.output;

import java.util.HashMap;

import ordt.parameters.Utils;

/** class to generate and store unique names based on equality of specified type T instances.
 * Provides a hash from InstanceProperties T -> String which is set/read via getName().
 * Optionally provides a hash from String -> InstanceProperties T which is set via setStringMap() and used
 * to provide a String -> String lookup via getNameFromString() */
public class UniqueNameSet<T extends InstanceProperties> {
	
	private HashMap<T, String> names = new HashMap<T, String>();
	private HashMap<String, T> stringMap = new HashMap<String, T>();
	private boolean reuseNames = false; 
	private boolean useNumericNames = false;
	private boolean allowStringMap = false;
	private String staticPrefix = "";
	private int uniqueCount = 0;  // count to be used for name generation when useGenericNames is specified
	
	/** define unique names based on equivalence of InstanceProperty child class instances.
	 * @param reuseNames - if true, equal instances will use the same name  
	 * @param useNumericNames - if true, names will use an integer rather than catenated instance path
	 * @param allowStringMap - if true, names will also be loaded into hash for numerics for String->String lookup
	 * @param staticPrefix - prefix to be used for all names
	 */
	public UniqueNameSet(boolean reuseNames, boolean useNumericNames, boolean allowStringMap, String staticPrefix) {
		this.reuseNames = reuseNames; 
		this.useNumericNames = useNumericNames;
		this.allowStringMap = allowStringMap;
		if (staticPrefix != null) this.staticPrefix = staticPrefix;
	}

    /** getName return info - string name and indication if not seen previously */
	public class UniqueNameSetInfo {
		public UniqueNameSetInfo(String name, boolean isNew) {
			this.name = name;
			this.isNew = isNew;
		}
		public String name;
		public boolean isNew = false;
	}
	
    /** return the name of an instance based on rules for this UniqueNameSet. New instances and their corresponding
     *  name will be saved in names hashMap depending on parameters.
     *  If reuseName is specified, then look for matching instance and return existing name on match.
	 * @param inst - instance whose name will be returned 
	 * @param staticPrefixOverride - if non-null, string will replace the default staticPrefix 
	 * @param prefix - additional prefix to be added to this instance's name (if name not being reused, after staticPrefix) 
	 * @param suffix - suffix to be added to this instance's name (if name not being reused)
	 */ 
	public UniqueNameSetInfo getName(T inst, String staticPrefixOverride, String prefix, String suffix) {
		boolean isNew = true;  // by default, new name
		String name = null;
		// save info in names hash if reusing classes or string mapping is enabled
		boolean useNamesHash = reuseNames || (allowStringMap && useNumericNames);  // numerics will be treated as reuse classes if allowStringMap specified
		// look for a matching name if using hashmap
		if (useNamesHash && names.containsKey(inst)) {
			name = names.get(inst);
			isNew = false;
		}
		// else create a new name 
		else {
			// create new name
			String newStaticPrefix = (staticPrefixOverride != null)? staticPrefixOverride : staticPrefix;
			name = Utils.usCatenate(newStaticPrefix, prefix);  // start with prefixes
			if (useNumericNames) name = Utils.usCatenate(name, Integer.toString(uniqueCount++));  // use integer name
			else {
				String instStr = (inst == null) ? "" : inst.getBaseName();
				name = Utils.usCatenate(name, instStr);  // use instance path name
			}
			name = Utils.usCatenate(name, suffix);  // add the suffix
			if (useNamesHash) names.put(inst, name);  // save new name to hashmap
		}
		//if (name.startsWith("block")) System.out.println("UniqueNameSet getName: name=" + name + ", isNew=" + isNew);
		return new UniqueNameSetInfo(name, isNew);
	}
	
    /** return the name of an instance with no name suffix based on rules for this UniqueNameSet. New instances and their corresponding
     *  name will be saved in names hashMap depending on parameters.
     *  If reuseName is specified, then look for matching instance and return existing name on match.
	 * @param inst - instance whose name will be returned 
	 * @param prefix - additional prefix to be added to this instance's name (if name not being reused, after staticPrefix) 
	 */ 
	public UniqueNameSetInfo getName(T inst, String prefix) {
		return getName(inst, null, prefix, null);
	}
	
    /** add an entry to stringMap (String->InstanceProperties) */ 
	public void setStringMap(String string, T inst) {
		stringMap.put(string, inst);
	}
	
    /** return the name using a 2 stage lookup - String->InstanceProperties from stringMap and then
     * InstanceProperties-> string using getNames.  Returns null if stringMap lookup fails.
	 */ 
	public String getNameFromString(String inString, String prefix) {
		if (!stringMap.containsKey(inString)) return null;
		T inst = stringMap.get(inString);
		if (inst==null) return null;
		UniqueNameSetInfo rinfo = getName(inst, prefix);
		return rinfo.name;
	}
	    
}
