package ordt.output.systemverilog.common;

import java.util.HashMap;
import java.util.Map;

public class SystemVerilogLocationMap {
    private static int nextAvailableId = 4;  // next available id after defaults
    private static Map<String, SystemVerilogLocationMap> locations = initLocations();
    private int id = 0;
    private SystemVerilogModule module;  // module associated with this location
    private static final int DEFAULT_INTERNAL_ID = 1;
    private static final int DEFAULT_EXTERNAL_ID = 2;
    
	public SystemVerilogLocationMap(int id, SystemVerilogModule module) {
		this.id = id;
		this.module = module;
	}

	public SystemVerilogLocationMap(int id) {
		this(id, null);
	}
	
	/** add a default locations to the map */
	private static Map<String, SystemVerilogLocationMap> initLocations() {
		HashMap<String, SystemVerilogLocationMap> locs = new HashMap<String, SystemVerilogLocationMap>();
		locs.put("NONE", new SystemVerilogLocationMap(0));
		locs.put("INTERNAL", new SystemVerilogLocationMap(DEFAULT_INTERNAL_ID));
		locs.put("EXTERNAL", new SystemVerilogLocationMap(DEFAULT_EXTERNAL_ID));
		return locs;
	}

	/** add a new location to the map and assign it a unique power 2 integer id.  id is returned (null returned if add failed) */
	public static Integer add(String name) {
		// now add the new location
		Integer newId = locations.containsKey(name)? null : nextAvailableId;
		if (newId != null) {
			locations.put(name, new SystemVerilogLocationMap(newId));
			nextAvailableId <<= 1;
		}
		else System.err.println("Unable to add location map (" + name + ")");
		return newId;
	}

	/** return the Id associated with specified name or null if not found */
	public static Integer getId(String name) {
		SystemVerilogLocationMap map = locations.get(name);
		return (map == null)? null : map.getId();
	}

	/** return encoded value of all defined locations */
	public static int allLocations() {
		if (nextAvailableId < 2) return 0;
		return nextAvailableId - 1;
	}

	/** return the not of a defined location encoding */
	public static Integer notLocations(Integer loc) {
		if (loc == null) return null;
		return allLocations() ^ loc;
	}
	
	protected SystemVerilogModule getModule() {
		return module;
	}

	protected void setModule(SystemVerilogModule module) {
		this.module = module;
	}

	protected int getId() {
		return id;
	}

	/** return the id of the generic internal location */
	public static Integer getInternalId() {
		return DEFAULT_INTERNAL_ID;
	}

	/** return the id of the generic external location */
	public static Integer getExternalId() {
		return DEFAULT_EXTERNAL_ID;
	}

}
