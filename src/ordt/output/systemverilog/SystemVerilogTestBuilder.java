/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.systemverilog;
// - header is only written for root builder

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.extract.Ordt;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.systemverilog.common.RemapRuleList;
import ordt.output.systemverilog.common.RemapRuleList.RemapRuleType;
import ordt.output.systemverilog.common.SystemVerilogModule;
import ordt.output.systemverilog.common.SystemVerilogSignal;
import ordt.output.systemverilog.io.SystemVerilogIOElement;
//import ordt.output.systemverilog.oldio.SystemVerilogIOSignal;
//import ordt.output.systemverilog.oldio.SystemVerilogIOSignalList;
import ordt.output.systemverilog.io.SystemVerilogIOSignal;
import ordt.output.systemverilog.io.SystemVerilogIOSignalList;
import ordt.parameters.ExtParameters;
import ordt.parameters.ExtParameters.SVDecodeInterfaceTypes;

public class SystemVerilogTestBuilder extends SystemVerilogBuilder {
	
    // define a new list to handle bench specific IO 
	protected SystemVerilogIOSignalList benchSigList = new SystemVerilogIOSignalList("bench");   // signals specific to the bench
	protected SystemVerilogIOSignalList primaryBfmToDecoderSigList = new SystemVerilogIOSignalList("primary bfm - decode");   // signals specific to the bench
	// module defines  
	protected SystemVerilogModule primaryBfm = new SystemVerilogModule(this, PIO, defaultClk, getDefaultReset());  // primary pio interface bfm
	protected SystemVerilogModule benchtop = new SystemVerilogModule(this, 0, defaultClk, getDefaultReset());  // bench top module

	public SystemVerilogTestBuilder(RegModelIntf model) {
		super(model);
	}
	
	public SystemVerilogTestBuilder(SystemVerilogTestBuilder parentBuilder, boolean isTestModule) {
		super(parentBuilder, isTestModule);
	}
	
	//---------------------------- methods to output verilog ----------------------------------------

	/** write out the verilog 
	 * @param bw */
	public void write(BufferedWriter bw) {
		bufferedWriter = bw;

		// before starting write, check that this addrmap is valid
		if (decoder.getDecodeList().isEmpty()) Ordt.errorExit("Minimum allowed address map size is " + this.getMinRegByteWidth() + "B (addrmap=" + getAddressMapName() + ")");
				
		// set bufferedwriter in all child builders so we can write to same file
		setChildBufferedWriters(bw);
		
		// write the top level testbench module
		writeTB();   
	}

	/** write out the basic testbench */   
	private  void writeTB() {		
				
		int clkPeriod = 10;  // use an even number so half period is int also
		
		// define the top module
		benchtop.setName(getModuleName() + "_test");
		
		// add bench io list
		benchtop.useIOList(benchSigList, null); 
		
		// write primary interface bfm module and add bfm control signals to bench list
		createPrimaryBfm();   // TODO handle secondary bfm
	
		// add dut child modules 
		addDutInstances(benchtop);
		
		// add primary bfm instance with names remapped
		if (decoder.hasSecondaryInterface()) {
			RemapRuleList rules = new RemapRuleList();
			if (ExtParameters.getSysVerRootDecoderInterface()==SVDecodeInterfaceTypes.PARALLEL) {
				rules.addRule("^dec_pio_.*$", RemapRuleType.ADD_PREFIX, "p1_d2h_");  // parallel (note h2d/d2h reflects decoder cascaded output i/f direction, not root i/f)
				rules.addRule("^pio_dec_.*$", RemapRuleType.ADD_PREFIX, "p1_h2d_");
				benchtop.addInstance(primaryBfm, "parallel_bfm", rules);
			}
			else {
				rules.addRule("^dec_leaf_.*$", RemapRuleType.ADD_PREFIX, "p1_");  // leaf 
				rules.addRule("^leaf_dec_.*$", RemapRuleType.ADD_PREFIX, "p1_");
				benchtop.addInstance(primaryBfm, "leaf_bfm", rules);
			}
		}
		else if (ExtParameters.getSysVerRootDecoderInterface()==SVDecodeInterfaceTypes.PARALLEL) {
			RemapRuleList rules = new RemapRuleList();
			rules.addRule("^dec_pio_.*$", RemapRuleType.ADD_PREFIX, "d2h_");  // parallel (note h2d/d2h reflects decoder cascaded output i/f direction, not root i/f)
			rules.addRule("^pio_dec_.*$", RemapRuleType.ADD_PREFIX, "h2d_");
			benchtop.addInstance(primaryBfm, "parallel_bfm", rules);  
		}
		else benchtop.addInstance(primaryBfm, "leaf_bfm");  // leaf
		
		// add all dut extern signals to the bench list
		addDutIOs(benchtop);  
		
		// add wire defs to bench
		benchtop.setShowDuplicateSignalErrors(false);
		//benchtop.addWireDefs(benchtop.getSignalList(PIO, DECODE));  // all ports from pio to decoder will be wires
		benchtop.addWireDefs(getPIOInputWires());  // all except p2 ports from pio to decode will be wires		
		benchtop.addWireDefs(benchtop.getSignalList(DECODE, PIO));  // all ports from decoder to pio will be wires
		benchtop.addWireDefs(benchtop.getSignalList(LOGIC, HW));  // all ports to hw will be wires
		benchtop.addWireDefs(benchtop.getSignalList(DECODE, HW));  // all ports to hw will be wires
		benchtop.addWireDefs(benchtop.getSignalList(PIO, HW));  // all ports to hw will be wires
		
		// add some local reg defines
		benchtop.addScalarReg("CLK");
		benchtop.addScalarReg("CLK_div2");
		benchtop.addScalarReg("CLK_div4");
		if (ExtParameters.systemverilogUseGatedLogicClk()) benchtop.addScalarReg("delayed_gclk_enable");
		
		// add reg defs to bench
		benchtop.addRegDefs(benchtop.getSignalList(HW, LOGIC));  // all ports from hw will be regs
		benchtop.addRegDefs(benchtop.getSignalList(HW, DECODE));  // all ports from hw will be regs
		benchtop.addRegDefs(benchtop.getSignalList(HW, PIO));  // all ports from hw will be regs
		benchtop.addRegDefs(getPIOInputRegs());  // p2 ports from pio to decode will be regs		
		
		// add sim block statements
		addSimBlocks(clkPeriod);
		
		// ----------- write out the testbench
				
		// add timescale
		writeStmt(0, "`timescale 1 ns / 100 ps");
		
		// write leaf bfm module and define interfacing signals
		int indentLevel = 0;
		primaryBfm.write();   
		
		// write the top module
		benchtop.writeNullModuleBegin(indentLevel);
		indentLevel++;

		benchtop.writeWireDefs(indentLevel);
		
		benchtop.writeRegDefs(indentLevel);  
		
		// write the simulation activity block
		benchtop.writeStatements(indentLevel);
		
		// write dut instances
		benchtop.writeChildInstances(indentLevel);
		
		// end the module
		indentLevel--;
		benchtop.writeModuleEnd(indentLevel);	
	}	

	/** return a list of bench pio-decode inputs that should be defined as regs (secondary decoder inputs) */
	private List<SystemVerilogSignal> getPIOInputRegs() {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
		for (SystemVerilogSignal sig : benchtop.getSignalList(PIO, DECODE))
		  if (sig.getName().startsWith("p2_")) outList.add(sig);
		return outList;
	}

    /** return a list of bench pio-decode inputs that should be defined as wires (primary decoder inputs)*/
	private List<SystemVerilogSignal> getPIOInputWires() {
		List<SystemVerilogSignal> outList = new ArrayList<SystemVerilogSignal>();
		for (SystemVerilogSignal sig : benchtop.getSignalList(PIO, DECODE))
		  if (!sig.getName().startsWith("p2_")) outList.add(sig);
		return outList;
	}

	/** generate the blocks generating clocks and launching a sim */
	private void addSimBlocks(int clkPeriod) {
		
		// start up clocks and init sim
		addSimStartBlocks(clkPeriod);

	   	// generate read/write tasks
	   	for (int size = ExtParameters.getMinDataSize(); size <= this.getMaxRegWidth(); size += ExtParameters.getMinDataSize()) {
		   	addWriteTask(size);
		   	addReadTask(size);
	   	}
		// perform pio reads/writes
		benchtop.addStatement("initial");
	    benchtop.addStatement("  begin");
	    // set reg dut inputs at t=0
		for (SystemVerilogSignal vsig : benchtop.getRegDefList().getSignalList()) { //benchtop.getRegDefList(); tbRegSignals
			if (!vsig.getName().endsWith("clk"))
				benchtop.addStatement("  " + vsig.getName() + " = 0;");
		}
	   	benchtop.addStatement("");
	   	// init bfm control sigs 
		benchtop.addStatement("  address = 0;");
		benchtop.addStatement("  wr_data = 0;");
		benchtop.addStatement("  wr_enable = 0;");
		benchtop.addStatement("  rd_compare = 0;");
		benchtop.addStatement("  rd_data = 0;");
		benchtop.addStatement("  type = 0;");
		benchtop.addStatement("  size = 0;");
		benchtop.addStatement("  leaf_go = 0;");

		// wait for reset to settle
	   	benchtop.addStatement("  repeat(5)");
	   	benchtop.addStatement("    @(posedge CLK);");
	   	benchtop.addStatement("");
	   	
        //
	   	// --------------------------------- do some reads/writes
	   	//

	   	// add test commands
		if (ExtParameters.hasTestCommands()) {
			for (String cmdStr: ExtParameters.getTestCommands()) {
				TestCommand cmd = new TestCommand(cmdStr);
				if (cmd.isValid()) cmd.addStatements();
				//System.out.println(cmdStr);	
			}
		}
		// if no commands, just issue a read and a write at 0x0
		else {
			// do a 32b write     
			benchtop.addStatement("write32(40'h0, 32'ha5a5a5a5, 0, address, wr_data, wr_enable, rd_compare, rd_data, type, size, leaf_go);");
		   	benchtop.addStatement("@ (posedge done)");
		   	benchtop.addStatement("   leaf_go = #2 1'b0; ");
		   	benchtop.addStatement("");
			// do a 32b read     
			benchtop.addStatement("read32(40'h0, 0, 0, address, wr_data, wr_enable, rd_compare, rd_data, type, size, leaf_go);");
		   	benchtop.addStatement("@ (posedge done)");
		   	benchtop.addStatement("   leaf_go = #2 1'b0; ");
		   	benchtop.addStatement("");			
		}
		
        // --------------
	   	
	   	benchtop.addStatement("end");
	   	benchtop.addStatement("");		
	}

	// -----------
	
	private enum cmdType {INVALID, READ, WRITE, WAIT, STMT};

	/** test command inner class */
	private class TestCommand {
		
		private cmdType cType = cmdType.INVALID; // command is invalid by default
		private HashMap<String, Integer> ints= new HashMap<String, Integer>();
		private HashMap<String, String> strs= new HashMap<String, String>();
		private HashMap<String, RegNumber> rnums= new HashMap<String, RegNumber>();
				
		TestCommand(String cmdStr) {
			
			// check for read and write form commands
			Pattern chkPat = Pattern.compile("^(write|read)\\s+(\\d+)\\s+(0x\\w+)(\\s+(0x\\w+))?(\\s+(0x\\w+))?");
			Matcher m = chkPat.matcher(cmdStr);
			if (m.matches()) {
				RegNumber address, data = null, enable = null;
				cType = "read".equals(m.group(1))? cmdType.READ : cmdType.WRITE;
				ints.put("size", Integer.valueOf(m.group(2)));  // save r/w size
				address = new RegNumber(m.group(3));
				address.setNumBase(NumBase.Hex);
				address.setNumFormat(NumFormat.Verilog);
				address.setVectorLen(ExtParameters.getLeafAddressSize());
				rnums.put("address", address);  // save r/w address
				// get write or compare data
				if (m.groupCount() > 4) {
					data = new RegNumber(m.group(5));
					data.setNumBase(NumBase.Hex);
					data.setNumFormat(NumFormat.Verilog);
					data.setVectorLen(ints.get("size"));
					if ((data != null) && data.isDefined()) rnums.put("data", data);  // save r/w data
				}
				// get write enable
				if (m.groupCount() > 6) {
					enable = new RegNumber(m.group(7));
					enable.setNumBase(NumBase.Hex);
					enable.setNumFormat(NumFormat.Verilog);
					enable.setVectorLen(decoder.getWriteEnableWidth());
					if ((enable != null) && enable.isDefined()) rnums.put("enable", enable);  // save enable data
				}
				if ((ints.get("size") == null) || (!address.isDefined()) || 
						(isWrite() && ((data == null) || !data.isDefined()))) cType = cmdType.INVALID;
			}
			
			// otherwise check for wait command
			else {
				chkPat = Pattern.compile("^wait\\s+(\\d+)");
				m = chkPat.matcher(cmdStr);
				if (m.matches()) {
					cType = cmdType.WAIT;
					ints.put("cycles", Integer.valueOf(m.group(1)));  // save wait time
					if (ints.get("cycles") == null) cType = cmdType.INVALID;
				}
				
				// otherwise check for set command
				else {
					chkPat = Pattern.compile("^statement\\s+(.*)");
					m = chkPat.matcher(cmdStr);
					if (m.matches()) {
						cType = cmdType.STMT;
						strs.put("stmt", m.group(1));  // save statement value
						if (strs.get("stmt") == null) cType = cmdType.INVALID;
					}					
				}
				
			}
			
			//System.out.println(this);
			if (!isValid()) Ordt.warnMessage("invalid test command found: " + cmdStr);
		}

		public void addStatements() {
			if (isRead() || isWrite()) {
				String typeStr = isRead()? "read" : "write"; 
				// call using write data and enable if a write, otherwise compare info if a read  //TODO - fix for 0 enable vec width
				String dataStr="";
				if (isWrite()) dataStr = ", " + rnums.get("data") + ", " + ((!hasWriteEnables())? "0" : 
					rnums.containsKey("enable")? rnums.get("enable") : getHexOnesString(decoder.getWriteEnableWidth()));
				else if (rnums.containsKey("data")) dataStr = ", 1'b1, " + rnums.get("data");  // read with compare
				else dataStr = ", 1'b0, " + ints.get("size") + "'h0";  // read w/o compare
				//System.out.println("SystemVerilogTestBuilder addStatements: " + typeStr + ints.get("size") + ", rnams=" + rnums);
				benchtop.addStatement(typeStr + ints.get("size") + "(" + rnums.get("address") + dataStr +   // task inputs
						", address, wr_data, wr_enable, rd_compare, rd_data, type, size, leaf_go);");  // task outputs
			   	benchtop.addStatement("@ (posedge done)");
			   	benchtop.addStatement("   leaf_go = #2 1'b0;");
			   	benchtop.addStatement("");				
			}
			else if (isWait()) {
			   	benchtop.addStatement("repeat(" + ints.get("cycles") + ")");
			   	benchtop.addStatement("   @(posedge CLK);");
			   	benchtop.addStatement("");								
			}
			else if (isStatement()) {
			   	benchtop.addStatement(strs.get("stmt"));
			   	benchtop.addStatement("");				
			}
		}
		
		public String toString() {
			String retStr = "type: " + cType;
			retStr += "\n  ints: ";
			for (String key : ints.keySet()) retStr += "\n    " + key + ": " + ints.get(key);
			retStr += "\n  strs: ";
			for (String key : strs.keySet()) retStr += "\n    " + key + ": " + strs.get(key);
			retStr += "\n  rnums: ";
			for (String key : rnums.keySet()) retStr += "\n    " + key + ": " + rnums.get(key);
			return retStr;
		}

		public boolean isValid() {
			return (cType != cmdType.INVALID);
		}
		private boolean isRead() {
			return (cType == cmdType.READ);
		}
		private boolean isWrite() {
			return (cType == cmdType.WRITE);
		}
		private boolean isWait() {
			return (cType == cmdType.WAIT);
		}
		private boolean isStatement() {
			return (cType == cmdType.STMT);
		}
	}
	// -----------
	
	/** start up clocks and init sim */
	private void addSimStartBlocks(int clkPeriod) {
		int clkHalfPeriod = clkPeriod/2;
		int simEndTime = ExtParameters.benchTotalTestTime();
		
		if (ExtParameters.systemverilogUseGatedLogicClk()) {
		   	// generate clocks
		   	benchtop.addStatement("always @(*)");
		   	benchtop.addStatement("   uclk = CLK;");
		   	benchtop.addStatement("always @(*)");
		   	benchtop.addStatement("   gclk = CLK & delayed_gclk_enable;");
		   	benchtop.addStatement("always @(posedge CLK)");
		   	benchtop.addStatement("   delayed_gclk_enable <= #1 gclk_enable;");
		}
		else {
		   	// generate clocks
		   	benchtop.addStatement("always @(*)");
		   	benchtop.addStatement("   clk = CLK;");
		}
	   	benchtop.addStatement("");
		
		// dump waves
		// iverilog output.v --> a.out
		// vvp a.out
		// gtkwave dump.vcd
		benchtop.addStatement("initial");
	    benchtop.addStatement("  begin");
	   	benchtop.addStatement("  $display(\" << Starting the Simulation >>\");");
	   	benchtop.addStatement("  $dumpfile(\"test.vcd\");");
	   	benchtop.addStatement("  $dumpvars(0," + getModuleName() + "_test);");
	   	benchtop.addStatement("  #" + simEndTime);
	   	benchtop.addStatement("  $dumpoff;");
	   	benchtop.addStatement("  $finish;");
	   	benchtop.addStatement("end");
	   	benchtop.addStatement("");
		
		// set up clock and reset
		benchtop.addStatement("initial");
	    benchtop.addStatement("  begin");
	   	benchtop.addStatement("  CLK = 1'b0; // at time 0");
	   	benchtop.addStatement("  CLK_div2 = 1'b0;");
	   	benchtop.addStatement("  CLK_div4 = 1'b0;");
	   	benchtop.addStatement("  reset = 0; // toggle reset");
	   	benchtop.addStatement("  #" + (clkHalfPeriod * 3) + " reset = 1'b1;");
	   	benchtop.addStatement("  $display(\" %0d: Applying reset...\", $time);");
	   	benchtop.addStatement("  #" + (clkPeriod * 3) + " reset = 1'b0;");
	   	benchtop.addStatement("  $display(\" %0d: Releasing reset...\", $time);");
	   	benchtop.addStatement("end");
	   	benchtop.addStatement("");

	   	// generate clocks
	   	benchtop.addStatement("always");
	   	benchtop.addStatement("   #" + (clkHalfPeriod) + " CLK = ~CLK;");
	   	benchtop.addStatement("");
	   	benchtop.addStatement("always @ (posedge CLK)");
	   	benchtop.addStatement("   CLK_div2 = ~CLK_div2; ");
	   	benchtop.addStatement("");
	   	benchtop.addStatement("always @ (posedge CLK_div2)");
	   	benchtop.addStatement("   CLK_div4 = ~CLK_div4; ");
	   	benchtop.addStatement("");	
	}

	/** create a read task of the specified size */
	private void addReadTask(int width) {
	   	benchtop.addStatement("// " + width + "b read task");
	   	benchtop.addStatement("task read" + width + ";");
		benchtop.addStatement("  input " + SystemVerilogSignal.genDefArrayString(0, ExtParameters.getLeafAddressSize()) + "in_address;");
		benchtop.addStatement("  input in_rd_compare;");  
		benchtop.addStatement("  input " + SystemVerilogSignal.genDefArrayString(0, width) + "in_rd_data;");  
		// outputs
		benchtop.addStatement("  output " + SystemVerilogSignal.genDefArrayString(0, ExtParameters.getLeafAddressSize()) + "address;");
		benchtop.addStatement("  output " + SystemVerilogSignal.genDefArrayString(0, getMaxRegWidth()) + "wr_data;");  
		benchtop.addStatement("  output " + SystemVerilogSignal.genDefArrayString(0, decoder.getWriteEnableWidth()) + "wr_enable;");  
		benchtop.addStatement("  output rd_compare;");
		benchtop.addStatement("  output " + SystemVerilogSignal.genDefArrayString(0, getMaxRegWidth()) + "rd_data;");  
		benchtop.addStatement("  output [1:0] type;");
		benchtop.addStatement("  output [3:0] size;");
		benchtop.addStatement("  output leaf_go;");
	   	benchtop.addStatement("  begin");
		benchtop.addStatement("    address = #1 in_address;");
		benchtop.addStatement("    wr_data = 0;"); 
		benchtop.addStatement("    wr_enable = 0;");  
		benchtop.addStatement("    rd_compare = in_rd_compare;");  
		benchtop.addStatement("    rd_data = 0;");  
		benchtop.addStatement("    rd_data [" + (width - 1) + ":0] = in_rd_data;");  
		benchtop.addStatement("    type = 2'b10;");
		benchtop.addStatement("    size = 4'd" + ((width/ExtParameters.getMinDataSize()) - 1) + ";");
		benchtop.addStatement("    leaf_go = 1'b1;");
	   	benchtop.addStatement("  end");
	   	benchtop.addStatement("endtask");
	   	benchtop.addStatement("");		
	}
	
	/** create a write task of the specified size */
	private void addWriteTask(int width) {
	   	benchtop.addStatement("// " + width + "b write task");
	   	benchtop.addStatement("task write" + width + ";");
		benchtop.addStatement("  input " + SystemVerilogSignal.genDefArrayString(0, ExtParameters.getLeafAddressSize()) + "in_address;");
		benchtop.addStatement("  input " + SystemVerilogSignal.genDefArrayString(0, width) + "in_wr_data;");  
		benchtop.addStatement("  input " + SystemVerilogSignal.genDefArrayString(0, decoder.getWriteEnableWidth()) + "in_wr_enable;"); 
		// outputs
		benchtop.addStatement("  output " + SystemVerilogSignal.genDefArrayString(0, ExtParameters.getLeafAddressSize()) + "address;");
		benchtop.addStatement("  output " + SystemVerilogSignal.genDefArrayString(0, getMaxRegWidth()) + "wr_data;");  
		benchtop.addStatement("  output " + SystemVerilogSignal.genDefArrayString(0, decoder.getWriteEnableWidth()) + "wr_enable;");  
		benchtop.addStatement("  output rd_compare;");
		benchtop.addStatement("  output " + SystemVerilogSignal.genDefArrayString(0, getMaxRegWidth()) + "rd_data;");  
		benchtop.addStatement("  output [1:0] type;");
		benchtop.addStatement("  output [3:0] size;");
		benchtop.addStatement("  output leaf_go;");
		//
	   	benchtop.addStatement("  begin");
		benchtop.addStatement("    address = #1 in_address;");
		benchtop.addStatement("    wr_data = 0;");  
		benchtop.addStatement("    wr_data [" + (width - 1) + ":0] = in_wr_data;");  
		benchtop.addStatement("    wr_enable = in_wr_enable;");  
		benchtop.addStatement("    rd_compare = 0;");  
		benchtop.addStatement("    rd_data = 0;");  
		benchtop.addStatement("    type = 0;");
		benchtop.addStatement("    size = 4'd" + ((width/ExtParameters.getMinDataSize()) - 1) + ";");
		benchtop.addStatement("    leaf_go = 1'b1;");
	   	benchtop.addStatement("  end");
	   	benchtop.addStatement("endtask");
	   	benchtop.addStatement("");	
	}

	/** create the primary bfm module */   
    private void createPrimaryBfm() {
		// create parallel or leaf type bfm
    	if (ExtParameters.getSysVerRootDecoderInterface()==SVDecodeInterfaceTypes.PARALLEL) createParallelBfm();
    	else if (ExtParameters.getSysVerRootDecoderInterface()==SVDecodeInterfaceTypes.LEAF) createLeafBfm();
    	else Ordt.errorExit("Testbench does not support " + ExtParameters.getSysVerRootDecoderInterface().name() + " root interface type.");
	}

	/** create the parallel bfm module */   
	private void createParallelBfm() {
		
		// start the leaf bfm module
		primaryBfm.setName(getModuleName() + "_test_parallel_bfm");
		
		// add bfm control sigs to bench specific IO list
		addCommonBfmIO();

		// add bfm to decoder interface
		if (getMapAddressWidth()>0) primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "pio_dec_address", 0, getMapAddressWidth());
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "pio_dec_write_data", 0, getMaxRegWidth());
		if (SystemVerilogDecodeModule.hasWriteEnables()) primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "pio_dec_write_enable", 0, decoder.getWriteEnableWidth());
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "pio_dec_read", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "pio_dec_write", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_pio_read_data", 0, getMaxRegWidth());
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_pio_ack", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_pio_nack", 0, 1);
		if (getMaxRegWordWidth() > 1) {
			primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "pio_dec_trans_size", 0, getMaxWordBitSize());
			primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_pio_trans_size", 0, getMaxWordBitSize());
		}
		
		// associate IO lists with the bfm
		primaryBfm.useIOList(benchSigList, HW);  // added sigs here will talk to bench 
		primaryBfm.useIOList(primaryBfmToDecoderSigList, DECODE);  // using a local interface list here since instance name remapping might occur in bench
		
		// define all outputs as reg
		List<SystemVerilogIOElement> outputs = primaryBfm.getOutputList();
		for (SystemVerilogIOElement sig: outputs) {
			if (!sig.isSignalSet()) primaryBfm.addVectorReg(sig.getName(), ((SystemVerilogIOSignal) sig).getLowIndex(), ((SystemVerilogIOSignal) sig).getSize());
		}
		// define internal regs
		primaryBfm.addVectorReg("trans_size", 0, 5);

		// init outputs from leaf bfm
		primaryBfm.addStatement("initial begin");
		for (SystemVerilogIOElement sig: outputs) {
			if (!sig.isSignalSet()) primaryBfm.addStatement("  " + sig.getName() + " = 0;");
		}
	   	primaryBfm.addStatement("end");
	   	primaryBfm.addStatement("");

		// if go toggles, initiate transaction
	   	primaryBfm.addStatement("always @(posedge leaf_go) begin");
	   	// apply inputs on next clock cycle
	   	primaryBfm.addStatement("  @(posedge CLK);");
	   	primaryBfm.addStatement("    #1 active = 1'b1;");  // indicate transaction active
	   	if (getMapAddressWidth()>0) primaryBfm.addStatement("    pio_dec_address = address" + SystemVerilogSignal.genRefArrayString(getAddressLowBit(), getMapAddressWidth()) + ";");
	   	primaryBfm.addStatement("    pio_dec_write_data = wr_data;");
	   	if (SystemVerilogDecodeModule.hasWriteEnables()) primaryBfm.addStatement("    pio_dec_write_enable = wr_enable;");
	   	primaryBfm.addStatement("    pio_dec_read = type[1];");
	   	primaryBfm.addStatement("    pio_dec_write = ~type[1];");
	   	primaryBfm.addStatement("    trans_size = {1'b0, size} + 5'b1;");
	   	if (getMaxWordBitSize()>0) primaryBfm.addStatement("    pio_dec_trans_size = size" + SystemVerilogSignal.genRefArrayString(0, getMaxWordBitSize()) + ";");
		primaryBfm.addStatement("    if (type[1] == 1'b0) begin");			
	   	primaryBfm.addStatement("      $display(\"%0d: initiating %d word write to address %x (data=%x)...\", $time, trans_size, address, wr_data);");
	   	primaryBfm.addStatement("    end");
		primaryBfm.addStatement("    else begin");			
	   	primaryBfm.addStatement("      $display(\"%0d: initiating %d word read to address %x...\", $time, trans_size, address);");
	   	primaryBfm.addStatement("    end");
	   	primaryBfm.addStatement("");

	   	// sample dut outputs on next clock cycle
	   	primaryBfm.addStatement("  @(posedge CLK);");
	   	primaryBfm.addStatement("    while (~dec_pio_ack & ~dec_pio_nack) begin");
	   	primaryBfm.addStatement("       @(posedge CLK);");
	   	primaryBfm.addStatement("    end");
	   	primaryBfm.addStatement("");

	   	// turn off valids and indicate done
	   	primaryBfm.addStatement("  pio_dec_read = 1'b0;");
	   	primaryBfm.addStatement("  pio_dec_write = 1'b0;");

	   	primaryBfm.addStatement("  done = 1'b1;");  // indicate transaction done

	   	primaryBfm.addStatement("  $display(\"  ack = %d\", dec_pio_ack);");
	   	primaryBfm.addStatement("  $display(\"  nack = %d\", dec_pio_nack);");	   	
	   	if (getMaxWordBitSize()>0) primaryBfm.addStatement("  $display(\"  return size = %x\", dec_pio_trans_size);");	   	
	   	// if a read, display return info
		primaryBfm.addStatement("  if (type[1] == 1'b1) begin");			
	   	primaryBfm.addStatement("    $display(\"  read data = %x\", dec_pio_read_data);");
		primaryBfm.addStatement("    if (rd_compare) begin");			
		primaryBfm.addStatement("      if (dec_pio_read_data !== rd_data) $display(\"  read compare FAILED - expected %x\", rd_data);");	
		primaryBfm.addStatement("      else $display(\"  read compare OK - expected %x\", rd_data);");	
	   	primaryBfm.addStatement("    end");
	   	primaryBfm.addStatement("  end");

	   	// indicate transaction is complete
	   	primaryBfm.addStatement("  #1 active = 1'b0;");  
	   	primaryBfm.addStatement("  #1 done = 1'b0;");  

	   	primaryBfm.addStatement("end");
	}

	/** create the leaf bfm module */   
	private  void createLeafBfm() {		
		// start the leaf bfm module
		primaryBfm.setName(getModuleName() + "_test_leaf_bfm");
		
		// add bfm control sigs to bench specific IO list
		addCommonBfmIO();
		
		// add bfm to decoder interface
		int dataSizeBits = (getMaxWordBitSize() <= 3) ? 3 : getMaxWordBitSize();  // leaf trans size is 3 or 4
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "leaf_dec_addr", 0, ExtParameters.getLeafAddressSize());
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "leaf_dec_wr_data", 0, getMaxRegWidth());
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "leaf_dec_valid", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "leaf_dec_wr_dvld", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "leaf_dec_cycle", 0, 2);
		primaryBfmToDecoderSigList.addSimpleVector(PIO, DECODE, "leaf_dec_wr_width", 0, dataSizeBits);
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_leaf_rd_data", 0, getMaxRegWidth());
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_leaf_ack", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_leaf_nack", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_leaf_accept", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_leaf_reject", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_leaf_retry_atomic", 0, 1);
		primaryBfmToDecoderSigList.addSimpleVector(DECODE, PIO, "dec_leaf_data_width", 0, dataSizeBits);
		
		// associate IO lists with the bfm
		primaryBfm.useIOList(benchSigList, HW);  // added sigs here will talk to bench 
		primaryBfm.useIOList(primaryBfmToDecoderSigList, DECODE);  // using a local interface list here since instance name remapping might occur in bench
		
		// define all outputs as reg
		List<SystemVerilogIOElement> outputs = primaryBfm.getOutputList();
		for (SystemVerilogIOElement sig: outputs) {
			if (!sig.isSignalSet()) primaryBfm.addVectorReg(sig.getName(), ((SystemVerilogIOSignal) sig).getLowIndex(), ((SystemVerilogIOSignal) sig).getSize());
		}
		// define internal regs
		primaryBfm.addVectorReg("trans_size", 0, 5);

		// init outputs from leaf bfm
		primaryBfm.addStatement("initial begin");
		for (SystemVerilogIOElement sig: outputs) {
			if (!sig.isSignalSet()) primaryBfm.addStatement("  " + sig.getName() + " = 0;");
		}
	   	primaryBfm.addStatement("end");
	   	primaryBfm.addStatement("");

		// if go toggles, initiate transaction
	   	primaryBfm.addStatement("always @(posedge leaf_go) begin");
	   	// apply inputs on next clock cycle
	   	primaryBfm.addStatement("  @(posedge CLK);");
	   	primaryBfm.addStatement("    #1 active = 1'b1;");  // indicate transaction active
	   	primaryBfm.addStatement("    leaf_dec_addr = address;");
	   	primaryBfm.addStatement("    leaf_dec_wr_data = wr_data;");
	   	primaryBfm.addStatement("    leaf_dec_valid = 1'b1;");
	   	primaryBfm.addStatement("    leaf_dec_wr_dvld = 1'b0;");
	   	primaryBfm.addStatement("    leaf_dec_cycle = type;");	   	
	   	primaryBfm.addStatement("    trans_size = {1'b0, size} + 5'b1;");
   
		primaryBfm.addStatement("    if (type[1] == 1'b0) begin");			
	   	primaryBfm.addStatement("      leaf_dec_wr_width = size" + SystemVerilogSignal.genRefArrayString(0, dataSizeBits) + ";");
	   	primaryBfm.addStatement("      $display(\"%0d: initiating %d word write to address %x (data=%x)...\", $time, trans_size, address, wr_data);");
	   	primaryBfm.addStatement("    end");
		primaryBfm.addStatement("    else begin");			
	   	primaryBfm.addStatement("      leaf_dec_wr_width = 0;");  // set width during read to zero
	   	primaryBfm.addStatement("      $display(\"%0d: initiating %d word read to address %x...\", $time, trans_size, address);");
	   	primaryBfm.addStatement("    end");
	   	primaryBfm.addStatement("");

	   	// sample dut outputs on next clock cycle
	   	primaryBfm.addStatement("  @(posedge CLK);");
	   	primaryBfm.addStatement("    leaf_dec_valid = 1'b0;");  // drop valid after 1 cycle
	   	primaryBfm.addStatement("    leaf_dec_wr_dvld <= ~type[1];");   // activate wr_valid later
	   	primaryBfm.addStatement("    while (~dec_leaf_reject & ~dec_leaf_ack & ~dec_leaf_nack) begin");
	   	primaryBfm.addStatement("       @(posedge CLK);");
	   	primaryBfm.addStatement("       leaf_dec_wr_dvld = 1'b0;");  // wr valid removed after a cycle
	   	primaryBfm.addStatement("    end");
	   	primaryBfm.addStatement("");

	   	// turn off valids and indicate done
	   	primaryBfm.addStatement("  leaf_dec_valid = 1'b0;");
	   	primaryBfm.addStatement("  leaf_dec_wr_dvld = 1'b0;");

	   	primaryBfm.addStatement("  done = 1'b1;");  // indicate transaction done

	   	primaryBfm.addStatement("  $display(\"  accept = %d\", dec_leaf_accept);");
	   	primaryBfm.addStatement("  $display(\"  reject = %d\", dec_leaf_reject);");
	   	primaryBfm.addStatement("  $display(\"  ack = %d\", dec_leaf_ack);");
	   	primaryBfm.addStatement("  $display(\"  nack = %d\", dec_leaf_nack);");	   	
		primaryBfm.addStatement("  $display(\"  return size = %x\", dec_leaf_data_width);");	   	
	   	primaryBfm.addStatement("  $display(\"  retry = %d\", dec_leaf_retry_atomic);");	
	   	// if a read, display return info
		primaryBfm.addStatement("  if (type[1] == 1'b1) begin");			
	   	primaryBfm.addStatement("    $display(\"  read data = %x\", dec_leaf_rd_data);");
		primaryBfm.addStatement("    if (rd_compare) begin");			
		primaryBfm.addStatement("      if (dec_leaf_rd_data !== rd_data) $display(\"  read compare FAILED - expected %x\", rd_data);");	
		primaryBfm.addStatement("      else $display(\"  read compare OK - expected %x\", rd_data);");	
	   	primaryBfm.addStatement("    end");
	   	primaryBfm.addStatement("  end");

	   	// indicate transaction is complete
	   	primaryBfm.addStatement("  #1 active = 1'b0;");  
	   	primaryBfm.addStatement("  #1 done = 1'b0;");  

	   	primaryBfm.addStatement("end");
	}

	/** add common bfm IO to signal list */
	private void addCommonBfmIO() {
		benchSigList.addSimpleVector(HW, PIO, "address", 0, ExtParameters.getLeafAddressSize());
		benchSigList.addSimpleVector(HW, PIO, "wr_data", 0, getMaxRegWidth());
		benchSigList.addSimpleVector(HW, PIO, "wr_enable", 0, decoder.getWriteEnableWidth());
		benchSigList.addSimpleScalar(HW, PIO, "rd_compare");
		benchSigList.addSimpleVector(HW, PIO, "rd_data", 0, getMaxRegWidth());
		benchSigList.addSimpleVector(HW, PIO, "type", 0,2);  
		benchSigList.addSimpleVector(HW, PIO, "size", 0, 4);
		benchSigList.addSimpleVector(HW, PIO, "leaf_go", 0, 1);
		benchSigList.addSimpleScalar(HW, PIO, "CLK");
		benchSigList.addSimpleScalar(PIO, HW, "active");
		benchSigList.addSimpleScalar(PIO, HW,  "done");
	}

}
