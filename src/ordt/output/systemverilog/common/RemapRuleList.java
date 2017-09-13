package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** signal name remapping rule class */
public class RemapRuleList { 
	private List<RemapRule> rules = new ArrayList<RemapRule>();  // TODO  build cache of remap info?  ...from/to remapping also?
	public enum RemapRuleType {SAME, ADD_PREFIX, ADD_SUFFIX, DIRECT_MAP, REGEX_GROUPS};
	/*
	 * SAME - use original name (or alternate base name if specified) on match
	 * ADD_PREFIX - add prefix specified by opStr on match
	 * ADD_SUFFIX - add suffix specified by opStr on match
	 * DIRECT_MAP - change to string specified by opStr on match
	 * REGEX_GROUPS - change to regex template specified by opStr on match
	 */

	
	/** add a pattern only rule to the list - first in list is highest match priority */
	public void addRule(String pattern, RemapRuleType remapType, String opStr) {
		rules.add(new RemapRule(pattern, null, null, remapType, opStr));
	}
	
	/** add a location only rule to the list - first in list is highest match priority */
	public void addRule(Integer from, Integer to, RemapRuleType remapType, String opStr) {
		rules.add(new RemapRule(null, from, to, remapType, opStr));
	}
	
	/** add a pattern/location rule to the list - first in list is highest match priority */
	public void addRule(String pattern, Integer from, Integer to, RemapRuleType remapType, String opStr) {
		rules.add(new RemapRule(pattern, from, to, remapType, opStr));
	}
	
	/** return the first resulting name of a match */
	public String getRemappedName(String oldName, String altBaseName, Integer sigFrom, Integer sigTo, boolean returnNullOnMismatch) {
		// return result of first matching rule
		Iterator<RemapRule> iter = rules.iterator();
		while (iter.hasNext()) {
			String newName = iter.next().getRemappedName(oldName, altBaseName, sigFrom, sigTo);
			if (newName != null) return newName;
		}
		return returnNullOnMismatch? null : oldName; // no rule match, so just return null or the original name
	}
	
	/** return the first resulting name of a match w/ no altBaseName and no null return */
	public String getRemappedName(String oldName, Integer sigFrom, Integer sigTo) {
		return getRemappedName(oldName, null, sigFrom, sigTo, false);
	}
	
	/** signal name remapping rule class */
	private class RemapRule { 
		private String pattern; // name pattern to be matched (null to ignore)
		private Integer from;  // from location encoding to be matched (null to ignore)
		private Integer to;  // to location encoding to be matched (null to ignore)
		private RemapRuleType remapType;
		private String opStr;
		
		public RemapRule(String pattern, Integer from, Integer to, RemapRuleType remapType, String opStr) {
			this.pattern=pattern;
			this.from=from;
			this.to=to;
			this.remapType=remapType;
			this.opStr=opStr;
		}
		
		/** return the resulting name of a match or null if no match
		 * 
		 * @param oldName - old signal name to be compared against rules and changed (required)
		 * @param altBaseName - if non-null, this string will be used to generate match output rather than oldName (for prefix/suffix ops)
		 * @param sigFrom - from location for this signal (null to ignore) 
		 * @param sigTo - to location for this signal (null to ignore)
		 * @return
		 */
		public String getRemappedName(String oldName, String altBaseName, Integer sigFrom, Integer sigTo) {
			// test for location match
			if (!isLocationMatch(sigFrom, sigTo)) return null;
			// test for a matching pattern
			boolean isMatch = true;  // set match to true assuming null pattern
			Matcher matcher = null;
			if (pattern!=null) {
				Pattern p = Pattern.compile(pattern);
				matcher = p.matcher(oldName);
				isMatch = matcher.matches();
			}
			if (isMatch) {
				String baseName = (altBaseName != null)? altBaseName : oldName;
				switch (remapType) {
				case SAME: return baseName;
				case ADD_PREFIX: return opStr + baseName;
				case ADD_SUFFIX: return baseName + opStr;
				case DIRECT_MAP: return opStr;
				case REGEX_GROUPS:
					String newName = opStr;
					if (pattern==null) return opStr;  // no pattern matching if no pattern
					for (int gidx=1; gidx<=matcher.groupCount(); gidx++)
						newName = newName.replaceFirst("%" + gidx, matcher.group(gidx));
					return newName;
				}
			}
			return null;
		}
				
		/** return true if signal to/from matches current rule
		 * 
		 * @param sigFrom - from location for this signal (null to ignaore) 
		 * @param sigTo - to location for this signal (null to ignore)
		 * @return
		 */
		private boolean isLocationMatch(Integer sigFrom, Integer sigTo) {
			// perform matched based on to/from if specified
			boolean fromMatch = (from==null) || ((sigFrom!=null) && ((sigFrom&from) != 0)); // if rule has from/to only match if non-null sig from/to
			boolean toMatch = (to==null) || ((sigTo!=null) && ((sigTo&to) != 0));
			return fromMatch && toMatch;
		}

	}
}


