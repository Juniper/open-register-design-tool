package ordt.output.systemverilog.common;

import ordt.output.common.OutputWriterIntf;
import ordt.output.systemverilog.common.io.SystemVerilogIOSignalList;

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

	public void setRemapRules(RemapRuleList rules) {
		this.rules=rules;		
	}
	
	/** add a rule to the list - first in list is highest match priority */
	public boolean hasRemapRules() {
		return rules != null;
	}
	
	/** return the first resulting name of a match */
	public String getRemappedSignal(String oldName, Integer sigFrom, Integer sigTo) {
		return hasRemapRules()? rules.getRemappedName(oldName, sigFrom, sigTo) : oldName;
	}

	/** write an instance of this module using specified writer */
	public void write(int indentLevel, OutputWriterIntf writer) {
		getMod().writeInstance(indentLevel, writer, this);
	}

	/** return a IOSignalList for this instance w/ remapped signals */
	public SystemVerilogIOSignalList getIOSignalList() {
		return getMod().getInstanceIOSignalList(this);
	}

}
