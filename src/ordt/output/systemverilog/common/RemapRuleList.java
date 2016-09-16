package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** signal name remapping rule class */
public class RemapRuleList { 
	private List<RemapRule> rules = new ArrayList<RemapRule>();
	public enum RemapRuleType {ADD_PREFIX, ADD_SUFFIX, REGEX_GROUPS};

	
	/** add a rule to the list - first in list is highest match priority */
	public void addRule(String pattern, RemapRuleType remapType, String opStr) {
		rules.add(new RemapRule(pattern, remapType, opStr));
	}
	
	/** return the first resulting name of a match */
	public String getNewName(String oldName) {
		// return result of first matching rule
		Iterator<RemapRule> iter = rules.iterator();
		while (iter.hasNext()) {
			String newName = iter.next().getNewName(oldName);
			if (newName != null) return newName;
		}
		return oldName;
	}
	
	/** signal name remapping rule class */
	private class RemapRule { 
		private String pattern;
		private RemapRuleType remapType;
		private String opStr;
		
		public RemapRule(String pattern, RemapRuleType remapType, String opStr) {
			this.pattern=pattern;
			this.remapType=remapType;
			this.opStr=opStr;
		}

		/** return the resulting name of a match or null if no match */
		public String getNewName(String oldName) {
			// test for a matching pattern
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(oldName);
			if (m.matches()) {
				switch (remapType) {
				case ADD_PREFIX: return opStr + oldName;
				case ADD_SUFFIX: return oldName + opStr;
				case REGEX_GROUPS:
					String newName = oldName;
					for (int gidx=1; gidx<=m.groupCount(); gidx++)
						newName = newName.replaceFirst("%" + gidx, m.group(gidx));
					return newName;
				}
			}
			return null;
		}
	}

}


