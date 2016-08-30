package ordt.output.systemverilog;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.OutputBuilder;
import ordt.parameters.ExtParameters;

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
	    setAllowLocalMapInternals(true);  // cascaded addrmaps will result in local non-ext regions   
		model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
	}
	
	//---------------------------- OutputBuilder overrides ----------------------------------

	@Override
	public void addSignal() {
	}

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
	public void addRootExternalRegisters() {
	}

	@Override
	public void addRegSet() {
	}

	@Override
	public void finishRegSet() {
		if (regSetProperties.isAddressMap()) {
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
		String modName = getAddressMapName() + "_child_map_info";
		writeStmt(indentLvl, "");
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
			String addrArray = SystemVerilogSignal.genDefArrayString(0, ExtParameters.getLeafAddressSize());
			writeStmt(indentLvl, "assign " + cInfo.getStartName() + " = " + cInfo.getStartStr() + ";");
			writeStmt(indentLvl, "assign " + cInfo.getEndName() + " = " + cInfo.getEndStr() +";");
		}	    
		writeStmt(--indentLvl, "endmodule");

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

	}
}
