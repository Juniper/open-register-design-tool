package ordt.output.systemverilog;

public class SystemVerilogDefinedSignal {
	String prefix;
	String suffix;
	Integer from;
	Integer to;

	public enum DefSignalAssoc {
		FIELD,
		REG,
		SIGNAL, 
		ANY
	};

	DefSignalAssoc assoc;
	
	public SystemVerilogDefinedSignal(DefSignalAssoc assoc, Integer from, Integer to, String prefix, String suffix) {
		this.assoc = assoc;
		this.from = from;
		this.to = to;
		this.prefix = prefix;
		this.suffix = suffix;
		//System.out.println("SystemVerilogDefinedSignal: from=" + from + ", to=" + to);
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
	public DefSignalAssoc getAssoc() {
		return assoc;
	}
	
	/** return full name of this signal */
	public String getFullName(String midStr, boolean addPrefix) {
		String pre = (!addPrefix || (prefix == null) || prefix.isEmpty())? "" : prefix;
		String suf = ((suffix == null) || suffix.isEmpty())? "" : "_" + suffix ;
		String mid = (midStr == null)? "" : midStr;
		return pre + mid + suf;
	}

}
