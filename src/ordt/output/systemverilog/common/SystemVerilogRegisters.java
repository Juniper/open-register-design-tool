/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.systemverilog.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.output.common.MsgUtils;
import ordt.output.common.OutputWriterIntf;

/** class to hold/generate systemverilog info associated with a set of registers (for a module)
 *  @author snellenbach      
 *  Jul 16, 2014
 *
 */
public class SystemVerilogRegisters {

	private HashMap<String, VerilogRegInfo> registers = new HashMap<String, VerilogRegInfo>();  // info for a set of registers grouped by name
	private HashMap<String, Boolean> resetActiveLow = new HashMap<String, Boolean>();  // reset polarity for each reset signal defined		
	private OutputWriterIntf writer;
	private String clkName;  // clock for this group of registers
	private boolean useAsyncResets = false;
	
	/** create VerilogRegisters 
	 * @param clkName */
	public SystemVerilogRegisters(OutputWriterIntf writer, String clkName, boolean useAsyncResets) {
		this.writer = writer;  // save reference to calling writer
		this.clkName = clkName;  // clock to be used for this register group
		this.useAsyncResets = useAsyncResets;
	}

	public void setWriter(OutputWriterIntf writer) {
		this.writer = writer;
	}

	/** retrieve register info by name or add create and return if a new name 
	 * @param name - name of this reg group
	 * @param clkName - clock to be used for this group
	 * @param useNegClkEdge - clock edge to be used for this group
	 * @param checkClockConsistency - if true, incoming clock name and edge type will be checked for consistency with previous values for this reg group
	 **/
	public VerilogRegInfo getOrCreate(String name, String clkName, boolean useNegClkEdge, boolean checkClockConsistency) {
		VerilogRegInfo regInfo = this.registers.get(name);
		if (regInfo == null) {
			regInfo = new VerilogRegInfo(name, clkName, useNegClkEdge); // define a new reg group using the active clock 
			this.registers.put(name, regInfo);  // save new reginfo in hashmap
		}
		else if (checkClockConsistency) regInfo.checkClock(clkName, useNegClkEdge);  // FIXME - this is called by combinassign with default clk
		return regInfo;
	}

	/** add a reset signal to reg group
	 *  @param reset - name of reset signal
	 *  @param activeLow - true if reset is active low
	 */
	public void addReset(String reset, boolean activeLow) {
		// add the reset stmt
		resetActiveLow.put(reset, activeLow);
		//System.out.println("SystemVerilogRegisters: addReset: reset=" + reset + ", activeLow=" + activeLow);
	}

	/** return the reset hashmap
	 */
	public HashMap<String, Boolean> getResets() {
		return resetActiveLow;
	}
	
	/** write out verilog for this set of regs 
	 * @param resolveNames */
	public void writeVerilog(int indentLevel) {
		for (String regName: registers.keySet()) {
			registers.get(regName).writeVerilog(indentLevel);
		}
	}

	// ------------------------------

	/** inner class to hold verilog info associated with a register group */   
	public  class VerilogRegInfo {
		private String name;   // name of this register/always group
		private String clock;  // clock name for synchronous assigns
		private boolean useNegClkEdge;
		private HashMap<String, List<String>> resetAssignList;  // reset assignments for each reset signal		
		private  List<String> regAssignList;    // list of synchronous reg assigns
		private  List<String> combinAssignList;    // list of combinatorial assign statements
		private  List<String> hiPrecCombinAssignList;    // list of high precedence combinatorial assign statements
		private  List<String> lowPrecCombinAssignList;    // list of low precedence combinatorial assign statements
		private HashSet<String> signalsWithResetAssigns;  // set of signals having a reset assign
		private boolean hasSimpleRegAssigns = true;  // allow refactoring is only simple assigns
		
		/**
		 * @param name - name of this reg group
		 */
		public VerilogRegInfo(String name, String clock, boolean useNegClkEdge) {
			this.setName(name);      // unique description for this group
			this.setClock(clock);    // use this clock name for this register group
			this.useNegClkEdge = useNegClkEdge;
			this.resetAssignList= new HashMap<String, List<String>>();  // reset assignments for each reset signal		
			this.regAssignList = new ArrayList<String>();    // list of synchronous reg assigns
			this.combinAssignList = new ArrayList<String>();    // list of combinatorial assign statements
			this.hiPrecCombinAssignList = new ArrayList<String>();    // list of high precedence combinatorial assign statements
			this.lowPrecCombinAssignList = new ArrayList<String>();    // list of low precedence combinatorial assign statements
			this.signalsWithResetAssigns= new HashSet<String>();  // set of signals having a reset assign		
		}
		
		/** verify that this reginfo instance has specified clock name and capture edge */
		public void checkClock(String clkName, boolean useNegClkEdge) {
			if (!this.clock.equals(clkName) || (this.useNegClkEdge != useNegClkEdge))
				MsgUtils.errorExit("mismatched clock info found for register group=" + name);
		}
		
		/** get name of this group */
		public String getName() {
			return name;
		}
		/** set name
		 *  @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/** get clock
		 *  @return the clock
		 */
		public String getClock() {
			return clock;
		}
		/** set clock
		 *  @param clock the clock to set
		 */
		public void setClock(String clock) {
			this.clock = clock;
		}
		/** add a reset assignment
		 *  @param reset - name of reset signal
		 *  @param stmt - verilog assignment statement
		 */
		public void addResetAssign(String reset, String stmt) {
			// if reset isn't defined then issue error message
			if (resetActiveLow.isEmpty())  {
				String errmsg = "Ordt".equals(MsgUtils.getProgName())? "No registers defined in addrmap " + writer.getWriterName() :
					"default reset signal is not defined before use";
				MsgUtils.errorExit(errmsg);
			}
			else if (!resetActiveLow.containsKey(reset))  {
				MsgUtils.errorExit("reset signal " + reset + " is not defined before use"); // + ", name=" + name + ", stmt=" + stmt);
			}
			// new reset signal so add it
			if (resetAssignList.get(reset) == null) {
				resetAssignList.put(reset, new ArrayList<String>());
			}
			// add the assign stmt
			resetAssignList.get(reset).add(stmt);
			// save signals with reset assingment
			String assignedSignal = extractAssignedSignalName(stmt);
			if (assignedSignal != null) signalsWithResetAssigns.add(assignedSignal);
		}
		
		/* extract assigned signal name from a sequential assignment string */
		private String extractAssignedSignalName(String stmt) {
			Pattern p = Pattern.compile("^\\s*(\\S+)\\s*<=\\s*");
			Matcher m = p.matcher(stmt);
			if (m.find()) {
				String sigName = m.group(1);
				return sigName;
			}
			return null;
		}

		/** add a sync register assignment
		 *  @param stmt - verilog assignment statement
		 */
		public void addRegAssign(String stmt) {
			// add the assign stmt
			regAssignList.add(stmt);
			// check for non-simple forms that can not be refactored
			  String assignedSignal = extractAssignedSignalName(stmt);
			  if (assignedSignal == null) {
				  hasSimpleRegAssigns = false;
				  //System.out.println("VerilogRegInfo addRegAssign: non-simple stmt=" + stmt);
			  }
		}
		/** add a list of reg assigns */
		public void addRegAssign(List<String> stmts) {
			regAssignList.addAll(stmts);
		}
		/** add a combinatorial assignment
		 *  @param stmt - verilog assignment statement
		 */
		public void addCombinAssign(String stmt) {
			// add the assign stmt
			combinAssignList.add(stmt);
		}
		/** add a list of combinatorial assignments
		 *  @param stmtList - list of verilog assignment statements
		 */
		public void addCombinAssign(List<String> stmts) {
			// add the assign stmt
			combinAssignList.addAll(stmts);
		}
		
		private boolean hasResetAssigns() {
			return !resetAssignList.isEmpty();
		}
		
		/** add a combinatorial assignment to one of the hi/low precedence lists
		 *  @param hiPrecedence - true if this statement will have high priority
		 *  @param stmt - verilog assignment statement
		 */
		public void addPrecCombinAssign(boolean hiPrecedence, String stmt) {
			//System.out.println("VerilogRegisters: reg=" + getName() + ", line=" + stmt);
			// add the assign stmtbased on priority
			if (hiPrecedence) hiPrecCombinAssignList.add(stmt);
			else lowPrecCombinAssignList.add(stmt);
		}
		
		/** write out high precedence verilog for this reg/group  */
		private void writeHiPrecedenceStatements(int indentLevel) {		
			if (!hiPrecCombinAssignList.isEmpty()) {
				Iterator<String> it = hiPrecCombinAssignList.iterator();
				while (it.hasNext()) {
					writer.writeStmt(indentLevel, it.next());
				}		   			
			}	
		}
		
		/** write out low precedence verilog for this reg/group  */
		private void writeLowPrecedenceStatements(int indentLevel) {		
			if (!lowPrecCombinAssignList.isEmpty()) {
				Iterator<String> it = lowPrecCombinAssignList.iterator();
				while (it.hasNext()) {
					writer.writeStmt(indentLevel, it.next());
				}		   			
			}	
		}
		
		/** write out verilog for this reg/group  */
		public void writeVerilog(int indentLevel) {
			
			// write combinatorial assignment block   
			if (!combinAssignList.isEmpty()) {
				writer.writeStmt(indentLevel, "//------- combinatorial assigns for " + name); 
				if (SystemVerilogModule.isLegacyVerilog()) writer.writeStmt(indentLevel++, "always @ (*) begin");  
				else writer.writeStmt(indentLevel++, "always_comb begin");  
				Iterator<String> it = combinAssignList.iterator();
				while (it.hasNext()) {
					String stmt = it.next();
					writer.writeStmt(indentLevel, stmt); 
				}		  
				// write any saved precedence statements
				writeLowPrecedenceStatements(indentLevel);
				writeHiPrecedenceStatements(indentLevel);
				writeBlockFooter(--indentLevel);
			}
			
			// write synchronous assignment block
			if (!regAssignList.isEmpty()) {
				//System.out.println("VerilogRegInfo writeVerilog: assigned reset sigs=" + signalsWithResetAssigns);
				writeSyncBlockHeader(indentLevel++, true);
				writeRegResets(indentLevel);
				writeRegAssigns(indentLevel);
				writeBlockFooter(--indentLevel);
			}
		}

		/** create synchronous always block header **
		 * 
		 * @param indentLevel
		 * @param allowAsyncResetSensitivities - if false, no async resets are added to sensitivity list even if group has async resets 
		 */
		private void writeSyncBlockHeader(int indentLevel, boolean allowAsyncResetSensitivities) {
			String noResetStr = allowAsyncResetSensitivities? "" : " (no reset)";
			writer.writeStmt(indentLevel, "//------- reg assigns for " + name + noResetStr);
			String asyncStr = (allowAsyncResetSensitivities && useAsyncResets && hasResetAssigns())? genAsyncString() : "";
			String clkEdgeType = this.useNegClkEdge? "negedge " : "posedge ";
			if (SystemVerilogModule.isLegacyVerilog()) writer.writeStmt(indentLevel, "always @ (" + clkEdgeType + clkName + asyncStr + ") begin");  
			else writer.writeStmt(indentLevel, "always_ff @ (" + clkEdgeType + clkName + asyncStr + ") begin");  
		}
		
		/** complete always block */
		private void writeBlockFooter(int indentLevel) {
			writer.writeStmt(indentLevel, "end");  
			writer.writeStmt(indentLevel, "");  		
		}

		/** generate always activation list for async resets */
		private String genAsyncString() {
			String outStr = "";
			for (String reset : resetAssignList.keySet()) {
				if (resetActiveLow.get(reset)) outStr += " or negedge " + reset;
				else outStr += " or posedge " + reset;		
			}
			return outStr;
		}
		/** write always block reset stmts */
		private  void writeRegResets(int indentLevel) {
			// write reset assigns for each reset signal
			String firstRst = "";
			for (String reset: resetAssignList.keySet()) {
				if (!resetAssignList.get(reset).isEmpty()) {
				   String notStr = (resetActiveLow.get(reset)) ? "! " : "";
				   writer.writeStmt(indentLevel++, firstRst + "if (" + notStr + reset + ") begin");  
				   firstRst = "else ";
				   Iterator<String> it = resetAssignList.get(reset).iterator();
				   while (it.hasNext()) {
					   String stmt = it.next();
					   writer.writeStmt(indentLevel, stmt);  
				   }		   					
				   writer.writeStmt(--indentLevel, "end");  
				}
			}
		}
		
		/** write always block assign stmts */
		private void writeRegAssigns(int indentLevel) {
			if (regAssignList.isEmpty()) return;
			boolean hasResets = hasResetAssigns();
			List<String> movedAssignList = new ArrayList<String>();
			if (hasResets) writer.writeStmt(indentLevel++, "else begin");  
			Iterator<String> it = regAssignList.iterator();
			while (it.hasNext()) {
				String stmt = it.next();
				// if simple form, move signals without reset assign out of else block
				String assignedSignal = extractAssignedSignalName(stmt);
				if (hasSimpleRegAssigns && hasResets && !signalsWithResetAssigns.contains(assignedSignal)) {
					//System.out.println("VerilogRegInfo writeRegAssigns: signal=" + assignedSignal + " has no reset and will be moved out of else block");
					movedAssignList.add(stmt);
				}
				else writer.writeStmt(indentLevel, stmt);
			}		   			
			if (hasResets) writer.writeStmt(--indentLevel, "end");  
			// move non-reset assigns out of else block
			if (movedAssignList.isEmpty()) return;
			if (useAsyncResets) {  // if using async resets move these to a separate always block
				writeBlockFooter(--indentLevel);
				writeSyncBlockHeader(indentLevel++, false);
			}
			it = movedAssignList.iterator();
			while (it.hasNext()) {
				String stmt = it.next();
				writer.writeStmt(indentLevel, stmt);
			}		   			
		}
		
	}  // end VerilogRegInfo
	
}
