package ordt.output.systemverilog.common;

import java.util.HashMap;
import java.util.Map;

public class SystemVerilogLocationMap {
    private static Integer nextAvailableId = null;
    private static Map<String, SystemVerilogLocationMap> locations = new HashMap<String, SystemVerilogLocationMap>();
    private int id = 0;
    private SystemVerilogModule module;  // module associated with this location
    
	public SystemVerilogLocationMap(int id, SystemVerilogModule module) {
		this.id = id;
		this.module = module;
	}
	
	public SystemVerilogLocationMap(int id) {
		this(id, null);
	}

	/** add a new location to the map and assign it a unique power 2 integer id.  id is returned (null returned if add failed) */
	public static Integer add(String name) {
		// if next id is uninitialized, add predefined locations
		if (nextAvailableId == null) {
			locations.put("NONE", new SystemVerilogLocationMap(0));
			locations.put("INTERNAL", new SystemVerilogLocationMap(1));
			locations.put("EXTERNAL", new SystemVerilogLocationMap(2));
			nextAvailableId = 4;
		}
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

	protected SystemVerilogModule getModule() {
		return module;
	}

	protected void setModule(SystemVerilogModule module) {
		this.module = module;
	}

	protected int getId() {
		return id;
	}

}
