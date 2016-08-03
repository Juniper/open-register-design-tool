package ordt.output.systemverilog;

public class SystemVerilogDefinedSignal {
	String prefix;
	String suffix;

	public enum DefSignalAssoc {
		FIELD,
		REG
	};

	DefSignalAssoc assoc;
	
	public SystemVerilogDefinedSignal(DefSignalAssoc assoc, String prefix, String suffix) {
		this.assoc = assoc;
		this.prefix = prefix;
		this.suffix = suffix;
	}
	public String getPrefix() {
		return prefix;
	}
	public String getSuffix() {
		return suffix;
	}
	public DefSignalAssoc getAssoc() {
		return assoc;
	}
	
	/** return full name of this signal */
	public String getFullName(String midStr, boolean addPrefix) {
		String pre = (!addPrefix || (prefix == null) || prefix.isEmpty())? "" : prefix + "_";
		String suf = ((suffix == null) || suffix.isEmpty())? "" : "_" + suffix ;
		String mid = (midStr == null)? "" : midStr;
		return pre + mid + suf;
	}

}
