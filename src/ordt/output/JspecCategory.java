/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output;

import java.util.HashMap;

import ordt.output.common.MsgUtils;

public class JspecCategory {
	private int value = 0;  // encoded category value
	private static Integer nextValue = 1;  // init next value (will be 2^n geo progression)
	private static HashMap<String, Integer> map = initMap();

	/** init category value by mapping to known categories in the map */
	public JspecCategory(String catString) {  
		if (catString == null) return;
		String [] cats = catString.split(",");
		// add value of each valid category in string
		for(int idx=0; idx<cats.length; idx++) {
			String cat = cats[idx].trim();
			if (!cat.isEmpty()) {
				if (map.containsKey(cat)) value += map.get(cat);  // add to encoded value
				// otherwise issue a warning and add the new category
				else {
					MsgUtils.warnMessage("Unknown category value " + cat + " detected.  Adding to category list.");
					map.put(cat, nextValue); nextValue *= 2;
					value += map.get(cat);  // add to encoded value
				}
			}
			//System.out.println("cat=" + cat);
		}
	}
	
	/** initialize set of known category values */
	private static HashMap<String, Integer> initMap() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		// DO NOT CHANGE THESE DEFAULT ENCODINGS
		map.put("STATIC_CONFIG", 1); 
		map.put("DYNAMIC_CONFIG", 2); 
		map.put("CONSTRAINED_CONFIG", 4); 
		map.put("STAT_COUNTER", 8); 
		map.put("ERROR_COUNTER", 16); 
		map.put("STATE", 32); 
		map.put("INTERRUPT", 64); 
		map.put("DIAGNOSTIC", 128); 
		map.put("DEBUG", 256); 
		map.put("CGATE_UNSAFE", 512); 
		nextValue = 1024; 
		return map;
	}
	
	/** returns true if at least one category is set */
	public boolean hasValue() {
		return value > 0;
	}
	
	/** returns the encoded integer value of ORed categories */
	public int getValue() {
		return value;
	}

	/** returns true if category specified by string is set */
	public boolean hasValue(String cat) {
		if (!map.containsKey(cat)) return false;
		return ((value & map.get(cat)) > 0);
	}

	@Override
	public String toString() {
		String retStr = "";
		for (String key: map.keySet()) {
			String prefix = retStr.isEmpty()? "" : ", ";
			if ((value & map.get(key)) > 0) retStr += prefix + key;
		}
		return retStr;
	}
	
	/** return the categoryMap */
	public static HashMap<String, Integer> getMap() {
		return map;
	}
	
	public static void main(String[] args) {
		JspecCategory jscat = new JspecCategory("STATE, INTERRUPT");
		System.out.println("catstr=" + jscat + ", has state=" + jscat.hasValue("STATE"));
		
		jscat = new JspecCategory("  STATE");
		System.out.println("catstr=" + jscat + ", has state=" + jscat.hasValue("STATE"));

		jscat = new JspecCategory("NEW");
		System.out.println("catstr=" + jscat + ", has state=" + jscat.hasValue("STATE"));
	
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + value;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JspecCategory other = (JspecCategory) obj;
		if (value != other.value)
			return false;
		return true;
	}
}
