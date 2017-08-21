package ordt.output.systemverilog.common;

public class SystemVerilogInstance {
	private SystemVerilogModule mod;
	private String name;
	private RemapRuleList rules = null;
	
	public SystemVerilogInstance(SystemVerilogModule mod, String name) {
		this.mod=mod;
		this.name=name;
		//System.out.println("SystemVerilogModule new: mod=" + mod.getName() + ", name=" + name);
	}

	public SystemVerilogInstance(SystemVerilogModule mod, String name, RemapRuleList rules) {
		this.mod=mod;
		this.name=name;
		this.rules=rules;
		//System.out.println("SystemVerilogModule new: mod=" + mod.getName() + ", name=" + name);
	}

	public SystemVerilogModule getMod() {
		return mod;
	}
	public String getName() {
		return name;
	}
	
	/** add a rule to the list - first in list is highest match priority */
	public boolean hasRemapRules() {
		return rules != null;
	}
	
	/** return the first resulting name of a match */
	public String getRemappedSignal(String oldName) {
		return hasRemapRules()? rules.getNewName(oldName) : oldName;
	}

}
