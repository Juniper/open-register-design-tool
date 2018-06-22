package ordt.output.systemverilog;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.AddressableInstanceProperties.ExtType;
import ordt.output.systemverilog.common.SystemVerilogSignal;
import ordt.output.OutputBuilder;
import ordt.output.RegSetProperties;
import ordt.output.RhsReference;
import ordt.parameters.ExtParameters;
import ordt.parameters.ExtParameters.SVChildInfoModes;

public class SystemVerilogChildInfoBuilder extends OutputBuilder {
	
	List<ChildInfo> cInfoList = new ArrayList<ChildInfo>();
	
	//---------------------------- constructors ----------------------------------
	
	public SystemVerilogChildInfoBuilder(RegModelIntf model) {
		setBaseBuilderID();   // set unique ID of this instance
		this.model = model;
	    setVisitEachReg(false);   // gen code for each reg
	    setVisitEachRegSet(true);   // gen code for each reg set
	    setVisitExternalRegisters(true);  //visit externals also?
	    setVisitEachExternalRegister(false);	    // treat external regs exactly as internals
		RhsReference.setInstancePropertyStack(instancePropertyStack);  // update pointer to the instance stack for rhs reference evaluation
		model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
	}
	
	//---------------------------- OutputBuilder overrides ----------------------------------

	@Override
	public void addField() {
	}

	@Override
	public void addAliasField() {
	}

	@Override
	public void addRegister() {
	}

	@Override
	public void finishRegister() {
	}

	@Override
	public void addRegSet() {
	}

	@Override
	public void finishRegSet() {
		if (regSetProperties.isAddressMap() || isLeafDecoderRegSet()) {
			//if (isLeafDecoderRegSet()) System.out.println("SystemVerilogChildInfoBuilder finishRegSet: leaf decoder=" + regSetProperties.getInstancePath());
			cInfoList.add(new ChildInfo(regSetProperties.getBaseName(), regSetProperties.getFullBaseAddress(), regSetProperties.getFullHighAddress()));
		}
	}

	@Override
	public void addRegMap() {
	}

	@Override
	public void finishRegMap() {
	}

	@Override
	protected void write(BufferedWriter bw) {
		int indentLvl = 0;
		// if child info type is module then write it out
		if (ExtParameters.getSysVerChildInfoMode() == SVChildInfoModes.MODULE) {
			String modName = getAddressMapName() + "_child_map_info";
			writeStmt(indentLvl, "//");
			writeStmt(indentLvl, "//---------- module " + modName);
			writeStmt(indentLvl, "//");
			writeStmt(indentLvl, "module " + modName);
			writeStmt(indentLvl++, "(");
			//------- output defs
			Iterator<ChildInfo> it = cInfoList.iterator();
			while (it.hasNext()) {
				ChildInfo cInfo = it.next();
				String suffix = it.hasNext()? "," : ""; 
				writeStmt(indentLvl, cInfo.getStartName() + ",");
				writeStmt(indentLvl, cInfo.getEndName() + suffix);
			}
			writeStmt(indentLvl, ");");
			it = cInfoList.iterator();
			while (it.hasNext()) {
				ChildInfo cInfo = it.next();
				String addrArray = SystemVerilogSignal.genDefArrayString(0, ExtParameters.getLeafAddressSize());
				writeStmt(indentLvl, "output " +  addrArray + cInfo.getStartName() + ";");
				writeStmt(indentLvl, "output " +  addrArray + cInfo.getEndName() + ";");
			}
			  //------- output assigns
			it = cInfoList.iterator();
			while (it.hasNext()) {
				ChildInfo cInfo = it.next();
				writeStmt(indentLvl, "assign " + cInfo.getStartName() + " = " + cInfo.getStartStr() + ";");
				writeStmt(indentLvl, "assign " + cInfo.getEndName() + " = " + cInfo.getEndStr() +";");
			}	    
			writeStmt(--indentLvl, "endmodule");			
		}
		// otherwise just write out perl assigns
		else {
			//------- output address range defines
			writeStmt(indentLvl, "#");
			writeStmt(indentLvl, "#---------- address start/end info for " + getAddressMapName());
			writeStmt(indentLvl, "#");
			Iterator<ChildInfo> it = cInfoList.iterator();
			while (it.hasNext()) {
				ChildInfo cInfo = it.next();
				writeStmt(indentLvl, cInfo.getPerlStartName() + " = \"" + cInfo.getStartStr() + "\";");
				writeStmt(indentLvl, cInfo.getPerlEndName() + " = \"" + cInfo.getEndStr() +"\";");
			}
		}
	}
	
	// ----------------------
	
	/** return true if current regset if direct child of a root BBV5 parent
	 */
	protected boolean isLeafDecoderRegSet() {
		boolean bbv5Ancestor = false;
		int descLevel = 0;  // descendent level
		Iterator<RegSetProperties> iter = regSetPropertyStack.iterator();
		while (iter.hasNext()) {
			RegSetProperties inst = iter.next();
			if (bbv5Ancestor) {
				descLevel++; // calculate descendent level
				if ((descLevel<3) && !iter.hasNext() && !inst.isReplicated()) return true; // if ancestor is bbv5 and this is current regset return true
				if (inst.isAddressMap()) descLevel=99;  // inhibit children if an addrmap or reps detected
			}
			if (inst.isRootExternal() && inst.hasExternalType(ExtType.BBV5)) bbv5Ancestor = true; // detect bbvr root
		}
		return false;
	}

	// ----------------------
	
	private class ChildInfo {
		String name;
		RegNumber start;
		RegNumber end;
		private ChildInfo(String name, RegNumber start, RegNumber end) {
			super();
			this.name = name;
			this.start = start;
			this.end = end;
		}
		/** return name of the start address signal */
		public String getStartName() {
			return name + "_start";
		}
		/** return name of the end address signal */
		public String getEndName() {
			return name + "_end";
		}
		public String getStartStr() {
			return start.toFormat(NumBase.Hex, NumFormat.Verilog);
		}
		public String getEndStr() {
			return end.toFormat(NumBase.Hex, NumFormat.Verilog);
		}
		/** return perl start varable name */
		public String getPerlStartName() {
			String prefix = ((getAddressMapName() == null) || getAddressMapName().isEmpty())? "" : getAddressMapName() + "_";
			return "$" + (prefix + getStartName()).toUpperCase();
		}
		/** return perl end varable name */
		public String getPerlEndName() {
			String prefix = ((getAddressMapName() == null) || getAddressMapName().isEmpty())? "" : getAddressMapName() + "_";
			return "$" + (prefix + getEndName()).toUpperCase();
		}

	}
}
