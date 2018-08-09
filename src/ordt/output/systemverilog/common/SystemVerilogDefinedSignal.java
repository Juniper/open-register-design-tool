package ordt.output.systemverilog.common;

import java.util.HashMap;
import java.util.Map;

public class SystemVerilogDefinedSignal {
	private String prefix;
	private String suffix;
	private Integer from;
	private Integer to;
	private Map<String, String> tags;  // optional info for this signal
	
	public SystemVerilogDefinedSignal(Integer from, Integer to, String prefix, String suffix) {
		this.from = from;
		this.to = to;
		this.prefix = prefix;
		this.suffix = suffix;
		//System.out.println("SystemVerilogDefinedSignal: from=" + from + ", to=" + to);
	}
	public SystemVerilogDefinedSignal(Integer from, Integer to, String prefix, String suffix, String tagName, String tagValue) {
		this(from, to, prefix, suffix);
		addTag(tagName, tagValue);
	}

	public String getPrefix() {
		return prefix;
	}
	public String getSuffix() {
		return suffix;
	}
	public Integer getFrom() {
		//System.out.println("SystemVerilogDefinedSignal getFrom: from=" + from);
		return from;
	}
	public Integer getTo() {
		//System.out.println("SystemVerilogDefinedSignal getTo: to=" + from);
		return to;
	}
	
	public boolean hasTag(String tagName) {
		return (tags != null) && tags.containsKey(tagName);
	}
	
	public String getTag(String tagName) {
		return (tags == null)? null : tags.get(tagName);
	}
	
	public void addTag(String tagName, String tagValue) {
		if (tags == null) tags = new HashMap<String, String>();
		tags.put(tagName, tagValue);
	}
	
	
	/** return full name of this signal */
	public String getFullName(String midStr, boolean addPrefix) {
		String pre = (!addPrefix || (prefix == null) || prefix.isEmpty())? "" : prefix;
		String suf = ((suffix == null) || suffix.isEmpty())? "" : "_" + suffix ;
		String mid = (midStr == null)? "" : midStr;
		return pre + mid + suf;
	}

}
