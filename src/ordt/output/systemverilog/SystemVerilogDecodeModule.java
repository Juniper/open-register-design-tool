/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.systemverilog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.systemverilog.SystemVerilogDefinedSignals.DefSignalType;
import ordt.output.systemverilog.common.SystemVerilogModule;
import ordt.output.systemverilog.common.SystemVerilogSignal;
import ordt.output.systemverilog.io.SystemVerilogIOSignalList;
import ordt.output.AddressableInstanceProperties;
//import ordt.output.RegProperties;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;
import ordt.parameters.ExtParameters.SVBlockSelectModes;
import ordt.parameters.ExtParameters.SVDecodeInterfaceTypes;

/** derived class for decoder module 
 *  note: this class is tightly coupled with builder - uses several builder methods
 */
public class SystemVerilogDecodeModule extends SystemVerilogModule {

	// set default (internal) pio interface signal names (these are post arbiter/post gated clock delay)
	// intermediate versions are derived from these
	protected final String pioInterfaceAddressName = "pio_dec_address";
	protected final String pioInterfaceWriteDataName = "pio_dec_write_data";
	protected final String pioInterfaceWriteEnableName = "pio_dec_write_enable";
	protected final String pioInterfaceTransactionSizeName = "pio_dec_trans_size";
	protected final String pioInterfaceRetTransactionSizeName = "dec_pio_trans_size";
	protected final String pioInterfaceWeName = "pio_dec_write";
	protected final String pioInterfaceReName = "pio_dec_read";
	protected final String pioInterfaceReadDataName = "dec_pio_read_data";
	protected final String pioInterfaceAckName = "dec_pio_ack";
	protected final String pioInterfaceNackName = "dec_pio_nack";
	protected final String pioInterfaceAckNextName = pioInterfaceAckName + "_next";  // early ack/nack needed in leaf i/f
	protected final String pioInterfaceNackNextName = pioInterfaceNackName + "_next";
	protected final String arbiterAtomicName = "arb_atomic_request";
	
	protected List<AddressableInstanceProperties> decoderList = new ArrayList<AddressableInstanceProperties>();    // list of address regs 
	protected SVDecodeInterfaceTypes primaryInterfaceType = SVDecodeInterfaceTypes.PARALLEL;  // default to parallel on primary interface
	protected SystemVerilogBuilder builder;  // builder creating this module

	public SystemVerilogDecodeModule(SystemVerilogBuilder builder, int insideLocs, String clkName) {
		super(builder, insideLocs, clkName, builder.getDefaultReset());
		this.builder = builder;  // save reference to calling builder
	}

	/** add a register or external interface to the decoder */ 
	public void addToDecode(AddressableInstanceProperties instProperties) {
		if ((instProperties.getRepCount() > ExtParameters.sysVerMaxInternalRegReps()) && !instProperties.isExternal()) 
			Ordt.errorExit("Replication count (" + instProperties.getRepCount() + ") exceeded max for internal register " + instProperties.getInstancePath() + ".  Set max_internal_reg_reps to override.");

		decoderList.add(instProperties);
		//if (regProperties.isExternal()) System.out.println("SystemVerilogDecoder addToDecode: adding ext " + regProperties.getInstancePath() + " at base=" + regProperties.getBaseAddress());
	}

	public List<AddressableInstanceProperties> getDecodeList() {
		return decoderList;
	}

	// -------------------------- IO list helper methods ----------------------------
	
	/** add a new scalar IO signal to the hw list based on sigType */
	public void addHwScalar(DefSignalType sigType) {
		this.addHwVector(sigType, 0, 1);
	}

	/** add a new vector IO signal  to the hw list based on sigType */
	public void addHwVector(DefSignalType sigType, int lowIndex, int size) {
		SystemVerilogIOSignalList sigList = ioHash.get(SystemVerilogBuilder.HW);  // get the hw siglist
		if (sigList == null) return;
		sigList.addVector(sigType, lowIndex, size); 
	}

	// ------------------------------ decoder pio interface methods -------------------------------
	
	public void setPrimaryInterfaceType(SVDecodeInterfaceTypes interfaceType) {
		//System.out.println("SystemVerilogDecoder setPrimaryInterfaceType: " + interfaceType);
		this.primaryInterfaceType = interfaceType;
	}

	private boolean hasPrimaryInterfaceType(SVDecodeInterfaceTypes type) {
		return (this.primaryInterfaceType == type);
	}

	private boolean hasSecondaryInterfaceType(SVDecodeInterfaceTypes type) {
		return (ExtParameters.getSysVerSecondaryDecoderInterface() == type);
	}

	/** returns true if this decoder has a secondary processor interface */
	boolean hasSecondaryInterface() {
		return ((builder.isBaseBuilder() || ExtParameters.secondaryOnChildAddrmaps()) && !hasSecondaryInterfaceType(SVDecodeInterfaceTypes.NONE));
	}

	private boolean hasInterfaceType(SVDecodeInterfaceTypes type) {
		return (hasPrimaryInterfaceType(type) || hasSecondaryInterfaceType(type));
	}

	/** return true if a primary or secondary interface of this decoder supports peer decoders and
	 * thus uses its base address to make address match decisions
	 */
	public boolean usesBaseAddress() {
		return (hasInterfaceType(SVDecodeInterfaceTypes.LEAF) || hasInterfaceType(SVDecodeInterfaceTypes.RING8) ||
		        hasInterfaceType(SVDecodeInterfaceTypes.RING16) || hasInterfaceType(SVDecodeInterfaceTypes.RING32));
	}

	/** create processor interface logic */  
	public void genPioInterfaces(AddressableInstanceProperties topRegProperties) {
		// generate the primary processor interface
		genPrimaryPioInterface(topRegProperties);
		
		// generate the secondary processor interface
		if (hasSecondaryInterface()) {
			genSecondaryPioInterface(topRegProperties);
			genInterfaceArbiter();		
		}
		
		// generate common internal pio interface code
		this.generateCommonPio(topRegProperties);
	}

	/** create primary processor interface */
	private void genPrimaryPioInterface(AddressableInstanceProperties topRegProperties) {
        //System.out.println("SystemVerilogDecode genPrimaryPioInterface: isS8=" + hasPrimaryInterfaceType(SVDecodeInterfaceTypes.SERIAL8));
		// create interface logic
		if (hasPrimaryInterfaceType(SVDecodeInterfaceTypes.LEAF)) this.genLeafPioInterface(topRegProperties, true);
		else if (hasPrimaryInterfaceType(SVDecodeInterfaceTypes.SERIAL8)) this.genSerial8PioInterface(topRegProperties, true);
		else if (hasPrimaryInterfaceType(SVDecodeInterfaceTypes.RING8)) this.genRingPioInterface(8, topRegProperties, true);
		else if (hasPrimaryInterfaceType(SVDecodeInterfaceTypes.RING16)) this.genRingPioInterface(16, topRegProperties, true);
		else if (hasPrimaryInterfaceType(SVDecodeInterfaceTypes.RING32)) this.genRingPioInterface(32, topRegProperties, true);
		else if (hasPrimaryInterfaceType(SVDecodeInterfaceTypes.PARALLEL)) this.genParallelPioInterface(topRegProperties, true, false);
		else if (hasPrimaryInterfaceType(SVDecodeInterfaceTypes.PARALLEL_PULSED)) this.genParallelPioInterface(topRegProperties, true, true);
		else {
			String instStr = (topRegProperties == null)? "root" : topRegProperties.getInstancePath();
			Ordt.errorExit("invalid decoder primary interface type specified for addrmap instance " + instStr);
		}
	}

	/** create secondary processor interface */
	private void genSecondaryPioInterface(AddressableInstanceProperties topRegProperties) {
		// create interface logic
		if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.NONE)) return;
		//if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.LEAF)) this.genLeafPioInterface(topRegProperties, false);
		if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.SERIAL8)) this.genSerial8PioInterface(topRegProperties, false);
		else if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.RING8)) this.genRingPioInterface(8, topRegProperties, false);
		else if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.RING16)) this.genRingPioInterface(16, topRegProperties, false);
		else if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.RING32)) this.genRingPioInterface(32, topRegProperties, false);
		else if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.PARALLEL)) this.genParallelPioInterface(topRegProperties, false, false);
		else if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.PARALLEL_PULSED)) this.genParallelPioInterface(topRegProperties, false, true);
		else if (hasSecondaryInterfaceType(SVDecodeInterfaceTypes.ENGINE1)) this.genEngine1Interface(topRegProperties);
		else {
			String instStr = (topRegProperties == null)? "root" : topRegProperties.getInstancePath();
			Ordt.errorExit("invalid decoder secondary interface type specified for addrmap instance " + instStr);
		}
	}

	/** return a signal name with prefix extended according to primary/secondary decoder state */
	private String getSigName(boolean isPrimary, String baseName) {
		String prefix = !hasSecondaryInterface()? "" : isPrimary? "p1_" : "p2_";
		return prefix + baseName;
	}

	/** return a group name prefix according to primary/secondary decoder state */
	private String getGroupPrefix(boolean isPrimary) {
		String prefix = !hasSecondaryInterface()? "" : isPrimary? "primary " : "secondary ";
		return prefix;
	}

	/** generate common internal pio interface code */
	private void generateCommonPio(AddressableInstanceProperties topRegProperties) {
		// add internal registered input sigs
		this.addScalarReg("pio_write_active");  // write indication
		this.addScalarReg("pio_read_active");  // read indication
		if (mapHasMultipleAddresses()) this.addVectorReg("pio_dec_address_d1", builder.getAddressLowBit(), builder.getMapAddressWidth());  // address
		this.addVectorReg("pio_dec_write_data_d1", 0, builder.getMaxRegWidth());  // input write data capture register 
		
        // pio read/write actives - enabled by ext re/we and disabled when ack/nack
		this.addResetAssign("pio i/f", builder.getDefaultReset(), "pio_write_active <= #1  1'b0;");
		this.addResetAssign("pio i/f", builder.getDefaultReset(), "pio_read_active <= #1  1'b0;");
		this.addRegAssign("pio i/f",  "pio_write_active <= #1  pio_write_active ? pio_no_acks : pio_activate_write;"); // active stays high until ack/nack 		   
		this.addRegAssign("pio i/f",  "pio_read_active <= #1  pio_read_active ? pio_no_acks : pio_activate_read;");  		   

		if (mapHasMultipleAddresses()) this.addRegAssign("pio i/f",  "pio_dec_address_d1 <= #1   " + pioInterfaceAddressName + ";");  // capture address if new transaction		   
		this.addRegAssign("pio i/f",  "pio_dec_write_data_d1 <= #1  " + pioInterfaceWriteDataName + ";"); // capture write data if new transaction
		
		// if write enables are specified, then capture
		if (hasWriteEnables()) {
			if ( (ExtParameters.getMinDataSize() <= ExtParameters.sysVerWriteEnableSize()) ||
					((ExtParameters.getMinDataSize() % ExtParameters.sysVerWriteEnableSize()) != 0) ) 
				Ordt.errorExit("Invalid write enable size (" + ExtParameters.sysVerWriteEnableSize() + ") specified - must be a factor of min_data_size (" + ExtParameters.getMinDataSize() + ")");
			this.addVectorReg("pio_dec_write_enable_d1", 0, getWriteEnableWidth());  // input write enable capture register (max width)
			this.addVectorReg("pio_dec_write_enable_full", 0, builder.getMaxRegWidth());  // expanded enable vector for internals
			this.addRegAssign("pio i/f",  "pio_dec_write_enable_d1 <= #1  " + pioInterfaceWriteEnableName + ";"); // capture write enable if new transaction
		}
		
		// if max transaction is larger than min, add transaction size signals 
		if (builder.getMaxRegWordWidth() > 1) {
			// register trans size input
			this.addVectorReg("pio_dec_trans_size_d1", 0, builder.getMaxWordBitSize());  // input trans size capture register
		    this.addRegAssign("pio i/f",  "pio_dec_trans_size_d1 <= #1  " + pioInterfaceTransactionSizeName + ";"); // capture trans size if new transaction
		    this.addVectorReg(pioInterfaceRetTransactionSizeName, 0, builder.getMaxWordBitSize());  //  register the size
			this.addResetAssign("pio i/f", builder.getDefaultReset(), pioInterfaceRetTransactionSizeName + " <= #1 " + builder.getMaxWordBitSize() + "'b0;");  // reset for delayed block select 
			this.addRegAssign("pio i/f",  pioInterfaceRetTransactionSizeName + " <= #1 reg_width;");  // use pio_width from decode to set 
			this.addVectorReg("reg_width", 0, builder.getMaxWordBitSize());   // size of current register
		}

		// ------------ add read data output regs
		this.addVectorReg(pioInterfaceReadDataName, 0, builder.getMaxRegWidth());  //  read data output  
		this.addVectorReg("dec_pio_read_data_d1", 0, builder.getMaxRegWidth());  // max width read data register  
		
		// select full size read word
		this.addCombinAssign("pio read data", pioInterfaceReadDataName + " = dec_pio_read_data_d1;"); 	   					
		this.addResetAssign("pio read data", builder.getDefaultReset(), "dec_pio_read_data_d1 <= #1  " + builder.getMaxRegWidth() + "'b0;");  // reset for read data 
		this.addRegAssign("pio read data",  "dec_pio_read_data_d1 <= #1 dec_pio_read_data_next;");  // capture return data	   

		// ------------- add ack/nack output regs
		this.addScalarReg(pioInterfaceAckName);  // return ack
		this.addScalarReg(pioInterfaceNackName);  // return nack
		this.addScalarReg(pioInterfaceAckNextName);  // next return ack
		this.addScalarReg(pioInterfaceNackNextName);  // next return nack

        // pio output reg assignments
		this.addResetAssign("pio ack/nack", builder.getDefaultReset(), pioInterfaceAckName + " <= #1 1'b0;");  // reset for ack
		this.addResetAssign("pio ack/nack", builder.getDefaultReset(), pioInterfaceNackName + " <= #1 1'b0;");  // reset for nack 
		this.addRegAssign("pio ack/nack",  pioInterfaceAckName + " <= #1 " + pioInterfaceAckName + " ? 1'b0 : " + pioInterfaceAckNextName + ";");  // return ack
		this.addRegAssign("pio ack/nack",  pioInterfaceNackName + " <= #1 " + pioInterfaceNackName + " ? 1'b0 : " + pioInterfaceNackNextName + ";");  // return nack		   
		
		this.addScalarReg("pio_internal_ack");    // set in decoder case	statement	   
		this.addScalarReg("pio_internal_nack");  
		this.addCombinAssign("pio ack/nack", "pio_internal_nack = (pio_read_active | pio_write_active) & ~pio_internal_ack & ~external_transaction_active;");  // internal nack  
		// if nack on partials specified, convert an ack with invalid size to a nack
		if (ExtParameters.sysVerNackPartialWrites() && (builder.getMaxRegWordWidth() > 1)) {
			this.addScalarReg("pio_partial_write");  
			this.addScalarReg("pio_partial_write_nack");  
			this.addCombinAssign("pio ack/nack",  "pio_partial_write = pio_write_active && (reg_width > " + pioInterfaceTransactionSizeName + ");");   
			this.addCombinAssign("pio ack/nack",  "pio_partial_write_nack = pio_partial_write && (pio_internal_ack | (pio_external_ack_next & external_transaction_active));");  // return ack	   
			this.addCombinAssign("pio ack/nack",  pioInterfaceAckNextName + " = !pio_partial_write && (pio_internal_ack | (pio_external_ack_next & external_transaction_active));");  // return ack	   
			this.addCombinAssign("pio ack/nack",  pioInterfaceNackNextName + " = (pio_partial_write_nack | pio_internal_nack | (pio_external_nack_next & external_transaction_active));");  // return nack		   
		}
		else {
			this.addCombinAssign("pio ack/nack",  pioInterfaceAckNextName + " = (pio_internal_ack | (pio_external_ack_next & external_transaction_active));");  // return ack	   
			this.addCombinAssign("pio ack/nack",  pioInterfaceNackNextName + " = (pio_internal_nack | (pio_external_nack_next & external_transaction_active));");  // return nack		   
		}
		
		this.addScalarReg("pio_external_ack");    // set in decoder case	statement via ios from hw		   
		this.addScalarReg("pio_external_nack");    		   
		this.addScalarReg("pio_external_ack_next");    // set in decoder case	statement via ios from hw		   
		this.addScalarReg("pio_external_nack_next");    
		
		this.addResetAssign("pio ack/nack", builder.getDefaultReset(), "pio_external_ack <= #1  1'b0;");  // reset for incoming ack 
		this.addResetAssign("pio ack/nack", builder.getDefaultReset(), "pio_external_nack <= #1  1'b0;");  // reset for incoming nack 
		this.addRegAssign("pio ack/nack",  "pio_external_ack <= #1 pio_external_ack_next;");  // capture ack	   
		this.addRegAssign("pio ack/nack",  "pio_external_nack <= #1 pio_external_nack_next;");  // capture ack	   

		// create signal to turn off read_active and write_active (use external ack/nack one cycle early
		this.addScalarReg("pio_no_acks");    		   
		this.addCombinAssign("pio ack/nack", "pio_no_acks = ~(" + pioInterfaceAckName + " | " + pioInterfaceNackName +
				                                                   " | pio_external_ack | pio_external_nack);"); 
		this.addScalarReg("pio_activate_write");    		   
		this.addCombinAssign("pio ack/nack", "pio_activate_write = (" + pioInterfaceWeName + 
				                                 " & ~(" + pioInterfaceAckName + " | " + pioInterfaceNackName + "));"); 
		this.addScalarReg("pio_activate_read");    		   
		this.addCombinAssign("pio ack/nack", "pio_activate_read = (" + pioInterfaceReName + 
				                                 " & ~(" + pioInterfaceAckName + " | " + pioInterfaceNackName + "));"); 
		
		//this.addCombinAssign("pio i/f", "$display($time, \" Entering pio i/f block...\");"); 

		this.addVectorReg("dec_pio_read_data_next", 0, builder.getMaxRegWidth());  // read data assignment signal
		this.addScalarReg("external_transaction_active");
		
		// if addr monitoring is enabled, then add these IO
		if (mapHasMultipleAddresses() && ExtParameters.sysVerIncludeAddrMonitor())  generateAddrMonitor(topRegProperties, "pio_dec_address_d1", 
					"pio_activate_write", "pio_activate_read", pioInterfaceAckNextName, pioInterfaceNackNextName);
	}

	/** generate the address monitor IO (output write ack/nack, read ack/nack pulses) */
	private void generateAddrMonitor(AddressableInstanceProperties addrInstProperties, String addrName,
			String writeName, String readName, String ackName, String nackName) {
		String instPathStr = (addrInstProperties == null)? "" : "_" + addrInstProperties.getBaseName();
		
		// create address monitor IOs
		String addrMonitorLowAddrName = "h2d" + instPathStr + "_addr_mon_low_addr";                      
		String addrMonitorHighAddrName = "h2d" + instPathStr + "_addr_mon_high_addr";                      
		
		String addrMonitorReadAckName = "d2h" + instPathStr + "_addr_mon_rd_ack_o";                      
		String addrMonitorReadNackName = "d2h" + instPathStr + "_addr_mon_rd_nack_o";                      
		String addrMonitorWriteAckName = "d2h" + instPathStr + "_addr_mon_wr_ack_o";                      
		String addrMonitorWriteNackName = "d2h" + instPathStr + "_addr_mon_wr_nack_o"; 
		
		String addrMonitorReadAckNextName = addrMonitorReadAckName + "_next";                      
		String addrMonitorReadNackNextName = addrMonitorReadNackName + "_next";                      
		String addrMonitorWriteAckNextName = addrMonitorWriteAckName + "_next";                      
		String addrMonitorWriteNackNextName = addrMonitorWriteNackName + "_next";   
		
		this.addSimpleVectorFrom(SystemVerilogBuilder.HW, addrMonitorLowAddrName, builder.getAddressLowBit(), builder.getMapAddressWidth());    // low address 
		this.addSimpleVectorFrom(SystemVerilogBuilder.HW, addrMonitorHighAddrName, builder.getAddressLowBit(), builder.getMapAddressWidth());    // high address 
		
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, addrMonitorReadAckName);     
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, addrMonitorReadNackName); 
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, addrMonitorWriteAckName); 
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, addrMonitorWriteNackName); 
		
		this.addScalarReg(addrMonitorReadAckName);
		this.addScalarReg(addrMonitorReadNackName);
		this.addScalarReg(addrMonitorWriteAckName);
		this.addScalarReg(addrMonitorWriteNackName);
		
		this.addResetAssign("address monitor", builder.getDefaultReset(), addrMonitorReadAckName + " <= #1  1'b0;");  
		this.addRegAssign("address monitor",  addrMonitorReadAckName + " <= #1  " + addrMonitorReadAckNextName + ";");  
		this.addResetAssign("address monitor", builder.getDefaultReset(), addrMonitorReadNackName + " <= #1  1'b0;");  
		this.addRegAssign("address monitor",  addrMonitorReadNackName + " <= #1  " + addrMonitorReadNackNextName + ";");  
		this.addResetAssign("address monitor", builder.getDefaultReset(), addrMonitorWriteAckName + " <= #1  1'b0;");  
		this.addRegAssign("address monitor",  addrMonitorWriteAckName + " <= #1  " + addrMonitorWriteAckNextName + ";");  
		this.addResetAssign("address monitor", builder.getDefaultReset(), addrMonitorWriteNackName + " <= #1  1'b0;");  
		this.addRegAssign("address monitor",  addrMonitorWriteNackName + " <= #1  " + addrMonitorWriteNackNextName + ";");  

		// create combi output values		
		this.addScalarWire(addrMonitorReadAckNextName);
		this.addScalarWire(addrMonitorReadNackNextName);
		this.addScalarWire(addrMonitorWriteAckNextName);
		this.addScalarWire(addrMonitorWriteNackNextName);
		
		// create address match signal
		this.addScalarWire("addr_monitor_match");
		this.addWireAssign("addr_monitor_match = ((" + addrName + " >= " + addrMonitorLowAddrName + ") && (" + addrName + " <= " + addrMonitorHighAddrName + "));");
		
		// assign monitor outputs
		this.addWireAssign(addrMonitorReadAckNextName + " = addr_monitor_match && " + readName + " && " + ackName + ";");
		this.addWireAssign(addrMonitorReadNackNextName + " = addr_monitor_match && " + readName + " && " + nackName + ";");
		this.addWireAssign(addrMonitorWriteAckNextName + " = addr_monitor_match && " + writeName + " && " + ackName + ";");
		this.addWireAssign(addrMonitorWriteNackNextName + " = addr_monitor_match && " + writeName + " && " + nackName + ";");
	}

	/** return true if write enables are specified */
	public static boolean hasWriteEnables() {
		return (ExtParameters.sysVerWriteEnableSize()>0);
	}

	/** return width of write enble vector */  
	public int getWriteEnableWidth() {
		if (!hasWriteEnables()) return 0;
		return builder.getMaxRegWidth() / ExtParameters.sysVerWriteEnableSize();
	}

	/** generate request arb logic between to proc interfaces */
	private void genInterfaceArbiter() {
		// set p1 internal interface names
		String p1AddressName = getSigName(true, this.pioInterfaceAddressName);
		String p1WriteDataName = getSigName(true, this.pioInterfaceWriteDataName);
		String p1WriteEnableName = getSigName(true, this.pioInterfaceWriteEnableName);
		String p1TransactionSizeName = getSigName(true, this.pioInterfaceTransactionSizeName);
		String p1RetTransactionSizeName = getSigName(true, this.pioInterfaceRetTransactionSizeName);
		String p1WeName = getSigName(true, this.pioInterfaceWeName);
		String p1ReName = getSigName(true, this.pioInterfaceReName);
		String p1ReadDataName = getSigName(true, this.pioInterfaceReadDataName);
		String p1AckName = getSigName(true, this.pioInterfaceAckName);
		String p1NackName = getSigName(true, this.pioInterfaceNackName);
		String p1AckNextName = getSigName(true, this.pioInterfaceAckNextName);
		String p1NackNextName = getSigName(true, this.pioInterfaceNackNextName);
		String p1AtomicName = getSigName(true, this.arbiterAtomicName);
		// set p2 internal interface names
		String p2AddressName = getSigName(false, this.pioInterfaceAddressName);
		String p2WriteDataName = getSigName(false, this.pioInterfaceWriteDataName);
		String p2WriteEnableName = getSigName(false, this.pioInterfaceWriteEnableName);
		String p2TransactionSizeName = getSigName(false, this.pioInterfaceTransactionSizeName);
		String p2RetTransactionSizeName = getSigName(false, this.pioInterfaceRetTransactionSizeName);
		String p2WeName = getSigName(false, this.pioInterfaceWeName);
		String p2ReName = getSigName(false, this.pioInterfaceReName);
		String p2ReadDataName = getSigName(false, this.pioInterfaceReadDataName);
		String p2AckName = getSigName(false, this.pioInterfaceAckName);
		String p2NackName = getSigName(false, this.pioInterfaceNackName);
		String p2AckNextName = getSigName(false, this.pioInterfaceAckNextName);
		String p2NackNextName = getSigName(false, this.pioInterfaceNackNextName);
		String p2AtomicName = getSigName(false, this.arbiterAtomicName);
		
		// create p1, p2 interface outputs
		this.addVectorWire(p1ReadDataName, 0, builder.getMaxRegWidth());  //  read data output  
		this.addScalarReg(p1AckName);  // return ack
		this.addScalarReg(p1NackName);  // return nack
		this.addScalarReg(p1AckNextName);  // return early ack
		this.addScalarReg(p1NackNextName);  // return early nack
		if (builder.getMaxRegWordWidth() > 1) this.addVectorWire(p1RetTransactionSizeName, 0, builder.getMaxWordBitSize());  //  register the size
		this.addVectorWire(p2ReadDataName, 0, builder.getMaxRegWidth());  //  read data output  
		this.addScalarReg(p2AckName);  // return ack
		this.addScalarReg(p2NackName);  // return nack
		this.addScalarReg(p2AckNextName);  // return early ack
		this.addScalarReg(p2NackNextName);  // return early nack
		if (builder.getMaxRegWordWidth() > 1) this.addVectorWire(p2RetTransactionSizeName, 0, builder.getMaxWordBitSize());  //  register the size
		
		// create common internal i/f inputs
		if (mapHasMultipleAddresses()) this.addVectorReg(pioInterfaceAddressName, builder.getAddressLowBit(), builder.getMapAddressWidth());  //  address to be used internally 
		this.addVectorReg(pioInterfaceWriteDataName, 0, builder.getMaxRegWidth());  //  wr data to be used internally 
		if (builder.getMaxRegWordWidth() > 1) this.addVectorReg(pioInterfaceTransactionSizeName, 0, builder.getMaxWordBitSize());  //  internal transaction size
		this.addScalarWire(pioInterfaceReName);  //  read enable to be used internally 
		this.addScalarWire(pioInterfaceWeName);  //  write enable be used internally 	
		// if write enables are specified, then capture
		if (hasWriteEnables()) this.addVectorReg(pioInterfaceWriteEnableName, 0, getWriteEnableWidth()); 
		
		// set sm signal names that will be set in sm 
		String arbiterReName = "arb_" + pioInterfaceReName;
		String arbiterWeName = "arb_" + pioInterfaceWeName;
		String arbiterReNextName = arbiterReName + "_next";
		String arbiterWeNextName = arbiterWeName + "_next";
		
		String arbStateName = "arbiter_state";                      
		String arbStateNextName = arbStateName + "_next";                      
		
		// assign secondary address check signal
		String arbiterValidSecAddressName = "arb_valid_sec_address";
		this.addScalarWire(arbiterValidSecAddressName); 
		// create address compare string
		String lowSecAddrStr = "";
		String highSecAddrStr = "";
		String valCompareStr = "1'b1";
		if (this.mapHasMultipleAddresses() && (ExtParameters.hasSecondaryLowAddress() || ExtParameters.hasSecondaryHighAddress())) {
			int lowAddrBit = builder.getAddressLowBit();   
			int addrSize = builder.getMapAddressWidth(); 
			if (ExtParameters.hasSecondaryLowAddress()) {
				RegNumber compareBits = ExtParameters.getSecondaryLowAddress().getSubVector(lowAddrBit, addrSize);
				lowSecAddrStr = "(" + p2AddressName + " >= " + compareBits.toFormat(NumBase.Hex, NumFormat.Verilog) + ")"; 
			}
			if (ExtParameters.hasSecondaryHighAddress()) {
				RegNumber compareBits = ExtParameters.getSecondaryHighAddress().getSubVector(lowAddrBit, addrSize);
				highSecAddrStr = "(" + p2AddressName + " <= " + compareBits.toFormat(NumBase.Hex, NumFormat.Verilog) + ")"; 
			}
			if (lowSecAddrStr.isEmpty()) 
				valCompareStr = (highSecAddrStr.isEmpty())? "1'b1" : highSecAddrStr;
			else
				valCompareStr = (highSecAddrStr.isEmpty())? lowSecAddrStr : "(" + lowSecAddrStr + " && " + highSecAddrStr + ")";
		}
		
		this.addWireAssign(arbiterValidSecAddressName + " = " + valCompareStr + ";");
		
        // create sm vars
		String groupName = "interface arbiter sm";  
		int stateBits = 3;
		this.addVectorReg(arbStateName, 0, stateBits);  
		this.addVectorReg(arbStateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), arbStateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  arbStateName + " <= #1  " + arbStateNextName + ";");  
		
		// registered r/w and ack/nack sm outputs
		this.addScalarReg(arbiterReName);
		this.addScalarReg(arbiterWeName);
		this.addScalarReg(arbiterReNextName);
		this.addScalarReg(arbiterWeNextName);
		this.addResetAssign(groupName, builder.getDefaultReset(), arbiterReName + " <= #1  1'b0;");  
		this.addRegAssign(groupName,  arbiterReName + " <= #1  " + arbiterReNextName + ";");  
		this.addResetAssign(groupName, builder.getDefaultReset(), arbiterWeName + " <= #1  1'b0;");  
		this.addRegAssign(groupName,  arbiterWeName + " <= #1  " + arbiterWeNextName + ";");  

		// state machine init values
		this.addCombinAssign(groupName,  arbStateNextName + " = " + arbStateName + ";");  
		this.addCombinAssign(groupName,  arbiterReNextName + " =  1'b0;");  // write active
		this.addCombinAssign(groupName,  arbiterWeNextName + " =  1'b0;");  // read active
			
		// state machine  /
		String IDLE = stateBits + "'h0"; 
		String P1_ACTIVE = stateBits + "'h1"; 
		String P2_ACTIVE = stateBits + "'h2"; 
		String P2_NACK = stateBits + "'h3"; 
		String P1_ATOMIC = stateBits + "'h4"; 
		String P2_ATOMIC = stateBits + "'h5"; 
				
		this.addCombinAssign(groupName, "case (" + arbStateName + ")"); 

		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");
		// go on request - p1 has priority in idle
		this.addCombinAssign(groupName, "      if (" + p1ReName + " || " + p1WeName + ") begin");  
		this.addCombinAssign(groupName, "        " + arbStateNextName + " = " + P1_ACTIVE + ";");  
		this.addCombinAssign(groupName, "        " + arbiterReNextName + " = " + p1ReName + ";");
		this.addCombinAssign(groupName, "        " + arbiterWeNextName + " = " + p1WeName + ";");
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "      else if (" + p2ReName + " || " + p2WeName + ") begin");  
		this.addCombinAssign(groupName, "        if (" + arbiterValidSecAddressName + ") begin");  
		this.addCombinAssign(groupName, "          " + arbStateNextName + " = " + P2_ACTIVE + ";");  
		this.addCombinAssign(groupName, "          " + arbiterReNextName + " = " + p2ReName + ";");
		this.addCombinAssign(groupName, "          " + arbiterWeNextName + " = " + p2WeName + ";");
		this.addCombinAssign(groupName, "        end"); 
		this.addCombinAssign(groupName, "        else " + arbStateNextName + " = " + P2_NACK + ";");  
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// P1_ACTIVE - interface 1 active
		this.addCombinAssign(groupName, "  " + P1_ACTIVE + ": begin // P1_ACTIVE");
		// if ack/ack, pass thru to intf and jump to next state
		this.addCombinAssign(groupName, "      if (" + pioInterfaceAckName + " || " + pioInterfaceNackName + ") begin");  
		this.addCombinAssign(groupName, "        if (" + p1AtomicName + ") " + arbStateNextName + " = " + P1_ATOMIC + ";");  // p1 atomic request
		this.addCombinAssign(groupName, "        else if (" + p2ReName + " || " + p2WeName + ") begin");  
		this.addCombinAssign(groupName, "          if (" + arbiterValidSecAddressName + ") begin");  
		this.addCombinAssign(groupName, "            " + arbStateNextName + " = " + P2_ACTIVE + ";");  
		this.addCombinAssign(groupName, "            " + arbiterReNextName + " = " + p2ReName + ";");
		this.addCombinAssign(groupName, "            " + arbiterWeNextName + " = " + p2WeName + ";");
		this.addCombinAssign(groupName, "          end"); 
		this.addCombinAssign(groupName, "          else " + arbStateNextName + " = " + P2_NACK + ";");  
		this.addCombinAssign(groupName, "        end"); 
		this.addCombinAssign(groupName, "        else " + arbStateNextName + " = " + IDLE + ";");  // else back to idle
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "      else if (!" + p1ReName + " && !" + p1WeName + ") " + arbStateNextName + " = " + IDLE + ";");  // back to idle if request disappears
		this.addCombinAssign(groupName, "      else begin"); 
		this.addCombinAssign(groupName, "        " + arbiterReNextName + " = " + p1ReName + ";");
		this.addCombinAssign(groupName, "        " + arbiterWeNextName + " = " + p1WeName + ";");
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// P2_ACTIVE - interface 2 active
		this.addCombinAssign(groupName, "  " + P2_ACTIVE + ": begin // P2_ACTIVE");
		// if ack/ack, pass thru to intf and jump to next state
		this.addCombinAssign(groupName, "      if (" + pioInterfaceAckName + " || " + pioInterfaceNackName + ") begin");  
		this.addCombinAssign(groupName, "        if (" + p2AtomicName + ") " + arbStateNextName + " = " + P2_ATOMIC + ";");  // p2 atomic request
		this.addCombinAssign(groupName, "        else if (" + p1ReName + " || " + p1WeName + ") begin");  
		this.addCombinAssign(groupName, "          " + arbStateNextName + " = " + P1_ACTIVE + ";");  
		this.addCombinAssign(groupName, "          " + arbiterReNextName + " = " + p1ReName + ";");
		this.addCombinAssign(groupName, "          " + arbiterWeNextName + " = " + p1WeName + ";");
		this.addCombinAssign(groupName, "        end"); 
		this.addCombinAssign(groupName, "        else " + arbStateNextName + " = " + IDLE + ";");  // else back to idle
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "      else if (!" + p2ReName + " && !" + p2WeName + ") " + arbStateNextName + " = " + IDLE + ";");  // back to idle if request disappears
		this.addCombinAssign(groupName, "      else begin"); 
		this.addCombinAssign(groupName, "        " + arbiterReNextName + " = " + p2ReName + ";");
		this.addCombinAssign(groupName, "        " + arbiterWeNextName + " = " + p2WeName + ";");
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// P2_NACK - invalid address so nack intr 2 transaction
		this.addCombinAssign(groupName, "  " + P2_NACK + ": begin // P2_NACK");
		//this.addCombinAssign(groupName, "      " + p2NackName + " = 1'b1;");
		this.addCombinAssign(groupName, "      " + arbStateNextName + " = " + IDLE + ";");  // back to idle
		this.addCombinAssign(groupName, "    end"); 

		// P1_ATOMIC - wait for next p1 request and lock out p2
		this.addCombinAssign(groupName, "  " + P1_ATOMIC + ": begin // P1_ATOMIC");
		this.addCombinAssign(groupName, "      if (" + p1ReName + " || " + p1WeName + ") begin");  
		this.addCombinAssign(groupName, "        " + arbStateNextName + " = " + P1_ACTIVE + ";");  
		this.addCombinAssign(groupName, "        " + arbiterReNextName + " = " + p1ReName + ";");
		this.addCombinAssign(groupName, "        " + arbiterWeNextName + " = " + p1WeName + ";");
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 

		// P2_ATOMIC - wait for next p2 request and lock out p1
		this.addCombinAssign(groupName, "  " + P2_ATOMIC + ": begin // P2_ATOMIC");
		this.addCombinAssign(groupName, "      if (" + p2ReName + " || " + p2WeName + ") begin");  
		this.addCombinAssign(groupName, "        if (" + arbiterValidSecAddressName + ") begin");  
		this.addCombinAssign(groupName, "          " + arbStateNextName + " = " + P2_ACTIVE + ";");  
		this.addCombinAssign(groupName, "          " + arbiterReNextName + " = " + p2ReName + ";");
		this.addCombinAssign(groupName, "          " + arbiterWeNextName + " = " + p2WeName + ";");
		this.addCombinAssign(groupName, "        end"); 
		this.addCombinAssign(groupName, "        else " + arbStateNextName + " = " + P2_NACK + ";");  
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + arbStateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 
		
		// create a separate always block to assign ack/nack
		groupName = "interface arbiter acks/nacks";  
		this.addCombinAssign(groupName,  p1AckName + " =  1'b0;");  // p1 ack
		this.addCombinAssign(groupName,  p1NackName + " =  1'b0;");   // p1 nack
		this.addCombinAssign(groupName,  p2AckName + " =  1'b0;");  // p2 ack
		this.addCombinAssign(groupName,  p2NackName + " =  1'b0;");   // p2 nack
		this.addCombinAssign(groupName, "if (" + arbStateName + " == " + P1_ACTIVE + ") begin");  
		this.addCombinAssign(groupName, "  " + p1AckName + " = " + pioInterfaceAckName + ";");
		this.addCombinAssign(groupName, "  " + p1NackName + " = " + pioInterfaceNackName + ";");
		this.addCombinAssign(groupName, "end"); 
		this.addCombinAssign(groupName, "else if (" + arbStateName + " == " + P2_ACTIVE + ") begin");  
		this.addCombinAssign(groupName, "  " + p2AckName + " = " + pioInterfaceAckName + ";");
		this.addCombinAssign(groupName, "  " + p2NackName + " = " + pioInterfaceNackName + ";");
		this.addCombinAssign(groupName, "end"); 
		this.addCombinAssign(groupName, "else if (" + arbStateName + " == " + P2_NACK + ") begin");  
		this.addCombinAssign(groupName, "  " + p2NackName + " = 1'b1;");
		this.addCombinAssign(groupName, "end"); 
		
		// create a separate always block to assign early ack/nack
		groupName = "interface arbiter early acks/nacks";  
		this.addCombinAssign(groupName,  p1AckNextName + " =  1'b0;");  // p1 next ack
		this.addCombinAssign(groupName,  p1NackNextName + " =  1'b0;");   // p1 next nack
		this.addCombinAssign(groupName,  p2AckNextName + " =  1'b0;");  // p2 next ack
		this.addCombinAssign(groupName,  p2NackNextName + " =  1'b0;");   // p2 next nack
		this.addCombinAssign(groupName, "if (" + arbStateName + " == " + P1_ACTIVE + ") begin");  
		this.addCombinAssign(groupName, "  " + p1AckNextName + " = " + pioInterfaceAckNextName + ";");
		this.addCombinAssign(groupName, "  " + p1NackNextName + " = " + pioInterfaceNackNextName + ";");
		this.addCombinAssign(groupName, "end"); 
		this.addCombinAssign(groupName, "else if (" + arbStateName + " == " + P2_ACTIVE + ") begin");  
		this.addCombinAssign(groupName, "  " + p2AckNextName + " = " + pioInterfaceAckNextName + ";");
		this.addCombinAssign(groupName, "  " + p2NackNextName + " = " + pioInterfaceNackNextName + ";");
		this.addCombinAssign(groupName, "end"); 
		//this.addCombinAssign(groupName, "else if (" + arbStateName + " == " + P2_NACK + ") begin");  
		//this.addCombinAssign(groupName, "  " + p2NackNextName + " = 1'b1;");   // Note this is a cycle late so addr guard wont work on secondary leaf
		//this.addCombinAssign(groupName, "end"); 
		
		// create a separate always block to assign ack
		groupName = "interface arbiter input select";  
		// default to p1 interface
		// by default, address, date and transaction size are from primary interface
		if (mapHasMultipleAddresses()) this.addCombinAssign(groupName,  pioInterfaceAddressName + " = " + p1AddressName + ";");
	    this.addCombinAssign(groupName,  pioInterfaceWriteDataName + " = " + p1WriteDataName + ";");
	    if (hasWriteEnables()) this.addCombinAssign(groupName,  pioInterfaceWriteEnableName + " = " + p1WriteEnableName + ";");
	    if (builder.getMaxRegWordWidth() > 1) this.addCombinAssign(groupName,  pioInterfaceTransactionSizeName + " = " + p1TransactionSizeName + ";");
	    
		this.addCombinAssign(groupName, "if (" + arbStateName + " == " + P2_ACTIVE + ") begin");  
		if (mapHasMultipleAddresses()) this.addCombinAssign(groupName, "  " + pioInterfaceAddressName + " = " + p2AddressName + ";");
	    this.addCombinAssign(groupName,  "  " + pioInterfaceWriteDataName + " = " + p2WriteDataName + ";");
	    if (hasWriteEnables()) this.addCombinAssign(groupName,  "  " + pioInterfaceWriteEnableName + " = " + p2WriteEnableName + ";");
	    if (builder.getMaxRegWordWidth() > 1) this.addCombinAssign(groupName,  "  " + pioInterfaceTransactionSizeName + " = " + p2TransactionSizeName + ";");
		this.addCombinAssign(groupName, "end"); 
				
		// assign responses back to interfaces
		this.addWireAssign(p1ReadDataName + " = " + pioInterfaceReadDataName + ";");
		this.addWireAssign(p2ReadDataName + " = " + pioInterfaceReadDataName + ";");
		if (builder.getMaxRegWordWidth() > 1) {
			this.addWireAssign(p1RetTransactionSizeName + " = " + pioInterfaceRetTransactionSizeName + ";");
			this.addWireAssign(p2RetTransactionSizeName + " = " + pioInterfaceRetTransactionSizeName + ";");
		}

		// generate re/we assigns - use delayed versions if gating is active
		assignReadWriteRequests(arbiterReName, arbiterWeName, pioInterfaceReName, pioInterfaceWeName, true);	

	}

	/** add parallel interface signals (internal intf signals passed thru to interface)
	 * @param pulseControls - if true, incoming read and write control IOs are assumed to be single pulse
	 */
	private void genParallelPioInterface(AddressableInstanceProperties topRegProperties, boolean isPrimary, boolean pulseControls) {
		//System.out.println("SystemVerilogDecodeModule: generating decoder with parallel interface, id=" + topRegProperties.getInstancePath());
		// set internal interface names
		String pioInterfaceAddressName = getSigName(isPrimary, this.pioInterfaceAddressName);
		String pioInterfaceWriteDataName = getSigName(isPrimary, this.pioInterfaceWriteDataName);
		String pioInterfaceWriteEnableName = getSigName(isPrimary, this.pioInterfaceWriteEnableName);
		String pioInterfaceTransactionSizeName = getSigName(isPrimary, this.pioInterfaceTransactionSizeName);
		String pioInterfaceRetTransactionSizeName = getSigName(isPrimary, this.pioInterfaceRetTransactionSizeName);
		String pioInterfaceWeName = getSigName(isPrimary, this.pioInterfaceWeName);
		String pioInterfaceReName = getSigName(isPrimary, this.pioInterfaceReName);
		String pioInterfaceReadDataName = getSigName(isPrimary, this.pioInterfaceReadDataName);
		String pioInterfaceAckName = getSigName(isPrimary, this.pioInterfaceAckName);
		String pioInterfaceNackName = getSigName(isPrimary, this.pioInterfaceNackName);
		String arbiterAtomicName = getSigName(isPrimary, this.arbiterAtomicName);
		// set IO names
		String ioAddressName = getSigName(isPrimary, "h2d_" + this.pioInterfaceAddressName);  // address
		String ioWriteDataName = getSigName(isPrimary, "h2d_" + this.pioInterfaceWriteDataName);  // write data
		String ioWriteEnableName = getSigName(isPrimary, "h2d_" + this.pioInterfaceWriteEnableName);  // write enable
		String ioTransactionSizeName = getSigName(isPrimary, "h2d_" + this.pioInterfaceTransactionSizeName);  // transaction size
		String ioRetTransactionSizeName = getSigName(isPrimary, "d2h_" + this.pioInterfaceRetTransactionSizeName);  // return transaction size
		String ioWeName = getSigName(isPrimary, "h2d_" + this.pioInterfaceWeName);  // write indication
		String ioReName = getSigName(isPrimary, "h2d_" + this.pioInterfaceReName);  // read indication
		String ioReadDataName = getSigName(isPrimary, "d2h_" + this.pioInterfaceReadDataName);  // read data
		String ioAckName = getSigName(isPrimary, "d2h_" + this.pioInterfaceAckName);  // ack indication
		String ioNackName = getSigName(isPrimary, "d2h_" + this.pioInterfaceNackName);  // nack indication
		if (topRegProperties != null) {  // if not root, match names of parent driving IO
			ioAddressName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.D2H_ADDR));  // address
			ioWriteDataName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.D2H_DATA));  // write data
			ioWriteEnableName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.D2H_ENABLE));  // write enable
			ioTransactionSizeName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.D2H_SIZE));  // transaction size
			ioRetTransactionSizeName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.H2D_RETSIZE));  // return transaction size
		    ioWeName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.D2H_WE));  // write indication
			ioReName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.D2H_RE));  // read indication
			ioReadDataName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.H2D_DATA));  // read data
			ioAckName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.H2D_ACK));  // ack indication
			ioNackName = getSigName(isPrimary, topRegProperties.getFullSignalName(DefSignalType.H2D_NACK));  // nack indication
		}

		// generate name-base IO names
		if (mapHasMultipleAddresses()) this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ioAddressName, builder.getAddressLowBit(), builder.getMapAddressWidth());  // address
		this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ioWriteDataName, 0, builder.getMaxRegWidth());  // write data
		if (hasWriteEnables()) this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ioWriteEnableName, 0, getWriteEnableWidth());  // write enable
		// if max transaction for this addrmap is larger than 1, add transaction size signals
		if (builder.getMaxRegWordWidth() > 1) {
		   this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ioTransactionSizeName, 0, builder.getMaxWordBitSize());  // transaction size
		   this.addSimpleVectorTo(SystemVerilogBuilder.PIO, ioRetTransactionSizeName, 0, builder.getMaxWordBitSize());  // return transaction size
		}
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, ioWeName);  // write indication
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, ioReName);  // read indication
	
		this.addSimpleVectorTo(SystemVerilogBuilder.PIO, ioReadDataName, 0, builder.getMaxRegWidth());  // read data
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, ioAckName);  // ack indication
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, ioNackName);  // nack indication
		
		// define internal interface inputs
		if (mapHasMultipleAddresses()) this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), builder.getMapAddressWidth());  //  address to be used internally 
		this.addVectorWire(pioInterfaceWriteDataName, 0, builder.getMaxRegWidth());  //  wr data to be used internally 
		if (hasWriteEnables()) this.addVectorWire(pioInterfaceWriteEnableName, 0, getWriteEnableWidth());  //  wr enable to be used internally 
		if (builder.getMaxRegWordWidth() > 1) this.addVectorWire(pioInterfaceTransactionSizeName, 0, builder.getMaxWordBitSize());  //  internal transaction size
		this.addScalarWire(pioInterfaceReName);  //  read enable to be used internally 
		this.addScalarWire(pioInterfaceWeName);  //  write enable be used internally 
		
		// disable atomic request to arbiter
		if (hasSecondaryInterface()) {
			this.addScalarWire(arbiterAtomicName);   
			this.addWireAssign(arbiterAtomicName + " = 1'b0;");
		}
		
		// assign input IOs to internal interface
		if (mapHasMultipleAddresses()) this.addWireAssign(pioInterfaceAddressName + " = " + ioAddressName + ";");   
		this.addWireAssign(pioInterfaceWriteDataName + " = " + ioWriteDataName + ";");
		if (hasWriteEnables()) this.addWireAssign(pioInterfaceWriteEnableName + " = " + ioWriteEnableName + ";");
		if (builder.getMaxRegWordWidth() > 1) this.addWireAssign(pioInterfaceTransactionSizeName + " = " + ioTransactionSizeName + ";");
		
		// if pulsed controls are specified, generate internal level-based controls
		String levelIoReName = ioReName + "_lvl";
		String levelIoWeName = ioWeName + "_lvl";
		if (pulseControls) {
			String groupName = getGroupPrefix(isPrimary) + "parallel pulsed i/f signals";  
			this.addScalarReg(levelIoReName);   
			this.addResetAssign(groupName, builder.getDefaultReset(), levelIoReName + " <= #1  1'b0;");  
			this.addScalarReg(levelIoWeName);   
			this.addResetAssign(groupName, builder.getDefaultReset(), levelIoWeName + " <= #1  1'b0;");  
			this.addRegAssign(groupName,  levelIoReName + " <= #1  " + ioReName  + " | (" + levelIoReName + " & ~(" + pioInterfaceAckName + " | " + pioInterfaceNackName + "));");
			this.addRegAssign(groupName,  levelIoWeName + " <= #1  " + ioWeName  + " | (" + levelIoWeName + " & ~(" + pioInterfaceAckName + " | " + pioInterfaceNackName + "));");
		}
		// generate re/we assigns - use delayed versions if this is a single primary
		String activeIoReName = pulseControls? levelIoReName : ioReName;
		String activeIoWeName = pulseControls? levelIoWeName : ioWeName;
		assignReadWriteRequests(activeIoReName, activeIoWeName, pioInterfaceReName, pioInterfaceWeName, !hasSecondaryInterface());
		
		// assign output IOs to internal interface
		if (builder.getMaxRegWordWidth() > 1) this.addWireAssign(ioRetTransactionSizeName + " = " + pioInterfaceRetTransactionSizeName + ";");	
		this.addWireAssign(ioReadDataName + " = " + pioInterfaceReadDataName + ";");
		this.addWireAssign(ioAckName + " = " + pioInterfaceAckName + ";");
		this.addWireAssign(ioNackName + " = " + pioInterfaceNackName + ";");
	}

	/** generate an assist engine (not valid for primary interface) */
	private void genEngine1Interface(AddressableInstanceProperties topRegProperties) {
		// exit with error if this addrmap has only a single address
		if (!mapHasMultipleAddresses()) Ordt.errorExit("Assist engine can only be used in addrmaps with multiple addresses");
		
		boolean isPrimary = false;  // engine1 is always secondary
		// set internal interface names
		String pioInterfaceAddressName = getSigName(isPrimary, this.pioInterfaceAddressName);
		String pioInterfaceWriteDataName = getSigName(isPrimary, this.pioInterfaceWriteDataName);
		String pioInterfaceWriteEnableName = getSigName(isPrimary, this.pioInterfaceWriteEnableName);
		String pioInterfaceTransactionSizeName = getSigName(isPrimary, this.pioInterfaceTransactionSizeName);
		//String pioInterfaceRetTransactionSizeName = getSigName(isPrimary, this.pioInterfaceRetTransactionSizeName);
		String pioInterfaceWeName = getSigName(isPrimary, this.pioInterfaceWeName);
		String pioInterfaceReName = getSigName(isPrimary, this.pioInterfaceReName);
		String pioInterfaceReadDataName = getSigName(isPrimary, this.pioInterfaceReadDataName);
		String pioInterfaceAckName = getSigName(isPrimary, this.pioInterfaceAckName);
		String pioInterfaceNackName = getSigName(isPrimary, this.pioInterfaceNackName);
		String arbiterAtomicName = getSigName(isPrimary, this.arbiterAtomicName);
		
		// compute max transaction size  
		int regWidth = builder.getMaxRegWidth();
		int regWords = builder.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = (regWords > 1);  // if transaction sizes need to be sent/received
		
		// set addr low bit and map width
		int addrWidth = builder.getMapAddressWidth();
		int addrLowBit = builder.getAddressLowBit();
		int transCountWidth = Math.max(addrWidth, 16);
		
		// engine1 IO hierarchy 
		String pathStr = (topRegProperties == null)? "" : "_" + topRegProperties.getBaseName();
		String e1BaseHierName = getSigName(isPrimary, "e1" + pathStr);                      
		String e1CntlHierName = "cntl";                      
		String e1CntlName = e1BaseHierName + "_" + e1CntlHierName;                      
		String e1CfgHierName = "cfg";                      
		String e1CfgName = e1BaseHierName + "_" + e1CfgHierName;                      
		String e1StatusHierName = "status";                      
		String e1StatusName = e1BaseHierName + "_" + e1StatusHierName;                      
		
		// engine1 control inputs 
		String e1StartHierName = "start_r"; 
		String e1StopHierName = "force_stop_r";
		
		// engine1 config inputs 		
		String e1ModeHierName = "mode_r"; 
		String e1StopOnReadHierName = "stop_on_read_r"; 
		String e1StopOnCountHierName = "stop_on_count_r"; 
		String e1TransDelayHierName = "trans_delay_r"; 
		String e1WrSizeHierName = "write_trans_size_r";
		String e1ReadCaptureModeHierName = "read_capture_mode_r";                      

		String e1WriteDataHierName = "write_data";                      
		String e1WriteMaskHierName = "write_mask";                      
		
		String e1AddressStartHierName = "address_start";                      
		String e1AddressStepHierName = "address_step";                      
		String e1MaxTransCountHierName = "max_trans_count";   // number of transactions                  

		String e1ReadMatchHierName = "read_match_data";                      
		String e1ReadMaskHierName = "read_mask";                      

		//
		String e1StartName = e1CntlName + "_" + e1StartHierName; 
		String e1StopName = e1CntlName + "_" + e1StopHierName;

		String e1ModeName = e1CfgName + "_" + e1ModeHierName; // 0-write, 1-read, 2-rmw
		String e1StopOnReadName = e1CfgName + "_" + e1StopOnReadHierName; // 0- disabled, 1-lt, 2-equals, 3-gt 
		String e1StopOnCountName = e1CfgName + "_" + e1StopOnCountHierName; 
		String e1TransDelayName = e1CfgName + "_" + e1TransDelayHierName; 
		String e1ReadCaptureModeName = e1CfgName + "_" + e1ReadCaptureModeHierName;  // 0 - none, 1 - min, 2 - max, 3 - all                    
		String e1WrSizeName = e1CfgName + "_" + e1WrSizeHierName;
		
		String e1WriteDataName = e1BaseHierName + "_" + e1WriteDataHierName + "_val_r";                      
		String e1WriteMaskName = e1BaseHierName + "_" + e1WriteMaskHierName + "_val_r";                      
		
		String e1AddressStartName = e1BaseHierName + "_" + e1AddressStartHierName + "_val_r";                      
		String e1AddressStepName = e1BaseHierName + "_" + e1AddressStepHierName + "_val_r";                      
		String e1MaxTransCountName = e1BaseHierName + "_" + e1MaxTransCountHierName + "_val_r";   // number of transactions                  

		String e1ReadMatchName = e1BaseHierName + "_" + e1ReadMatchHierName + "_val_r"; // used for stop on read                     
		String e1ReadMaskName = e1BaseHierName + "_" + e1ReadMaskHierName + "_val_r";  // used for stop on read                      

		// engine1 outputs
		String e1StateHierName = "state_w";                      
		String e1NackErrorHierName = "nack_error_intr"; 
		//String e1BadSizeErrorHierName = "bad_size_error_w"); 
		String e1BadAddressErrorHierName = "bad_address_error_intr"; 

		String e1LastReadDataHierName = "last_read_data";                      

		//
		String e1StateName = e1StatusName + "_" + e1StateHierName;                      
		String e1NackErrorName = e1StatusName + "_" + e1NackErrorHierName; 
		//String e1BadSizeErrorName = e1StatusName + "_" + e1BadSizeErrorHierName; 
		String e1BadAddressErrorName = e1StatusName + "_" + e1BadAddressErrorHierName; 
		
		String e1LastReadDataName = e1BaseHierName + "_" + e1LastReadDataHierName + "_val_w";                      

		// create hierarchical IO so we can encapsulate in an interface (will mimic this structure in rdl reg structure for simple connect)
		int stateBits = 3;  // state machine bits
        int delayCountBits = 10;  // delay counter bits
		pushIOSignalSet(false, SystemVerilogBuilder.DECODE, SystemVerilogBuilder.PIO, null, e1BaseHierName,  1,  true,  true,  false, "interface", null); // base e1 intf
		// cntl reg
		pushIOSignalSet(false, SystemVerilogBuilder.DECODE, SystemVerilogBuilder.PIO, null, e1CntlHierName,  1,  true,  false,  false,  null, null); 
		addScalarFrom(SystemVerilogBuilder.PIO, null, e1StartHierName);
		addScalarFrom(SystemVerilogBuilder.PIO, null, e1StopHierName);
		popIOSignalSet(SystemVerilogBuilder.PIO);  // end cntl reg
		// cfg reg
		pushIOSignalSet(false, SystemVerilogBuilder.DECODE, SystemVerilogBuilder.PIO, null, e1CfgHierName,  1,  true,  false,  false,  null, null); 
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1ModeHierName, 0, 2);
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1StopOnReadHierName, 0, 2);
		addScalarFrom(SystemVerilogBuilder.PIO, null, e1StopOnCountHierName);
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1TransDelayHierName, 0, delayCountBits);
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1ReadCaptureModeHierName, 0, 2);
		if (useTransactionSize) addVectorFrom(SystemVerilogBuilder.PIO, null, e1WrSizeHierName, 0, regWordBits);
		popIOSignalSet(SystemVerilogBuilder.PIO);  // end cfg reg
		// write inputs
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1WriteDataHierName + "_val_r", 0, regWidth);
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1WriteMaskHierName + "_val_r", 0, regWidth);
		// address inputs
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1AddressStartHierName + "_val_r", addrLowBit, addrWidth);
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1AddressStepHierName + "_val_r", addrLowBit, addrWidth);
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1MaxTransCountHierName + "_val_r", addrLowBit, transCountWidth); // make at least 16b 
		// read inputs
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1ReadMatchHierName + "_val_r", 0, regWidth);
		addVectorFrom(SystemVerilogBuilder.PIO, null, e1ReadMaskHierName + "_val_r", 0, regWidth);
		// status reg
		pushIOSignalSet(false, SystemVerilogBuilder.DECODE, SystemVerilogBuilder.PIO, null, e1StatusHierName,  1,  true,  false, false, null, null); 
		addVectorTo(SystemVerilogBuilder.PIO, null, e1StateHierName, 0, stateBits);
		addScalarTo(SystemVerilogBuilder.PIO, null, e1NackErrorHierName);
		addScalarTo(SystemVerilogBuilder.PIO, null, e1BadAddressErrorHierName);
		popIOSignalSet(SystemVerilogBuilder.PIO);  
		// last read output
		addVectorTo(SystemVerilogBuilder.PIO, null, e1LastReadDataHierName + "_val_w", 0, regWidth);
		popIOSignalSet(SystemVerilogBuilder.PIO);  // end base hier
				
		// engine1 state machine signals  
		String e1StateNextName = getSigName(isPrimary, "e1_state_next");                      
		String e1TransCountName = getSigName(isPrimary, "e1_trans_count");                 
		String e1TransCountNextName = e1TransCountName + "_next";                 
		String e1DelayCountName = getSigName(isPrimary, "e1_delay_count");                 
		String e1DelayCountNextName = e1DelayCountName + "_next";                 

		String e1AddressNextName = pioInterfaceAddressName + "_next";                      
		String e1WrDataNextName = pioInterfaceWriteDataName + "_next"; 
		String e1TransSizeNextName = pioInterfaceTransactionSizeName + "_next"; 
		String e1WeNextName = pioInterfaceWeName + "_next"; 
		String e1ReNextName = pioInterfaceReName + "_next"; 
		String e1AtomicNextName = arbiterAtomicName + "_next"; 
		String e1LastReadDataNextName = e1LastReadDataName + "_next";                       
		
		// define the internal interface signals
		this.addVectorReg(pioInterfaceWriteDataName, 0, builder.getMaxRegWidth());  //  wr data to be used internally 
		this.addVectorReg(pioInterfaceAddressName, addrLowBit, addrWidth);  //  address to be used internally 
		this.addScalarReg(pioInterfaceReName);  //  read enable to be used internally 
		this.addScalarReg(pioInterfaceWeName);  //  write enable be used internally 
		this.addScalarReg(arbiterAtomicName);
		if (useTransactionSize) this.addVectorReg(pioInterfaceTransactionSizeName, 0, regWordBits);
		
		// tie off enables
		if (hasWriteEnables()) {
			Ordt.warnMessage("Assist engine decoder interface will not generate write data enables.");
			this.addVectorWire(pioInterfaceWriteEnableName, 0, getWriteEnableWidth()); 
			this.addWireAssign(pioInterfaceWriteEnableName + " = " + SystemVerilogBuilder.getHexOnesString(getWriteEnableWidth()) + ";");
		}
		
		// define error outputs
		this.addScalarReg(e1NackErrorName);
		//this.addScalarReg(e1BadSizeErrorName);
		this.addScalarReg(e1BadAddressErrorName);

		// assign sm outputs to internal p2 interface
		String groupName = getGroupPrefix(isPrimary) + "engine1 i/f signals";  
		this.addVectorReg(e1WrDataNextName, 0, builder.getMaxRegWidth());  //  wr data to be used internally 
		this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceWriteDataName + " <= #1  " + builder.getMaxRegWidth() + "'b0;");  
		this.addRegAssign(groupName,  pioInterfaceWriteDataName + " <= #1  " + e1WrDataNextName + ";");  
		this.addVectorReg(e1AddressNextName, addrLowBit, addrWidth + 1); // extra bit for rollover
		this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceAddressName + " <= #1  " + addrWidth + "'b0;");  
		this.addRegAssign(groupName,  pioInterfaceAddressName + " <= #1  " + e1AddressNextName + SystemVerilogSignal.genRefArrayString(addrLowBit, addrWidth) + ";");  
		this.addScalarReg(e1ReNextName);   
		this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceReName + " <= #1  1'b0;");  
		this.addRegAssign(groupName,  pioInterfaceReName + " <= #1  " + e1ReNextName + ";");  
		this.addScalarReg(e1WeNextName);   
		this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceWeName + " <= #1  1'b0;");  
		this.addRegAssign(groupName,  pioInterfaceWeName + " <= #1  " + e1WeNextName + ";");  
		this.addScalarReg(e1AtomicNextName);
		this.addResetAssign(groupName, builder.getDefaultReset(), arbiterAtomicName + " <= #1  1'b0;");  
		this.addRegAssign(groupName,  arbiterAtomicName + " <= #1  " + e1AtomicNextName + ";");  
		if (useTransactionSize) {
			this.addVectorReg(e1TransSizeNextName, 0, regWordBits);
			this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceTransactionSizeName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  pioInterfaceTransactionSizeName + " <= #1  " + e1TransSizeNextName + ";");  
		}
		// assign sm outputs to external interface
		this.addVectorReg(e1LastReadDataName, 0, builder.getMaxRegWidth());  //  rd data to be output externally
		this.addVectorReg(e1LastReadDataNextName, 0, builder.getMaxRegWidth());  
		this.addResetAssign(groupName, builder.getDefaultReset(), e1LastReadDataName + " <= #1  " + builder.getMaxRegWidth() + "'b0;");  
		this.addRegAssign(groupName,  e1LastReadDataName + " <= #1  " + e1LastReadDataNextName + ";");  
		
		// now create state machine vars
		groupName = getGroupPrefix(isPrimary) + "engine1 i/f sm";  
		this.addVectorReg(e1StateName, 0, stateBits);  
		this.addVectorReg(e1StateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), e1StateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  e1StateName + " <= #1  " + e1StateNextName + ";");  

		//  define transaction counter and next value
		this.addVectorReg(e1TransCountName, addrLowBit, transCountWidth + 1); 
		this.addVectorReg(e1TransCountNextName, addrLowBit, transCountWidth + 1); 
		this.addResetAssign(groupName, builder.getDefaultReset(), e1TransCountName + " <= #1  " + transCountWidth + 1 + "'b0;");  
		this.addRegAssign(groupName,  e1TransCountName + " <= #1  " + e1TransCountNextName + ";");  

		//  define delay counter and next value
		this.addVectorReg(e1DelayCountName, 0, delayCountBits); 
		this.addVectorReg(e1DelayCountNextName, 0, delayCountBits); 
		this.addResetAssign(groupName, builder.getDefaultReset(), e1DelayCountName + " <= #1  " + delayCountBits + "'b0;");  
		this.addRegAssign(groupName,  e1DelayCountName + " <= #1  " + e1DelayCountNextName + ";");  
		
		// state machine init values
		this.addCombinAssign(groupName,  e1StateNextName + " = " + e1StateName + ";");  
		this.addCombinAssign(groupName,  e1ReNextName + " =  1'b0;");  
		this.addCombinAssign(groupName,  e1WeNextName + " =  1'b0;");  
		this.addCombinAssign(groupName,  e1AtomicNextName + " =  1'b0;");  
		this.addCombinAssign(groupName,  e1WrDataNextName + " = " + pioInterfaceWriteDataName + ";");  
		this.addCombinAssign(groupName,  e1LastReadDataNextName + " = " + e1LastReadDataName + ";");  
		if (useTransactionSize)
			this.addCombinAssign(groupName,  e1TransSizeNextName + " = " + pioInterfaceTransactionSizeName + ";"); 
		// init error outputs to off 
		this.addCombinAssign(groupName,  e1NackErrorName + " =  1'b0;");   
		//this.addCombinAssign(groupName,  e1BadSizeErrorName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  e1BadAddressErrorName + " =  1'b0;");  
		// init transaction count and address
		this.addCombinAssign(groupName,  e1TransCountNextName + " = " + e1TransCountName + ";"); // transaction count holds by default
		this.addCombinAssign(groupName,  e1AddressNextName + " = {1'b0, " + pioInterfaceAddressName + "};"); // address holds by default
		this.addCombinAssign(groupName,  e1DelayCountNextName + " = " + delayCountBits + "'b0;"); // delay count is cleared by default
			
		// engine transaction modes     0-write, 1-read, 2-rmw
		//String MODE_WO = "2'h0"; 
		String MODE_RO = "2'h1"; 
		String MODE_RMW = "2'h2"; 
		
		// read data capture modes    0 - none, 1 - min, 2 - max, 3 - all
		//String RD_CAP_NONE = "2'h0"; 
		String RD_CAP_MIN = "2'h1"; 
		String RD_CAP_MAX = "2'h2"; 
		String RD_CAP_ALL = "2'h3"; 
		
	    // stop on read modes     0- disabled, 1-lt, 2-equals, 3-gt
		//String RD_STOP_OFF = "2'h0"; 
		String RD_STOP_LT = "2'h1"; 
		String RD_STOP_EQ = "2'h2"; 
		String RD_STOP_GT = "2'h3"; 
		
		// state machine states
		String IDLE = stateBits + "'h0"; 
		String READ_WAIT = stateBits + "'h1"; 
		String WRITE_WAIT = stateBits + "'h2"; 
		String READ = stateBits + "'h3"; 
		String WRITE = stateBits + "'h4"; 
				
		this.addCombinAssign(groupName, "case (" + e1StateName + ")"); 

		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");  
		// init transaction count, address and transaction size
		this.addCombinAssign(groupName, "      " + e1TransCountNextName + " = " + addrWidth + 1 + "'b0;");
		this.addCombinAssign(groupName, "      " + e1AddressNextName + " = {1'b0, " + e1AddressStartName + "};");  // set start address
		if (useTransactionSize)
		    this.addCombinAssign(groupName, "      " + e1TransSizeNextName + " = " + e1WrSizeName + ";");  // set transaction size
		// go on e1_start
		this.addCombinAssign(groupName, "      if (" + e1StartName + " && !" + e1StopName + ") begin");  
        // if looking for min read value, init read data high
		this.addCombinAssign(groupName, "        if (" + e1ReadCaptureModeName + " == " + RD_CAP_MIN + ")");
		this.addCombinAssign(groupName, "          " +  e1LastReadDataNextName + " = ~" + builder.getMaxRegWidth() + "'b0;");  
		this.addCombinAssign(groupName, "        else");
		this.addCombinAssign(groupName, "          " +  e1LastReadDataNextName + " = " + builder.getMaxRegWidth() + "'b0;");  
		// if rmw or read-only then read first
		this.addCombinAssign(groupName, "        if ((" + e1ModeName + " == " + MODE_RO + ") || (" + e1ModeName + " == " + MODE_RMW + ")) begin");  
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + READ_WAIT + ";");
		this.addCombinAssign(groupName, "        end"); 
		// otherwise write-only
		this.addCombinAssign(groupName, "        else begin");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + WRITE_WAIT + ";");  
		this.addCombinAssign(groupName, "          " + e1WrDataNextName + " = " + e1WriteDataName + ";");  // no rmw so use write data as-is
		this.addCombinAssign(groupName, "        end"); 
		this.addCombinAssign(groupName, "      end");  // start
		this.addCombinAssign(groupName, "    end"); 

		// READ_WAIT
		this.addCombinAssign(groupName, "  " + READ_WAIT + ": begin // READ_WAIT"); 
		// start the delay counter
		this.addCombinAssign(groupName, "      " + e1DelayCountNextName + " = " + e1DelayCountName + " + " + delayCountBits + "'b1;");
        // if stop we're done
		this.addCombinAssign(groupName, "      if (" + e1StopName + ") begin");
		this.addCombinAssign(groupName, "        " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "      end"); 
		// go to read on delay count done
		this.addCombinAssign(groupName, "      else if (" + e1DelayCountName + " == " + e1TransDelayName + ") begin");  
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + READ + ";");
		this.addCombinAssign(groupName, "          " + e1ReNextName + " =  1'b1;");  
		this.addCombinAssign(groupName, "          " + e1AtomicNextName + " =  (" + e1ModeName + " == " + MODE_RMW + ");");
		this.addCombinAssign(groupName, "      end");  // count done
		// if stop on read match we're done
		this.addCombinAssign(groupName, "      else if (|" + e1TransCountName + ") begin");  // non-zero trans count
		this.addCombinAssign(groupName, "        if ((" + e1StopOnReadName + " == " + RD_STOP_EQ + ") && ((" + e1LastReadDataName + " & ~" + e1ReadMaskName + ") == " + e1ReadMatchName + "))");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "        if ((" + e1StopOnReadName + " == " + RD_STOP_LT + ") && ((" + e1LastReadDataName + " & ~" + e1ReadMaskName + ") < " + e1ReadMatchName + "))");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "        if ((" + e1StopOnReadName + " == " + RD_STOP_GT + ") && ((" + e1LastReadDataName + " & ~" + e1ReadMaskName + ") > " + e1ReadMatchName + "))");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 

		// WRITE_WAIT
		this.addCombinAssign(groupName, "  " + WRITE_WAIT + ": begin // WRITE_WAIT"); 
		// start the delay counter
		this.addCombinAssign(groupName, "      " + e1DelayCountNextName + " = " + e1DelayCountName + " + " + delayCountBits + "'b1;");
        // if stop we're done
		this.addCombinAssign(groupName, "      if (" + e1StopName + ") begin");
		this.addCombinAssign(groupName, "        " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "      end"); 
		// go to read on delay count done
		this.addCombinAssign(groupName, "      else if (" + e1DelayCountName + " == " + e1TransDelayName + ") begin");  
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + WRITE + ";");  
		this.addCombinAssign(groupName, "          " + e1WeNextName + " =  1'b1;");  
		this.addCombinAssign(groupName, "      end");  // count done
		// if stop on read match we're done
		this.addCombinAssign(groupName, "      else if (|" + e1TransCountName + ") begin");  // non-zero trans count
		this.addCombinAssign(groupName, "        if ((" + e1StopOnReadName + " == " + RD_STOP_EQ + ") && ((" + e1LastReadDataName + " & ~" + e1ReadMaskName + ") == " + e1ReadMatchName + "))");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "        if ((" + e1StopOnReadName + " == " + RD_STOP_LT + ") && ((" + e1LastReadDataName + " & ~" + e1ReadMaskName + ") < " + e1ReadMatchName + "))");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "        if ((" + e1StopOnReadName + " == " + RD_STOP_GT + ") && ((" + e1LastReadDataName + " & ~" + e1ReadMaskName + ") > " + e1ReadMatchName + "))");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 

		// READ
		this.addCombinAssign(groupName, "  " + READ + ": begin // READ");
        // go on ack received
		this.addCombinAssign(groupName, "      if (" + pioInterfaceAckName + ") begin");  // ack 
        // bump next transactions count and address
		this.addCombinAssign(groupName, "        if (" + e1ModeName + " == " + MODE_RO + ") begin");
		this.addCombinAssign(groupName, "          " + e1TransCountNextName + " = " + e1TransCountName + " + " + addrWidth + 1 + "'b1;"); // bump the count
		this.addCombinAssign(groupName, "          " + e1AddressNextName + " = " + pioInterfaceAddressName + " + " + e1AddressStepName + ";");  // bump the address
		this.addCombinAssign(groupName, "        end"); 
        // capture read data 
		this.addCombinAssign(groupName, "        if (" + e1ReadCaptureModeName + " == " + RD_CAP_ALL + ")");
		this.addCombinAssign(groupName, "          " + e1LastReadDataNextName + " = " + pioInterfaceReadDataName + ";");
		this.addCombinAssign(groupName, "        if ((" + e1ReadCaptureModeName + " == " + RD_CAP_MIN + ") && (" + pioInterfaceReadDataName + " < " + e1LastReadDataName + "))");
		this.addCombinAssign(groupName, "          " + e1LastReadDataNextName + " = " + pioInterfaceReadDataName + ";"); 
		this.addCombinAssign(groupName, "        if ((" + e1ReadCaptureModeName + " == " + RD_CAP_MAX + ") && (" + pioInterfaceReadDataName + " > " + e1LastReadDataName + "))");
		this.addCombinAssign(groupName, "          " + e1LastReadDataNextName + " = " + pioInterfaceReadDataName + ";"); 
		// if stop we're done
		this.addCombinAssign(groupName, "        if (" + e1StopName + ") begin");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "        end"); 
		// else straight to write if rmw
		this.addCombinAssign(groupName, "        else if (" + e1ModeName + " == " + MODE_RMW + ") begin");   
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + WRITE + ";");
		this.addCombinAssign(groupName, "          " + e1WeNextName + " =  1'b1;");  
		this.addCombinAssign(groupName, "          " + e1WrDataNextName + " = ((" + e1WriteDataName + " & ~" + e1WriteMaskName + ") | (" +
				                                                           pioInterfaceReadDataName + " & " + e1WriteMaskName + "));");  // save masked read data 
		this.addCombinAssign(groupName, "        end"); 
		// else check for max or invalid trans count or do next read
		this.addCombinAssign(groupName, "        else if (" + e1ModeName + " == " + MODE_RO + ") begin");   
		this.addCombinAssign(groupName, "          if (" + e1StopOnCountName + " && (" + e1TransCountNextName + " == {1'b0," + e1MaxTransCountName + "}))");
		this.addCombinAssign(groupName, "            " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "          else if (" + e1AddressNextName + SystemVerilogSignal.genRefArrayString(addrLowBit + addrWidth, 1) + " == 1'b1) begin");
		this.addCombinAssign(groupName, "            " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "            " + e1BadAddressErrorName + " =  1'b1;"); 
		this.addCombinAssign(groupName, "          end"); 
		this.addCombinAssign(groupName, "          else begin"); 
		this.addCombinAssign(groupName, "            " + e1StateNextName + " = " + READ_WAIT + ";");
		this.addCombinAssign(groupName, "          end"); 
		this.addCombinAssign(groupName, "        end"); 
		// else invalid case (write-only)
		this.addCombinAssign(groupName, "        else "+ e1StateNextName + " = " + IDLE + ";"); 
		this.addCombinAssign(groupName, "      end"); 
		// error on nack
		this.addCombinAssign(groupName, "      else if (" + pioInterfaceNackName + ") begin"); 
		this.addCombinAssign(groupName, "        " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "        " + e1NackErrorName + " =  1'b1;"); 
		this.addCombinAssign(groupName, "      end"); 
		// else wait here
		this.addCombinAssign(groupName, "      else begin"); 
		this.addCombinAssign(groupName, "        " + e1ReNextName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + e1AtomicNextName + " =  (" + e1ModeName + " == " + MODE_RMW + ");");  
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 

		// WRITE
		this.addCombinAssign(groupName, "  " + WRITE + ": begin // WRITE");
        // go on ack received
		this.addCombinAssign(groupName, "      if (" + pioInterfaceAckName + ") begin");  
        // bump next transactions count and address
		this.addCombinAssign(groupName, "        if (" + e1ModeName + " != " + MODE_RO + ") begin");
		this.addCombinAssign(groupName, "          " + e1TransCountNextName + " = " + e1TransCountName + " + " + addrWidth + 1 + "'b1;"); // bump the count
		this.addCombinAssign(groupName, "          " + e1AddressNextName + " = " + pioInterfaceAddressName + " + " + e1AddressStepName + ";");  // bump the address
		this.addCombinAssign(groupName, "        end"); 
        // if max count reached or stop we're done
		this.addCombinAssign(groupName, "        if ((" + e1StopOnCountName + " && (" + e1TransCountNextName + " == {1'b0," + e1MaxTransCountName + "})) || " + e1StopName + ") begin");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "        end"); 
		// other wisedetect an out of range address    
		this.addCombinAssign(groupName, "        else if (" + e1AddressNextName + SystemVerilogSignal.genRefArrayString(addrLowBit + addrWidth, 1) + " == 1'b1) begin");
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "          " + e1BadAddressErrorName + " =  1'b1;"); 
		this.addCombinAssign(groupName, "        end"); 
		// else next read
		this.addCombinAssign(groupName, "        else if (" + e1ModeName + " == " + MODE_RMW + ") begin");   
		this.addCombinAssign(groupName, "          " + e1StateNextName + " = " + READ_WAIT + ";");
		this.addCombinAssign(groupName, "        end"); 
		// else another write
		this.addCombinAssign(groupName, "        else begin");
		this.addCombinAssign(groupName, "          "+ e1StateNextName + " = " + WRITE_WAIT + ";"); 
		this.addCombinAssign(groupName, "        end"); 
		this.addCombinAssign(groupName, "      end");  // ack
		// error on nack
		this.addCombinAssign(groupName, "      else if (" + pioInterfaceNackName + ") begin");  // nack 
		this.addCombinAssign(groupName, "        " + e1StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "        " + e1NackErrorName + " =  1'b1;"); 
		this.addCombinAssign(groupName, "      end");
		// else wait here
		this.addCombinAssign(groupName, "      else begin");
		this.addCombinAssign(groupName, "        " + e1WeNextName + " =  1'b1;");  
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + e1StateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 
		
		genEngine1Rdl(pathStr, 0, stateBits, delayCountBits, regWidth, regWords, regWordBits, useTransactionSize, addrWidth, addrLowBit, transCountWidth);
	}

	/** create rdl register definitions  */
	private void genEngine1Rdl(String nameSuffix, int rev, int stateBits, int delayCountBits, int regWidth, int regWords, int regWordBits, boolean useTransactionSize,
			int addrWidth, int addrLowBit, int transCountWidth) {
		List<String> outList = new ArrayList<String>();
		String fullName = "assist_engine_1" + nameSuffix;
		outList.add("");
		outList.add("// assist engine rdl: " + nameSuffix);
		outList.add("regfile " + fullName + " {");
		outList.add("  name = \"Assist engine v1 registers\";");
        // version register
		outList.add("  reg {");
		outList.add("    name = \"Version register\";");
		outList.add("    field { sw=r; hw=na;");
		outList.add("      name = \"Engine rev\";");
		outList.add("    } rev[4] = 4'd" + rev + ";");
		outList.add("    field { sw=r; hw=na;");
		outList.add("      name = \"Engine version\";");
		outList.add("    } ver[11:8] = 4'd1;");
		outList.add("    field { sw=r; hw=na;");
		outList.add("      name = \"Max word size\";");
		outList.add("    } max_word_size[20:16] = 5'd" + regWords + ";");
		outList.add("  } version;");
		// set of registers in engine interface
		outList.add("  regfile {");
		outList.add("    use_new_interface;");
		// cntl register
		outList.add("    reg {");
		outList.add("      name = \"Control register\";");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("        name = \"stop engine\";");
		outList.add("      } force_stop[0:0] = 1'b0;");
		outList.add("      field { sw=rw; hw=r; singlepulse; dontcompare;");
		outList.add("        name = \"start engine\";");
		outList.add("      } start[1:1] = 1'b0;");
		outList.add("    } cntl;");
		// cfg register
		outList.add("    reg {");
		outList.add("      name = \"Config register\";");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("        name = \"engine transaction mode\";");
		outList.add("        desc = \"modes: 0-write only, 1-read only, 2-read modify write\";");
		outList.add("      } mode[1:0] = 2'b0;");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("        name = \"stop on read mode\";");
		outList.add("        desc = \"modes: 0-disabled, 1-lt mask/match, 2-equals match/mask, 3-gt mask/match\";");
		outList.add("      } stop_on_read[5:4] = 2'b0;");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("        name = \"stop on transaction count\";");
		outList.add("      } stop_on_count[6:6] = 1'b0;");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("        name = \"read capture mode\";");
		outList.add("        desc = \"modes: 0-disabled, 1-capture min value, 2-capture max value, 3-capture all\";");
		outList.add("      } read_capture_mode[9:8] = 2'b0;");
		if (useTransactionSize) {
			outList.add("      field { sw=rw; hw=r;");
			outList.add("        name = \"Write transaction size\";");
			outList.add("        desc = \"Size of write transaction in words (0=32bit, 1=64bit, etc)\";");
			outList.add("      } write_trans_size[" + (regWordBits + 11) + ":12] = " + regWordBits + "'b0;");
		}
		outList.add("      field { sw=rw; hw=r;");
		outList.add("        name = \"delay between transactions\";");
		outList.add("      } trans_delay[" + (delayCountBits + 15) + ":16] = " + delayCountBits + "'b0;");
		outList.add("    } cfg @0x4;");
		// status register
		outList.add("    reg {");
		outList.add("      name = \"Status register\";");
		outList.add("      field { sw=r; hw=w;");
		outList.add("        name = \"nack error\";");
		outList.add("      } nack_error[0:0] = 1'b0;");
		outList.add("      field { sw=r; hw=na; intr;");
		outList.add("        name = \"bad address error\";");
		outList.add("      } bad_address_error[1:1] = 1'b0;");
		outList.add("      field { sw=r; hw=na; intr;");
		outList.add("        name = \"engine state\";");
		outList.add("      } state[" + (stateBits + 3) + ":4] = " + stateBits + "'b0;");
		outList.add("    } status @0x8;");
		// address control registers
		outList.add("    reg {");
		outList.add("      name = \"Address start register\";");
		if (addrWidth + addrLowBit > 32) outList.add("      regwidth = 64;");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("      } val[" + (addrLowBit + addrWidth - 1) + ":" + addrLowBit + "] = " + addrWidth + "'b0;");
		outList.add("    } address_start @0x20;");
		outList.add("    reg {");
		outList.add("      name = \"Address step register\";");
		if (addrWidth + addrLowBit > 32) outList.add("      regwidth = 64;");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("      } val[" + (addrLowBit + addrWidth - 1) + ":" + addrLowBit + "] = " + addrWidth + "'b0;");
		outList.add("    } address_step @0x28;");
		// transaction count register
		outList.add("    reg {");
		outList.add("      name = \"Max transaction count register\";");
		if (transCountWidth + addrLowBit > 32) outList.add("      regwidth = 64;");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("      } val[" + (addrLowBit + transCountWidth - 1) + ":" + addrLowBit + "] = " + transCountWidth + "'b0;");
		outList.add("    } max_trans_count @0x30;");
		// write inputs
		outList.add("    reg {");
		outList.add("      name = \"Write data register\";");
		outList.add("      regwidth = " + regWidth + ";");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("      } val[" + (regWidth - 1) + ":0] = " + regWidth + "'b0;");
		outList.add("    } write_data @0x80;");
		outList.add("    reg {");
		outList.add("      name = \"Write mask register\";");
		outList.add("      desc = \"Mask of write data when in read-modify-write mode:  0-use write data, 1-use current read capture data\";");
		outList.add("      regwidth = " + regWidth + ";");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("      } val[" + (regWidth - 1) + ":0] = " + regWidth + "'b0;");
		outList.add("    } write_mask;");
		// read inputs
		outList.add("    reg {");
		outList.add("      name = \"Stop on read match register\";");
		outList.add("      regwidth = " + regWidth + ";");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("      } val[" + (regWidth - 1) + ":0] = " + regWidth + "'b0;");
		outList.add("    } read_match_data;");
		outList.add("    reg {");
		outList.add("      name = \"Stop on read mask register\";");
		outList.add("      desc = \"Mask of read data used when stop on read is active:  0-match read data, 1-match zero\";");
		outList.add("      regwidth = " + regWidth + ";");
		outList.add("      field { sw=rw; hw=r;");
		outList.add("      } val[" + (regWidth - 1) + ":0] = " + regWidth + "'b0;");
		outList.add("    } read_mask;");
		// last read outputs
		outList.add("    reg {");
		outList.add("      name = \"Read data capture register\";");
		outList.add("      regwidth = " + regWidth + ";");
		outList.add("      field { sw=r; hw=w;");
		outList.add("      } val[" + (regWidth - 1) + ":0] = " + regWidth + "'b0;");
		outList.add("    } last_read_data;");
		// close out e1 regfile/interface
		outList.add("  } e1 @0x400;");
		outList.add("};");
		outList.add("");
		
		for (String stmt: outList) System.out.println(stmt);
		//builder.simple_write(fullName + ".rdl", "assist engine rdl", "//", outList);
	}

	/** add interface signals for address maps talking to a leaf */
	private void genLeafPioInterface(AddressableInstanceProperties topRegProperties, boolean isPrimary) {
		//System.out.println("SystemVerilogDecodeModule: generating decoder with leaf interface");
		// set internal interface names
		String pioInterfaceAddressName = getSigName(isPrimary, this.pioInterfaceAddressName);
		String pioInterfaceWriteDataName = getSigName(isPrimary, this.pioInterfaceWriteDataName);
		String pioInterfaceWriteEnableName = getSigName(isPrimary, this.pioInterfaceWriteEnableName);
		String pioInterfaceTransactionSizeName = getSigName(isPrimary, this.pioInterfaceTransactionSizeName);
		String pioInterfaceRetTransactionSizeName = getSigName(isPrimary, this.pioInterfaceRetTransactionSizeName);
		String pioInterfaceWeName = getSigName(isPrimary, this.pioInterfaceWeName);
		String pioInterfaceReName = getSigName(isPrimary, this.pioInterfaceReName);
		String pioInterfaceReadDataName = getSigName(isPrimary, this.pioInterfaceReadDataName);
		String pioInterfaceAckName = getSigName(isPrimary, this.pioInterfaceAckName);
		String pioInterfaceNackName = getSigName(isPrimary, this.pioInterfaceNackName);
		String pioInterfaceAckNextName = getSigName(isPrimary, this.pioInterfaceAckNextName);
		String pioInterfaceNackNextName = getSigName(isPrimary, this.pioInterfaceNackNextName);
		String arbiterAtomicName = getSigName(isPrimary, this.arbiterAtomicName);
		// set IO names
		String ioWrDataName = getSigName(isPrimary, "leaf_dec_wr_data");
		String ioRdData = getSigName(isPrimary, "dec_leaf_rd_data");
		String ioAck = getSigName(isPrimary, "dec_leaf_ack");  // ack indication
		String ioNack = getSigName(isPrimary, "dec_leaf_nack");  // nack indication
		String ioAddr = getSigName(isPrimary, "leaf_dec_addr");  // full external address
		String ioBlockSel = getSigName(isPrimary, "leaf_dec_block_sel");  // input block select		
		String ioAddrStart = getSigName(isPrimary, "addr_start");  // output address mask
		String ioAddrEnd = getSigName(isPrimary, "addr_end");  // output address match
		String ioAccept = getSigName(isPrimary, "dec_leaf_accept");  // block accept indication
		String ioReject = getSigName(isPrimary, "dec_leaf_reject");  // block reject indication
		String ioValid = getSigName(isPrimary, "leaf_dec_valid");  // transaction valid indicator
		String ioWrValid = getSigName(isPrimary, "leaf_dec_wr_dvld");  // write transaction valid indicator
		String ioCycle = getSigName(isPrimary, "leaf_dec_cycle");  // transaction type indicator
		String ioRetryAtomic = getSigName(isPrimary, "dec_leaf_retry_atomic");
		String ioWrWidth = getSigName(isPrimary, "leaf_dec_wr_width");   // write data width
		String ioRdWidth = getSigName(isPrimary, "dec_leaf_data_width");  // read data width returned	
		// set internal names
		String sigBlockSelAddr = getSigName(isPrimary, "block_sel_addr");
		String sigBlockSel = getSigName(isPrimary, "block_sel");
		
		// define the internal interface signals
		this.addVectorWire(pioInterfaceWriteDataName, 0, builder.getMaxRegWidth());  //  wr data to be used internally 
		if (mapHasMultipleAddresses()) this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), builder.getMapAddressWidth());  //  address to be used internally 
		this.addScalarWire(pioInterfaceReName);  //  read enable to be used internally 
		this.addScalarWire(pioInterfaceWeName);  //  write enable be used internally 
		
		// tie off enables
		if (hasWriteEnables()) {
			Ordt.warnMessage("Leaf decoder interface will not generate write data enables.");
			this.addVectorWire(pioInterfaceWriteEnableName, 0, getWriteEnableWidth()); 
			this.addWireAssign(pioInterfaceWriteEnableName + " = " + SystemVerilogBuilder.getHexOnesString(getWriteEnableWidth()) + ";");
		}
		
		// disable atomic request to arbiter
		if (hasSecondaryInterface()) {
			this.addScalarWire(arbiterAtomicName);   
			this.addWireAssign(arbiterAtomicName + " = 1'b0;");
		}

		// data vectors and ack/nack are passed through
		this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ioWrDataName, 0, builder.getMaxRegWidth());  // write data
		this.addWireAssign(pioInterfaceWriteDataName + " = " + ioWrDataName + ";");
		
		this.addSimpleVectorTo(SystemVerilogBuilder.PIO, ioRdData, 0, builder.getMaxRegWidth());  // read data
		this.addWireAssign(ioRdData + " = " + pioInterfaceReadDataName + ";");
		
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, ioAck);  // ack indication
		this.addWireAssign(ioAck + " = " + pioInterfaceAckName + ";");

		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, ioNack);  // nack indication
		this.addWireAssign(ioNack + " = " + pioInterfaceNackName + ";");
		
		// generate the address and block decode selects
		this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ioAddr, 0, ExtParameters.getLeafAddressSize() );  // full external address
		if (mapHasMultipleAddresses()) this.addWireAssign(pioInterfaceAddressName + " = " + ioAddr + builder.genRefAddressArrayString() + ";");
		
		// if always selecting this block no need for sel signal
		boolean alwaysSelected = (ExtParameters.getSystemverilogBlockSelectMode() == SVBlockSelectModes.ALWAYS);
		if (!alwaysSelected || ExtParameters.systemverilogExportStartEnd())
			this.addVectorWire(sigBlockSelAddr, 0, ExtParameters.getLeafAddressSize());  //  define base address signal 
		// set base address for his decoder - if a secondary interface, use specified alternate base address if specified
		RegNumber baseAddr = (!isPrimary && ExtParameters.hasSecondaryBaseAddress())? new RegNumber(ExtParameters.getSecondaryBaseAddress()) :
			                                                                          new RegNumber(ExtParameters.getPrimaryBaseAddress());  
		baseAddr.setVectorLen(ExtParameters.getLeafAddressSize());
		if (isPrimary && (topRegProperties != null)) baseAddr = topRegProperties.getFullBaseAddress();  // override with local address if a child
		// if base address is a parameter use it
		if (isPrimary && builder.addBaseAddressParameter() && !alwaysSelected) 
		   this.addWireAssign(sigBlockSelAddr + " = BASE_ADDR;");  // parameter define
		else if (!alwaysSelected || ExtParameters.systemverilogExportStartEnd()) 
		   this.addWireAssign(sigBlockSelAddr + " = " + baseAddr.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");  // constant define
		
		// check for ignored lower bits in base address 
		int lowAddrBit = builder.getAddressLowBit();   
		int addrSize = builder.getMapAddressWidth();    
		RegNumber lowBaseBits = baseAddr.getSubVector(lowAddrBit, addrSize);
		if ((lowBaseBits != null) && (lowBaseBits.isNonZero()))  
			Ordt.warnMessage("Non zero base address value below bit " + (lowAddrBit + addrSize) + " will be ignored");
				
		// create external block select if specified
		if (ExtParameters.getSystemverilogBlockSelectMode() == SVBlockSelectModes.EXTERNAL) {
			builder.getAddressRanges().setListEnd(builder.getCurrentMapSize());  // update list now that map size is known 
			//builder.getAddressRanges().list();
			int numSelects = builder.getAddressRanges().size();
			if (numSelects == 1) {
				this.addScalarWire(sigBlockSel);  //  block selected indicator
				this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, ioBlockSel);  // input block select 			
				this.addWireAssign(sigBlockSel + " = " + ioBlockSel + ";");							
			}
			else {
				this.addScalarReg(sigBlockSel);  //  block selected indicator
				for (Integer idx = 0; idx < numSelects; idx++) {
					String idxStr = (idx==0) ? "" : idx.toString();	
					this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, ioBlockSel + idxStr);  // input block select 		
					if (idx==0) this.addCombinAssign("block selects",  sigBlockSel + " = " + ioBlockSel + ";");  
					else this.addCombinAssign("block selects",  sigBlockSel + " = " + sigBlockSel + " | " + ioBlockSel + idxStr + ";");
				}
			}
		}
		// otherwise compute select internally
		else if (ExtParameters.getSystemverilogBlockSelectMode() == SVBlockSelectModes.INTERNAL) {
			this.addScalarWire(sigBlockSel);  //  block selected indicator
			this.addWireAssign(sigBlockSel + " = " + "(" + sigBlockSelAddr + builder.getBlockSelectBits() + " == " + ioAddr + " " +
					builder.getBlockSelectBits() + ");");			
		}
		// otherwise select is always active
		else {
			this.addScalarWire(sigBlockSel);  //  block selected indicator
			this.addWireAssign(sigBlockSel + " = 1'b1;");			
		}
		
		// export start/end signals if specified
		if (ExtParameters.systemverilogExportStartEnd()) {
			builder.getAddressRanges().setListEnd(builder.getCurrentMapSize());  // update list now that map size is known 
			//builder.getAddressRanges().list();
			int numSelects = builder.getAddressRanges().size();
			for (Integer idx = 0; idx < numSelects; idx++) {
				String idxStr = (idx==0) ? "" : idx.toString();	
				this.addSimpleVectorTo(SystemVerilogBuilder.PIO, ioAddrStart + idxStr, 0, ExtParameters.getLeafAddressSize());  // output address mask
				this.addSimpleVectorTo(SystemVerilogBuilder.PIO, ioAddrEnd + idxStr, 0, ExtParameters.getLeafAddressSize());  // output address match
				this.addVectorWire(ioAddrStart + idxStr, 0, ExtParameters.getLeafAddressSize());   
				this.addVectorWire(ioAddrEnd + idxStr, 0, ExtParameters.getLeafAddressSize()); 
				// set start and end outputs from range list
				String startOffsetStr = (idx==0) ? "" : " + " + builder.getAddressRanges().getStart(idx).toFormat(NumBase.Hex, NumFormat.Verilog);
				String endOffsetStr = " + " + builder.getAddressRanges().getEnd(idx).toFormat(NumBase.Hex, NumFormat.Verilog);
				this.addWireAssign(ioAddrStart + idxStr + " = " + sigBlockSelAddr + startOffsetStr + ";");  
				this.addWireAssign(ioAddrEnd + idxStr + " = " + sigBlockSelAddr + endOffsetStr + ";");
			}
		}

		//this.addResetAssign(getGroupPrefix(isPrimary) + "leaf i/f", builder.getDefaultReset(), "block_select_d1 <= #1  1'b0;");  // reset for delayed block select 
		//this.addRegAssign(getGroupPrefix(isPrimary) + "leaf i/f",  "block_select_d1 <= #1 block_sel;");  
		
		// generate valid and wr_dvld active signals 
		this.addScalarReg(ioValid + "_hld1");  //  delayed valid active
		this.addResetAssign(getGroupPrefix(isPrimary) + "leaf i/f", builder.getDefaultReset(), ioValid + "_hld1 <= #1  1'b0;");  
		this.addRegAssign(getGroupPrefix(isPrimary) + "leaf i/f",  ioValid + "_hld1 <= #1 " + ioValid + "_hld1_next;");
		
		this.addScalarReg(ioValid + "_hld1_next");  //  valid activated at valid input, deactivated at ack/nack
		this.addCombinAssign(getGroupPrefix(isPrimary) + "leaf i/f",  ioValid + "_hld1_next = " + ioValid + " | " + ioValid + "_hld1;");  
		this.addCombinAssign(getGroupPrefix(isPrimary) + "leaf i/f",  "if (" + pioInterfaceAckNextName + " | " + pioInterfaceNackNextName + ") " + ioValid + "_hld1_next = 1'b0;");  
		this.addScalarWire(ioValid + "_active");  //  active if valid or valid_dly

		this.addScalarReg(ioWrValid + "_hld1");  //  delayed wr_dvld active
		this.addResetAssign(getGroupPrefix(isPrimary) + "leaf i/f", builder.getDefaultReset(), ioWrValid + "_hld1 <= #1  1'b0;");  
		this.addRegAssign(getGroupPrefix(isPrimary) + "leaf i/f",  ioWrValid + "_hld1 <= #1 " + ioWrValid + "_hld1_next;");
		
		this.addScalarReg(ioWrValid + "_hld1_next");  //  wr_dvld activated at wr_dvld input, deactivated at ack/nack/valid
		this.addCombinAssign(getGroupPrefix(isPrimary) + "leaf i/f",  ioWrValid + "_hld1_next = " + ioWrValid + " | " + ioWrValid + "_hld1;");  
		this.addCombinAssign(getGroupPrefix(isPrimary) + "leaf i/f",  "if (" + pioInterfaceAckNextName + " | " + pioInterfaceNackNextName + " | " + ioValid + ") " + ioWrValid + "_hld1_next = 1'b0;");  
		this.addScalarWire(ioWrValid + "_active");  //  active if wr_dvld or wr_dvld_dly

		this.addWireAssign(ioWrValid + "_active = " + ioWrValid + " | " + ioWrValid + "_hld1;");	
		this.addWireAssign(ioValid + "_active = " + ioValid + " | " + ioValid + "_hld1;");			
				
		// generate accept/reject signals
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, ioAccept);  // block accept indication
		this.addWireAssign(ioAccept + " = " + ioValid + " & " + sigBlockSel + ";");

		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, ioReject);  // block reject indication
		this.addWireAssign(ioReject + " = " + ioValid + " & ~" + sigBlockSel + ";");

		// add transaction valid inputs
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, ioValid);  // transaction valid indicator
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, ioWrValid);  // write transaction valid indicator
		this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ioCycle, 0, 2);  // transaction type indicator

		// generate re/we assigns - use delayed versions if this is a single primary
		assignReadWriteRequests(sigBlockSel + " & " + ioValid + "_active & (" + ioCycle + " == 2'b10)",
				                sigBlockSel + " & " + ioWrValid + "_active & (" + ioCycle + "[1] == 1'b0)", 
				                pioInterfaceReName, pioInterfaceWeName, !hasSecondaryInterface());

		// generate atomic retry output if a write smaller than reg_width being accessed  
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, ioRetryAtomic);
		
		// always generate min 3bit width IOs
		int externalDataBitSize = (builder.getMaxWordBitSize() <= 3) ? 3 : builder.getMaxWordBitSize(); 
		this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ioWrWidth, 0, externalDataBitSize);   // write data width
		this.addSimpleVectorTo(SystemVerilogBuilder.PIO, ioRdWidth, 0, externalDataBitSize);  // read data width returned

		// if max transaction is larger than min, add a transaction size/retry signals 
		if (builder.getMaxRegWordWidth() > 1) {
			// add trans size from leaf
			this.addVectorWire(pioInterfaceTransactionSizeName, 0, builder.getMaxWordBitSize());  //  internal transaction size
			this.addWireAssign(pioInterfaceTransactionSizeName + " = " + ioWrWidth + SystemVerilogSignal.genRefArrayString(0, builder.getMaxWordBitSize()) + ";"); 
			
			// generate data width output back to leaf  
			int unusedDataBits = externalDataBitSize - builder.getMaxWordBitSize();
			if (unusedDataBits > 0) this.addWireAssign(ioRdWidth + SystemVerilogSignal.genRefArrayString(builder.getMaxWordBitSize(), unusedDataBits) + " = 0;"); 
			this.addWireAssign(ioRdWidth + SystemVerilogSignal.genRefArrayString(0, builder.getMaxWordBitSize()) + " = " + pioInterfaceRetTransactionSizeName + ";"); 
			
			// if a write larger than interface then retry (only valid during ack)
			this.addScalarReg(ioRetryAtomic);  //  register the output
			this.addScalarWire(ioRetryAtomic + "_next");  
			this.addResetAssign(getGroupPrefix(isPrimary) + "leaf i/f", builder.getDefaultReset(), ioRetryAtomic + " <= #1  1'b0;");  // reset for retry atomic 
			this.addRegAssign(getGroupPrefix(isPrimary) + "leaf i/f", ioRetryAtomic + " <= #1 " + ioRetryAtomic + "_next;");  
			this.addWireAssign(ioRetryAtomic + "_next = " + sigBlockSel + " & " + ioWrValid + "_active & (" + ioCycle + " == 2'b00)" + 
			                       " & (" + ioWrWidth + SystemVerilogSignal.genRefArrayString(0, builder.getMaxWordBitSize()) + " < reg_width);");  // Note: reg_width bypasses internal arbiter
		}
		else {
			// min sized reg space so just set size to zero and inhibit retry
			this.addWireAssign(ioRetryAtomic + " = 1'b0;");  
			this.addWireAssign(ioRdWidth + " = " + externalDataBitSize + "'b0;"); 
		}
	}

	/** add serial8 pio interface */
	private void genSerial8PioInterface(AddressableInstanceProperties topRegProperties, boolean isPrimary) {  
		//System.out.println("SystemVerilogDecodeModule: generating decoder with external interface, id=" + topRegProperties.getInstancePath());
		// set internal interface names
		String pioInterfaceAddressName = getSigName(isPrimary, this.pioInterfaceAddressName);
		String pioInterfaceWriteDataName = getSigName(isPrimary, this.pioInterfaceWriteDataName);
		String pioInterfaceWriteEnableName = getSigName(isPrimary, this.pioInterfaceWriteEnableName);
		String pioInterfaceTransactionSizeName = getSigName(isPrimary, this.pioInterfaceTransactionSizeName);
		String pioInterfaceRetTransactionSizeName = getSigName(isPrimary, this.pioInterfaceRetTransactionSizeName);
		String pioInterfaceWeName = getSigName(isPrimary, this.pioInterfaceWeName);
		String pioInterfaceReName = getSigName(isPrimary, this.pioInterfaceReName);
		String pioInterfaceReadDataName = getSigName(isPrimary, this.pioInterfaceReadDataName);
		String pioInterfaceAckName = getSigName(isPrimary, this.pioInterfaceAckName);
		String pioInterfaceNackName = getSigName(isPrimary, this.pioInterfaceNackName);
		String arbiterAtomicName = getSigName(isPrimary, this.arbiterAtomicName);
		// set IO names
		String prefix = (topRegProperties == null)? "s8" : "d2s_" + topRegProperties.getBaseName();
		String serial8CmdValidName = getSigName(isPrimary, prefix + "_cmd_valid");                      
		String serial8CmdDataName = getSigName(isPrimary, prefix + "_cmd_data"); 
		prefix = (topRegProperties == null)? "s8" : "s2d_" + topRegProperties.getBaseName();
		String serial8ResValidName = getSigName(isPrimary, prefix + "_res_valid");                      
		String serial8ResDataName = getSigName(isPrimary, prefix + "_res_data");                      
		// set internal names
		String s8StateName = getSigName(isPrimary, "s8_state");                      
		String s8StateNextName = getSigName(isPrimary, "s8_state_next");                      
		String s8AddrCntName = getSigName(isPrimary, "s8_addr_cnt");                      
		String s8AddrCntNextName = getSigName(isPrimary, "s8_addr_cnt_next");                      
		String s8DataCntName = getSigName(isPrimary, "s8_data_cnt");                      
		String s8DataCntNextName = getSigName(isPrimary, "s8_data_cnt_next"); 
		String s8AddrAccumName = getSigName(isPrimary, "s8_addr_accum");                      
		String s8AddrAccumNextName = getSigName(isPrimary, "s8_addr_accum_next");                      
		String s8WrAccumName = getSigName(isPrimary, "s8_wdata_accum");                      
		String s8WrAccumNextName = getSigName(isPrimary, "s8_wdata_accum_next");                      
		String s8WrStateCaptureName = getSigName(isPrimary, "s8_wr_state_capture");                      
		String s8WrStateCaptureNextName = getSigName(isPrimary, "s8_wr_state_capture_next");                      
		String s8RdCaptureName = getSigName(isPrimary, "s8_rdata_capture");                      
		String s8RdCaptureNextName = getSigName(isPrimary, "s8_rdata_capture_next");     
		
		// check for valid serial8 width
		int transactionsInWord = ExtParameters.getMinDataSize()/8;
		boolean multiTransactionWord = (transactionsInWord>1);
		if (!multiTransactionWord) Ordt.errorExit("Serial8 interface type does not support 8b max width regions.  Use parallel interface instead.");
		
		// tie off enables
		if (hasWriteEnables()) {
			Ordt.warnMessage("Serial8 decoder interface will not generate write data enables.");
			this.addVectorWire(pioInterfaceWriteEnableName, 0, getWriteEnableWidth()); 
			this.addWireAssign(pioInterfaceWriteEnableName + " = " + SystemVerilogBuilder.getHexOnesString(getWriteEnableWidth()) + ";");
		}
		
		// create module IOs
		//  inputs
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, serial8CmdValidName);     // stays high while all cmd addr/data/cntl xferred 
		this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, serial8CmdDataName, 0, 8);  

		// outputs  
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, serial8ResValidName);     // stays high while all res cntl/data xferred
		this.addSimpleVectorTo(SystemVerilogBuilder.PIO, serial8ResDataName, 0, 8);     
		
		// calculate max number of 8b xfers required for address 
		int addressWidth = builder.getMapAddressWidth();
		int addrXferCount = 0;
		if (addressWidth > 0) {
			addrXferCount = (int) Math.ceil(addressWidth/8.0);
			//System.out.println("SystemVerilogBuilder genSerial8PioInterface: addr width=" + addressWidth + ", addr count=" + addrXferCount);
		}
		
		// compute max transaction size in words and number of bits to represent (4b max)
		int regWidth = builder.getMaxRegWidth();
		int regWords = builder.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = (regWords > 1);  // if transaction sizes need to be sent/received
		
		// now create state machine vars
		String groupName = getGroupPrefix(isPrimary) + "serial8 i/f";  
		int stateBits = 3;
		this.addVectorReg(s8StateName, 0, stateBits);  
		this.addVectorReg(s8StateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), s8StateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  s8StateName + " <= #1  " + s8StateNextName + ";");  

		// s8 cmd inputs will feed into sm
		//this.addScalarWire(serial8CmdValidName);  
		//this.addVectorWire(serial8CmdDataName, 0, 8); 
        // s8 res outputs will be set in sm 
		this.addScalarReg(serial8ResValidName);  
		this.addVectorReg(serial8ResDataName, 0, 8); 

		// add address accumulate reg 
		if (addressWidth > 0) {
			this.addVectorReg(s8AddrAccumName, builder.getAddressLowBit(), addressWidth);  
			this.addVectorReg(s8AddrAccumNextName, builder.getAddressLowBit(), addressWidth);  
			this.addRegAssign(groupName,  s8AddrAccumName + " <= #1  " + s8AddrAccumNextName + ";");  
			this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), addressWidth);  
			this.addWireAssign(pioInterfaceAddressName + " = " + s8AddrAccumName + ";");  // input addr is set from accum reg			
		}

		// add write data accumulate reg
		this.addVectorReg(s8WrAccumName, 0, regWidth);  
		this.addVectorReg(s8WrAccumNextName, 0, regWidth);  
		this.addRegAssign(groupName,  s8WrAccumName + " <= #1  " + s8WrAccumNextName + ";");  
		this.addVectorWire(pioInterfaceWriteDataName, 0, regWidth);  
		this.addWireAssign(pioInterfaceWriteDataName + " = " + s8WrAccumName + ";");  // input data is set from accum reg
		
		// will need to capture cmd size
		String pioInterfaceTransactionSizeNextName = pioInterfaceTransactionSizeName + "_next";  // cmd size next will be set in sm
		if (useTransactionSize) {
			this.addVectorReg(pioInterfaceTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(pioInterfaceTransactionSizeNextName, 0, regWordBits);  // res size will be set in sm
			this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceTransactionSizeName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  pioInterfaceTransactionSizeName + " <= #1  " + pioInterfaceTransactionSizeNextName + ";");  
		}

		// add capture reg for write transaction indicator
		this.addScalarReg(s8WrStateCaptureName);  
		this.addScalarReg(s8WrStateCaptureNextName);  
		this.addRegAssign(groupName,  s8WrStateCaptureName + " <= #1  " + s8WrStateCaptureNextName + ";");  

		// add capture reg for read data
		this.addVectorReg(s8RdCaptureName, 0, regWidth);  
		this.addVectorReg(s8RdCaptureNextName, 0, regWidth);  
		this.addRegAssign(groupName,  s8RdCaptureName + " <= #1  " + s8RdCaptureNextName + ";"); 

		// address byte count
		int addrXferCountBits = Utils.getBits(addrXferCount);
		if (addrXferCountBits > 0) {
			this.addVectorReg(s8AddrCntName, 0, addrXferCountBits);  
			this.addVectorReg(s8AddrCntNextName, 0, addrXferCountBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), s8AddrCntName + " <= #1  " + addrXferCountBits + "'b0;");  
			this.addRegAssign(groupName,  s8AddrCntName + " <= #1  " + s8AddrCntNextName + ";");  			
		}
		
		// data byte count
		int maxDataXferCount = regWords * ExtParameters.getMinDataSize()/8;
		int maxDataXferCountBits = Utils.getBits(maxDataXferCount);
		this.addVectorReg(s8DataCntName, 0, maxDataXferCountBits);  
		this.addVectorReg(s8DataCntNextName, 0, maxDataXferCountBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), s8DataCntName + " <= #1  " + maxDataXferCountBits + "'b0;");  
		this.addRegAssign(groupName,  s8DataCntName + " <= #1  " + s8DataCntNextName + ";"); 

		// define internal interface signals that will be set in sm 
		String s8pioInterfaceReName = "s8_" + pioInterfaceReName;
		String s8pioInterfaceWeName = "s8_" + pioInterfaceWeName;
		this.addScalarReg(s8pioInterfaceReName);
		this.addScalarReg(s8pioInterfaceWeName);
		
		// state machine init values
		this.addCombinAssign(groupName,  s8StateNextName + " = " + s8StateName + ";");  
		this.addCombinAssign(groupName,  serial8ResValidName + " =  1'b0;");  // return valid
		this.addCombinAssign(groupName,  serial8ResDataName + " =  8'b0;");   // return data
		this.addCombinAssign(groupName,  s8pioInterfaceWeName + " =  1'b0;");  // write active
		this.addCombinAssign(groupName,  s8pioInterfaceReName + " =  1'b0;");  // read active
		this.addCombinAssign(groupName,  s8WrStateCaptureNextName + " = " + s8WrStateCaptureName + ";");  // write indicator
		if (addressWidth > 0)
			this.addCombinAssign(groupName,  s8AddrAccumNextName + " = " + s8AddrAccumName + ";");  // address accumulate
		this.addCombinAssign(groupName,  s8WrAccumNextName + " = " + s8WrAccumName + ";");  // write data accumulate
		this.addCombinAssign(groupName,  s8RdCaptureNextName + " = " + s8RdCaptureName + ";");  // read data capture
		// init cmd size capture
		if (useTransactionSize)
			this.addCombinAssign(groupName,  pioInterfaceTransactionSizeNextName + " = " + pioInterfaceTransactionSizeName + ";"); 
		// init counter values
		if (addrXferCountBits > 0) {
			this.addCombinAssign(groupName,  s8AddrCntNextName + " = "  + addrXferCountBits + "'b0;");
		}
		this.addCombinAssign(groupName,  s8DataCntNextName + " = "  + maxDataXferCountBits + "'b0;");
			
		// state machine  /
		String IDLE = stateBits + "'h0"; 
		String CMD_ADDR = stateBits + "'h1"; 
		String CMD_DATA = stateBits + "'h2"; 
		String RES_WAIT = stateBits + "'h3";
		String RES_READ = stateBits + "'h4";
		String BAD_CMD = stateBits + "'h5";
				
		this.addCombinAssign(groupName, "case (" + s8StateName + ")"); 

		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");
		// init accumulator/capture regs
		this.addCombinAssign(groupName, "      " + s8WrStateCaptureNextName + " = 1'b0;");  
		if (addressWidth > 0)
			this.addCombinAssign(groupName, "      " + s8AddrAccumNextName + " = " + addressWidth + "'b0;");
		this.addCombinAssign(groupName, "      " + s8WrAccumNextName + " = " + regWidth + "'b0;");
		// go on cmd valid - capture r/w indicator, addr size, and transaction size
		this.addCombinAssign(groupName, "      if (" + serial8CmdValidName + ") begin");  
		this.addCombinAssign(groupName, "        " + s8WrStateCaptureNextName + " = " + serial8CmdDataName + "[7];");  // bit 7 = write
		if (useTransactionSize)
		    this.addCombinAssign(groupName, "        " + pioInterfaceTransactionSizeNextName + " = " + serial8CmdDataName + SystemVerilogSignal.genRefArrayString(0, regWordBits) + ";");  // bits 3:0 = transaction size
        // abort cmd if address count is wrong
		this.addCombinAssign(groupName, "        if (" + serial8CmdDataName + "[6:4] != 3'd" + addrXferCount + ") " + s8StateNextName + " = " + BAD_CMD +  ";");  // bits 6:4 = check for valid address xfer count  
        // if an address then get it next
		if (addrXferCount > 0) {  
			this.addCombinAssign(groupName, "        else " + s8StateNextName + " = " + CMD_ADDR + ";");  
		}
		// otherwise get cmd data if a write or assert read and wait for internal read response
		else {
			this.addCombinAssign(groupName, "        else if (" + s8WrStateCaptureNextName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
			this.addCombinAssign(groupName, "        else " + s8StateNextName + " = " + RES_WAIT + ";");  
		}
		this.addCombinAssign(groupName, "      end");  //cmd valid
		this.addCombinAssign(groupName, "    end"); 
		
        // if an address needed then add CMD_ADDR state
		if (addrXferCount > 0) {   
			// CMD_ADDR
			this.addCombinAssign(groupName, "  " + CMD_ADDR + ": begin // CMD_ADDR");
			// if more than one address then bump the count
			if (addrXferCountBits > 0) {  
				this.addCombinAssign(groupName,  "      " + s8AddrCntNextName + " = " + s8AddrCntName + ";");
				this.addCombinAssign(groupName, "      if (" + serial8CmdValidName + ") begin");  
				this.addCombinAssign(groupName,  "        " + s8AddrCntNextName + " = " + s8AddrCntName + " + " + addrXferCountBits + "'b1;");
				// accumulate the address
				for (int idx=0; idx<addrXferCount; idx++) {
					prefix = (idx == 0)? "" : "else ";
					this.addCombinAssign(groupName, "        " + prefix + "if (" + s8AddrCntName + " == " + addrXferCountBits + "'d" + idx + ")");
					int lowbit = idx*8 + builder.getAddressLowBit();
					int size = ((idx+1)*8 > addressWidth)? addressWidth - idx*8 : 8;  // last xfer can be smaller
					this.addCombinAssign(groupName, "          " + s8AddrAccumNextName + SystemVerilogSignal.genRefArrayString(lowbit, size) + " = " + serial8CmdDataName + SystemVerilogSignal.genRefArrayString(0, size) + ";");
				}
	
				// if addr done, send data if a write or wait for read response 
				this.addCombinAssign(groupName, "        if (" + s8AddrCntName + " == " + addrXferCountBits + "'d" + (addrXferCount - 1) + ") begin");
				this.addCombinAssign(groupName, "          if (" + s8WrStateCaptureName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "          else " + s8StateNextName + " = " + RES_WAIT + ";");
				this.addCombinAssign(groupName, "        end"); 
			}
			// else a single addr xfer
			else {
				this.addCombinAssign(groupName, "      if (" + serial8CmdValidName + ") begin");  
				// set address
				this.addCombinAssign(groupName, "        " + s8AddrAccumNextName  + " = " + serial8CmdDataName + SystemVerilogSignal.genRefArrayString(0, addressWidth) + ";");  
				// next send data if a write or wait for read response 
				this.addCombinAssign(groupName, "        if (" + s8WrStateCaptureName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "        else " + s8StateNextName + " = " + RES_WAIT + ";");  
			}
			this.addCombinAssign(groupName, "      end"); 
			this.addCombinAssign(groupName, "    end"); 
		}
		
		// CMD_DATA - capture write data
		this.addCombinAssign(groupName, "  " + CMD_DATA + ": begin // CMD_DATA");
		this.addCombinAssign(groupName,  "      " + s8DataCntNextName + " = " + s8DataCntName + ";");
		this.addCombinAssign(groupName, "      if (" + serial8CmdValidName + ") begin");  
		//  bump the data count
		this.addCombinAssign(groupName,  "        " + s8DataCntNextName + " = " + s8DataCntName + " + " + maxDataXferCountBits + "'b1;");
		// get the data slice
		for (int idx=0; idx<maxDataXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "        "+ prefix + "if (" + s8DataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
			this.addCombinAssign(groupName, "          "  + s8WrAccumNextName + SystemVerilogSignal.genRefArrayString(8*idx, 8) + " = " + serial8CmdDataName + ";");
		}
		// if done, move to res wait  
		String finalCntStr = getSerialMaxDataCountStr(useTransactionSize, transactionsInWord, pioInterfaceTransactionSizeName);  // get final data count compare string
		this.addCombinAssign(groupName, "        if (" + s8DataCntName + " == " + finalCntStr + ")");
		this.addCombinAssign(groupName, "          " + s8StateNextName + " = " + RES_WAIT + ";");
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
			
		// RES_WAIT
		this.addCombinAssign(groupName, "  " + RES_WAIT + ": begin  // RES_WAIT");
		// activate either read or write request
		this.addCombinAssign(groupName, "      " + s8pioInterfaceWeName + " = " + s8WrStateCaptureName + ";");  // write active
		this.addCombinAssign(groupName, "      " + s8pioInterfaceReName + " = ~" + s8WrStateCaptureName + ";");  // read active
		// go on ack/nack, capture read data and set first word of response (contains ack/nack and return xfer size) 
		this.addCombinAssign(groupName, "      " + "if (" + pioInterfaceAckName + " | " + pioInterfaceNackName + ") begin"); 
		this.addCombinAssign(groupName, "        " + serial8ResValidName + " =  1'b1;");  // res is valid
		this.addCombinAssign(groupName, "        " + s8RdCaptureNextName + " = " + pioInterfaceReadDataName + ";");  // capture read data  
		this.addCombinAssign(groupName, "        " + serial8ResDataName + "[7] = " + pioInterfaceNackName + ";");  // set nack bit  
		if (useTransactionSize) {
			this.addCombinAssign(groupName,  "        " + serial8ResDataName + SystemVerilogSignal.genRefArrayString(0, regWordBits) + " = " + pioInterfaceRetTransactionSizeName + ";");  
		}
		// if a read then send the data else we're done
		this.addCombinAssign(groupName, "        if (~" + s8WrStateCaptureName + ") " + s8StateNextName + " = " + RES_READ + ";");  
		this.addCombinAssign(groupName, "        else "+ s8StateNextName + " = " + IDLE + ";");
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end");
		
		// RES_READ - send read data
		this.addCombinAssign(groupName, "  " + RES_READ + ": begin  // RES_READ");  
		this.addCombinAssign(groupName, "      " + serial8ResValidName + " =  1'b1;");  // res is valid
		//  bump the data count
		this.addCombinAssign(groupName, "      " + s8DataCntNextName + " = " + s8DataCntName + " + " + maxDataXferCountBits + "'b1;");
		// send the data slice while 
		for (int idx=0; idx<maxDataXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "      "+ prefix + "if (" + s8DataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
			this.addCombinAssign(groupName, "        " + serial8ResDataName + " = " + s8RdCaptureName + SystemVerilogSignal.genRefArrayString(8*idx, 8) + ";");
		}
		// if final count we're done 
		finalCntStr = getSerialMaxDataCountStr(useTransactionSize, transactionsInWord, pioInterfaceRetTransactionSizeName);  // get final data count compare string
		this.addCombinAssign(groupName, "      if (" + s8DataCntName + " == " + finalCntStr + ") " + s8StateNextName + " = " + IDLE + ";");
		this.addCombinAssign(groupName, "    end"); 
		
		// BAD_CMD
		this.addCombinAssign(groupName, "  " + BAD_CMD + ": begin  // BAD_CMD");
		// wait for cmdValid to deassert then send a nack cntl word
		this.addCombinAssign(groupName, "      if (~" + serial8CmdValidName + ") begin");
		this.addCombinAssign(groupName, "        " + serial8ResValidName + " =  1'b1;");  // res is valid
		this.addCombinAssign(groupName, "        " + serial8ResDataName + "[7] = 1'b1;");  // set nack bit  
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + s8StateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 				

		this.addScalarWire(pioInterfaceReName);  //  read enable internal interface
		this.addScalarWire(pioInterfaceWeName);  //  write enable internal interface
				
		// disable atomic request to arbiter
		if (hasSecondaryInterface()) {
			this.addScalarWire(arbiterAtomicName);   
			this.addWireAssign(arbiterAtomicName + " = 1'b0;");
		}

		// generate re/we assigns - use delayed versions if this is a single primary
		assignReadWriteRequests(s8pioInterfaceReName, s8pioInterfaceWeName, pioInterfaceReName, pioInterfaceWeName, !hasSecondaryInterface());	
	}
	
	/** create serial8/ring interface max data count compare string using transactions per word and transaction size
	 *  
	 * @param useTransactionSize - more than a single word width in this mem region
	 * @param transactionsInWord - number of 8b transactions in each word
	 * @param transactionSizeName - name of size variable
	 */
	private String getSerialMaxDataCountStr(boolean useTransactionSize, int transactionsInWord, String transactionSizeName) {
		boolean multiTransactionWord = (transactionsInWord>1);
		String finalWordCntStr = multiTransactionWord? Utils.getBits(transactionsInWord) + "'b" + Integer.toBinaryString(transactionsInWord-1) : "";
		String finalCntStr = finalWordCntStr;  // single word xfer is default
		if (useTransactionSize) finalCntStr = multiTransactionWord? "{" + transactionSizeName + ", " + finalWordCntStr + "}" : transactionSizeName;
		return finalCntStr;
	}

	/** add ring pio interface */ 
	private void genRingPioInterface(int ringWidth, AddressableInstanceProperties topRegProperties, boolean isPrimary) {
		//System.out.println("SystemVerilogDecodeModule: generating decoder with external interface, id=" + topRegProperties.getInstancePath());
		// set internal interface names
		String pioInterfaceAddressName = getSigName(isPrimary, this.pioInterfaceAddressName);
		String pioInterfaceWriteDataName = getSigName(isPrimary, this.pioInterfaceWriteDataName);
		String pioInterfaceWriteEnableName = getSigName(isPrimary, this.pioInterfaceWriteEnableName);
		String pioInterfaceTransactionSizeName = getSigName(isPrimary, this.pioInterfaceTransactionSizeName);
		String pioInterfaceRetTransactionSizeName = getSigName(isPrimary, this.pioInterfaceRetTransactionSizeName);
		String pioInterfaceWeName = getSigName(isPrimary, this.pioInterfaceWeName);
		String pioInterfaceReName = getSigName(isPrimary, this.pioInterfaceReName);
		String pioInterfaceReadDataName = getSigName(isPrimary, this.pioInterfaceReadDataName);
		String pioInterfaceAckName = getSigName(isPrimary, this.pioInterfaceAckName);
		String pioInterfaceNackName = getSigName(isPrimary, this.pioInterfaceNackName);
		String arbiterAtomicName = getSigName(isPrimary, this.arbiterAtomicName);
		// set IO names
		String prefix = (topRegProperties == null)? "r" + ringWidth : "d2r_" + topRegProperties.getBaseName();  
		String ringCmdValidName = getSigName(isPrimary, prefix + "_cmd_valid");                      
		String ringCmdDataName = getSigName(isPrimary, prefix + "_cmd_data");                      	
		prefix = (topRegProperties == null)? "r" + ringWidth : "r2d_" + topRegProperties.getBaseName();
		String ringResValidName = getSigName(isPrimary, prefix + "_res_valid");                      
		String ringResDataName = getSigName(isPrimary, prefix + "_res_data");                      	
		// set internal names
		String sigBlockBaseAddr = getSigName(isPrimary, "block_base_address");
		String ringStateName = getSigName(isPrimary, "r" + ringWidth + "_state");                      
		String ringStateNextName = getSigName(isPrimary, "r" + ringWidth + "_state_next");                      
		String ringDataCntName = getSigName(isPrimary, "r" + ringWidth + "_data_cnt");                      
		String ringDataCntNextName = getSigName(isPrimary, "r" + ringWidth + "_data_cnt_next"); 
		String outFifoAdvanceName = getSigName(isPrimary, "r" + ringWidth + "_out_fifo_advance");  // set in sm
		String outFifoDataRootName = getSigName(isPrimary, "r" + ringWidth + "_out_fifo_data_dly");
		String cmdValidDlyRootName = getSigName(isPrimary, "r" + ringWidth + "_cmdValid_dly");
		String cmdDataDlyRootName = getSigName(isPrimary, "r" + ringWidth + "_cmdData_dly");
		String resValidDlyRootName = getSigName(isPrimary, "r" + ringWidth + "_resValid_dly");
		String resDataDlyRootName = getSigName(isPrimary, "r" + ringWidth + "_resData_dly");
		String ringAddrAccumName = getSigName(isPrimary, "r" + ringWidth + "_addr_accum");                      
		String ringAddrAccumNextName = getSigName(isPrimary, "r" + ringWidth + "_addr_accum_next");                      
		String ringWrAccumName = getSigName(isPrimary, "r" + ringWidth + "_wdata_accum");                      
		String ringWrAccumNextName = getSigName(isPrimary, "r" + ringWidth + "_wdata_accum_next");                      
		String ringWrStateCaptureName = getSigName(isPrimary, "r" + ringWidth + "_wr_state_capture");                      
		String ringWrStateCaptureNextName = getSigName(isPrimary, "r" + ringWidth + "_wr_state_capture_next");                      
		String ringRdCaptureName = getSigName(isPrimary, "r" + ringWidth + "_rdata_capture");                      
		String ringRdCaptureNextName = getSigName(isPrimary, "r" + ringWidth + "_rdata_capture_next");                      
		String ringAddrCntName = getSigName(isPrimary, "r" + ringWidth + "_addr_cnt");                      
		String ringAddrCntNextName = getSigName(isPrimary, "r" + ringWidth + "_addr_cnt_next");                      
		String ringAddrCntCaptureName = getSigName(isPrimary, "r" + ringWidth + "_addr_cnt_capture");                      
		String ringAddrCntCaptureNextName = getSigName(isPrimary, "r" + ringWidth + "_addr_cnt_capture_next");                      
		String ringNotMineName = getSigName(isPrimary, "r" + ringWidth + "_not_mine");                      
		String ringNotMineNextName = getSigName(isPrimary, "r" + ringWidth + "_not_mine_next");                      
		
		// check for valid ring width
		int transactionsInWord = ExtParameters.getMinDataSize()/ringWidth;
		if (ringWidth > ExtParameters.getMinDataSize()) Ordt.errorExit(ringWidth + "b ring interface type does not support min data size less than " + ringWidth + "b.");
		
		// tie off enables
		if (hasWriteEnables()) {
			Ordt.warnMessage("Ring decoder interface will not generate write data enables.");
			this.addVectorWire(pioInterfaceWriteEnableName, 0, getWriteEnableWidth()); 
			this.addWireAssign(pioInterfaceWriteEnableName + " = " + SystemVerilogBuilder.getHexOnesString(getWriteEnableWidth()) + ";");
		}
		
		// create module IOs		
		//  inputs
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, ringCmdValidName);     // stays high while all cmd addr/data/cntl xferred 
		this.addSimpleVectorFrom(SystemVerilogBuilder.PIO, ringCmdDataName, 0, ringWidth);  

		// outputs  
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, ringResValidName);     // stays high while all res cntl/data xferred
		this.addSimpleVectorTo(SystemVerilogBuilder.PIO, ringResDataName, 0, ringWidth);     
		
		// create the block base address
		this.addVectorWire(sigBlockBaseAddr, 0, ExtParameters.getLeafAddressSize());  //  base address of block 
		// set base address for his decoder - if a secondary interface, use specified alternate base address if specified
		RegNumber baseAddr = (!isPrimary && ExtParameters.hasSecondaryBaseAddress())? new RegNumber(ExtParameters.getSecondaryBaseAddress()) :
			                                                                          new RegNumber(ExtParameters.getPrimaryBaseAddress());  
		baseAddr.setVectorLen(ExtParameters.getLeafAddressSize());
		if (isPrimary && topRegProperties != null) baseAddr = topRegProperties.getFullBaseAddress();  // override with local address if a child
		// if base address is a parameter use it
		if (isPrimary && builder.addBaseAddressParameter()) 
		   this.addWireAssign(sigBlockBaseAddr + " = BASE_ADDR;");  // parameter define
		else 
		   this.addWireAssign(sigBlockBaseAddr + " = " + baseAddr.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");  // constant define
		
        // set field info according to ringWidth
		//                addr bits      offset         data size bits   r/w bit   ack/nack
		// ring  8 -    2 = max 3 = 24b  + 0 = 24b     3 = max 8 = 256b     5        7/6
		// ring 16 -    2 = max 3 = 48b  + 0 = 48b     4 = max 16 = 512b    7        15/14
		// ring 32 -    2 = max 3 = 96b  + 16 = 112b   4 = max 16 = 512b    7        15/14
		
		// defaults (16b ring)
		int addrBitIndex = 4;      // low index for address
		int rwBitIndex = 7;        // read/write index 
		int ackBitIndex = 14;      // index for ack
		int nackBitIndex = 15;      // index for nack 
		
		int maxAddrXferCount = 3;  // max 3 addr xfers allowed in all width cases
		int addrOffset = 0;  // default to no address bits in control word
		int maxRegWordBits = 4;    // max 4 32b words allowed
		
		// overrides if an alternate width
		if (ringWidth == 8) {
			// shift fields to fit in 8b
			addrBitIndex = 3;   // low index for address
			rwBitIndex = 5;        // read/write index 
			ackBitIndex = 6;      // index for ack
			nackBitIndex = 7;      // index for nack 
			maxRegWordBits = 3;    // limit to 3 32b words allowed
		}
		else if (ringWidth == 32) {
			addrOffset = 16;  // upper 16b are address bits in control word 
		}
		
		// further limit max number of address transfers by the full address range
		int maxAddrBits = ExtParameters.getLeafAddressSize() - builder.getAddressLowBit(); 
		int nonCntlAddrBits = (maxAddrBits > addrOffset)? maxAddrBits - addrOffset : 0;
		int fullAddressMaxXfers = (int) Math.ceil(nonCntlAddrBits/(double) ringWidth);
		maxAddrXferCount = fullAddressMaxXfers < maxAddrXferCount ? fullAddressMaxXfers : maxAddrXferCount;
		int addrXferCountBits = Utils.getBits(maxAddrXferCount+1);  // need + 1 since comparing w next count
		
		// error if insufficient bits to address this addrmap region 
		int addressWidth = builder.getMapAddressWidth();
		int addressableBits = ringWidth * maxAddrXferCount;
		if (addressableBits < addressWidth) 
			Ordt.errorExit("Insufficient address bits in " + ringWidth + "b ring decoder interface (" + addressableBits + ") for " + addressWidth +"b region " + topRegProperties.getInstancePath());
		//System.out.println("SystemVerilogDecodeModule genRingPioInterface: addressWidth=" + addressWidth + ", maxAddrXferCount=" + maxAddrXferCount + ", addrXferCountBits=" + addrXferCountBits);
				
		// compute max transaction size in 32b words and number of bits to represent (4 max) 
		int regWidth = builder.getMaxRegWidth();
		int regWords = builder.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = (regWords > 1);  // if transaction sizes need to be sent/received
		//System.out.println("SystemVerilogDecodeModule genRingPioInterface: max width=" + regWidth + ", top max=" + topRegProperties.getRegWidth() + ", words=" + regWords + ", useT=" + useTransactionSize);
		
		// error if insufficient data count bits for max reg width
		if (regWordBits > maxRegWordBits)
			Ordt.errorExit("Unable to access " + regWidth + "b registers within " + ringWidth + "b ring decoder for region " + topRegProperties.getInstancePath());

		// now create state machine vars		
		String groupName = getGroupPrefix(isPrimary) + "ring" + ringWidth + " i/f";  
		int stateBits = 4;
		this.addVectorReg(ringStateName, 0, stateBits);  
		this.addVectorReg(ringStateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), ringStateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  ringStateName + " <= #1  " + ringStateNextName + ";");  

		// ring cmd inputs will feed into sm (after cmd delay regs)
		//this.addScalarWire(ringCmdValidName);  
		//this.addVectorWire(ringCmdDataName, 0, ringWidth); 
        // ring res outputs will be set in sm (after res delay regs)
		this.addScalarWire(ringResValidName);  
		this.addVectorWire(ringResDataName, 0, ringWidth); 

		// create out fifo signals  
		int outFifoSize = maxAddrXferCount + 1;  // max depth is addr words plus 1 cntl word
		String [] outFifoDataName = new String [outFifoSize+1];
		for (int idx=0; idx<outFifoSize+1; idx++) {
			outFifoDataName[idx] = outFifoDataRootName + idx;
		}
		this.addScalarReg(outFifoAdvanceName); 
		this.addVectorReg(outFifoDataName[0], 0, ringWidth); // out data 0 is set in state machine
		for (int idx=1; idx<outFifoSize+1; idx++) {
			this.addVectorReg(outFifoDataName[idx], 0, ringWidth); 
			this.addResetAssign(groupName, builder.getDefaultReset(), outFifoDataName[idx] + " <= #1  " + ringWidth + "'b0;");  
			this.addRegAssign(groupName,  "if (" + outFifoAdvanceName +") " + outFifoDataName[idx] + " <= #1  " + outFifoDataName[idx-1] + ";");  
		}

		// create delayed cmd signals
		int cmdDelayCount = (int) Math.ceil(ExtParameters.sysVerRingInterNodeDelay()/2.0);  // half of delay
		String [] cmdValidDlyName = new String [cmdDelayCount+1];
		String [] cmdDataDlyName = new String [cmdDelayCount+1];
		for (int idx=0; idx<cmdDelayCount+1; idx++) {
			cmdValidDlyName[idx] = cmdValidDlyRootName + idx;
			cmdDataDlyName[idx] = cmdDataDlyRootName + idx;
		}
		this.addScalarWire(cmdValidDlyName[0]);  // cmd delay 0 is set from IO
		this.addVectorWire(cmdDataDlyName[0], 0, ringWidth); 
		for (int idx=1; idx<cmdDelayCount+1; idx++) {
			this.addScalarReg(cmdValidDlyName[idx]);  
			this.addVectorReg(cmdDataDlyName[idx], 0, ringWidth); 
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdDataDlyName[idx] + " <= #1  " + ringWidth + "'b0;");  
			this.addRegAssign(groupName,  cmdValidDlyName[idx] + " <= #1  " + cmdValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  cmdDataDlyName[idx] + " <= #1  " + cmdDataDlyName[idx-1] + ";");  
		}
        // assign ring cmd inputs to predelay 
		this.addWireAssign(cmdValidDlyName[0] + " = " + ringCmdValidName + ";");  
		this.addWireAssign(cmdDataDlyName[0] + " = " + ringCmdDataName + ";");

		// create delayed res signals  
		int resDelayCount = (int) Math.floor(ExtParameters.sysVerRingInterNodeDelay()/2.0);  // half of delay
		String [] resValidDlyName = new String [resDelayCount+1];
		String [] resDataDlyName = new String [resDelayCount+1];
		for (int idx=0; idx<resDelayCount+1; idx++) {
			resValidDlyName[idx] = resValidDlyRootName + idx;
			resDataDlyName[idx] = resDataDlyRootName + idx;
		}
		
		this.addScalarReg(resValidDlyName[0]);  // res delay 0 is set in state machine
		this.addVectorReg(resDataDlyName[0], 0, ringWidth); 
		for (int idx=1; idx<resDelayCount+1; idx++) {
			this.addScalarReg(resValidDlyName[idx]);  
			this.addVectorReg(resDataDlyName[idx], 0, ringWidth); 
			this.addResetAssign(groupName, builder.getDefaultReset(), resValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), resDataDlyName[idx] + " <= #1  " + ringWidth + "'b0;");  
			this.addRegAssign(groupName,  resValidDlyName[idx] + " <= #1  " + resValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  resDataDlyName[idx] + " <= #1  " + resDataDlyName[idx-1] + ";");  
		}
        // assign ring outputs outputs to delayed versions 
		this.addWireAssign(ringResValidName + " = " + resValidDlyName[resDelayCount] + ";");
		this.addWireAssign(ringResDataName + " = " + resDataDlyName[resDelayCount] + ";");

		// add address accumulate reg 
		if (addressWidth > 0) {
			this.addVectorReg(ringAddrAccumName, builder.getAddressLowBit(), addressWidth);  
			this.addVectorReg(ringAddrAccumNextName, builder.getAddressLowBit(), addressWidth);  
			this.addRegAssign(groupName,  ringAddrAccumName + " <= #1  " + ringAddrAccumNextName + ";");  
			this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), addressWidth);  
			this.addWireAssign(pioInterfaceAddressName + " = " + ringAddrAccumName + ";");  // input addr is set from accum reg			
		}

		// add write data accumulate reg
		this.addVectorReg(ringWrAccumName, 0, regWidth);  
		this.addVectorReg(ringWrAccumNextName, 0, regWidth);  
		this.addRegAssign(groupName,  ringWrAccumName + " <= #1  " + ringWrAccumNextName + ";");  
		this.addVectorWire(pioInterfaceWriteDataName, 0, regWidth);  
		this.addWireAssign(pioInterfaceWriteDataName + " = " + ringWrAccumName + ";");  // input data is set from accum reg
		
		// will need to capture cmd size
		String pioInterfaceTransactionSizeNextName = pioInterfaceTransactionSizeName + "_next";  // cmd size next will be set in sm
		if (useTransactionSize) {
			this.addVectorReg(pioInterfaceTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(pioInterfaceTransactionSizeNextName, 0, regWordBits);  // res size will be set in sm
			this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceTransactionSizeName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  pioInterfaceTransactionSizeName + " <= #1  " + pioInterfaceTransactionSizeNextName + ";");  
		}

		// add capture reg for write transaction indicator
		this.addScalarReg(ringWrStateCaptureName);  
		this.addScalarReg(ringWrStateCaptureNextName);  
		this.addRegAssign(groupName,  ringWrStateCaptureName + " <= #1  " + ringWrStateCaptureNextName + ";");  

		// add capture reg for read data
		this.addVectorReg(ringRdCaptureName, 0, regWidth);  
		this.addVectorReg(ringRdCaptureNextName, 0, regWidth);  
		this.addRegAssign(groupName,  ringRdCaptureName + " <= #1  " + ringRdCaptureNextName + ";"); 

		// address xfer count
		this.addVectorReg(ringAddrCntName, 0, addrXferCountBits);  
		this.addVectorReg(ringAddrCntNextName, 0, addrXferCountBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), ringAddrCntName + " <= #1  " + addrXferCountBits + "'b0;");  
		this.addRegAssign(groupName,  ringAddrCntName + " <= #1  " + ringAddrCntNextName + ";");  
		
		// add capture reg for address xfer count
		this.addVectorReg(ringAddrCntCaptureName, 0, addrXferCountBits);  
		this.addVectorReg(ringAddrCntCaptureNextName, 0, addrXferCountBits);  
		this.addRegAssign(groupName,  ringAddrCntCaptureName + " <= #1  " + ringAddrCntCaptureNextName + ";"); 
		
		// add ring msb no match indicator
		this.addScalarReg(ringNotMineName);  
		this.addScalarReg(ringNotMineNextName);  
		this.addResetAssign(groupName, builder.getDefaultReset(), ringNotMineName + " <= #1  1'b0;");  
		this.addRegAssign(groupName,  ringNotMineName + " <= #1  " + ringNotMineNextName + ";");  
		
		// data byte count 
		int maxDataXferCount = regWords * ExtParameters.getMinDataSize()/ringWidth; 
		int maxDataXferCountBits = Utils.getBits(maxDataXferCount);
		boolean useDataCounter = maxDataXferCountBits > 0;
		if (useDataCounter) {
			this.addVectorReg(ringDataCntName, 0, maxDataXferCountBits);  
			this.addVectorReg(ringDataCntNextName, 0, maxDataXferCountBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), ringDataCntName + " <= #1  " + maxDataXferCountBits + "'b0;");  
			this.addRegAssign(groupName,  ringDataCntName + " <= #1  " + ringDataCntNextName + ";"); 
		}
		//System.out.println("SystemVerilogDecodeModule genRingPioInterface: max data xfers=" + maxDataXferCount + ", bits=" + maxDataXferCountBits);


		// define internal interface signals that will be set in sm 
		String ringpioInterfaceReName = "r" + ringWidth + "_" + pioInterfaceReName;
		String ringpioInterfaceWeName = "r" + ringWidth + "_" + pioInterfaceWeName;
		this.addScalarReg(ringpioInterfaceReName);
		this.addScalarReg(ringpioInterfaceWeName);
		
		// state machine init values
		this.addCombinAssign(groupName,  ringStateNextName + " = " + ringStateName + ";");  
		this.addCombinAssign(groupName,  resValidDlyName[0] + " =  1'b0;");  // return valid
		this.addCombinAssign(groupName,  resDataDlyName[0] + " =  " + ringWidth + "'b0;");   // return data
		this.addCombinAssign(groupName,  ringpioInterfaceWeName + " =  1'b0;");  // write active
		this.addCombinAssign(groupName,  ringpioInterfaceReName + " =  1'b0;");  // read active 
		this.addCombinAssign(groupName,  ringWrStateCaptureNextName + " = " + ringWrStateCaptureName + ";");  // write indicator
		this.addCombinAssign(groupName,  ringAddrCntCaptureNextName + " = " + ringAddrCntCaptureName + ";");  // address xfer count
		if (addressWidth > 0)
			this.addCombinAssign(groupName,  ringAddrAccumNextName + " = " + ringAddrAccumName + ";");  // address accumulate
		this.addCombinAssign(groupName,  ringNotMineNextName + " =  1'b0;");  // default to matching address
		this.addCombinAssign(groupName,  ringWrAccumNextName + " = " + ringWrAccumName + ";");  // write data accumulate
		this.addCombinAssign(groupName,  ringRdCaptureNextName + " = " + ringRdCaptureName + ";");  // read data capture
		// init cmd size capture
		if (useTransactionSize)
			this.addCombinAssign(groupName,  pioInterfaceTransactionSizeNextName + " = " + pioInterfaceTransactionSizeName + ";"); 
		// init counter values
		this.addCombinAssign(groupName,  ringAddrCntNextName + " = "  + addrXferCountBits + "'b0;");
		
		if (useDataCounter) this.addCombinAssign(groupName,  ringDataCntNextName + " = "  + maxDataXferCountBits + "'b0;"); 
		this.addCombinAssign(groupName,  outFifoAdvanceName + " =  1'b0;");  // no fifo advance 
		this.addCombinAssign(groupName,  outFifoDataName[0] + " = " + cmdDataDlyName[cmdDelayCount] + ";");   
			
		// state machine 
		String IDLE = stateBits + "'h0"; 
		String CMD_ADDR = stateBits + "'h1"; 
		String CMD_DATA = stateBits + "'h2"; 
		String RES_WAIT = stateBits + "'h3";
		String RES_READ = stateBits + "'h4";
		String CMD_BYPASS = stateBits + "'h5"; 
		String CMD_FLUSH = stateBits + "'h6";   
		String RESPONSE_BYPASS = stateBits + "'h7";   
		String RESPONSE_FLUSH = stateBits + "'h8";   
				
		this.addCombinAssign(groupName, "case (" + ringStateName + ")"); 

		// IDLE
		this.addCombinAssign(groupName,     "  " + IDLE + ": begin // IDLE");
		// init accumulator/capture regs
		this.addCombinAssign(groupName,     "      " + ringWrStateCaptureNextName + " = 1'b0;");  
		if (addressWidth > 0)
			this.addCombinAssign(groupName, "      " + ringAddrAccumNextName + " = " + addressWidth + "'b0;");
		this.addCombinAssign(groupName,     "      " + ringWrAccumNextName + " = " + regWidth + "'b0;");
		// go on cmd valid - capture r/w indicator, addr size, and transaction size
		this.addCombinAssign(groupName,     "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		this.addCombinAssign(groupName,     "        " + outFifoAdvanceName + " =  1'b1;");  // save cntl word
		this.addCombinAssign(groupName,     "        " + ringWrStateCaptureNextName + " = " + cmdDataDlyName[cmdDelayCount] + "[" + rwBitIndex + "];");  // write bit 
		this.addCombinAssign(groupName,     "        " + ringAddrCntCaptureNextName + " = " + cmdDataDlyName[cmdDelayCount] + SystemVerilogSignal.genRefArrayString(addrBitIndex, addrXferCountBits) + ";");  // addr xfer count bits
		if (useTransactionSize)
		    this.addCombinAssign(groupName, "        " + pioInterfaceTransactionSizeNextName + " = " + cmdDataDlyName[cmdDelayCount] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + ";");  // bits n:0 = transaction size
        // if an address offset then detect an address mismatch and save these bits
		if (addrOffset > 0) {
            genRingAddrSliceAssigns(groupName, 0, addrOffset, addrOffset, addressWidth, 
            		ringNotMineNextName, ringNotMineName, ringAddrAccumNextName, cmdDataDlyName[cmdDelayCount], sigBlockBaseAddr);
		}
		// if ack or nack are already set then skip
		this.addCombinAssign(groupName,     "        if (" + cmdDataDlyName[cmdDelayCount] + "[" + nackBitIndex + "] | " + cmdDataDlyName[cmdDelayCount] + "[" + ackBitIndex + "]) begin");  
		this.addCombinAssign(groupName,     "          if (~" +  ringWrStateCaptureNextName + ") " + ringStateNextName + " = " + RESPONSE_BYPASS + ";"); // bypass data also if a read response 
		this.addCombinAssign(groupName,     "          else " + ringStateNextName + " = " + RESPONSE_FLUSH + ";"); // flush cmd word from fifo if a write response 
		this.addCombinAssign(groupName,     "        end");  //cmd valid
        // if an address then get it next  
		this.addCombinAssign(groupName,     "        else if (" + ringAddrCntCaptureNextName + " > " + addrXferCountBits + "'d0) " + ringStateNextName + " = " + CMD_ADDR + ";");  
		// otherwise get cmd data if a write or assert read and wait for internal read response
		this.addCombinAssign(groupName,     "        else if (" + ringWrStateCaptureNextName + ") " + ringStateNextName + " = " + CMD_DATA + ";");  
		this.addCombinAssign(groupName,     "        else " + ringStateNextName + " = " + RES_WAIT + ";");  
		this.addCombinAssign(groupName,     "      end");  //cmd valid
		this.addCombinAssign(groupName,     "    end"); 
		
		// CMD_ADDR
		this.addCombinAssign(groupName,  "  " + CMD_ADDR + ": begin // CMD_ADDR");
		this.addCombinAssign(groupName,  "      " + ringAddrCntNextName + " = " + ringAddrCntName + ";"); // hold value
		this.addCombinAssign(groupName,  "      " + ringNotMineNextName + " = " + ringNotMineName + ";");  // hold matching address value 
		this.addCombinAssign(groupName,  "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		this.addCombinAssign(groupName,  "        " + outFifoAdvanceName + " =  1'b1;");  // save addr word
		this.addCombinAssign(groupName,  "        " + ringAddrCntNextName + " = " + ringAddrCntName + " + " + addrXferCountBits + "'b1;");
		// accumulate the address
		for (int idx=0; idx<maxAddrXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "        " + prefix + "if (" + ringAddrCntName + " == " + addrXferCountBits + "'d" + idx + ") begin");
			// generate compare/assign strings
            genRingAddrSliceAssigns(groupName, idx*ringWidth + addrOffset, 0, ringWidth, addressWidth, 
            		ringNotMineNextName, ringNotMineName, ringAddrAccumNextName, cmdDataDlyName[cmdDelayCount], sigBlockBaseAddr);
			this.addCombinAssign(groupName, "        end"); 
		}

		// if addr done, send data if a write or wait for read response 
		// if this xfer isn't for me then either capture write data or just send the accumulated data in the bypass fifo
		this.addCombinAssign(groupName, "        if (" + ringAddrCntNextName + " == " + ringAddrCntCaptureName + ") begin"); 
		this.addCombinAssign(groupName, "          if (" + ringNotMineNextName + ") begin");  
		this.addCombinAssign(groupName, "            " +  ringAddrCntNextName + " = "  + addrXferCountBits + "'b0;");
		this.addCombinAssign(groupName, "            if (" + ringWrStateCaptureNextName + ") " + ringStateNextName + " = " + CMD_BYPASS + ";");  
		this.addCombinAssign(groupName, "            else " + ringStateNextName + " = " + CMD_FLUSH + ";");  
		this.addCombinAssign(groupName, "          end"); 
		this.addCombinAssign(groupName, "          else if (" + ringWrStateCaptureName + ") " + ringStateNextName + " = " + CMD_DATA + ";");  
		this.addCombinAssign(groupName, "          else " + ringStateNextName + " = " + RES_WAIT + ";");
		this.addCombinAssign(groupName, "        end"); 

		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// CMD_DATA - capture write data
		this.addCombinAssign(groupName, "  " + CMD_DATA + ": begin // CMD_DATA");
		String finalCntStr = getSerialMaxDataCountStr(useTransactionSize, transactionsInWord, pioInterfaceTransactionSizeName);
		if (useDataCounter) { 
			this.addCombinAssign(groupName,  "      " + ringDataCntNextName + " = " + ringDataCntName + ";");
		    this.addCombinAssign(groupName,  "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
			//  bump the data count
			this.addCombinAssign(groupName,  "        " + ringDataCntNextName + " = " + ringDataCntName + " + " + maxDataXferCountBits + "'b1;");
			// get the data slice
			for (int idx=0; idx<maxDataXferCount; idx++) {
				prefix = (idx == 0)? "" : "else ";
				this.addCombinAssign(groupName, "        "+ prefix + "if (" + ringDataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
				this.addCombinAssign(groupName, "          "  + ringWrAccumNextName + SystemVerilogSignal.genRefArrayString(ringWidth*idx, ringWidth) + " = " + cmdDataDlyName[cmdDelayCount] + ";");
			}
			// if done, move to res wait
			this.addCombinAssign(groupName, "        if (" + ringDataCntName + " == " + finalCntStr + ")");
			this.addCombinAssign(groupName, "          " + ringStateNextName + " = " + RES_WAIT + ";");
			this.addCombinAssign(groupName, "      end"); 
		}
		else {
		    this.addCombinAssign(groupName,  "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
			this.addCombinAssign(groupName, "          "  + ringWrAccumNextName + SystemVerilogSignal.genRefArrayString(0, ringWidth) + " = " + cmdDataDlyName[cmdDelayCount] + ";");
			this.addCombinAssign(groupName, "          "  + ringStateNextName + " = " + RES_WAIT + ";");
			this.addCombinAssign(groupName, "      end"); 			
		}
		this.addCombinAssign(groupName, "    end"); 
			
		// RES_WAIT
		this.addCombinAssign(groupName, "  " + RES_WAIT + ": begin  // RES_WAIT");
		// activate either read or write request
		this.addCombinAssign(groupName, "      " + ringpioInterfaceWeName + " = " + ringWrStateCaptureName + ";");  // set internal write active
		this.addCombinAssign(groupName, "      " + ringpioInterfaceReName + " = ~" + ringWrStateCaptureName + ";");  // set internal read active
		// go on ack/nack, capture read data and set first word of response (contains ack/nack and return xfer size) 
		this.addCombinAssign(groupName,   "      " + "if (" + pioInterfaceAckName + " | " + pioInterfaceNackName + ") begin");  
		this.addCombinAssign(groupName,   "        " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		this.addCombinAssign(groupName,   "        " + ringRdCaptureNextName + " = " + pioInterfaceReadDataName + ";");  // capture read data  
		this.addCombinAssign(groupName,   "        " + resDataDlyName[0] + "[" + nackBitIndex + "] = " + pioInterfaceNackName + ";");  // set nack bit  
		this.addCombinAssign(groupName,   "        " + resDataDlyName[0] + "[" + ackBitIndex + "] = ~" + pioInterfaceNackName + ";");  // set ack bit  
		this.addCombinAssign(groupName,   "        " + resDataDlyName[0] + "[" + rwBitIndex + "] = " + ringWrStateCaptureName + ";");  // set write bit  
		if (useTransactionSize) {
			this.addCombinAssign(groupName,  "        " + resDataDlyName[0] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + " = " + pioInterfaceRetTransactionSizeName + ";");  
		}
		// if a read then send the data else we're done
		this.addCombinAssign(groupName,   "        if (~" + ringWrStateCaptureName + ") " + ringStateNextName + " = " + RES_READ + ";");  
		this.addCombinAssign(groupName,   "        else "+ ringStateNextName + " = " + IDLE + ";");
		this.addCombinAssign(groupName,   "      end"); 
		this.addCombinAssign(groupName,   "    end");
		
		// RES_READ - send read data
		this.addCombinAssign(groupName,     "  " + RES_READ + ": begin  // RES_READ");  
		this.addCombinAssign(groupName,     "      " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		if (useDataCounter) {
			//  bump the data count
			this.addCombinAssign(groupName,     "      " + ringDataCntNextName + " = " + ringDataCntName + " + " + maxDataXferCountBits + "'b1;");
			// send the data slice while 
			for (int idx=0; idx<maxDataXferCount; idx++) {
				prefix = (idx == 0)? "" : "else ";
				this.addCombinAssign(groupName, "      "+ prefix + "if (" + ringDataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
				this.addCombinAssign(groupName, "        " + resDataDlyName[0] + " = " + ringRdCaptureName + SystemVerilogSignal.genRefArrayString(ringWidth*idx, ringWidth) + ";");
			}

			// if final count we're done
			String finalRetCntStr = getSerialMaxDataCountStr(useTransactionSize, transactionsInWord, pioInterfaceRetTransactionSizeName);
			this.addCombinAssign(groupName, "      if (" + ringDataCntName + " == " + finalRetCntStr + ") " + ringStateNextName + " = " + IDLE + ";");
		}
		else {
			this.addCombinAssign(groupName, "      " + resDataDlyName[0] + " = " + ringRdCaptureName + SystemVerilogSignal.genRefArrayString(0, ringWidth) + ";");
			this.addCombinAssign(groupName, "      " + ringStateNextName + " = " + IDLE + ";");	
		}
		this.addCombinAssign(groupName,     "    end"); 
		
		// CMD_BYPASS - feed write data into bypass fifo  
		this.addCombinAssign(groupName,     "  " + CMD_BYPASS + ": begin // CMD_BYPASS");
		if (useDataCounter) this.addCombinAssign(groupName,     "      " + ringDataCntNextName + " = " + ringDataCntName + ";");
		this.addCombinAssign(groupName,     "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		//  bump the data count
		if (useDataCounter) this.addCombinAssign(groupName,     "        " + ringDataCntNextName + " = " + ringDataCntName + " + " + maxDataXferCountBits + "'b1;");
        // advance the fifo and set output valid
		this.addCombinAssign(groupName,     "        " + outFifoAdvanceName + " =  1'b1;");  // advance fifo
		this.addCombinAssign(groupName,     "        " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		// if done, move to flush
		if (useDataCounter) this.addCombinAssign(groupName,     "        if (" + ringDataCntName + " == " + finalCntStr + ")"); 
		this.addCombinAssign(groupName,     "          " + ringStateNextName + " = " + CMD_FLUSH + ";");
		this.addCombinAssign(groupName,     "      end"); 
		// set the RES_output to fifo element depending on xfer addr size 
		for (int idx=0; idx<maxAddrXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "      " + prefix + "if (" + ringAddrCntCaptureName + " == " + addrXferCountBits + "'d" + idx + ")");
			this.addCombinAssign(groupName, "        " + resDataDlyName[0] + " = " + outFifoDataName[idx+1] + ";");
		}
		this.addCombinAssign(groupName,     "    end"); 
		
		// CMD_FLUSH  - shift data out of bypass fifo 
		this.addCombinAssign(groupName,     "  " + CMD_FLUSH + ": begin  // CMD_FLUSH"); 
        // transfer out address xfer count + 1
		this.addCombinAssign(groupName,     "      " + outFifoAdvanceName + " =  1'b1;");  // save addr word
		this.addCombinAssign(groupName,     "      " + ringAddrCntNextName + " = " + ringAddrCntName + " + " + addrXferCountBits + "'b1;");
		this.addCombinAssign(groupName,     "      if (" + ringAddrCntName + " == " + ringAddrCntCaptureName + ") " + ringStateNextName + " = " + IDLE + ";");
		// set the RES_output to fifo element depending on xfer addr size 
		this.addCombinAssign(groupName,     "      " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		for (int idx=0; idx<maxAddrXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "      " + prefix + "if (" + ringAddrCntCaptureName + " == " + addrXferCountBits + "'d" + idx + ")");
			this.addCombinAssign(groupName, "        " + resDataDlyName[0] + " = " + outFifoDataName[idx+1] + ";");
		}
		this.addCombinAssign(groupName,     "    end"); 
		
		// RESPONSE_BYPASS - forward responses from previous nodes on the ring  
		this.addCombinAssign(groupName,     "  " + RESPONSE_BYPASS + ": begin // RESPONSE_BYPASS");
		if (useDataCounter) this.addCombinAssign(groupName,     "      " + ringDataCntNextName + " = " + ringDataCntName + ";");
		this.addCombinAssign(groupName,     "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		//  bump the data count
		if (useDataCounter) this.addCombinAssign(groupName,     "        " + ringDataCntNextName + " = " + ringDataCntName + " + " + maxDataXferCountBits + "'b1;");
        //  set output valid
		this.addCombinAssign(groupName,     "        " + outFifoAdvanceName + " =  1'b1;");  // save word
		this.addCombinAssign(groupName,     "        " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		// if done, flush fifo and back to idle
		if (useDataCounter) this.addCombinAssign(groupName,     "        if (" + ringDataCntName + " == " + finalCntStr + ")"); 
		this.addCombinAssign(groupName,     "          " + ringStateNextName + " = " + RESPONSE_FLUSH + ";");
		this.addCombinAssign(groupName,     "      end"); 
		this.addCombinAssign(groupName,     "      " + resDataDlyName[0] + " = " + outFifoDataName[1] + ";");
		this.addCombinAssign(groupName,     "    end"); 
		
		// RESPONSE_FLUSH  - shift data out of bypass fifo 
		this.addCombinAssign(groupName,     "  " + RESPONSE_FLUSH + ": begin  // RESPONSE_FLUSH"); 
        // transfer out 1 word
		this.addCombinAssign(groupName,     "      " + outFifoAdvanceName + " =  1'b1;");  // save addr word
		this.addCombinAssign(groupName,     "      " + ringStateNextName + " = " + IDLE + ";");
		// set the RES_output to fifo element depending on xfer addr size 
		this.addCombinAssign(groupName,     "      " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		this.addCombinAssign(groupName,     "      " + resDataDlyName[0] + " = " + outFifoDataName[1] + ";");
		this.addCombinAssign(groupName,     "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + ringStateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 				

		this.addScalarWire(pioInterfaceReName);  //  read enable internal interface
		this.addScalarWire(pioInterfaceWeName);  //  write enable internal interface
				
		// disable atomic request to arbiter
		if (hasSecondaryInterface()) {
			this.addScalarWire(arbiterAtomicName);   
			this.addWireAssign(arbiterAtomicName + " = 1'b0;");
		}

		// generate re/we assigns - use delayed versions if this is a single primary
		assignReadWriteRequests(ringpioInterfaceReName, ringpioInterfaceWeName, pioInterfaceReName, pioInterfaceWeName, !hasSecondaryInterface());
	}

	/** generate address accumulate and match detect assigns 
	 * 
	 * @param groupName - lable for comb assigns
	 * @param currentAddressOffset - current bit offset (number of addr bits already accumulated)
	 * @param ringDataOffset - bit offset in ring data where address info starts
	 * @param currentAddrSliceWidth - size of slice being accumulated
	 * @param addressWidth - address width of current map
	 * @param ringNotMineNextName - addr mismatch next state signal
	 * @param ringNotMineName - addr mismatch current state signal
	 * @param ringAddrAccumNextName - address accumulator signal
	 * @param ringData - incoming ring data signal
	 */
	private void genRingAddrSliceAssigns(String groupName, int currentAddressOffset, int ringDataOffset, int currentAddrSliceWidth, int addressWidth, 
			String ringNotMineNextName, String ringNotMineName, String ringAddrAccumNextName, String ringData, String sigBlockBaseAddr) {
		int lowbit = currentAddressOffset + builder.getAddressLowBit();  // full address offset
		int lsbCheckSize = (currentAddressOffset + currentAddrSliceWidth > addressWidth)? Math.max(0, addressWidth - currentAddressOffset) : currentAddrSliceWidth;  // last valid local addr xfer is btwn 0 and ringWidth b (0 after)
		int msbCheckSize = currentAddrSliceWidth - lsbCheckSize;  // for ring, we'll use base address to test uppr addr bits in this xfer, could be all check size
		// if msbs are in this xfer, then look for an address mismatch 
		if (msbCheckSize > 0) {
			if ((lowbit + currentAddrSliceWidth) > ExtParameters.getLeafAddressSize()) 
				msbCheckSize = ExtParameters.getLeafAddressSize() - lowbit;
			//String matchStr = topRegProperties.getFullBaseAddress().getSubVector(lowbit + lsbCheckSize, msbCheckSize).toFormat(NumBase.Hex, NumFormat.Verilog);
			String matchStr = sigBlockBaseAddr + SystemVerilogSignal.genRefArrayString(lowbit + lsbCheckSize, msbCheckSize);
			//block_base_address
			this.addCombinAssign(groupName, "          " + ringNotMineNextName + " = " + ringNotMineName + " | (" + ringData + SystemVerilogSignal.genRefArrayString(ringDataOffset + lsbCheckSize, msbCheckSize) + " != " + matchStr + ");");  // default to matching address
		}
		//  only accumulate address if in local range
		if (currentAddressOffset < addressWidth) {
			String addrRangeString = (addressWidth > 1)? SystemVerilogSignal.genRefArrayString(lowbit, lsbCheckSize) : "";
		    this.addCombinAssign(groupName, "          " + ringAddrAccumNextName + addrRangeString + " = " + ringData + SystemVerilogSignal.genRefArrayString(ringDataOffset, lsbCheckSize) + ";");  		
		}
		
	}

	/** generate re/we assigns directly or from delayed versions if clock gating is enabled 
	 * @param readReqIn - incoming read request signal/verilog expression
	 * @param writeReqIn - incoming write request signal/verilog expression
	 * @param readReqOut - outgoing read request signal to be assigned
	 * @param writeReqOut - outgoing write request signal to be assigned
	 * @param allowClkGateDelay - if true and clock gating is specified, add req delay logic
	 * */
	private void assignReadWriteRequests(String readReqIn, String writeReqIn, String readReqOut, String writeReqOut, boolean allowClkGateDelay) {
		// if gated logic clock, create an enable output and delay read/write activation 
		if (ExtParameters.systemverilogUseGatedLogicClk() && allowClkGateDelay) {
			
			// delayed request signals
			String cgateDelayedReName = pioInterfaceReName + "_cgate_dly";
			String cgateDelayedWeName = pioInterfaceWeName + "_cgate_dly";
			int maxDelay = ExtParameters.systemverilogGatedLogicAccessDelay();
			maxDelay = (maxDelay < 1)? 1 : maxDelay;
			
			// define delayed elements
			for (int dly = 1; dly <= maxDelay; dly++) {
				this.addScalarReg(cgateDelayedReName + dly);  //  delayed read active
				this.addScalarReg(cgateDelayedWeName + dly);  //  delayed write active
				this.addResetAssign("clock gate delay", builder.getDefaultReset(), cgateDelayedReName + dly + " <= #1  1'b0;");  
				this.addResetAssign("clock gate delay", builder.getDefaultReset(), cgateDelayedWeName + dly + " <= #1  1'b0;");
				// first delay just picks up input requests, even if final stage
				if (dly == 1) {
					this.addRegAssign("clock gate delay",  cgateDelayedReName + dly + " <= #1 " + readReqIn + ";");
					this.addRegAssign("clock gate delay",  cgateDelayedWeName + dly + " <= #1 " + writeReqIn + ";"); 
				}
				// other delay stages must turn off if request falls
				else {
					this.addRegAssign("clock gate delay",  cgateDelayedReName + dly + " <= #1 " +
                            "(" + cgateDelayedReName + (dly - 1) + " & !" + cgateDelayedReName + dly + ") | " +  // turn on a cycle after previous stage
                            "(" + cgateDelayedReName + dly + " & !(" + cgateDelayedReName + "1 & !(" + readReqIn + ")));"); // off on falling base req
					this.addRegAssign("clock gate delay",  cgateDelayedWeName + dly + " <= #1 " + 
                            "(" + cgateDelayedWeName + (dly - 1) + " & !" + cgateDelayedWeName + dly + ") | " +  // turn on a cycle after previous stage
                            "(" + cgateDelayedWeName + dly + " & !(" + cgateDelayedWeName + "1 & !(" + writeReqIn + ")));"); // off on falling base req
				}
				/* otherwise stage is just delayed previous 
				else {
					this.addRegAssign("clock gate delay",  cgateDelayedReName + dly + " <= #1 " + cgateDelayedReName + (dly - 1) + ";");
					this.addRegAssign("clock gate delay",  cgateDelayedWeName + dly + " <= #1 " + cgateDelayedWeName + (dly - 1) + ";");
				}*/
			}
			
			// create gate enable output on first delay
			String suffix = (builder.getBuilderID() > 0)? "_" + builder.getBuilderID() : "";
			this.addSimpleScalarTo(SystemVerilogBuilder.PIO, "gclk_enable" + suffix);  // clock enable output
			this.addWireAssign("gclk_enable" + suffix + " = " + cgateDelayedReName + "1 | " + cgateDelayedWeName + "1;");
			
			// assign the delayed read/write requests		
			this.addWireAssign(readReqOut + " = " + readReqIn + " & " + cgateDelayedReName + maxDelay + ";"); 
			this.addWireAssign(writeReqOut + " = " + writeReqIn + " & " + cgateDelayedWeName + maxDelay + ";");
			
		}
		// otherwise just generate request signals with no delay
		else {
			this.addWireAssign(readReqOut + " = " + readReqIn + ";");   
			this.addWireAssign(writeReqOut + " = " + writeReqIn + ";");
		}
	}

	// ------------------------------ decoder external hw interface methods -------------------------------
	
	/** class containing common signal names, etc for an external interface */
	private class ExternalInterfaceInfo  {
		public String decodeToHwName;   // write data                   
		public String decodeToHwEnableName;   // write enable                   
		public String decodeToHwWeName; // we
		public String decodeToHwReName; //  re
		public String hwToDecodeName; //  read data
		public String hwToDecodeAckName; //  ext ack
		public String hwToDecodeNackName; //  ext nack
		public String decodeToHwAddrName; // address
		public String decodeToHwTransactionSizeName;  // size of r/w transaction in words
		public String hwToDecodeTransactionSizeName;  // size of return read transaction in word
		public Boolean hasAddress;   // true if external region has an address
		public Boolean hasSize;   // true if external region has size signals

		/** extract external interface info from instance properties */
		public ExternalInterfaceInfo(AddressableInstanceProperties addrInstProperties) {
			   this.decodeToHwName = addrInstProperties.getFullSignalName(DefSignalType.D2H_DATA) + "_ex";   // write data                   
			   this.decodeToHwEnableName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ENABLE) + "_ex";   // write enable                   
			   this.decodeToHwWeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_WE) + "_ex"; //  we
			   this.decodeToHwReName = addrInstProperties.getFullSignalName(DefSignalType.D2H_RE) + "_ex"; //  re
			   this.hwToDecodeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_DATA) + "_ex"; //  read data
			   this.hwToDecodeAckName = addrInstProperties.getFullSignalName(DefSignalType.H2D_ACK) + "_ex"; //  ext ack
			   this.hwToDecodeNackName = addrInstProperties.getFullSignalName(DefSignalType.H2D_NACK) + "_ex"; //  ext nack
			   this.decodeToHwAddrName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ADDR) + "_ex"; // address
			   this.decodeToHwTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_SIZE) + "_ex";  // size of r/w transaction in words
			   this.hwToDecodeTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE) + "_ex";  // size of return read transaction in words
			   this.hasAddress = (addrInstProperties.getExtAddressWidth() > 0)  && !addrInstProperties.isSingleExtReg();
			   this.hasSize = (addrInstProperties.getMaxRegWordWidth() > 1) && !addrInstProperties.isSingleExtReg();
		}
	}
	
	// ------------------------------ decoder external hw interface methods -------------------------------
	
	/** generate common external interface constructs - creates logic between the address decoder and
	 *  common interface signals that are used in all external interface variants
	 *  @return - ExternalInterfaceInfo for the common ext interface extracted from AddressableInstanceProperties */
	private ExternalInterfaceInfo generateBaseExternalInterface(AddressableInstanceProperties addrInstProperties) { 
		//extract interface info
		ExternalInterfaceInfo extIf = new ExternalInterfaceInfo(addrInstProperties);
		
		// create verilog names used internally in decoder block (ack, nack, rdata pass thru to decoder as is)
		String intDecodeToHwName = addrInstProperties.getFullSignalName(DefSignalType.D2H_DATA) + "_next";   // write data                   
		String intDecodeToHwEnableName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ENABLE) + "_next";   // write enable                   
		String intDecodeToHwWeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_WE) + "_next"; // we
		String intDecodeToHwReName = addrInstProperties.getFullSignalName(DefSignalType.D2H_RE) + "_next"; // re
		String intDecodeToHwAddrName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ADDR) + "_next"; // address
		String intDecodeToHwTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_SIZE) + "_next";  // size of r/w transaction in words
		String intHwToDecodeTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE) + "_d1";  // size of return read transaction in words

		// register the outputs and assign output reg values
		this.addVectorReg(extIf.decodeToHwName, 0, addrInstProperties.getMaxRegWidth());
		if (hasWriteEnables()) this.addVectorReg(extIf.decodeToHwEnableName, 0, addrInstProperties.getWriteEnableWidth());
		this.addScalarReg(extIf.decodeToHwWeName);  
		this.addScalarReg(extIf.decodeToHwReName);  

		// add next signal assignments also
		this.addVectorReg(intDecodeToHwName, 0, addrInstProperties.getMaxRegWidth());  
		if (hasWriteEnables()) this.addVectorReg(intDecodeToHwEnableName, 0, addrInstProperties.getWriteEnableWidth());
		this.addScalarReg(intDecodeToHwWeName);  
		this.addScalarReg(intDecodeToHwReName);  
		// reset output signals
		this.addResetAssign("external i/f", builder.getDefaultReset(), extIf.decodeToHwWeName + " <= #1  1'b0;" );   
		this.addResetAssign("external i/f", builder.getDefaultReset(), extIf.decodeToHwReName + " <= #1  1'b0;" );

		String ackInhibitStr = "~" + extIf.hwToDecodeAckName + " & ~" + extIf.hwToDecodeNackName;
		this.addRegAssign("external i/f",  extIf.decodeToHwName + " <= #1  " + intDecodeToHwName + ";");  // assign next to flop
		if (hasWriteEnables()) this.addRegAssign("external i/f",  extIf.decodeToHwEnableName + " <= #1  " + intDecodeToHwEnableName + ";");  // assign next to flop
		this.addRegAssign("external i/f",  extIf.decodeToHwWeName + " <= #1  " + intDecodeToHwWeName + " & " + ackInhibitStr  + ";");  // assign next to flop
		this.addRegAssign("external i/f",  extIf.decodeToHwReName + " <= #1  " + intDecodeToHwReName + " & " + ackInhibitStr  + ";");  // assign next to flop

		// if size of external range is greater than one reg we'll need an external address  
		if (extIf.hasAddress) {  
			this.addVectorReg(extIf.decodeToHwAddrName, addrInstProperties.getExtLowBit(), addrInstProperties.getExtAddressWidth());  
			this.addVectorReg(intDecodeToHwAddrName, addrInstProperties.getExtLowBit(), addrInstProperties.getExtAddressWidth());  
			this.addRegAssign("external i/f",  extIf.decodeToHwAddrName + " <= #1  " + intDecodeToHwAddrName  + ";");  // assign next to flop
		}	

		// if size of max pio transaction is greater than one word need to add transaction size/retry info 
		if (extIf.hasSize) { 
			int regWordBits = Utils.getBits(addrInstProperties.getMaxRegWordWidth());
			//if (addrInstProperties.getMaxRegWordWidth()==3) System.out.println("SystemVerilogDecodeModule generateBaseExternalInterface: regWordBits=" + regWordBits);

			this.addVectorReg(extIf.decodeToHwTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(intDecodeToHwTransactionSizeName, 0, regWordBits);  
			this.addRegAssign("external i/f",  extIf.decodeToHwTransactionSizeName + " <= #1  " + intDecodeToHwTransactionSizeName  + ";");  // assign next to flop

			this.addVectorReg(intHwToDecodeTransactionSizeName, 0, regWordBits);  
			this.addResetAssign("external i/f", builder.getDefaultReset(), intHwToDecodeTransactionSizeName + " <= #1  " + regWordBits +"'b0;");  // reset input size flop
			this.addRegAssign("external i/f",  intHwToDecodeTransactionSizeName + " <= #1  " + extIf.hwToDecodeTransactionSizeName + ";");  // assign input size to flop
		}	
		return extIf;
	}

	/** generate PARALLEL external interface shim logic */  
	public void generateExternalInterface_PARALLEL(AddressableInstanceProperties addrInstProperties, boolean optimize, boolean keepNack) {
	    //System.out.println("SystemVerilogDecodeModule generateExternalInterface_PARALLEL: " + addrInstProperties.getId() + ", optimize=" + optimize + ", keepNack=" + keepNack);
		// generate common external interface constructs
		ExternalInterfaceInfo extIf = generateBaseExternalInterface(addrInstProperties);
		
		// define external signal names
		String decodeToHwName = addrInstProperties.getFullSignalName(DefSignalType.D2H_DATA);   // write data                   
		String decodeToHwEnableName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ENABLE);   // write enable                   
		String decodeToHwWeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_WE); //  we
		String decodeToHwReName = addrInstProperties.getFullSignalName(DefSignalType.D2H_RE); //  re
		String hwToDecodeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_DATA); //  read data
		String hwToDecodeAckName = addrInstProperties.getFullSignalName(DefSignalType.H2D_ACK); //  ext ack
		String hwToDecodeNackName = addrInstProperties.getFullSignalName(DefSignalType.H2D_NACK); //  ext nack
		String decodeToHwAddrName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ADDR); // address
		String decodeToHwTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_SIZE);  // size of r/w transaction in words
		String hwToDecodeTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE);  // size of return read transaction in words
		
		// use state machine if this is a contiguous replicated reg
		boolean useAckStateMachine = optimize && (addrInstProperties.getMaxRegByteWidth() == addrInstProperties.getAlignedSize().toLong());
		
		// if not optimized interface or writeable then add write external data/wr outputs
		if (!useAckStateMachine || addrInstProperties.isSwWriteable()) {
			this.addHwVector(DefSignalType.D2H_DATA, 0, addrInstProperties.getMaxRegWidth());    // add write data to decode to hw signal list
			if (hasWriteEnables()) this.addHwVector(DefSignalType.D2H_ENABLE, 0, addrInstProperties.getWriteEnableWidth());    // add write data to decode to hw signal list
			this.addHwScalar(DefSignalType.D2H_WE);
			if (!useAckStateMachine) this.addWireAssign(decodeToHwWeName + " = " + extIf.decodeToHwWeName +  ";");
			else this.addScalarReg(decodeToHwWeName); 
			this.addWireAssign(decodeToHwName + " = " + extIf.decodeToHwName +  ";");
			if (hasWriteEnables()) this.addWireAssign(decodeToHwEnableName + " = " + extIf.decodeToHwEnableName +  ";");    // add write enable to decode to hw signal list 
		}
		
		// if not optimized interface or readable then add read external data/rd io
		this.addVectorWire(extIf.hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		int regWordBits = Utils.getBits(addrInstProperties.getMaxRegWordWidth());
		if (!useAckStateMachine || addrInstProperties.isSwReadable()) {
			this.addHwScalar(DefSignalType.D2H_RE);
			this.addHwVector(DefSignalType.H2D_DATA, 0, addrInstProperties.getMaxRegWidth());    // add read data to hw to decode signal list
			if (!useAckStateMachine) this.addWireAssign(decodeToHwReName + " = " + extIf.decodeToHwReName +  ";");
			else this.addScalarReg(decodeToHwReName);        
			this.addWireAssign(extIf.hwToDecodeName + " = " + hwToDecodeName +  ";");   
		}
		else { // otherwise tie off read data input
			this.addWireAssign(extIf.hwToDecodeName + " = " + addrInstProperties.getMaxRegWidth() + "'b0;");   
		}
		
		this.addHwScalar(DefSignalType.H2D_ACK);     // ack input is always needed

		// use state machine if read/write limited (regs only), so need to gen nacks or is a reg
	    if (useAckStateMachine) {
		    //System.out.println("SystemVerilogDecodeModule generateExternalInterface_PARALLEL: " + addrInstProperties.getId() + " will useAckMachine");
			// create internal interface signals 
			this.addScalarReg(extIf.hwToDecodeAckName);  // ack nack are now generated in sm
			this.addScalarReg(extIf.hwToDecodeNackName);
			
			// generate validSizeCond by comparing decodeToHwTransactionSizeName value with regWordWidth - 1
			String regSizeStr = regWordBits + "'d" + (addrInstProperties.getMaxRegWordWidth() - 1);
			String validSizeCond = "par_" + addrInstProperties.getBaseName() + "_valid_size"; 
			this.addScalarWire(validSizeCond);
			if (extIf.hasSize) {  
				this.addWireAssign(validSizeCond + " = (" + extIf.decodeToHwTransactionSizeName + " == " + regSizeStr + ");");
				this.addVectorWire(extIf.hwToDecodeTransactionSizeName, 0, regWordBits);
				this.addWireAssign(extIf.hwToDecodeTransactionSizeName + " = " + regSizeStr + ";");  // tie off width
			}
			else
				this.addWireAssign(validSizeCond + " = 1'b1;");  // not a wide reg space, so always valid
			
	    	// add nack input if override specified
			if (keepNack) this.addHwScalar(DefSignalType.H2D_NACK);
			
			// generate validReadCond - nack on addr out of range or if not readable
			String validReadCond = "par_" + addrInstProperties.getBaseName() + "_valid_read"; 
			this.addScalarWire(validReadCond);
			if (!addrInstProperties.isSwReadable())
				this.addWireAssign(validReadCond + " = 1'b0;");  // no reads are valid
			else {
				RegNumber extMaxAddr = builder.getExternalRegBytes().getSubVector(addrInstProperties.getExtLowBit(), addrInstProperties.getExtAddressWidth());
				if (extIf.hasAddress && extMaxAddr.isNonZero()) { // if max addr is less than allowed range, add compare stmt
					this.addWireAssign(validReadCond + " = (" + extIf.decodeToHwAddrName + " < " + extMaxAddr.toFormat(NumBase.Hex, NumFormat.Verilog) + ");");
				}
				else
					this.addWireAssign(validReadCond + " = 1'b1;");  // all reads are valid
			}
			
			// generate validWriteCond - nack on addr out of range or if not writeable
			String validWriteCond = "par_" + addrInstProperties.getBaseName() + "_valid_write"; 
			this.addScalarWire(validWriteCond);
			if (!addrInstProperties.isSwWriteable())
				this.addWireAssign(validWriteCond + " = 1'b0;");  // no writes are valid
			else {
				RegNumber extMaxAddr = builder.getExternalRegBytes().getSubVector(addrInstProperties.getExtLowBit(), addrInstProperties.getExtAddressWidth());
				if (extIf.hasAddress && extMaxAddr.isNonZero()) { // if max addr is less than allowed range, add compare stmt
					this.addWireAssign(validWriteCond + " = (" + extIf.decodeToHwAddrName + " < " + extMaxAddr.toFormat(NumBase.Hex, NumFormat.Verilog) + ");");
				}
				else 
					this.addWireAssign(validWriteCond + " = 1'b1;");  // all writes are valid
			}
	    	
			// address shift to lsb0 
			if (extIf.hasAddress) { 
				int shiftBits = Utils.isPowerOf2(addrInstProperties.getMaxRegWordWidth())? Utils.getBits(addrInstProperties.getMaxRegWordWidth()) : 0;
				int newAddrWidth = addrInstProperties.getExtAddressWidth() - shiftBits;
				int newLowBit = addrInstProperties.getExtLowBit() + shiftBits;
				this.addHwVector(DefSignalType.D2H_ADDR, 0, newAddrWidth);
				String arrayStr = (addrInstProperties.getExtAddressWidth()>1)? SystemVerilogSignal.genRefArrayString(newLowBit, newAddrWidth) : "";
				this.addWireAssign(decodeToHwAddrName + " = " + extIf.decodeToHwAddrName + arrayStr + ";");  
			    //System.out.println("SystemVerilogDecodeModule generateExternalInterface_PARALLEL: " + addrInstProperties.getId() + ", reps=" + addrInstProperties.getRepCount()+ ", regbwidth=" + addrInstProperties.getMaxRegByteWidth() + ", shiftBits=" + shiftBits );
			}
			
			// generate rd/wr pulses if valid_size and valid_addr
			// generate ack/nack based on address compare, valid_size, valid_addr, rd_ack, and wr_status
			String parStateName = "par_" + addrInstProperties.getBaseName() + "_state";                      
			String groupName = addrInstProperties.getBaseName() + " optimized ext parallel i/f";
			generateAckNackMachine(parStateName, groupName, false, 
					extIf.decodeToHwReName, extIf.decodeToHwWeName, validReadCond, validWriteCond, validSizeCond, hwToDecodeAckName, keepNack? hwToDecodeNackName : null,  // optional nack input
					addrInstProperties.isSwReadable()? decodeToHwReName : null, addrInstProperties.isSwWriteable()? decodeToHwWeName : null, // eliminate re/we outputs if needed
					extIf.hwToDecodeAckName, extIf.hwToDecodeNackName);
	    }
	    // otherwise use basic parallel interface
	    else {
	    	// add nack input
			this.addHwScalar(DefSignalType.H2D_NACK);

			// create internal interface signals 
			this.addScalarWire(extIf.hwToDecodeAckName);
			this.addScalarWire(extIf.hwToDecodeNackName);
			// add assigns
			this.addWireAssign(extIf.hwToDecodeAckName + " = " + hwToDecodeAckName +  ";");
			this.addWireAssign(extIf.hwToDecodeNackName + " = " + hwToDecodeNackName +  ";");
		
			// if size of external range is greater than one reg we'll need an external address  
			if (extIf.hasAddress) {  
				this.addHwVector(DefSignalType.D2H_ADDR, addrInstProperties.getExtLowBit(), addrInstProperties.getExtAddressWidth());
				this.addWireAssign(decodeToHwAddrName + " = " + extIf.decodeToHwAddrName +  ";");  
			}

			// if size of max pio transaction is greater than one word need to add transaction size/retry info 
			if (extIf.hasSize) { 
				this.addHwVector(DefSignalType.D2H_SIZE, 0, Utils.getBits(addrInstProperties.getMaxRegWordWidth()));
				this.addWireAssign(decodeToHwTransactionSizeName + " = " + extIf.decodeToHwTransactionSizeName +  ";");
				this.addHwVector(DefSignalType.H2D_RETSIZE, 0, Utils.getBits(addrInstProperties.getMaxRegWordWidth()));
				this.addVectorWire(extIf.hwToDecodeTransactionSizeName, 0, regWordBits);
				this.addWireAssign(extIf.hwToDecodeTransactionSizeName + " = " + hwToDecodeTransactionSizeName +  ";");
			}	

	    }
	    //System.out.println("SystemVerilogDecodeModule generateExternalInterface_PARALLEL: " + addrInstProperties.getId() + ", reps=" + addrInstProperties.getRepCount()+ ", regbwidth=" + addrInstProperties.getMaxRegByteWidth() + ", asize=" + addrInstProperties.getAlignedSize() + ", read/write=" + addrInstProperties.isSwReadable() + "/" + addrInstProperties.isSwWriteable());


	}

	/** generate BBV5 external interface shim logic */  
	public void generateExternalInterface_BBV5(AddressableInstanceProperties addrInstProperties) {
		//Jrdl.warnMessage("SystemVerilogBuilder gen_BBV5, ext type=" + regProperties.getExternalType() + ", id=" + regProperties.getId());
		// generate common external interface constructs
		ExternalInterfaceInfo extIf = generateBaseExternalInterface(addrInstProperties);
		
		// create incoming signals (default IOs that now need to be set)
		this.addVectorWire(extIf.hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		this.addScalarReg(extIf.hwToDecodeAckName);
		this.addScalarReg(extIf.hwToDecodeNackName);

		// create module IOs
		String topBackboneReqName = "d2bb_" + addrInstProperties.getBaseName() + "_req";                      
		String topBackboneRdName = "d2bb_" + addrInstProperties.getBaseName() + "_rd";                      
		String topBackboneWrDvldName = "d2bb_" + addrInstProperties.getBaseName() + "_wr_dvld";                      
		String topBackboneWrWidthName = "d2bb_" + addrInstProperties.getBaseName() + "_wr_width";                      
		String topBackboneWrDataName = "d2bb_" + addrInstProperties.getBaseName() + "_wr_data";                      
		String topBackboneAddrName = "d2bb_" + addrInstProperties.getBaseName() + "_addr"; 
		
		//String backboneTopCmdReturnedName = "bb2d_" + regProperties.getBaseName() + "_cmd_returned";                      
		String backboneTopReqName = "bb2d_" + addrInstProperties.getBaseName() + "_req";                      
		String backboneTopDvldName = "bb2d_" + addrInstProperties.getBaseName() + "_dvld";                      
		//String backboneTopStatus = "bb2d_" + regProperties.getBaseName() + "_status";                      
		String backboneTopNack = "bb2d_" + addrInstProperties.getBaseName() + "_nack";                      
		String backboneTopWrRetry = "bb2d_" + addrInstProperties.getBaseName() + "_wr_retry";                      
		String backboneTopWidthName = "bb2d_" + addrInstProperties.getBaseName() + "_width";                      
		String backboneTopDataName = "bb2d_" + addrInstProperties.getBaseName() + "_data";  
		
		//  outputs
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, topBackboneReqName);     
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, topBackboneRdName);     //1=read, 0=write
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, topBackboneWrDvldName);     //write data valid
		this.addSimpleVectorTo(SystemVerilogBuilder.HW, topBackboneWrWidthName, 0, 4);    //number of words 
		this.addSimpleVectorTo(SystemVerilogBuilder.HW, topBackboneWrDataName, 0, 32);    // add write data to decode to hw signal list 
		this.addSimpleVectorTo(SystemVerilogBuilder.HW, topBackboneAddrName, 0, ExtParameters.getLeafAddressSize());    // address 

		//this.addScalarFrom(SystemVerilogBuilder.HW, backboneTopCmdReturnedName);   
		this.addSimpleScalarFrom(SystemVerilogBuilder.HW, backboneTopReqName);     // stays high while all res data xfered
		this.addSimpleScalarFrom(SystemVerilogBuilder.HW, backboneTopDvldName);     // data valid - ony high on valid 32b data 
		this.addSimpleScalarFrom(SystemVerilogBuilder.HW, backboneTopNack);     
		this.addSimpleScalarFrom(SystemVerilogBuilder.HW, backboneTopWrRetry);      
		//this.addVectorFrom(SystemVerilogBuilder.HW, backboneTopStatus, 0, 4);     // 3=retry, 2=ack, 1=error, 0=nack
		this.addSimpleVectorFrom(SystemVerilogBuilder.HW, backboneTopWidthName, 0, 4);   //number of words  
		this.addSimpleVectorFrom(SystemVerilogBuilder.HW, backboneTopDataName, 0, 32);   //data  
		
		// use a timeout input if option specified
		int backboneTopTimeoutBits = 8; // set timeout counter size - default to 8 bits
		String backboneTopTimeoutValue = "8'hFF";  
		if (ExtParameters.sysVerBBV5TimeoutInput()) {
			//System.out.println("SystemVerilogDecodeModule generateExternalInterface_BBV5: using timeout input for " + regProperties.getBaseName());
			backboneTopTimeoutBits = 12; // set timeout counter size
			backboneTopTimeoutValue = "bb2d_" + addrInstProperties.getBaseName() + "_timeout";  // use input signal for timeout
			this.addSimpleVectorFrom(SystemVerilogBuilder.HW, backboneTopTimeoutValue, 0, backboneTopTimeoutBits);   //timeout input  
		}
		// if size of external range is greater than one reg we'll need to set external address bits 
		RegNumber newBase = addrInstProperties.getFullBaseAddress();  
		//RegNumber newBase = new RegNumber(ExtParameters.getLeafBaseAddress());  
		//newBase.setVectorLen(ExtParameters.getLeafAddressSize());
		//newBase.add(regProperties.getBaseAddress());
		if (extIf.hasAddress) {
			//decodes.get(regProperties.getBaseName() + " (external BBV5)", topBackboneAddrName + " = " + 
			//    newBase.toFormat(NumBase.Hex, NumFormat.Verilog) + "& " + decodeToHwAddrName + ";");    
			this.addWireAssign(topBackboneAddrName + " = " + newBase.toFormat(NumBase.Hex, NumFormat.Verilog) + " | (" + extIf.decodeToHwAddrName + " << " + addrInstProperties.getExtLowBit() + ");");
			//System.out.println("SystemVerilogBuilder generateExternalInterface_BBV5:base address=" + regProperties.getBaseAddress());
		}
		else 
			this.addWireAssign(topBackboneAddrName + " = " + newBase.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");

		
		// if size of max pio transaction is greater than one word need to set transaction size/retry info 
		int regWords = addrInstProperties.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		//System.out.println("SystemVerilogBuilder generateExternalInterface_BBV5: regwords=" + regWords + ", bits=" + regWordBits);
		if (extIf.hasSize) {
			this.addWireAssign(topBackboneWrWidthName + " = 4'b0 | " + extIf.decodeToHwTransactionSizeName + ";");
			// create inbound size signal
			this.addVectorWire(extIf.hwToDecodeTransactionSizeName, 0, regWordBits);
			this.addWireAssign(extIf.hwToDecodeTransactionSizeName + " = " + backboneTopWidthName + SystemVerilogSignal.genRefArrayString(0, regWordBits) + ";");
		}
		else
			this.addWireAssign(topBackboneWrWidthName + " = 4'b" + (regWords-1) + ";");

		// now create state machine vars
		String bbStateName = "bb_" + addrInstProperties.getBaseName() + "_state";                      
		String bbStateNextName = "bb_" + addrInstProperties.getBaseName() + "_state_next";                      
		String bbWordCntName = "bb_" + addrInstProperties.getBaseName() + "_word_cnt";                      
		String bbWordCntNextName = "bb_" + addrInstProperties.getBaseName() + "_word_cnt_next";                      
		String bbWaitCntName = "bb_" + addrInstProperties.getBaseName() + "_wait_cnt";                      
		String bbWaitCntNextName = "bb_" + addrInstProperties.getBaseName() + "_wait_cnt_next";                      
		String bbRdAccumName = "bb_" + addrInstProperties.getBaseName() + "_rdata_accum";                      
		String bbRdAccumNextName = "bb_" + addrInstProperties.getBaseName() + "_rdata_accum_next";                      
		String bbTimeoutCntName = "bb_" + addrInstProperties.getBaseName() + "_timeout_cnt";                      
		String bbTimeoutCntNextName = "bb_" + addrInstProperties.getBaseName() + "_timeout_cnt_next";                      

		String groupName = addrInstProperties.getBaseName() + " bbv5 i/f";
		int stateBits = 3;
		this.addVectorReg(bbStateName, 0, stateBits);  
		this.addVectorReg(bbStateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), bbStateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  bbStateName + " <= #1  " + bbStateNextName + ";");  
		this.addScalarReg(topBackboneReqName);  
		this.addScalarReg(topBackboneRdName);  
		this.addScalarReg(topBackboneWrDvldName);  
		this.addVectorReg(topBackboneWrDataName, 0, 32); 
		
		// add read data accumulate reg
		this.addVectorReg(bbRdAccumName, 0, addrInstProperties.getMaxRegWidth());  
		this.addVectorReg(bbRdAccumNextName, 0, addrInstProperties.getMaxRegWidth());  
		this.addRegAssign(groupName,  bbRdAccumName + " <= #1  " + bbRdAccumNextName + ";");  
		this.addWireAssign(extIf.hwToDecodeName + " = " + bbRdAccumName + ";");

		// if wide regs in ext region, need word count
		if (regWordBits > 0) {
			this.addVectorReg(bbWordCntName, 0, regWordBits);  
			this.addVectorReg(bbWordCntNextName, 0, regWordBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), bbWordCntName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  bbWordCntName + " <= #1  " + bbWordCntNextName + ";");  			
		}
		
		// if an 8 bit ring, need to count wait cycles
		boolean is8bit = (addrInstProperties.getExternalType().getParm("width") == 8);
		if (is8bit) {
			this.addVectorReg(bbWaitCntName, 0, 2);  
			this.addVectorReg(bbWaitCntNextName, 0, 2);  
			this.addResetAssign(groupName, builder.getDefaultReset(), bbWaitCntName + " <= #1  2'b0;");  
			this.addRegAssign(groupName,  bbWaitCntName + " <= #1  " + bbWaitCntNextName + ";");  			
		}
		
		// timeout counter
		this.addVectorReg(bbTimeoutCntName, 0, backboneTopTimeoutBits);
		this.addVectorReg(bbTimeoutCntNextName, 0, backboneTopTimeoutBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), bbTimeoutCntName + " <= #1  " + backboneTopTimeoutBits + "'b0;");  
		this.addRegAssign(groupName,  bbTimeoutCntName + " <= #1  " + bbTimeoutCntNextName + ";");  			
		
		// state machine init values
		this.addCombinAssign(groupName,  bbStateNextName + " = " + bbStateName + ";");  
		this.addCombinAssign(groupName,  topBackboneReqName + " =  1'b0;");  
		this.addCombinAssign(groupName,  topBackboneRdName + " =  1'b0;");  
		this.addCombinAssign(groupName,  topBackboneWrDvldName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  extIf.hwToDecodeAckName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  extIf.hwToDecodeNackName + " =  1'b0;"); 
		// put upper addr bits on wr data by default
		this.addCombinAssign(groupName,  topBackboneWrDataName + " = " + topBackboneAddrName + SystemVerilogSignal.genRefArrayString(ExtParameters.getLeafAddressSize()-32, 32) + ";"); 
		if (regWordBits > 0) {
			this.addCombinAssign(groupName,  bbWordCntNextName + " = "  + bbWordCntName + ";");
			//this.addCombinAssign(groupName,  bbRdAccumNextName + " = {" + (regProperties.getRegWidth()-32) + "'b0, " + backboneTopDataName + "};"); 
		}
		//else
	    this.addCombinAssign(groupName,  bbRdAccumNextName + " = " + bbRdAccumName + ";"); 
			
		if (is8bit)
			this.addCombinAssign(groupName,  bbWaitCntNextName + " =  2'b0;"); 
		this.addCombinAssign(groupName,  bbTimeoutCntNextName + " =  " + backboneTopTimeoutBits + "'b0;"); 

		// state machine
		String IDLE = stateBits + "'h0"; 
		String CMD_WRITE = stateBits + "'h1"; 
		String CMD_WAIT = stateBits + "'h2"; 
		String RES_WAIT = stateBits + "'h3";
		String RES_READ = stateBits + "'h4";
		String RES_DONE_ACK = stateBits + "'h5";
		String RES_DONE_NACK = stateBits + "'h6";
		
		this.addCombinAssign(groupName, "case (" + bbStateName + ")"); 
		
		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");
		if (regWordBits > 0)
			this.addCombinAssign(groupName,  "      " + bbWordCntNextName + " = "  + regWordBits + "'b0;"); // reset word count
		this.addCombinAssign(groupName, "      if (" + extIf.decodeToHwReName + ") begin");  
		this.addCombinAssign(groupName, "        " + topBackboneReqName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + topBackboneRdName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + bbStateNextName + " = " + RES_WAIT + ";");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "      else if (" + extIf.decodeToHwWeName + ") begin");  
		//this.addCombinAssign(groupName, "        " + topBackboneReqName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + bbStateNextName + " = " + CMD_WRITE + ";");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "    end"); 

		// CMD_WRITE
		this.addCombinAssign(groupName, "  " + CMD_WRITE + ": begin // CMD_WRITE"); 
		this.addCombinAssign(groupName, "      " + topBackboneReqName + " =  1'b1;"); 
		if (regWordBits > 0) {  // select the current write word 
			for (int word=0; word < regWords; word++) {
				String prefix = (word==0) ? "" : "else ";
				this.addCombinAssign(groupName, "      " + prefix + "if (" + bbWordCntName + " == " + regWordBits + "'d" + word + ")");  
				this.addCombinAssign(groupName, "        " + topBackboneWrDataName + " = " + extIf.decodeToHwName + "[" + (word*32 + 31) + ":" + (word*32) + "];"); 
			}
		}
		else  // otherwise just use lower 32b
			this.addCombinAssign(groupName, "      " + topBackboneWrDataName + " = " + extIf.decodeToHwName + "[31:0];"); 
			
		this.addCombinAssign(groupName, "      " + topBackboneWrDvldName + " =  1'b1;");  
		this.addCombinAssign(groupName, "      " + bbStateNextName + " = " + CMD_WAIT + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// CMD_WAIT
		this.addCombinAssign(groupName, "  " + CMD_WAIT + ": begin // CMD_WAIT");
		String waitPrefix = "";
		if (is8bit) {
			this.addCombinAssign(groupName, "      " + bbWaitCntNextName + " = " + bbWaitCntName + " + 2'b1;");
			this.addCombinAssign(groupName, "      " + "if ("  + bbWaitCntName + " == 2'h2) begin");
			waitPrefix = "  ";
		}
		if (regWordBits > 0) { // if not last word back to CMD_WRITE and bump the wr word count
			this.addCombinAssign(groupName, "      " + waitPrefix + bbWordCntNextName + " = " + bbWordCntName + " + " + regWordBits + "'b1;");
			this.addCombinAssign(groupName, "      " + waitPrefix + "if (" + bbWordCntName + " == " + topBackboneWrWidthName + ") begin");  
			this.addCombinAssign(groupName, "        " + waitPrefix + bbStateNextName + " = " + RES_WAIT + ";");  
			this.addCombinAssign(groupName, "      " + waitPrefix + "end");  
			this.addCombinAssign(groupName, "      " + waitPrefix + "else begin");  
			this.addCombinAssign(groupName, "        " + topBackboneReqName + " =  1'b1;"); // req stays high until last dvld
			this.addCombinAssign(groupName, "        " + waitPrefix + bbStateNextName + " = " + CMD_WRITE + ";");  
			this.addCombinAssign(groupName, "      " + waitPrefix + "end");  
		}
		else {
			this.addCombinAssign(groupName, "      " + waitPrefix + bbStateNextName + " = " + RES_WAIT + ";");  			
		}
		if (is8bit) 
			this.addCombinAssign(groupName, "      end");
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_WAIT
		this.addCombinAssign(groupName, "  " + RES_WAIT + ": begin  // RES_WAIT");
		this.addCombinAssign(groupName, "      " + topBackboneRdName + " = " + extIf.decodeToHwReName + ";"); // assert if rd  
		this.addCombinAssign(groupName, "      " + "if ("  + bbTimeoutCntName + " != " + backboneTopTimeoutValue + ")"); 
		this.addCombinAssign(groupName, "        " + bbTimeoutCntNextName + " = " + bbTimeoutCntName + " + " + backboneTopTimeoutBits + "'b1;");  // run timeout counter
		if (regWordBits > 0)
			this.addCombinAssign(groupName,  bbWordCntNextName + " = "  + regWordBits + "'b0;"); // reset word count
		// if a write or nack/error/retry then we're done else wait for read data
		this.addCombinAssign(groupName, "      " + "if (" + backboneTopReqName + ") begin");  
		this.addCombinAssign(groupName, "        " + "if (" + backboneTopNack + ")");  
		this.addCombinAssign(groupName, "          " + bbStateNextName + " = " + RES_DONE_NACK + ";");  
		this.addCombinAssign(groupName, "        " + "else if (" + extIf.decodeToHwWeName + " | " + backboneTopWrRetry + ")");  
		this.addCombinAssign(groupName, "          " + bbStateNextName + " = " + RES_DONE_ACK + ";");  
		this.addCombinAssign(groupName, "        " + "else");  
		this.addCombinAssign(groupName, "          " + bbStateNextName + " = " + RES_READ + ";");  
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "      " + "else if ("  + bbTimeoutCntName + " == " + backboneTopTimeoutValue + ")"); 
		this.addCombinAssign(groupName, "        " + bbStateNextName + " = " + RES_DONE_NACK + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_READ
		this.addCombinAssign(groupName, "  " + RES_READ + ": begin  // RES_READ");  
		this.addCombinAssign(groupName, "      " + topBackboneRdName + " =  1'b1;");  
		this.addCombinAssign(groupName, "      " + "if ("  + bbTimeoutCntName + " != " + backboneTopTimeoutValue + ")"); 
		this.addCombinAssign(groupName, "        " + bbTimeoutCntNextName + " = " + bbTimeoutCntName + " + " + backboneTopTimeoutBits + "'b1;");  // run timeout counter
		this.addCombinAssign(groupName, "      " + "if (" + /* backboneTopReqName + " && " +*/ backboneTopDvldName + ") begin");  
		if (regWordBits > 0) { // if a wide reg then accumulate rd reg and bump the word count
			this.addCombinAssign(groupName, "        " + bbWordCntNextName + " = " + bbWordCntName + " + " + regWordBits + "'b1;");
			for (int word=0; word < regWords; word++) {
				String prefix = (word==0) ? "" : "else ";
				this.addCombinAssign(groupName, "        " + prefix + "if (" + bbWordCntName + " == " + regWordBits + "'d" + word + ")");  
				this.addCombinAssign(groupName, "          " + bbRdAccumNextName + "[" + (word*32 + 31) + ":" + (word*32) + "] = " + backboneTopDataName + ";"); 
			}
			this.addCombinAssign(groupName, "        " + "if (" + bbWordCntName + " == " + backboneTopWidthName + ")");  
			this.addCombinAssign(groupName, "          " + bbStateNextName + " = " + RES_DONE_ACK + ";");  
		}
		else {
			this.addCombinAssign(groupName, "        " + bbRdAccumNextName + " = " + backboneTopDataName + ";"); 
			this.addCombinAssign(groupName, "        " + bbStateNextName + " = " + RES_DONE_ACK + ";");  
		}
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "      " + "else if ("  + bbTimeoutCntName + " == " + backboneTopTimeoutValue + ")"); 
		this.addCombinAssign(groupName, "        " + bbStateNextName + " = " + RES_DONE_NACK + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_ACK
		this.addCombinAssign(groupName, "  " + RES_DONE_ACK + ": begin // RES_DONE_ACK");
		this.addCombinAssign(groupName, "      " + extIf.hwToDecodeAckName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + bbStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_NACK
		this.addCombinAssign(groupName, "  " + RES_DONE_NACK + ": begin // RES_DONE_NACK");
		this.addCombinAssign(groupName, "      " + extIf.hwToDecodeNackName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + bbStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + bbStateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 
	
	}

	/** generate SRAM external interface shim logic */  
	public void generateExternalInterface_SRAM(AddressableInstanceProperties addrInstProperties) {
		//Jrdl.warnMessage("SystemVerilogBuilder gen_SRAM, ext type=" + regProperties.getExternalType() + ", id=" + regProperties.getId());
		// generate common external interface constructs
		ExternalInterfaceInfo extIf = generateBaseExternalInterface(addrInstProperties);
		
		// create incoming signals (default IOs that now need to be set)
		this.addVectorWire(extIf.hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		this.addScalarReg(extIf.hwToDecodeAckName);
		this.addScalarReg(extIf.hwToDecodeNackName);

		// create module IOs
		String decodeToSrRdName = "d2sr_" + addrInstProperties.getBaseName() + "_rd";                      
		String decodeToSrWrName = "d2sr_" + addrInstProperties.getBaseName() + "_wr";                      
		String decodeToSrWrDataName = "d2sr_" + addrInstProperties.getBaseName() + "_wr_data";                      
		String decodeToSrWrEnableName = "d2sr_" + addrInstProperties.getBaseName() + "_wr_enable";                      
		String decodeToSrAddrName = "d2sr_" + addrInstProperties.getBaseName() + "_addr"; 
		
		String srToDecodeAck = "sr2d_" + addrInstProperties.getBaseName() + "_ack";                      
		String srToDecodeNack = "sr2d_" + addrInstProperties.getBaseName() + "_nack";                      
		String srToDecodeRdDataName = "sr2d_" + addrInstProperties.getBaseName() + "_rd_data";  
		
		//  inputs
		this.addSimpleScalarFrom(SystemVerilogBuilder.HW, srToDecodeAck);   // ack
		this.addSimpleScalarFrom(SystemVerilogBuilder.HW, srToDecodeNack);   // nack
		this.addSimpleVectorFrom(SystemVerilogBuilder.HW, srToDecodeRdDataName, 0, addrInstProperties.getMaxRegWidth());   // read data  
		
		//  outputs
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, decodeToSrRdName);  // read pulse    
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, decodeToSrWrName);  // write pulse  
		this.addSimpleVectorTo(SystemVerilogBuilder.HW, decodeToSrWrDataName, 0, addrInstProperties.getMaxRegWidth());    // write data  
		if (hasWriteEnables()) this.addSimpleVectorTo(SystemVerilogBuilder.HW, decodeToSrWrEnableName, 0, addrInstProperties.getWriteEnableWidth());    // write data  
		this.addScalarReg(decodeToSrRdName);
		this.addScalarReg(decodeToSrWrName);

		// compute address size
		int regWordBits = Utils.getBits(addrInstProperties.getMaxRegWordWidth());
		int srAddrBits = addrInstProperties.getExtAddressWidth() - regWordBits;
		String srRegSize = regWordBits + "'d" + (addrInstProperties.getMaxRegWordWidth() - 1);
		//System.out.println("SystemVerilogBuilder gen.._SRAM: ext addr width=" + regProperties.getExtAddressWidth());
		//System.out.println("SystemVerilogBuilder gen.._SRAM: reg word width=" + regProperties.getRegWordWidth() + ", bits=" + Utils.getBits(regProperties.getRegWordWidth()));

		// check that at least 2 regs in SRAM address space
		if (srAddrBits < 1) Ordt.errorExit("External SRAM-type regfile must have at least 2 registers, inst=" + addrInstProperties.getInstancePath());
		// create address output
		this.addSimpleVectorTo(SystemVerilogBuilder.HW, decodeToSrAddrName, regWordBits + addrInstProperties.getExtLowBit(), srAddrBits);    // address  
		
		// assign wr_data and addr outputs
		this.addWireAssign(decodeToSrWrDataName + " = " + extIf.decodeToHwName +  ";");
		if (hasWriteEnables()) this.addWireAssign(decodeToSrWrEnableName + " = " + extIf.decodeToHwEnableName +  ";");
		this.addWireAssign(decodeToSrAddrName + " = " + extIf.decodeToHwAddrName + SystemVerilogSignal.genRefArrayString(regWordBits + addrInstProperties.getExtLowBit(), srAddrBits) + ";");

		// generate valid_size by comparing decodeToHwTransactionSizeName value with regWordWidth - 1
		String srValidSizeName = "dsr_" + addrInstProperties.getBaseName() + "_valid_size"; 
		this.addScalarWire(srValidSizeName);
		if ((addrInstProperties.getMaxRegWordWidth() > 1) && !addrInstProperties.isSingleExtReg()) {
			this.addWireAssign(srValidSizeName + " = (" + extIf.decodeToHwTransactionSizeName + " == " + srRegSize + ");");
		}
		else
			this.addWireAssign(srValidSizeName + " = 1'b1;");  // not a wide reg space, so always valid
		
		// set fixed external reg size
		if ((addrInstProperties.getMaxRegWordWidth() > 1) && !addrInstProperties.isSingleExtReg()) {
			this.addVectorWire(extIf.hwToDecodeTransactionSizeName, 0, regWordBits);
			this.addWireAssign(extIf.hwToDecodeTransactionSizeName + " = " + srRegSize + ";");
		}
		
		// set rd_data input
		this.addWireAssign(extIf.hwToDecodeName + " = " + srToDecodeRdDataName +  ";");

		// generate valid_addr - compare decodeToSrAddrName to max address of space
		String srValidAddrName = "dsr_" + addrInstProperties.getBaseName() + "_valid_addr"; 
		this.addScalarWire(srValidAddrName);
		RegNumber extMaxAddr = builder.getExternalRegBytes().getSubVector(addrInstProperties.getExtLowBit(), addrInstProperties.getExtAddressWidth());
		if (extMaxAddr.isNonZero()) // if max addr is less than allowed range, add compare stmt
			this.addWireAssign(srValidAddrName + " = (" + extIf.decodeToHwAddrName + " < " + extMaxAddr.toFormat(NumBase.Hex, NumFormat.Verilog) + ");");
		else
			this.addWireAssign(srValidAddrName + " = 1'b1;");
		//System.out.println("SystemVerilogBuilder gen.._SRAM: ext size (Bytes)=" + getExternalRegBytes() + ", extMax=" + extMaxAddr.toFormat(NumBase.Hex, NumFormat.Verilog));  

		// generate rd/wr pulses if valid_size and valid_addr
		// generate ack/nack based on address compare, valid_size, valid_addr, rd_ack, and wr_status
		String srStateName = "sr_" + addrInstProperties.getBaseName() + "_state";                      
		String groupName = addrInstProperties.getBaseName() + " sr i/f";
		generateAckNackMachine(srStateName, groupName, true,
				extIf.decodeToHwReName, extIf.decodeToHwWeName, srValidAddrName, srValidAddrName, srValidSizeName, srToDecodeAck, srToDecodeNack, 
				decodeToSrRdName, decodeToSrWrName, extIf.hwToDecodeAckName, extIf.hwToDecodeNackName);		
	}

	/** generate state machine to generate acks/nacks internally based on transaction size, address being accessed.  
	 *  Allows simplification of the external interface.
	 * 
	 * @param ackStateName - name of state variable
	 * @param groupName - group name of generated statements
	 * @param pulseRdWr - if true read/write signals will only pulse for a cycle
	 * @param readIn - read active input
	 * @param writeIn - write active input
	 * @param validReadCondition - condition string, if false read will be nacked
	 * @param validWriteCondition - condition string, if false write will be nacked
	 * @param validSizeCondition - condition string, if false write will be acked
	 * @param ackIn - ack input from ext
	 * @param nackIn - nack input from ext (null if none)
	 * @param readOut - read active output to ext  (null if none)
	 * @param writeOut - write active output to ext  (null if none)
	 * @param ackOut - ack output
	 * @param nackOut - nack output
	 */
	private void generateAckNackMachine(String ackStateName, String groupName, boolean pulseRdWr, 
			String readIn, String writeIn, String validReadCondition, String validWriteCondition, String validSizeCondition, String ackIn, String nackIn, 
			String readOut, String writeOut, String ackOut, String nackOut) {
		
		String ackStateNextName = ackStateName + "_next";   
		
		// create state machine vars
		int stateBits = 2;
		this.addVectorReg(ackStateName, 0, stateBits);  
		this.addVectorReg(ackStateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), ackStateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  ackStateName + " <= #1  " + ackStateNextName + ";");
		
		// state machine init values
		this.addCombinAssign(groupName,  ackStateNextName + " = " + ackStateName + ";");  
		if (readOut != null) this.addCombinAssign(groupName,  readOut + " =  1'b0;");  
		if (writeOut != null) this.addCombinAssign(groupName,  writeOut + " =  1'b0;");  
		this.addCombinAssign(groupName,  ackOut + " =  1'b0;"); 
		this.addCombinAssign(groupName,  nackOut + " =  1'b0;"); 
		
		// state machine
		String IDLE = stateBits + "'h0"; 
		String ACK_WAIT = stateBits + "'h1"; 
		String NACK = stateBits + "'h2"; 
		String ACK = stateBits + "'h3";
		
		this.addCombinAssign(groupName, "case (" + ackStateName + ")"); 
		
		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");
		this.addCombinAssign(groupName, "      if (" + readIn + ") begin");  
		this.addCombinAssign(groupName, "        if (~" + validReadCondition + ") " + ackStateNextName + " = " + NACK + ";");  
		//this.addCombinAssign(groupName, "        else if (~" + srValidSizeName + ") " + srStateNextName + " = " + ACK + ";");  
		this.addCombinAssign(groupName, "        else begin");  
		if (readOut != null) this.addCombinAssign(groupName, "        " + readOut + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + ackStateNextName + " = " + ACK_WAIT + ";");  
		this.addCombinAssign(groupName, "        end");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "      else if (" + writeIn + ") begin");  
		this.addCombinAssign(groupName, "        if (~" + validWriteCondition + ") " + ackStateNextName + " = " + NACK + ";");  
		this.addCombinAssign(groupName, "        else if (~" + validSizeCondition + ") " + ackStateNextName + " = " + ACK + ";");  
		this.addCombinAssign(groupName, "        else begin");  
		if (writeOut != null) this.addCombinAssign(groupName, "        " + writeOut + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + ackStateNextName + " = " + ACK_WAIT + ";");  
		this.addCombinAssign(groupName, "        end");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "    end"); 
		
		// ACK_WAIT
		this.addCombinAssign(groupName, "  " + ACK_WAIT + ": begin // ACK_WAIT");
		this.addCombinAssign(groupName, "      if (" + ackIn + ") begin");  
		this.addCombinAssign(groupName, "        " + ackOut + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + ackStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "      end");
		if (nackIn != null) {
		  this.addCombinAssign(groupName, "      else if (" + nackIn + ") begin");  
		  this.addCombinAssign(groupName, "        " + nackOut + " =  1'b1;");  
		  this.addCombinAssign(groupName, "        " + ackStateNextName + " = " + IDLE + ";");  
		  this.addCombinAssign(groupName, "      end");  
		}
		if (!pulseRdWr && (readOut != null)) this.addCombinAssign(groupName, "        " + readOut + " = " + readIn + ";");  
		if (!pulseRdWr && (writeOut != null)) this.addCombinAssign(groupName, "        " + writeOut + " = " + writeIn + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// NACK
		this.addCombinAssign(groupName, "  " + NACK + ": begin // NACK");
		this.addCombinAssign(groupName, "      " + nackOut + " =  1'b1;");  
		this.addCombinAssign(groupName, "      " + ackStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// ACK
		this.addCombinAssign(groupName, "  " + ACK + ": begin // ACK");
		this.addCombinAssign(groupName, "      " + ackOut + " =  1'b1;");  
		this.addCombinAssign(groupName, "      " + ackStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + ackStateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 
		
	}

	/** generate SERIAL8 external interface shim logic */  
	public void generateExternalInterface_SERIAL8(AddressableInstanceProperties addrInstProperties) {   
		//Jrdl.warnMessage("SystemVerilogBuilder gen_SERIAL8, ext type=" + regProperties.getExternalType()  + ", id=" + regProperties.getId());

		// check for valid serial8 width
		int transactionsInWord = ExtParameters.getMinDataSize()/8;
		boolean multiTransactionWord = (transactionsInWord>1);
		if (!multiTransactionWord) Ordt.errorExit("Serial8 external region (" + addrInstProperties.getInstancePath() + ") does not support 8b max width regions.  Use parallel interface instead.");
		
		// generate common external interface constructs
		ExternalInterfaceInfo extIf = generateBaseExternalInterface(addrInstProperties);

		// create incoming signals (default IOs that now need to be set)
		this.addVectorWire(extIf.hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		this.addScalarReg(extIf.hwToDecodeAckName);
		this.addScalarReg(extIf.hwToDecodeNackName);
		
		// create module IOs
		String serial8CmdValidName = "d2s_" + addrInstProperties.getBaseName() + "_cmd_valid";                      
		String serial8CmdDataName = "d2s_" + addrInstProperties.getBaseName() + "_cmd_data";                      
		
		String serial8ResValidName = "s2d_" + addrInstProperties.getBaseName() + "_res_valid";                      
		String serial8ResDataName = "s2d_" + addrInstProperties.getBaseName() + "_res_data";                      
		
		//  outputs
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, serial8CmdValidName);     // stays high while all cmd addr/data/cntl xferred 
		this.addSimpleVectorTo(SystemVerilogBuilder.HW, serial8CmdDataName, 0, 8);  

		// inputs  
		this.addSimpleScalarFrom(SystemVerilogBuilder.HW, serial8ResValidName);     // stays high while all res cntl/data xferred
		this.addSimpleVectorFrom(SystemVerilogBuilder.HW, serial8ResDataName, 0, 8);     
		
		// calculate number of 8b xfers required for address (same for all transctions to this i/f)
		int addrXferCount = 0;
		if ( extIf.hasAddress) {
			addrXferCount = (int) Math.ceil(addrInstProperties.getExtAddressWidth()/8.0);
			//System.out.println("SystemVerilogBuilder generateExternalInterface_Serial8: addr width=" + regProperties.getExtAddressWidth() + ", addr count=" + addrXferCount);
		}
		
		// compute max transaction size in 32b words and number of bits to represent (4 max) 
		int regWords = addrInstProperties.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = extIf.hasSize;  // if transaction sizes need to be sent/received
		
		// now create state machine vars
		String s8StateName = "s8_" + addrInstProperties.getBaseName() + "_state";                      
		String s8StateNextName = "s8_" + addrInstProperties.getBaseName() + "_state_next";                      
		String s8AddrCntName = "s8_" + addrInstProperties.getBaseName() + "_addr_cnt";                      
		String s8AddrCntNextName = "s8_" + addrInstProperties.getBaseName() + "_addr_cnt_next";                      
		String s8DataCntName = "s8_" + addrInstProperties.getBaseName() + "_data_cnt";                      
		String s8DataCntNextName = "s8_" + addrInstProperties.getBaseName() + "_data_cnt_next"; 

		String groupName = addrInstProperties.getBaseName() + " serial8 i/f";  
		int stateBits = 3;
		this.addVectorReg(s8StateName, 0, stateBits);  
		this.addVectorReg(s8StateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), s8StateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  s8StateName + " <= #1  " + s8StateNextName + ";");  

		// create delayed cmd signals
		int delayCount = addrInstProperties.getExternalType().getParm("delay");
		String [] cmdValidDlyName = new String [delayCount+1];
		String [] cmdDataDlyName = new String [delayCount+1];
		for (int idx=0; idx<delayCount+1; idx++) {
			cmdValidDlyName[idx] = "s8_" + addrInstProperties.getBaseName() + "_cmdValid_dly" + idx;
			cmdDataDlyName[idx] = "s8_" + addrInstProperties.getBaseName() + "_cmdData_dly" + idx;
		}
		this.addScalarReg(cmdValidDlyName[0]);  // cmd delay 0 is set in state machine
		this.addVectorReg(cmdDataDlyName[0], 0, 8); 
		for (int idx=1; idx<delayCount+1; idx++) {
			this.addScalarReg(cmdValidDlyName[idx]);  
			this.addVectorReg(cmdDataDlyName[idx], 0, 8); 
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdDataDlyName[idx] + " <= #1  8'b0;");  
			this.addRegAssign(groupName,  cmdValidDlyName[idx] + " <= #1  " + cmdValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  cmdDataDlyName[idx] + " <= #1  " + cmdDataDlyName[idx-1] + ";");  
		}
        // assign s8 outputs outputs to delayed versions 
		this.addScalarWire(serial8CmdValidName);  
		this.addVectorWire(serial8CmdDataName, 0, 8); 
		this.addWireAssign(serial8CmdValidName + " = " + cmdValidDlyName[delayCount] + ";");
		this.addWireAssign(serial8CmdDataName + " = " + cmdDataDlyName[delayCount] + ";");

		// create delayed res signals
		String [] resValidDlyName = new String [delayCount+1];
		String [] resDataDlyName = new String [delayCount+1];
		for (int idx=0; idx<delayCount+1; idx++) {
			resValidDlyName[idx] = "s8_" + addrInstProperties.getBaseName() + "_resValid_dly" + idx;
			resDataDlyName[idx] = "s8_" + addrInstProperties.getBaseName() + "_resData_dly" + idx;
		}
		this.addScalarWire(resValidDlyName[0]);  // res delay 0 is set from IO
		this.addVectorWire(resDataDlyName[0], 0, 8); 
		for (int idx=1; idx<delayCount+1; idx++) {
			this.addScalarReg(resValidDlyName[idx]);  
			this.addVectorReg(resDataDlyName[idx], 0, 8); 
			this.addResetAssign(groupName, builder.getDefaultReset(), resValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), resDataDlyName[idx] + " <= #1  8'b0;");  
			this.addRegAssign(groupName,  resValidDlyName[idx] + " <= #1  " + resValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  resDataDlyName[idx] + " <= #1  " + resDataDlyName[idx-1] + ";");  
		}
        // assign s8 inputs to predelay 
		this.addWireAssign(resValidDlyName[0] + " = " + serial8ResValidName + ";");  
		this.addWireAssign(resDataDlyName[0] + " = " + serial8ResDataName + ";");
		
		// add read data accumulate reg
		String s8RdAccumName = "s8_" + addrInstProperties.getBaseName() + "_rdata_accum";                      
		String s8RdAccumNextName = "s8_" + addrInstProperties.getBaseName() + "_rdata_accum_next";                      
		this.addVectorReg(s8RdAccumName, 0, addrInstProperties.getMaxRegWidth());  
		this.addVectorReg(s8RdAccumNextName, 0, addrInstProperties.getMaxRegWidth());  
		this.addRegAssign(groupName,  s8RdAccumName + " <= #1  " + s8RdAccumNextName + ";");  
		this.addWireAssign(extIf.hwToDecodeName + " = " + s8RdAccumName + ";");
		
		//  will need to capture res width
		String hwToDecodeTransactionSizeNextName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE) + "_next";  // res size will be set in sm
		if (useTransactionSize) {
			this.addVectorReg(extIf.hwToDecodeTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(hwToDecodeTransactionSizeNextName, 0, regWordBits);  // res size will be set in sm
			this.addResetAssign(groupName, builder.getDefaultReset(), extIf.hwToDecodeTransactionSizeName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  extIf.hwToDecodeTransactionSizeName + " <= #1  " + hwToDecodeTransactionSizeNextName + ";");  
		}

		// will need to capture nack state
		String rxNackCaptureName = addrInstProperties.getFullSignalName(DefSignalType.H2D_NACK) + "_early"; 
		String rxNackCaptureNextName = rxNackCaptureName + "_next"; 
		this.addScalarReg(rxNackCaptureName);  
		this.addScalarReg(rxNackCaptureNextName);  
		this.addResetAssign(groupName, builder.getDefaultReset(), rxNackCaptureName + " <= #1  1'b0;");  
		this.addRegAssign(groupName,  rxNackCaptureName + " <= #1  " + rxNackCaptureNextName + ";");  
		
        // address byte count
		int addrXferCountBits = Utils.getBits(addrXferCount); 
		if (addrXferCountBits > 0) {
			this.addVectorReg(s8AddrCntName, 0, addrXferCountBits);  
			this.addVectorReg(s8AddrCntNextName, 0, addrXferCountBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), s8AddrCntName + " <= #1  " + addrXferCountBits + "'b0;");  
			this.addRegAssign(groupName,  s8AddrCntName + " <= #1  " + s8AddrCntNextName + ";");  			
		}
		
		// data byte count
		int maxDataXferCount = regWords * ExtParameters.getMinDataSize()/8;
		int maxDataXferCountBits = Utils.getBits(maxDataXferCount);

		this.addVectorReg(s8DataCntName, 0, maxDataXferCountBits);  
		this.addVectorReg(s8DataCntNextName, 0, maxDataXferCountBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), s8DataCntName + " <= #1  " + maxDataXferCountBits + "'b0;");  
		this.addRegAssign(groupName,  s8DataCntName + " <= #1  " + s8DataCntNextName + ";"); 
		//System.out.println("SystemVerilogBuilder generateExternalInterface_Serial8: max data xfers=" + maxDataXferCount);
				
		// state machine init values
		this.addCombinAssign(groupName,  s8StateNextName + " = " + s8StateName + ";");  
		this.addCombinAssign(groupName,  cmdValidDlyName[0] + " =  1'b0;");  
		this.addCombinAssign(groupName,  cmdDataDlyName[0] + " =  8'b0;");  
		this.addCombinAssign(groupName,  extIf.hwToDecodeAckName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  extIf.hwToDecodeNackName + " =  1'b0;");
		this.addCombinAssign(groupName,  rxNackCaptureNextName + " = " + rxNackCaptureName + ";");  
		this.addCombinAssign(groupName,  s8RdAccumNextName + " = " + s8RdAccumName + ";");
		// init res size capture
		if (useTransactionSize)
			this.addCombinAssign(groupName,  hwToDecodeTransactionSizeNextName + " = " + extIf.hwToDecodeTransactionSizeName + ";"); 
		// init counter values
		if (addrXferCountBits > 0) {
			this.addCombinAssign(groupName,  s8AddrCntNextName + " = "  + addrXferCountBits + "'b0;");
		}
		this.addCombinAssign(groupName,  s8DataCntNextName + " = "  + maxDataXferCountBits + "'b0;");
			
		// state machine 
		String IDLE = stateBits + "'h0"; 
		String CMD_ADDR = stateBits + "'h1"; 
		String CMD_DATA = stateBits + "'h2"; 
		String RES_WAIT = stateBits + "'h3";
		String RES_READ = stateBits + "'h4";
		String RES_DONE_ACK = stateBits + "'h5";
		String RES_DONE_NACK = stateBits + "'h6";
		
		this.addCombinAssign(groupName, "case (" + s8StateName + ")"); 

		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");
		this.addCombinAssign(groupName, "      " + rxNackCaptureNextName + " = 1'b0;");  
		this.addCombinAssign(groupName, "      " + s8RdAccumNextName + " = " + addrInstProperties.getMaxRegWidth() + "'b0;");
		// go on read or write request
		this.addCombinAssign(groupName, "      if (" + extIf.decodeToHwReName + " | " + extIf.decodeToHwWeName + ") begin");  
		this.addCombinAssign(groupName, "        " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set  
		this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + "[7] = " + extIf.decodeToHwWeName + ";");  // bit 7 = write
		this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + "[6:4] = 3'd" + addrXferCount + ";");  // bits 6:4 = address xfers  
		if (useTransactionSize)
		    this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + " = " + extIf.decodeToHwTransactionSizeName + ";");  // bits 3:0 = transaction size
        // if an address then send it next
		if (addrXferCount > 0) {  
			this.addCombinAssign(groupName, "        " + s8StateNextName + " = " + CMD_ADDR + ";");  
		}
		// otherwise send data if a write or wait for read response
		else {
			this.addCombinAssign(groupName, "        if (" + extIf.decodeToHwWeName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
			this.addCombinAssign(groupName, "        else " + s8StateNextName + " = " + RES_WAIT + ";");  
		}
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "    end"); 
		
        // if an address then add CMD_ADDR state
		if (addrXferCount > 0) {  
			// CMD_ADDR
			this.addCombinAssign(groupName, "  " + CMD_ADDR + ": begin // CMD_ADDR");
			this.addCombinAssign(groupName, "      " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set 
			// if more than one address then bump the count
			if (addrXferCountBits > 0) {  
				this.addCombinAssign(groupName,  "      " + s8AddrCntNextName + " = " + s8AddrCntName + " + " + addrXferCountBits + "'b1;");
				// select address slice
				for (int idx=0; idx<addrXferCount; idx++) {
					String prefix = (idx == 0)? "" : "else ";
					this.addCombinAssign(groupName, "      " + prefix + "if (" + s8AddrCntName + " == " + addrXferCountBits + "'d" + idx + ")");
					int lowbit = idx*8 + addrInstProperties.getExtLowBit();
					int size = ((idx+1)*8 > addrInstProperties.getExtAddressWidth())? addrInstProperties.getExtAddressWidth() - idx*8 : 8;  // last xfer can be smaller
					this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + " = " + extIf.decodeToHwAddrName + SystemVerilogSignal.genRefArrayString(lowbit, size) + ";");
				}
	
				// if addr done, send data if a write or wait for read response 
				this.addCombinAssign(groupName, "      if (" + s8AddrCntName + " == " + addrXferCountBits + "'d" + (addrXferCount-1) + ") begin");
				this.addCombinAssign(groupName, "        if (" + extIf.decodeToHwWeName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "        else " + s8StateNextName + " = " + RES_WAIT + ";");
				this.addCombinAssign(groupName, "      end"); 
			}
			// else a single addr xfer
			else {
				// set address
				this.addCombinAssign(groupName, "      " + cmdDataDlyName[0] + SystemVerilogSignal.genRefArrayString(0, addrInstProperties.getExtAddressWidth()) + " = " + extIf.decodeToHwAddrName  + ";");  
				// next send data if a write or wait for read response 
				this.addCombinAssign(groupName, "      if (" + extIf.decodeToHwWeName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "      else " + s8StateNextName + " = " + RES_WAIT + ";");  
			}
			this.addCombinAssign(groupName, "    end"); 
		}
		
		// CMD_DATA
		this.addCombinAssign(groupName, "  " + CMD_DATA + ": begin // CMD_DATA");
		this.addCombinAssign(groupName, "      " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set 
		//  bump the data count
		this.addCombinAssign(groupName,  "      " + s8DataCntNextName + " = " + s8DataCntName + " + " + maxDataXferCountBits + "'b1;");
		// select the data slice
		for (int idx=0; idx<maxDataXferCount; idx++) {
			String prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "      "+ prefix + "if (" + s8DataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
			this.addCombinAssign(groupName, "        "  + cmdDataDlyName[0] + " = " + extIf.decodeToHwName + SystemVerilogSignal.genRefArrayString(8*idx, 8) + ";");
		}
		// if done, move to res wait
		String finalCntStr = getSerialMaxDataCountStr(useTransactionSize, transactionsInWord, extIf.decodeToHwTransactionSizeName);
		this.addCombinAssign(groupName, "      if (" + s8DataCntName + " == " + finalCntStr + ")");
		this.addCombinAssign(groupName, "        " + s8StateNextName + " = " + RES_WAIT + ";");
		this.addCombinAssign(groupName, "    end"); 
			
		// RES_WAIT
		this.addCombinAssign(groupName, "  " + RES_WAIT + ": begin  // RES_WAIT");
		// first word of response contains ack/nack and xfer size 
		this.addCombinAssign(groupName, "      " + "if (" + resValidDlyName[delayCount] + ") begin");  // save bit7 nack, 3:0 size
		this.addCombinAssign(groupName, "        " + rxNackCaptureNextName + " = " + resDataDlyName[delayCount] + "[7];");  
		if (useTransactionSize) {
			this.addCombinAssign(groupName,  "        " + hwToDecodeTransactionSizeNextName + " = " + resDataDlyName[delayCount] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + ";");  
		}
		// if a read then get the data else ack/nack
		this.addCombinAssign(groupName, "        if (" + extIf.decodeToHwReName + ") " + s8StateNextName + " = " + RES_READ + ";");  
		this.addCombinAssign(groupName, "        else if (" + rxNackCaptureNextName + ") " + s8StateNextName + " = " + RES_DONE_NACK + ";");
		this.addCombinAssign(groupName, "        else "+ s8StateNextName + " = " + RES_DONE_ACK + ";");
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_READ
		this.addCombinAssign(groupName, "  " + RES_READ + ": begin  // RES_READ");  
		//  bump the data count
		this.addCombinAssign(groupName, "      " + s8DataCntNextName + " = " + s8DataCntName + ";");
		// collect the data slice while res valid is asserted  
		this.addCombinAssign(groupName, "      " + "if (" + resValidDlyName[delayCount] + ") begin");
		this.addCombinAssign(groupName, "        " + s8DataCntNextName + " = " + s8DataCntName + " + " + maxDataXferCountBits + "'b1;");
		for (int idx=0; idx<maxDataXferCount; idx++) {
			String prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "        "+ prefix + "if (" + s8DataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
			this.addCombinAssign(groupName, "          " + s8RdAccumNextName + SystemVerilogSignal.genRefArrayString(8*idx, 8) + " = " + resDataDlyName[delayCount] + ";");
		}
		this.addCombinAssign(groupName, "      end"); 
		// otherwise move to ack or nack done
		this.addCombinAssign(groupName, "      " + "else if (" + rxNackCaptureName + ") " + s8StateNextName + " = " + RES_DONE_NACK + ";");
		finalCntStr = getSerialMaxDataCountStr(useTransactionSize, transactionsInWord, extIf.hwToDecodeTransactionSizeName);
		this.addCombinAssign(groupName, "      if (" + s8DataCntName + " == " + finalCntStr + ") " + s8StateNextName + " = " + RES_DONE_ACK + ";");
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_ACK
		this.addCombinAssign(groupName, "  " + RES_DONE_ACK + ": begin // RES_DONE_ACK");
		this.addCombinAssign(groupName, "      " + extIf.hwToDecodeAckName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + s8StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_NACK
		this.addCombinAssign(groupName, "  " + RES_DONE_NACK + ": begin // RES_DONE_NACK");
		this.addCombinAssign(groupName, "      " + extIf.hwToDecodeNackName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + s8StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + s8StateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 				
	}

	/** generate RING external interface shim logic */    
	public void generateExternalInterface_RING(int ringWidth, AddressableInstanceProperties addrInstProperties) {
		//Jrdl.warnMessage("SystemVerilogBuilder gen_RING, ext type=" + regProperties.getExternalType()  + ", id=" + regProperties.getId());

		// check for valid ring width
		int transactionsInWord = ExtParameters.getMinDataSize()/ringWidth;
		if (ringWidth > ExtParameters.getMinDataSize()) Ordt.errorExit(ringWidth + "b ring external region (" + addrInstProperties.getInstancePath() + ") does not support min data size less than " + ringWidth + "b.");
		
		// generate common external interface constructs
		ExternalInterfaceInfo extIf = generateBaseExternalInterface(addrInstProperties);

		// create incoming signals (default IOs that now need to be set)
		this.addVectorWire(extIf.hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		this.addScalarReg(extIf.hwToDecodeAckName);
		this.addScalarReg(extIf.hwToDecodeNackName);
		
		// create module IOs
		String ringCmdValidName = "d2r_" + addrInstProperties.getBaseName() + "_cmd_valid";                      
		String ringCmdDataName = "d2r_" + addrInstProperties.getBaseName() + "_cmd_data";                      
		
		String ringResValidName = "r2d_" + addrInstProperties.getBaseName() + "_res_valid";                      
		String ringResDataName = "r2d_" + addrInstProperties.getBaseName() + "_res_data";                      
		
		//  outputs
		this.addSimpleScalarTo(SystemVerilogBuilder.HW, ringCmdValidName);     // stays high while all cmd addr/data/cntl xferred 
		this.addSimpleVectorTo(SystemVerilogBuilder.HW, ringCmdDataName, 0, ringWidth);  

		// inputs  
		this.addSimpleScalarFrom(SystemVerilogBuilder.HW, ringResValidName);     // stays high while all res cntl/data xferred
		this.addSimpleVectorFrom(SystemVerilogBuilder.HW, ringResDataName, 0, ringWidth);     

        // set field info according to ringWidth
		//                addr bits      offset         data size bits   r/w bit   ack/nack
		// ring  8 -    2 = max 3 = 24b  + 0 = 24b     3 = max 8 = 256b     5        7/6
		// ring 16 -    2 = max 3 = 48b  + 0 = 48b     4 = max 16 = 512b    7        15/14
		// ring 32 -    2 = max 3 = 96b  + 16 = 112b   4 = max 16 = 512b    7        15/14
		
		// defaults (16b ring)
		int addrBitIndex = 4;      // low index for address
		int rwBitIndex = 7;        // read/write index 
		int ackBitIndex = 14;      // index for ack
		int nackBitIndex = 15;      // index for nack 
		
		int maxAddrXferCount = 3;  // max 3 addr xfers allowed in all width cases
		int addrOffset = 0;  // default to no address bits in control word
		int maxRegWordBits = 4;    // max 4 32b words allowed
		
		// overrides if an alternate width
		if (ringWidth == 8) {
			// shift fields to fit in 8b
			addrBitIndex = 3;   // low index for address
			rwBitIndex = 5;        // read/write index 
			ackBitIndex = 6;      // index for ack
			nackBitIndex = 7;      // index for nack 
			maxRegWordBits = 3;    // limit to 3 32b words allowed
		}
		else if (ringWidth == 32) {
			addrOffset = 16;  // upper 16b are address bits in control word 
		}
		
		// further limit max number of address transfers by the full address range
		int maxAddrBits = ExtParameters.getLeafAddressSize() - builder.getAddressLowBit(); 
		int nonCntlAddrBits = (maxAddrBits > addrOffset)? maxAddrBits - addrOffset : 0;  // compute address bits not in cntl word
		int fullAddressMaxXfers = (int) Math.ceil(nonCntlAddrBits/(double) ringWidth);
		maxAddrXferCount = fullAddressMaxXfers < maxAddrXferCount ? fullAddressMaxXfers : maxAddrXferCount;
				
		// calculate number of xfers required for address (same for all transctions to this i/f)
		int addrXferCount = 0;
		int localAddrBits = addrInstProperties.getExtAddressWidth(); 
		if ( extIf.hasAddress) {
			nonCntlAddrBits = (localAddrBits > addrOffset)? localAddrBits - addrOffset : 0;  // compute address bits not in cntl word
			addrXferCount = (int) Math.ceil(nonCntlAddrBits/(double) ringWidth);
			//System.out.println("SystemVerilogBuilder generateExternalInterface_ring: addr width=" + regProperties.getExtAddressWidth() + ", addr count=" + addrXferCount);
			// error if insufficient bits to address this region 
			if (addrXferCount > maxAddrXferCount) 
				Ordt.errorExit("Insufficient address bits to access " + ringWidth + "b ring external region " + addrInstProperties.getInstancePath());
		}
		int addrXferCountBits = Utils.getBits(addrXferCount);

		// compute max transaction size in 32b words and number of bits to represent (4 max) 
		int regWords = addrInstProperties.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = extIf.hasSize;  // if transaction sizes need to be sent/received
		// error if max reg size is too big for this region 
		if (regWordBits > maxRegWordBits) 
			Ordt.errorExit("Max register width (" + addrInstProperties.getMaxRegWidth() + ") is too large for " + ringWidth + "b ring external region " + addrInstProperties.getInstancePath());
		
		// now create state machine vars
		String ringStateName = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_state";                      
		String ringStateNextName = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_state_next";                      
		String ringAddrCntName = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_addr_cnt";                      
		String ringAddrCntNextName = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_addr_cnt_next";                      
		String ringDataCntName = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_data_cnt";                      
		String ringDataCntNextName = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_data_cnt_next"; 

		String groupName = addrInstProperties.getBaseName() + " ring" + ringWidth + " i/f";  
		int stateBits = 3;
		this.addVectorReg(ringStateName, 0, stateBits);  
		this.addVectorReg(ringStateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), ringStateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  ringStateName + " <= #1  " + ringStateNextName + ";");  

		// create delayed cmd signals
		int delayCount = addrInstProperties.getExternalType().getParm("delay");
		String [] cmdValidDlyName = new String [delayCount+1];
		String [] cmdDataDlyName = new String [delayCount+1];
		for (int idx=0; idx<delayCount+1; idx++) {
			cmdValidDlyName[idx] = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_cmdValid_dly" + idx;
			cmdDataDlyName[idx] = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_cmdData_dly" + idx;
		}
		this.addScalarReg(cmdValidDlyName[0]);  // cmd delay 0 is set in state machine
		this.addVectorReg(cmdDataDlyName[0], 0, ringWidth); 
		for (int idx=1; idx<delayCount+1; idx++) {
			this.addScalarReg(cmdValidDlyName[idx]);  
			this.addVectorReg(cmdDataDlyName[idx], 0, ringWidth); 
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdDataDlyName[idx] + " <= #1  " + ringWidth + "'b0;");  
			this.addRegAssign(groupName,  cmdValidDlyName[idx] + " <= #1  " + cmdValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  cmdDataDlyName[idx] + " <= #1  " + cmdDataDlyName[idx-1] + ";");  
		}
        // assign ring outputs outputs to delayed versions 
		this.addScalarWire(ringCmdValidName);  
		this.addVectorWire(ringCmdDataName, 0, ringWidth); 
		this.addWireAssign(ringCmdValidName + " = " + cmdValidDlyName[delayCount] + ";");
		this.addWireAssign(ringCmdDataName + " = " + cmdDataDlyName[delayCount] + ";");

		// create delayed res signals  
		String [] resValidDlyName = new String [delayCount+1];
		String [] resDataDlyName = new String [delayCount+1];
		for (int idx=0; idx<delayCount+1; idx++) {
			resValidDlyName[idx] = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_resValid_dly" + idx;
			resDataDlyName[idx] = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_resData_dly" + idx;
		}
		this.addScalarWire(resValidDlyName[0]);  // res delay 0 is set from IO
		this.addVectorWire(resDataDlyName[0], 0, ringWidth); 
		for (int idx=1; idx<delayCount+1; idx++) {
			this.addScalarReg(resValidDlyName[idx]);  
			this.addVectorReg(resDataDlyName[idx], 0, ringWidth); 
			this.addResetAssign(groupName, builder.getDefaultReset(), resValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), resDataDlyName[idx] + " <= #1  " + ringWidth + "'b0;");  
			this.addRegAssign(groupName,  resValidDlyName[idx] + " <= #1  " + resValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  resDataDlyName[idx] + " <= #1  " + resDataDlyName[idx-1] + ";");  
		}
        // assign ring inputs to predelay 
		this.addWireAssign(resValidDlyName[0] + " = " + ringResValidName + ";");  
		this.addWireAssign(resDataDlyName[0] + " = " + ringResDataName + ";");
		
		// add read data accumulate reg
		String ringRdAccumName = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_rdata_accum";                      
		String ringRdAccumNextName = "r" + ringWidth + "_" + addrInstProperties.getBaseName() + "_rdata_accum_next";                      
		this.addVectorReg(ringRdAccumName, 0, addrInstProperties.getMaxRegWidth());  
		this.addVectorReg(ringRdAccumNextName, 0, addrInstProperties.getMaxRegWidth());  
		this.addRegAssign(groupName,  ringRdAccumName + " <= #1  " + ringRdAccumNextName + ";");  
		this.addWireAssign(extIf.hwToDecodeName + " = " + ringRdAccumName + ";");
		
		//  will need to capture res width
		String hwToDecodeTransactionSizeNextName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE) + "_next";  // res size will be set in sm
		if (useTransactionSize) {
			this.addVectorReg(extIf.hwToDecodeTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(hwToDecodeTransactionSizeNextName, 0, regWordBits);  // res size will be set in sm
			this.addResetAssign(groupName, builder.getDefaultReset(), extIf.hwToDecodeTransactionSizeName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  extIf.hwToDecodeTransactionSizeName + " <= #1  " + hwToDecodeTransactionSizeNextName + ";");  
		}

        // address byte count
		if (addrXferCountBits > 0) {
			this.addVectorReg(ringAddrCntName, 0, addrXferCountBits);  
			this.addVectorReg(ringAddrCntNextName, 0, addrXferCountBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), ringAddrCntName + " <= #1  " + addrXferCountBits + "'b0;");  
			this.addRegAssign(groupName,  ringAddrCntName + " <= #1  " + ringAddrCntNextName + ";");  			
		}
		
		// data byte count
		int maxDataXferCount = regWords * ExtParameters.getMinDataSize()/ringWidth;
		int maxDataXferCountBits = Utils.getBits(maxDataXferCount);
		boolean useDataCounter = maxDataXferCountBits > 0;
		if (useDataCounter) {
			this.addVectorReg(ringDataCntName, 0, maxDataXferCountBits);
			this.addVectorReg(ringDataCntNextName, 0, maxDataXferCountBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), ringDataCntName + " <= #1  " + maxDataXferCountBits + "'b0;");  
			this.addRegAssign(groupName,  ringDataCntName + " <= #1  " + ringDataCntNextName + ";"); 			
		}
		//System.out.println("SystemVerilogDecodeModule generateExternalInterface_ring: max data xfers=" + maxDataXferCount + ", bits=" + maxDataXferCountBits);
				
		// state machine init values
		this.addCombinAssign(groupName,  ringStateNextName + " = " + ringStateName + ";");  
		this.addCombinAssign(groupName,  cmdValidDlyName[0] + " =  1'b0;");  
		this.addCombinAssign(groupName,  cmdDataDlyName[0] + " =  " + ringWidth + "'b0;");  
		this.addCombinAssign(groupName,  extIf.hwToDecodeAckName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  extIf.hwToDecodeNackName + " =  1'b0;");
		this.addCombinAssign(groupName,  ringRdAccumNextName + " = " + ringRdAccumName + ";");
		// init res size capture
		if (useTransactionSize)
			this.addCombinAssign(groupName,  hwToDecodeTransactionSizeNextName + " = " + extIf.hwToDecodeTransactionSizeName + ";"); 
		// init counter values
		if (addrXferCountBits > 0) {
			this.addCombinAssign(groupName,  ringAddrCntNextName + " = "  + addrXferCountBits + "'b0;");
		}
		if (useDataCounter) this.addCombinAssign(groupName,  ringDataCntNextName + " = "  + maxDataXferCountBits + "'b0;");
			
		// state machine 
		String IDLE = stateBits + "'h0"; 
		String CMD_ADDR = stateBits + "'h1"; 
		String CMD_DATA = stateBits + "'h2"; 
		String RES_WAIT = stateBits + "'h3";
		String RES_READ = stateBits + "'h4";
		String RES_DONE_ACK = stateBits + "'h5";
		String RES_DONE_NACK = stateBits + "'h6";
		
		this.addCombinAssign(groupName, "case (" + ringStateName + ")"); 

		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");
		this.addCombinAssign(groupName, "      " + ringRdAccumNextName + " = " + addrInstProperties.getMaxRegWidth() + "'b0;");
		// go on read or write request
		this.addCombinAssign(groupName, "      if (" + extIf.decodeToHwReName + " | " + extIf.decodeToHwWeName + ") begin");  
		this.addCombinAssign(groupName, "        " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set  
		this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + "[" + rwBitIndex + "] = " + extIf.decodeToHwWeName + ";");  //  write bit
		this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + SystemVerilogSignal.genRefArrayString(addrBitIndex , 2) + " = 2'd" + addrXferCount + ";");  // address xfer bits 
		if (useTransactionSize)
		    this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + " = " + extIf.decodeToHwTransactionSizeName + ";");  // bits 3:0 = transaction size
        // if a portion of address is in first word
		if (addrOffset > 0)
		   genRingExtAddrAssign(groupName, addrInstProperties, 0, addrOffset, ringWidth - addrOffset, extIf.decodeToHwAddrName, cmdDataDlyName[0]);

		// if an address then send it next
		if (addrXferCount > 0) {  
			this.addCombinAssign(groupName, "        " + ringStateNextName + " = " + CMD_ADDR + ";");  
		}
		// otherwise send data if a write or wait for read response
		else {
			this.addCombinAssign(groupName, "        if (" + extIf.decodeToHwWeName + ") " + ringStateNextName + " = " + CMD_DATA + ";");  
			this.addCombinAssign(groupName, "        else " + ringStateNextName + " = " + RES_WAIT + ";");  
		}
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "    end"); 
		
        // if an address then add CMD_ADDR state
		if (addrXferCount > 0) {  
			// CMD_ADDR
			this.addCombinAssign(groupName, "  " + CMD_ADDR + ": begin // CMD_ADDR");  
			this.addCombinAssign(groupName, "      " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set 
			// if more than one address then bump the count
			if (addrXferCountBits > 0) {  
				this.addCombinAssign(groupName,  "      " + ringAddrCntNextName + " = " + ringAddrCntName + " + " + addrXferCountBits + "'b1;");
				// select address slice
				for (int idx=0; idx<addrXferCount; idx++) {
					String prefix = (idx == 0)? "" : "else ";
					this.addCombinAssign(groupName, "      " + prefix + "if (" + ringAddrCntName + " == " + addrXferCountBits + "'d" + idx + ")");
					genRingExtAddrAssign(groupName, addrInstProperties, idx*ringWidth + addrOffset, 0, ringWidth, extIf.decodeToHwAddrName, cmdDataDlyName[0]);
				}
	
				// if addr done, send data if a write or wait for read response 
				this.addCombinAssign(groupName, "      if (" + ringAddrCntName + " == " + addrXferCountBits + "'d" + (addrXferCount-1) + ") begin");
				this.addCombinAssign(groupName, "        if (" + extIf.decodeToHwWeName + ") " + ringStateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "        else " + ringStateNextName + " = " + RES_WAIT + ";");
				this.addCombinAssign(groupName, "      end"); 
			}
			// else a single addr xfer
			else {
				// set address - use upper bits from base addr
				int lowbit = addrInstProperties.getExtLowBit();
				int size = addrInstProperties.getExtAddressWidth();  // single xfer so just use addr size
				int addrPadLowbit = lowbit + addrInstProperties.getExtAddressWidth();  // for ring, we'll use base address to fill all addr bits in this xfer
				int addrPadSize = Math.min(ExtParameters.getLeafAddressSize() - addrPadLowbit, ringWidth - size);  // for ring, we'll use base address to fill all addr bits in this xfer
				if (addrPadSize>0) {
					String addrPadStr = addrInstProperties.getFullBaseAddress().getSubVector(addrPadLowbit, addrPadSize).toFormat(NumBase.Hex, NumFormat.Verilog);
					this.addCombinAssign(groupName, "      " + cmdDataDlyName[0] + " = {" + addrPadStr + ", " + extIf.decodeToHwAddrName + "};");						
				}
				else
					this.addCombinAssign(groupName, "      " + cmdDataDlyName[0] + " = " + extIf.decodeToHwAddrName + ";");						
				// next send data if a write or wait for read response 
				this.addCombinAssign(groupName, "      if (" + extIf.decodeToHwWeName + ") " + ringStateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "      else " + ringStateNextName + " = " + RES_WAIT + ";");  
			}
			this.addCombinAssign(groupName, "    end"); 
		}
		
		// CMD_DATA
		this.addCombinAssign(groupName, "  " + CMD_DATA + ": begin // CMD_DATA");
		this.addCombinAssign(groupName, "      " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set 
		if (useDataCounter) {
			//  bump the data count
			this.addCombinAssign(groupName,  "      " + ringDataCntNextName + " = " + ringDataCntName + " + " + maxDataXferCountBits + "'b1;");
			// select the data slice
			for (int idx=0; idx<maxDataXferCount; idx++) {
				String prefix = (idx == 0)? "" : "else ";
				this.addCombinAssign(groupName, "      "+ prefix + "if (" + ringDataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
				this.addCombinAssign(groupName, "        "  + cmdDataDlyName[0] + " = " + extIf.decodeToHwName + SystemVerilogSignal.genRefArrayString(ringWidth*idx, ringWidth) + ";");
			}
			// if done, move to res wait
			String finalCntStr = getSerialMaxDataCountStr(useTransactionSize, transactionsInWord, extIf.decodeToHwTransactionSizeName);
			this.addCombinAssign(groupName, "      if (" + ringDataCntName + " == " + finalCntStr + ")"); 
			this.addCombinAssign(groupName, "        " + ringStateNextName + " = " + RES_WAIT + ";");			
		}
		else {
			this.addCombinAssign(groupName, "        "  + cmdDataDlyName[0] + " = " + extIf.decodeToHwName + SystemVerilogSignal.genRefArrayString(0, ringWidth) + ";");
			this.addCombinAssign(groupName, "        " + ringStateNextName + " = " + RES_WAIT + ";");			
		}
		this.addCombinAssign(groupName, "    end"); 
			
		// RES_WAIT
		this.addCombinAssign(groupName,   "  " + RES_WAIT + ": begin  // RES_WAIT");
		// first word of response contains ack/nack and xfer size 
		this.addCombinAssign(groupName,   "      " + "if (" + resValidDlyName[delayCount] + ") begin");  // save nack, size
		if (useTransactionSize) {
			this.addCombinAssign(groupName,  "        " + hwToDecodeTransactionSizeNextName + " = " + resDataDlyName[delayCount] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + ";");  
		}
		// if a valid read response then get the data else ack/nack
		this.addCombinAssign(groupName,   "        if (" + extIf.decodeToHwReName + " & " + resDataDlyName[delayCount] + "[" + ackBitIndex + "] & ~" + resDataDlyName[delayCount] + "[" + nackBitIndex + "]) " + ringStateNextName + " = " + RES_READ + ";");  
		this.addCombinAssign(groupName,   "        else if (~" + resDataDlyName[delayCount] + "[" + ackBitIndex + "]) " + ringStateNextName + " = " + RES_DONE_NACK + ";"); // if no ack then nack it
		this.addCombinAssign(groupName,   "        else "+ ringStateNextName + " = " + RES_DONE_ACK + ";");  // write ack
		this.addCombinAssign(groupName,   "      end"); 
		this.addCombinAssign(groupName,   "    end"); 
		
		// RES_READ
		this.addCombinAssign(groupName,     "  " + RES_READ + ": begin  // RES_READ"); 
		if (useDataCounter) {
			//  bump the data count
			this.addCombinAssign(groupName,     "      " + ringDataCntNextName + " = " + ringDataCntName + ";");
			// collect the data slice while res valid is asserted  
			this.addCombinAssign(groupName,     "      " + "if (" + resValidDlyName[delayCount] + ") begin");
			this.addCombinAssign(groupName,     "        " + ringDataCntNextName + " = " + ringDataCntName + " + " + maxDataXferCountBits + "'b1;");
			for (int idx=0; idx<maxDataXferCount; idx++) {
				String prefix = (idx == 0)? "" : "else ";
				this.addCombinAssign(groupName, "        "+ prefix + "if (" + ringDataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
				this.addCombinAssign(groupName, "          " + ringRdAccumNextName + SystemVerilogSignal.genRefArrayString(ringWidth*idx, ringWidth) + " = " + resDataDlyName[delayCount] + ";");
			}
			this.addCombinAssign(groupName,     "      end"); 
			// move to ack when done 
			String finalCntStr = getSerialMaxDataCountStr(useTransactionSize, transactionsInWord, extIf.hwToDecodeTransactionSizeName);
			this.addCombinAssign(groupName, "      if (" + ringDataCntName + " == " + finalCntStr + ") " + ringStateNextName + " = " + RES_DONE_ACK + ";");
		}
		else {
			this.addCombinAssign(groupName,     "      " + "if (" + resValidDlyName[delayCount] + ") begin");
			this.addCombinAssign(groupName,     "        " + ringRdAccumNextName + SystemVerilogSignal.genRefArrayString(0, ringWidth) + " = " + resDataDlyName[delayCount] + ";");
		    this.addCombinAssign(groupName,     "        " + ringStateNextName + " = " + RES_DONE_ACK + ";");
			this.addCombinAssign(groupName,     "      end"); 
		}
		this.addCombinAssign(groupName,     "    end"); 
		
		// RES_DONE_ACK
		this.addCombinAssign(groupName, "  " + RES_DONE_ACK + ": begin // RES_DONE_ACK");
		this.addCombinAssign(groupName, "      " + extIf.hwToDecodeAckName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + ringStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_NACK
		this.addCombinAssign(groupName, "  " + RES_DONE_NACK + ": begin // RES_DONE_NACK");
		this.addCombinAssign(groupName, "      " + extIf.hwToDecodeNackName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + ringStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + ringStateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 				
	}
	
	/** generate address accumulate and match detect assigns 
	 * 
	 * @param groupName - label for comb assigns
	 * @param addrInstProperties - current prorerties for this external region
	 * @param currentAddressOffset - current bit offset (number of addr bits already accumulated)
	 * @param ringDataOffset - bit offset in ring data where address info starts
	 * @param currentAddrSliceWidth - size of slice being accumulated
	 * @param decodeToHwAddrName - name of signal containing full address to be sent
	 * @param ringData - outgoing ring data signal
	 */
	private void genRingExtAddrAssign(String groupName, AddressableInstanceProperties addrInstProperties, int currentAddressOffset, int ringDataOffset,
			int currentAddrSliceWidth, String decodeToHwAddrName, String ringData) {
		int lowbit = currentAddressOffset + addrInstProperties.getExtLowBit();  // low bit index in full address
		//System.out.println("SystemVerilogDecodeModule genRingExtAddrAssign: , currentAddressOffset=" + currentAddressOffset + ", getExtLowBit=" + addrInstProperties.getExtLowBit() + ", lowbit=" + lowbit );
		int regionAddrSize = (currentAddressOffset + currentAddrSliceWidth > addrInstProperties.getExtAddressWidth())? addrInstProperties.getExtAddressWidth() - currentAddressOffset : currentAddrSliceWidth;  // last xfer can be smaller
		int addrPadLowbit = lowbit + regionAddrSize;  // for ring, we'll use base address to fill all addr bits in this xfer
		int addrPadSize = Math.min(ExtParameters.getLeafAddressSize() - addrPadLowbit, currentAddrSliceWidth - regionAddrSize);  // for ring, we'll use base address to fill all addr bits in this xfer
		int zeroPadSize = currentAddrSliceWidth - regionAddrSize - addrPadSize;
		String zeroPadStr = (zeroPadSize>0)? zeroPadSize + "'d0, " : "";
		String ringDataRange = (ringDataOffset > 0)? SystemVerilogSignal.genRefArrayString(ringDataOffset, currentAddrSliceWidth) : "";
		if ((addrPadSize>0) || (zeroPadSize > 0)) {
			String addrPadStr = (addrPadSize>0)?  addrInstProperties.getFullBaseAddress().getSubVector(addrPadLowbit, addrPadSize).toFormat(NumBase.Hex, NumFormat.Verilog) : "";
			//System.out.println("SystemVerilogDecodeModule genRingExtAddrAssign: adding outbound addr padding, size=" + regionAddrSize + ", FullBaseAddress=" + addrInstProperties.getFullBaseAddress() + ", addrPadLowbit=" + addrPadLowbit + ", addrPadSize=" + addrPadSize + ", addrPad=" + addrPadStr + ", zeroPadSize=" + zeroPadSize);
			this.addCombinAssign(groupName, "        " + ringData + ringDataRange + " = {" + zeroPadStr + addrPadStr + ", " + decodeToHwAddrName + SystemVerilogSignal.genRefArrayString(lowbit, regionAddrSize) + "};");						
		}
		else
			this.addCombinAssign(groupName, "        " + ringData + ringDataRange + " = " + decodeToHwAddrName + SystemVerilogSignal.genRefArrayString(lowbit, regionAddrSize) + ";");						
	}


	// ------------------------------ write overrides -------------------------------

	/** write the decode module - override to add decode block */
	@Override
	public void write() {
		// start the module
		int indentLevel = 0;
		writeModuleBegin(indentLevel);
		indentLevel++;
		
		// write internal structures
		writeModuleInternals(indentLevel);
		
		// write the decoder block
		writeDecodeBlock(indentLevel);

		indentLevel--;
		writeModuleEnd(indentLevel);
	}
	
	/** display reg/address info */
	private  void writeDecodeBlock(int indentLevel) { 
		if (decoderList.isEmpty()) return;
		
		writeStmt(indentLevel, "");
		writeStmt(indentLevel, "//------- address decode");
		if (SystemVerilogBuilder.isLegacyVerilog()) writeStmt(indentLevel++, "always @ (*) begin");  
		else writeStmt(indentLevel++, "always_comb begin");
		// init internal ack outputs to 0
		writeStmt(indentLevel, "pio_internal_ack = 1'b0;");  
		writeStmt(indentLevel, "external_transaction_active = 1'b0;"); 
		// init ext ack/nack
		writeStmt(indentLevel, "pio_external_ack_next = 1'b0;");  
		writeStmt(indentLevel, "pio_external_nack_next = 1'b0;");  
		// init read data to 0
		writeStmt(indentLevel, "dec_pio_read_data_next = " + builder.getMaxRegWidth() + "'b0;");

		// set default reg_width  
		if (builder.getMaxRegWordWidth() > 1) {
			writeStmt(indentLevel, "reg_width = " + builder.getMaxWordBitSize() + "'d0;");  // default to size 0
		}

		// if write enables, generate a full width enable vector
		if (hasWriteEnables()) {
			writeStmt(indentLevel, "pio_dec_write_enable_full = " + builder.getMaxRegWidth() + "'d0;");  // default to all bits disabled 
			for (int idx=0; idx<getWriteEnableWidth(); idx++) {
				int enSize = ExtParameters.sysVerWriteEnableSize();
				writeStmt(indentLevel, "if (pio_dec_write_enable_d1[" + idx + "]) pio_dec_write_enable_full" + SystemVerilogSignal.genRefArrayString(idx * enSize, enSize) + " = " + SystemVerilogBuilder.getHexOnesString(enSize) + ";");
			}
		}
		// set decoder defaults by iterating through reglist  
		Iterator<AddressableInstanceProperties> it = decoderList.iterator();   
		writeStmt(indentLevel, "");
		while (it.hasNext()) {
			AddressableInstanceProperties elem = it.next();
			
			// generate default assigns for externals
			if (elem.isExternal()) {
				// generate reg write data assignment
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_DATA) + "_next = pio_dec_write_data_d1" + SystemVerilogSignal.genRefArrayString(0, elem.getMaxRegWidth()) + ";"); // regardless of transaction size assign based on regsize
				if (hasWriteEnables()) writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_ENABLE) + "_next = pio_dec_write_enable_d1" + SystemVerilogSignal.genRefArrayString(0, elem.getWriteEnableWidth()) + ";");
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_WE) + "_next = 1'b0;");  // we defaults to 0
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_RE) + "_next = 1'b0;");  // we defaults to 0
				// if an address is required then add it 
				if ( (elem.getExtAddressWidth() > 0 ) && !elem.isSingleExtReg())
				   writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_ADDR) + "_next = pio_dec_address_d1 " + elem.getExtAddressArrayString() + ";");  // address is resistered value from pio
				// if data sizes are needed then add - also check for single register here and inhibit
				if ( (elem.getMaxRegWidth() > ExtParameters.getMinDataSize())  && !elem.isSingleExtReg()) {
				       int widthBits = Utils.getBits(elem.getMaxRegWordWidth());
				       String extSizeIdxStr = (builder.getMaxWordBitSize()>1)? SystemVerilogSignal.genRefArrayString(0, widthBits) : "";
					   writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_SIZE) + "_next = pio_dec_trans_size_d1" + extSizeIdxStr + ";");
				}
			}
			// internal reg so init enables and data
			else {
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_DATA) + " = pio_dec_write_data_d1 " + SystemVerilogSignal.genRefArrayString(0, elem.getMaxRegWidth()) +";");  // regardless of transaction size assign based on regsize
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_WE) + " = 1'b0;");  // we defaults to 0
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_RE) + " = 1'b0;");  // re defaults to 0
				// construct full width enable vector for internals  TODO - build full vector above and reference here
				if (hasWriteEnables()) writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_ENABLE) + " = pio_dec_write_enable_full " + SystemVerilogSignal.genRefArrayString(0, elem.getMaxRegWidth()) +";");
				
			}
		}	

		// now build case statement by iterating through reg list
		writeStmt(indentLevel, "");
		if (mapHasMultipleAddresses()) writeStmt(indentLevel, "casez(pio_dec_address_d1)");   // begin case statement
		it = decoderList.iterator();
		while (it.hasNext()) {
			AddressableInstanceProperties elem = it.next();
			writeStmt(indentLevel, "//  Register: "+ elem.getInstancePath() + "     Address: " + elem.getBaseAddress() + "     External: " + elem.isExternal()); 
 			//System.out.println("//  Register: "+ elem.getInstancePath() + "     Address: " + elem.getBaseAddress() + "     External: " + elem.isExternal()); 
			//System.out.println("  reg width=" + elem.getRegWidth() + ",  words=" + regWords + "  bits=" + regWordBits);

			// external, so capture external ack/nack, create re/we to hw, and capture read data
			if (elem.isExternal()) {
				// getNextAddress holds max value of address map at this point   
				if (mapHasMultipleAddresses()) writeStmt(indentLevel++, getExtDecodeAddressString(elem) + ":");
				writeStmt(indentLevel++, "begin"); 
				//System.out.println("VerilogBuilder: external region, regsize=" + elem.getRegWidth() + ", regwordbits=" + getBits(elem.getRegWordWidth()) + ", sizebits=" + elem.getExtAddressWidth());

	            // if this is a wide register 
				if (elem.getMaxRegWordWidth() > 1) {
					
					// if this is a single ext register, handle size internally
					if (elem.isSingleExtReg()) {
						// write the size dependent assigns
						writeStmt(indentLevel, "reg_width = " + builder.getMaxWordBitSize() + "'d" + (elem.getMaxRegWordWidth() - 1) + ";");  // set size for this register  						
						// assign write enable / clobber the we if wrong size
						writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_WE) + "_next = pio_write_active & (pio_dec_trans_size_d1 >= reg_width) & ~(pio_external_ack | pio_external_nack);");
						// generate a fake ack on invalid size
						writeStmt(indentLevel, "pio_external_ack_next = " + elem.getFullSignalName(DefSignalType.H2D_ACK) + "_ex | (pio_write_active & (pio_dec_trans_size_d1 < reg_width));"); 
					}
					
					// otherwise this is a larger ext region so use size io
					else {
						// write the size dependent assigns
					    String widthIdxStr = (builder.getMaxWordBitSize()>1)? SystemVerilogSignal.genRefArrayString(0, Utils.getBits(elem.getMaxRegWordWidth())) : "";
						writeStmt(indentLevel, "reg_width" + widthIdxStr + " = " + elem.getFullSignalName(DefSignalType.H2D_RETSIZE) + "_d1;");  // set size for this register  
						// assign write enable 
						writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_WE) + "_next = pio_write_active & ~(pio_external_ack | pio_external_nack);");		// write goes thru even if invalid				
						writeStmt(indentLevel, "pio_external_ack_next = " + elem.getFullSignalName(DefSignalType.H2D_ACK) + "_ex;"); 
					}
				   
				}
				else {
					writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_WE) + "_next = pio_write_active & ~(pio_external_ack | pio_external_nack);"); 
					writeStmt(indentLevel, "pio_external_ack_next = " + elem.getFullSignalName(DefSignalType.H2D_ACK) + "_ex;"); 
				}
				
				// ext transaction is active if a valid read or write
				writeStmt(indentLevel, "external_transaction_active = pio_read_active | pio_write_active;"); 
				// create read enable
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_RE) + "_next = pio_read_active & ~(pio_external_ack | pio_external_nack);"); 
				// capture ack/nack/read data
				writeStmt(indentLevel, "pio_external_nack_next = " + elem.getFullSignalName(DefSignalType.H2D_NACK) + "_ex;");  
				writeStmt(indentLevel--, "dec_pio_read_data_next " + elem.getMaxRegArrayString() + " = " + elem.getFullSignalName(DefSignalType.H2D_DATA) + "_ex;");
				writeStmt(indentLevel--, "end");  								
			}
			
			// else internal, so create internal ack, re/we to logic, and capture read data
			else {
				//RegProperties regElem = (RegProperties) elem;  // internal must be a reg so cast - allows readable/writable properties to be accessed
				// getNextAddress holds max value of address map at this point 
				if (mapHasMultipleAddresses()) writeStmt(indentLevel++, getIntDecodeAddressString(elem) + ":");  
				writeStmt(indentLevel++, "begin");
				
	            // if this is a wide register 
				if (elem.getMaxRegWordWidth() > 1) {
					String writeEnableString = "pio_write_active & ~" + pioInterfaceAckName + " & (pio_dec_trans_size_d1 >= reg_width)";  // suppress write if invalid trans size
					
					// write the size dependent assigns
					writeStmt(indentLevel, "reg_width = " + builder.getMaxWordBitSize() + "'d" + (elem.getMaxRegWordWidth() - 1) + ";");  // set size for this register  						
					// create write enable
					writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_WE) + " = " + writeEnableString + ";");  
					// create read enable
					writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_RE) + " = pio_read_active & ~" + pioInterfaceAckName + ";");   					
				}
				else {
					writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_WE) + " = pio_write_active & ~" + pioInterfaceAckName + ";");  
					writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_RE) + " = pio_read_active & ~" + pioInterfaceAckName + ";");   					
				}
				// generate internal ack based on sw r/w settings of register
				if (!elem.isSwWriteable()) 
				   writeStmt(indentLevel, "pio_internal_ack =  pio_read_active;");  
				else if (!elem.isSwReadable()) 
				   writeStmt(indentLevel, "pio_internal_ack =  pio_write_active;");  
				else 
				   writeStmt(indentLevel, "pio_internal_ack =  pio_read_active | pio_write_active;"); 
				
				writeStmt(indentLevel--, "dec_pio_read_data_next " + elem.getMaxRegArrayString() + " = " + elem.getFullSignalName(DefSignalType.L2D_DATA) + ";");  
				writeStmt(indentLevel--, "end");  				
			}
		}	
     
		if (mapHasMultipleAddresses()) writeStmt(indentLevel, "endcase");  		

		writeStmt(--indentLevel, "end");  
		writeStmt(indentLevel, "");  		
	}

	private void writeStmt(int indentLevel, String string) {
		builder.writeStmt(indentLevel, string);
	}

	/** generate the bit string for decoder case statement w/ an internal address */
	private  String getIntDecodeAddressString(AddressableInstanceProperties elem) {
		// compute the size of this address map
		RegNumber mapSize = builder.getCurrentMapSize();
		StringBuilder decodeString = new StringBuilder(elem.getBaseAddress().getBinString(builder.getAddressLowBit(), builder.getAddressHighBit(mapSize)));  
		// if this is a wide word then dont care lsbs
		int regWordBits = elem.getMaxRegWordHighBit() + 1;
		int adrLen = decodeString.length();
		if (regWordBits > 0) {
			for (int idx=adrLen-regWordBits; idx<adrLen; idx++)
				decodeString.setCharAt(idx, '?');
		}
		return builder.getMapAddressWidth() + "'b" + decodeString;  // TODO - check for zerod wide reg bits?
	}
	
	/** generate the bit string for decoder case statement w/ an external address */
	private  String getExtDecodeAddressString(AddressableInstanceProperties elem) {
		String intString = getIntDecodeAddressString(elem).replace('?', '0');  // start with internal string for base address (remove wildcards) // FIXME - this has base addr of last rep
		int decodeAddrWidth = intString.length();
		int extAddrWidth = elem.getExtAddressWidth();
		// now split base address string
		if (extAddrWidth > 0) {
			String topBits = intString.substring(0, decodeAddrWidth - extAddrWidth);
			String botBits = intString.substring(decodeAddrWidth - extAddrWidth);
			//Jrdl.infoMessage("SystemVerilogDocoder getExtDecodeAddressString: elem.getBaseAddress()=" + elem.getBaseAddress() + ", builder.getExtBase=" + builder.getExternalBaseAddress() );
			//Jrdl.infoMessage("SystemVerilogDocoder getExtDecodeAddressString: intStr=" + intString + ", decodeAddrWidth=" + decodeAddrWidth + ", extAddrWidth=" + extAddrWidth  + ", top=" + topBits + ", bot=" + botBits + ", repeat=" + Jrdl.repeat('0', extAddrWidth));

			if (!botBits.equals(Utils.repeat('0', extAddrWidth))) {
				RegNumber align = new RegNumber(2);
				align.pow(extAddrWidth + elem.getExtLowBit());
			    Ordt.errorMessage("external " + elem.getInstancePath() + " base address (" + elem.getBaseAddress() + 
			    		") is not aligned on " + align.toFormat(NumBase.Hex, NumFormat.Address) + " boundary");
			}
			return topBits + Utils.repeat('?', extAddrWidth);
		}
		else return intString;  // no external address needed, so just use base address
	}

	/** return true if this map contains multiple addressed elements */
	private boolean mapHasMultipleAddresses() {
		return builder.getMapAddressWidth() >= 1;
	}

}
