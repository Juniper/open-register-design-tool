package ordt.output.systemverilog.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ordt.output.common.OutputWriterIntf;
import ordt.output.systemverilog.common.SystemVerilogModule.SystemVerilogParameter;
import ordt.output.systemverilog.common.io.SystemVerilogIOElement;
import ordt.output.systemverilog.common.io.SystemVerilogIOSignalList;

public class SystemVerilogInstance {
	private SystemVerilogModule mod;  // module
	private String name;  // name of this instance
	private RemapRuleList rules = null;  // IO remapping rules
	private HashMap<String, String> paramMap = new HashMap<String, String>();  // parameter settings
	
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

	/** set value of parameter to be used in instance call */
	public void setParameterValue(String parName, String value) {
		paramMap.put(parName, value);
	}

	/** return true if parameter has an instance value assigned */
	public boolean hasParameterValue(String parName) {
		return paramMap.containsKey(parName);
	}

	/** return assigned instance value of specified parameter */
	public String getParameterValue(String parName) {
		return paramMap.get(parName);
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
		List<SystemVerilogIOElement> ioList = getMod().getInputOutputList();
		if (ioList.isEmpty()) return;
		if (SystemVerilogModule.isLegacyVerilog() || hasRemapRules()) {
	    	writer.writeStmt(indentLevel++, getMod().getName() + " " + getParameterInstanceString() + getName() + " (");   // more elements so use comma
			Iterator<SystemVerilogIOElement> it = ioList.iterator();
			while (it.hasNext()) {
				SystemVerilogIOElement elem = it.next();
				String suffix = it.hasNext()? ")," : ") );";
				String remappedSignal = getRemappedSignal(elem.getFullName(), elem.getFrom(), elem.getTo());
				writer.writeStmt(indentLevel, "." + elem.getFullName() + "(" + remappedSignal + suffix); 
			}		   		    	
	    }
	    else {
	    	writer.writeStmt(indentLevel++, getMod().getName() + " " + getParameterInstanceString() + getName() + " ( .* );");   // more elements so use comma	    	
	    }
	    writer.writeStmt(indentLevel--, "");   
	}
	
	/** return parameter instance string for this module (assumes parms are passed up to parent level TODO) */
	private String getParameterInstanceString() {
		List<SystemVerilogParameter> parameterList = getMod().getParameterList();  // get parametrs defined for this module
		String retStr = (!parameterList.isEmpty())? "#(" : "";
		Iterator<SystemVerilogParameter> iter = parameterList.iterator();
		while(iter.hasNext()) {
			String parmName = iter.next().getName();  // get base parameter name
			if (hasParameterValue(parmName)) parmName = getParameterValue(parmName);  // use instance override for this parameter
			String suffix = iter.hasNext()? ", " : ") ";
			retStr += parmName + suffix;
		}
		return retStr;
	}

	/** return a IOSignalList for this instance w/ remapped signals */
	public SystemVerilogIOSignalList getIOSignalList() {
		return getMod().getInstanceIOSignalList(this);
	}

}
