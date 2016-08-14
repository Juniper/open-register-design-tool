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
import ordt.output.RegProperties;
import ordt.output.systemverilog.SystemVerilogDefinedSignals.DefSignalType;
import ordt.output.AddressableInstanceProperties;
import ordt.output.InstanceProperties.ExtType;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;
import ordt.parameters.ExtParameters.SVBlockSelectModes;
import ordt.parameters.ExtParameters.SVDecodeInterfaceTypes;

/** derived class for decoder module 
 *  note: this class is tightly coupled with builder - uses several builder methods
 */
public class SystemVerilogDecodeModule extends SystemVerilogModule {

	// set default (internal) pio interface signal names
	protected final String pioInterfaceAddressName = "pio_dec_address";
	protected final String pioInterfaceWriteDataName = "pio_dec_write_data";
	protected final String pioInterfaceTransactionSizeName = "pio_dec_trans_size";
	protected final String pioInterfaceRetTransactionSizeName = "dec_pio_trans_size";
	protected final String pioInterfaceWeName = "pio_dec_write";
	protected final String pioInterfaceReName = "pio_dec_read";
	protected final String pioInterfaceReadDataName = "dec_pio_read_data";
	protected final String pioInterfaceAckName = "dec_pio_ack";
	protected final String pioInterfaceNackName = "dec_pio_nack";
	
	protected List<AddressableInstanceProperties> decoderList = new ArrayList<AddressableInstanceProperties>();    // list of address regs 
	protected SVDecodeInterfaceTypes interfaceType = SVDecodeInterfaceTypes.DEFAULT;  // default interface

	public SystemVerilogDecodeModule(SystemVerilogBuilder builder, int insideLocs, String clkName) {
		super(builder, insideLocs, clkName);
	}

	/** add a register or external interface to the decoder */ 
	public void addToDecode(AddressableInstanceProperties instProperties) {
		decoderList.add(instProperties);
		//if (regProperties.isExternal()) System.out.println("SystemVerilogDecoder addToDecode: adding ext " + regProperties.getInstancePath() + " at base=" + regProperties.getBaseAddress());
	}

	public List<AddressableInstanceProperties> getDecodeList() {
		return decoderList;
	}

	// ------------------------------ decoder pio interface methods -------------------------------
	
    public SVDecodeInterfaceTypes getInterfaceType() {
		return interfaceType;
	}

	public void setInterfaceType(SVDecodeInterfaceTypes interfaceType) {
		//System.out.println("SystemVerilogDecoder setInterfaceType: " + interfaceType);
		this.interfaceType = interfaceType;
	}

	public boolean hasInterface(SVDecodeInterfaceTypes type) {
		return (this.interfaceType == type);
	}

	/** add pio-decoder signals and capture signals to decoder lists */
	public void genPioInterface(RegProperties topRegProperties) {
        //System.out.println("SystemVerilogDecode genPioInterface: type=" + getInterfaceType() + ", isS8=" + hasInterface(SVDecodeInterfaceTypes.SERIAL8));
		// create interface logic differently for base addrmap vs children
		if (hasInterface(SVDecodeInterfaceTypes.LEAF)) this.genLeafPioInterface(topRegProperties);
		else if (hasInterface(SVDecodeInterfaceTypes.SERIAL8)) this.genSerial8PioInterface(topRegProperties);
		else if (hasInterface(SVDecodeInterfaceTypes.RING16)) this.genRing16PioInterface(topRegProperties);
		else this.genDefaultPioInterface(topRegProperties);
		
		// generate common internal pio interface code
		this.generateCommonPio();	
	}

	/** generate common internal pio interface code */
	private void generateCommonPio() {
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

		// ------------- add ack/nack outut regs
		this.addScalarReg(pioInterfaceAckName);  // return ack
		this.addScalarReg(pioInterfaceNackName);  // return nack
		this.addScalarReg(pioInterfaceAckName + "_next");  // next return ack
		this.addScalarReg(pioInterfaceNackName + "_next");  // next return nack

        // pio output reg assignments
		this.addResetAssign("pio ack/nack", builder.getDefaultReset(), pioInterfaceAckName + " <= #1 1'b0;");  // reset for ack
		this.addResetAssign("pio ack/nack", builder.getDefaultReset(), pioInterfaceNackName + " <= #1 1'b0;");  // reset for nack 
		this.addRegAssign("pio ack/nack",  pioInterfaceAckName + " <= #1 " + pioInterfaceAckName + " ? 1'b0 : " + pioInterfaceAckName + "_next;");  // return ack		   
		this.addRegAssign("pio ack/nack",  pioInterfaceNackName + " <= #1 " + pioInterfaceNackName + " ? 1'b0 : " + pioInterfaceNackName + "_next;");  // return nack		   
		
		this.addScalarReg("pio_internal_ack");    // set in decoder case	statement	   
		this.addScalarReg("pio_internal_nack");  
		this.addCombinAssign("pio ack/nack", "pio_internal_nack = (pio_read_active | pio_write_active) & ~pio_internal_ack & ~external_transaction_active;");  // internal nack  
		this.addCombinAssign("pio ack/nack",  pioInterfaceAckName + "_next = (pio_internal_ack | (pio_external_ack_next & external_transaction_active));");  // return ack	   
		this.addCombinAssign("pio ack/nack",  pioInterfaceNackName + "_next = (pio_internal_nack | (pio_external_nack_next & external_transaction_active));");  // return nack		   
		
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
	}

	/** add interface signals for nested addrmaps */
	private void genDefaultPioInterface(RegProperties topRegProperties) {
		//System.out.println("SystemVerilogDecodeModule: generating decoder with external interface, id=" + topRegProperties.getInstancePath());
		// generate name-base IO names
		if (mapHasMultipleAddresses()) this.addVectorFrom(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.D2H_ADDR), builder.getAddressLowBit(), builder.getMapAddressWidth());  // address
		this.addVectorFrom(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.D2H_DATA), 0, builder.getMaxRegWidth());  // write data
		// if max transaction for this addrmap is larger than 1, add transaction size signals
		if (builder.getMaxRegWordWidth() > 1) {
		   this.addVectorFrom(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.D2H_SIZE), 0, builder.getMaxWordBitSize());  // transaction size
		   this.addVectorTo(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.H2D_RETSIZE), 0, builder.getMaxWordBitSize());  // return transaction size
		}
		this.addScalarFrom(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.D2H_WE));  // write indication
		this.addScalarFrom(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.D2H_RE));  // read indication
	
		this.addVectorTo(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.H2D_DATA), 0, builder.getMaxRegWidth());  // read data
		this.addScalarTo(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.H2D_ACK));  // ack indication
		this.addScalarTo(SystemVerilogBuilder.PIO, topRegProperties.getFullSignalName(DefSignalType.H2D_NACK));  // nack indication
		
		// define internal interface inputs
		if (mapHasMultipleAddresses()) this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), builder.getMapAddressWidth());  //  address to be used internally 
		this.addVectorWire(pioInterfaceWriteDataName, 0, builder.getMaxRegWidth());  //  wr data to be used internally 
		if (builder.getMaxRegWordWidth() > 1) this.addVectorWire(pioInterfaceTransactionSizeName, 0, builder.getMaxWordBitSize());  //  internal transaction size
		this.addScalarWire(pioInterfaceReName);  //  read enable to be used internally 
		this.addScalarWire(pioInterfaceWeName);  //  write enable be used internally 
		
		// assign input IOs to internal interface
		if (mapHasMultipleAddresses()) this.addWireAssign(pioInterfaceAddressName + " = " + topRegProperties.getFullSignalName(DefSignalType.D2H_ADDR) + ";");   
		this.addWireAssign(pioInterfaceWriteDataName + " = " + topRegProperties.getFullSignalName(DefSignalType.D2H_DATA) + ";");		
		if (builder.getMaxRegWordWidth() > 1) this.addWireAssign(pioInterfaceTransactionSizeName + " = " + topRegProperties.getFullSignalName(DefSignalType.D2H_SIZE) + ";");
		// generate re/we assigns directly or from delayed versions if clock gating is enabled
		assignReadWriteRequests(topRegProperties.getFullSignalName(DefSignalType.D2H_RE), topRegProperties.getFullSignalName(DefSignalType.D2H_WE));
		
		// assign output IOs to internal interface
		if (builder.getMaxRegWordWidth() > 1) this.addWireAssign(topRegProperties.getFullSignalName(DefSignalType.H2D_RETSIZE) + " = " + pioInterfaceRetTransactionSizeName + ";");	
		this.addWireAssign(topRegProperties.getFullSignalName(DefSignalType.H2D_DATA) + " = " + pioInterfaceReadDataName + ";");
		this.addWireAssign(topRegProperties.getFullSignalName(DefSignalType.H2D_ACK) + " = " + pioInterfaceAckName + ";");
		this.addWireAssign(topRegProperties.getFullSignalName(DefSignalType.H2D_NACK) + " = " + pioInterfaceNackName + ";");
	}

	/** add interface signals for address maps talking to a leaf */
	private void genLeafPioInterface(RegProperties topRegProperties) {
		//System.out.println("SystemVerilogDecodeModule: generating decoder with leaf interface, root instance");
		// add interface signals for base address maps - convert leaf interface to internal interface
		
		// define the internal interface signals
		this.addVectorWire(pioInterfaceWriteDataName, 0, builder.getMaxRegWidth());  //  wr data to be used internally 
		if (mapHasMultipleAddresses()) this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), builder.getMapAddressWidth());  //  address to be used internally 
		this.addScalarWire(pioInterfaceReName);  //  read enable to be used internally 
		this.addScalarWire(pioInterfaceWeName);  //  write enable be used internally 

		// data vectors and ack/nack are passed through
		this.addVectorFrom(SystemVerilogBuilder.PIO, "leaf_dec_wr_data", 0, builder.getMaxRegWidth());  // write data
		this.addWireAssign(pioInterfaceWriteDataName + " = " + "leaf_dec_wr_data;");
		
		this.addVectorTo(SystemVerilogBuilder.PIO, "dec_leaf_rd_data", 0, builder.getMaxRegWidth());  // read data
		this.addWireAssign("dec_leaf_rd_data = " + pioInterfaceReadDataName + ";");
		
		this.addScalarTo(SystemVerilogBuilder.PIO, "dec_leaf_ack");  // ack indication
		this.addWireAssign("dec_leaf_ack = " + pioInterfaceAckName + ";");

		this.addScalarTo(SystemVerilogBuilder.PIO, "dec_leaf_nack");  // nack indication
		this.addWireAssign("dec_leaf_nack = " + pioInterfaceNackName + ";");
		
		// generate the address and block decode selects
		this.addVectorFrom(SystemVerilogBuilder.PIO, "leaf_dec_addr", 0, ExtParameters.getLeafAddressSize() );  // full external address
		if (mapHasMultipleAddresses()) this.addWireAssign(pioInterfaceAddressName + " = " + "leaf_dec_addr" + builder.genRefAddressArrayString() + ";");
		
		// if always selecting this block no need for sel signal
		boolean alwaysSelected = (ExtParameters.getSystemverilogBlockSelectMode() == SVBlockSelectModes.ALWAYS);
		if (!alwaysSelected || ExtParameters.systemverilogExportStartEnd())
			this.addVectorWire("block_sel_addr", 0, ExtParameters.getLeafAddressSize());  //  define base address signal 
		
		RegNumber baseAddr = new RegNumber(ExtParameters.getLeafBaseAddress());  // topRegProperties.getFullBaseAddress()
		baseAddr.setVectorLen(ExtParameters.getLeafAddressSize());
		if (topRegProperties != null) baseAddr = topRegProperties.getFullBaseAddress();  // override with local address is a child
		// if base address is a parameter use it
		if (builder.addBaseAddressParameter() && !alwaysSelected) 
		   this.addWireAssign("block_sel_addr = BASE_ADDR;");  // parameter define
		else if (!alwaysSelected || ExtParameters.systemverilogExportStartEnd()) 
		   this.addWireAssign("block_sel_addr = " + baseAddr.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");  // constant define
		
		// check for ignored lower bits in base address  // TODO should this check also be in others (ring16 eg)
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
				this.addScalarWire("block_sel");  //  block selected indicator
				this.addScalarFrom(SystemVerilogBuilder.PIO, "leaf_dec_block_sel");  // input block select 			
				this.addWireAssign("block_sel = leaf_dec_block_sel;");							
			}
			else {
				this.addScalarReg("block_sel");  //  block selected indicator
				for (Integer idx = 0; idx < numSelects; idx++) {
					String idxStr = (idx==0) ? "" : idx.toString();	
					this.addScalarFrom(SystemVerilogBuilder.PIO, "leaf_dec_block_sel" + idxStr);  // input block select 		
					if (idx==0) this.addCombinAssign("block selects",  "block_sel = leaf_dec_block_sel;");  
					else this.addCombinAssign("block selects",  "block_sel = block_sel | leaf_dec_block_sel" + idxStr + ";");
				}
			}
		}
		// otherwise compute select internally
		else if (ExtParameters.getSystemverilogBlockSelectMode() == SVBlockSelectModes.INTERNAL) {
			this.addScalarWire("block_sel");  //  block selected indicator
			this.addWireAssign("block_sel = " + "(block_sel_addr" + builder.getBlockSelectBits() + " == leaf_dec_addr " +
					builder.getBlockSelectBits() + ");");			
		}
		// otherwise select is always active
		else {
			this.addScalarWire("block_sel");  //  block selected indicator
			this.addWireAssign("block_sel = 1'b1;");			
		}
		
		// export start/end signals if specified
		if (ExtParameters.systemverilogExportStartEnd()) {
			builder.getAddressRanges().setListEnd(builder.getCurrentMapSize());  // update list now that map size is known 
			//builder.getAddressRanges().list();
			int numSelects = builder.getAddressRanges().size();
			for (Integer idx = 0; idx < numSelects; idx++) {
				String idxStr = (idx==0) ? "" : idx.toString();	
				this.addVectorTo(SystemVerilogBuilder.PIO, "addr_start" + idxStr, 0, ExtParameters.getLeafAddressSize());  // output address mask
				this.addVectorTo(SystemVerilogBuilder.PIO, "addr_end" + idxStr, 0, ExtParameters.getLeafAddressSize());  // output address match
				this.addVectorWire("addr_start" + idxStr, 0, ExtParameters.getLeafAddressSize());   
				this.addVectorWire("addr_end" + idxStr, 0, ExtParameters.getLeafAddressSize()); 
				// set start and end outputs from range list
				String startOffsetStr = (idx==0) ? "" : " + " + builder.getAddressRanges().getStart(idx).toFormat(NumBase.Hex, NumFormat.Verilog);
				String endOffsetStr = " + " + builder.getAddressRanges().getEnd(idx).toFormat(NumBase.Hex, NumFormat.Verilog);
				this.addWireAssign("addr_start" + idxStr + " = block_sel_addr" + startOffsetStr + ";");  
				this.addWireAssign("addr_end" + idxStr + " = block_sel_addr" + endOffsetStr + ";");
			}
		}

		//this.addResetAssign("leaf i/f", builder.getDefaultReset(), "block_select_d1 <= #1  1'b0;");  // reset for delayed block select 
		//this.addRegAssign("leaf i/f",  "block_select_d1 <= #1 block_sel;");  
		
		// generate valid and wr_dvld active signals 
		this.addScalarReg("leaf_dec_valid_hld1");  //  delayed valid active
		this.addResetAssign("leaf i/f", builder.getDefaultReset(), "leaf_dec_valid_hld1 <= #1  1'b0;");  
		this.addRegAssign("leaf i/f",  "leaf_dec_valid_hld1 <= #1 leaf_dec_valid_hld1_next;");
		
		this.addScalarReg("leaf_dec_valid_hld1_next");  //  valid activated at valid input, deactivated at ack/nack
		this.addCombinAssign("leaf i/f",  "leaf_dec_valid_hld1_next = leaf_dec_valid | leaf_dec_valid_hld1;");  
		this.addCombinAssign("leaf i/f",  "if (dec_pio_ack_next | dec_pio_nack_next) leaf_dec_valid_hld1_next = 1'b0;");  
		this.addScalarWire("leaf_dec_valid_active");  //  active if valid or valid_dly

		this.addScalarReg("leaf_dec_wr_dvld_hld1");  //  delayed wr_dvld active
		this.addResetAssign("leaf i/f", builder.getDefaultReset(), "leaf_dec_wr_dvld_hld1 <= #1  1'b0;");  
		this.addRegAssign("leaf i/f",  "leaf_dec_wr_dvld_hld1 <= #1 leaf_dec_wr_dvld_hld1_next;");
		
		this.addScalarReg("leaf_dec_wr_dvld_hld1_next");  //  wr_dvld activated at wr_dvld input, deactivated at ack/nack/valid
		this.addCombinAssign("leaf i/f",  "leaf_dec_wr_dvld_hld1_next = leaf_dec_wr_dvld | leaf_dec_wr_dvld_hld1;");  
		this.addCombinAssign("leaf i/f",  "if (dec_pio_ack_next | dec_pio_nack_next | leaf_dec_valid) leaf_dec_wr_dvld_hld1_next = 1'b0;");  
		this.addScalarWire("leaf_dec_wr_dvld_active");  //  active if wr_dvld or wr_dvld_dly

		this.addWireAssign("leaf_dec_wr_dvld_active = leaf_dec_wr_dvld | leaf_dec_wr_dvld_hld1;");	
		this.addWireAssign("leaf_dec_valid_active = leaf_dec_valid | leaf_dec_valid_hld1;");			
				
		// generate accept/reject signals
		this.addScalarTo(SystemVerilogBuilder.PIO, "dec_leaf_accept");  // block accept indication
		this.addWireAssign("dec_leaf_accept = leaf_dec_valid & block_sel;");

		this.addScalarTo(SystemVerilogBuilder.PIO, "dec_leaf_reject");  // block reject indication
		this.addWireAssign("dec_leaf_reject = leaf_dec_valid & ~block_sel;");

		// add transaction valid inputs
		this.addScalarFrom(SystemVerilogBuilder.PIO, "leaf_dec_valid");  // transaction valid indicator
		this.addScalarFrom(SystemVerilogBuilder.PIO, "leaf_dec_wr_dvld");  // write transaction valid indicator
		this.addVectorFrom(SystemVerilogBuilder.PIO, "leaf_dec_cycle", 0, 2);  // transaction type indicator

		// generate re/we assigns directly or from delayed versions if clock gating is enabled
		assignReadWriteRequests("block_sel & leaf_dec_valid_active & (leaf_dec_cycle == 2'b10)",
				                "block_sel & leaf_dec_wr_dvld_active & (leaf_dec_cycle[1] == 1'b0)");

		// generate atomic retry output if a write smaller than reg_width being accessed  
		this.addScalarTo(SystemVerilogBuilder.PIO, "dec_leaf_retry_atomic");
		
		// always generate min 3bit width IOs
		int externalDataBitSize = (builder.getMaxWordBitSize() <= 3) ? 3 : builder.getMaxWordBitSize(); 
		this.addVectorFrom(SystemVerilogBuilder.PIO, "leaf_dec_wr_width ", 0, externalDataBitSize);   // write data width
		this.addVectorTo(SystemVerilogBuilder.PIO, "dec_leaf_data_width", 0, externalDataBitSize);  // read data width returned

		// if max transaction is larger than min, add a transaction size/retry signals 
		if (builder.getMaxRegWordWidth() > 1) {
			// add trans size from leaf
			this.addVectorWire(pioInterfaceTransactionSizeName, 0, builder.getMaxWordBitSize());  //  internal transaction size
			this.addWireAssign(pioInterfaceTransactionSizeName + " = leaf_dec_wr_width" + SystemVerilogSignal.genRefArrayString(0, builder.getMaxWordBitSize()) + ";"); 
			
			// generate data width output back to leaf  
			int unusedDataBits = externalDataBitSize - builder.getMaxWordBitSize();
			if (unusedDataBits > 0) this.addWireAssign("dec_leaf_data_width" + SystemVerilogSignal.genRefArrayString(builder.getMaxWordBitSize(), unusedDataBits) + " = 0;"); 
			this.addWireAssign("dec_leaf_data_width" + SystemVerilogSignal.genRefArrayString(0, builder.getMaxWordBitSize()) + " = " + pioInterfaceRetTransactionSizeName + ";"); 
			
			// if a write larger than interface then retry (only valid during ack)
			this.addScalarReg("dec_leaf_retry_atomic");  //  register the output
			this.addScalarWire("dec_leaf_retry_atomic_next");  
			this.addResetAssign("leaf i/f", builder.getDefaultReset(), "dec_leaf_retry_atomic <= #1  1'b0;");  // reset for retry atomic 
			this.addRegAssign("leaf i/f", "dec_leaf_retry_atomic <= #1 dec_leaf_retry_atomic_next;");  
			this.addWireAssign("dec_leaf_retry_atomic_next = block_sel & leaf_dec_wr_dvld_active & (leaf_dec_cycle == 2'b00)" + 
			                       " & (leaf_dec_wr_width" + SystemVerilogSignal.genRefArrayString(0, builder.getMaxWordBitSize()) + " < reg_width);");  
		}
		else {
			// min sized reg space so just set size to zero and inhibit retry
			this.addWireAssign("dec_leaf_retry_atomic = 1'b0;");  
			this.addWireAssign("dec_leaf_data_width" + " = " + externalDataBitSize + "'b0;"); 
		}
	}

	/** add serial8 pio interface */
	private void genSerial8PioInterface(RegProperties topRegProperties) {  
		//System.out.println("SystemVerilogDecodeModule: generating decoder with external interface, id=" + topRegProperties.getInstancePath());
		// add interface signals - convert serial interface to internal interface
		// create module IOs
		String prefix = (topRegProperties == null)? "s8" : "d2s_" + topRegProperties.getBaseName();
		String serial8CmdValidName = prefix + "_cmd_valid";                      
		String serial8CmdDataName = prefix + "_cmd_data";                      
		
		prefix = (topRegProperties == null)? "s8" : "s2d_" + topRegProperties.getBaseName();
		String serial8ResValidName = prefix + "_res_valid";                      
		String serial8ResDataName = prefix + "_res_data";                      
		
		//  inputs
		this.addScalarFrom(SystemVerilogBuilder.PIO, serial8CmdValidName);     // stays high while all cmd addr/data/cntl xferred 
		this.addVectorFrom(SystemVerilogBuilder.PIO, serial8CmdDataName, 0, 8);  

		// outputs  
		this.addScalarTo(SystemVerilogBuilder.PIO, serial8ResValidName);     // stays high while all res cntl/data xferred
		this.addVectorTo(SystemVerilogBuilder.PIO, serial8ResDataName, 0, 8);     
		
		// calculate max number of 8b xfers required for address 
		int addressWidth = builder.getMapAddressWidth();
		int addrXferCount = 0;
		if (addressWidth > 0) {
			addrXferCount = (int) Math.ceil(addressWidth/8.0);
			//System.out.println("SystemVerilogBuilder genSerial8PioInterface: addr width=" + addressWidth + ", addr count=" + addrXferCount);
		}
		
		// compute max transaction size in 32b words and number of bits to represent (4 max) 
		int regWidth = builder.getMaxRegWidth();
		int regWords = builder.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = (regWords > 1);  // if transaction sizes need to be sent/received
		
		// now create state machine vars
		String s8StateName = "s8_state";                      
		String s8StateNextName = "s8_state_next";                      
		String s8AddrCntName = "s8_addr_cnt";                      
		String s8AddrCntNextName = "s8_addr_cnt_next";                      
		String s8DataCntName = "s8_data_cnt";                      
		String s8DataCntNextName = "s8_data_cnt_next"; 
		
		String groupName = "serial8 i/f";  
		int stateBits = 3;
		this.addVectorReg(s8StateName, 0, stateBits);  
		this.addVectorReg(s8StateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), s8StateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  s8StateName + " <= #1  " + s8StateNextName + ";");  

		// s8 cmd inputs will feed into sm
		this.addScalarWire(serial8CmdValidName);  
		this.addVectorWire(serial8CmdDataName, 0, 8); 
        // s8 res outputs will be set in sm 
		this.addScalarReg(serial8ResValidName);  
		this.addVectorReg(serial8ResDataName, 0, 8); 

		// add address accumulate reg 
		String s8AddrAccumName = "s8_addr_accum";                      
		String s8AddrAccumNextName = "s8_addr_accum_next";                      
		if (addressWidth > 0) {
			this.addVectorReg(s8AddrAccumName, builder.getAddressLowBit(), addressWidth);  
			this.addVectorReg(s8AddrAccumNextName, builder.getAddressLowBit(), addressWidth);  
			this.addRegAssign(groupName,  s8AddrAccumName + " <= #1  " + s8AddrAccumNextName + ";");  
			this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), addressWidth);  
			this.addWireAssign(pioInterfaceAddressName + " = " + s8AddrAccumName + ";");  // input addr is set from accum reg			
		}

		// add write data accumulate reg
		String s8WrAccumName = "s8_wdata_accum";                      
		String s8WrAccumNextName = "s8_wdata_accum_next";                      
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
		String s8WrStateCaptureName = "s8_wr_state_capture";                      
		String s8WrStateCaptureNextName = "s8_wr_state_capture_next";                      
		this.addScalarReg(s8WrStateCaptureName);  
		this.addScalarReg(s8WrStateCaptureNextName);  
		this.addRegAssign(groupName,  s8WrStateCaptureName + " <= #1  " + s8WrStateCaptureNextName + ";");  

		// add capture reg for read data
		String s8RdCaptureName = "s8_rdata_capture";                      
		String s8RdCaptureNextName = "s8_rdata_capture_next";                      
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
		int maxDataXferCount = regWords * 4;
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
		String finalCntStr = "2'b11";
		if (useTransactionSize) finalCntStr = "{" + pioInterfaceTransactionSizeName + ", 2'b11}";
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
		if (useTransactionSize)
			this.addCombinAssign(groupName, "      if (" + s8DataCntName + " == {" + pioInterfaceRetTransactionSizeName + ", 2'b11}) " + s8StateNextName + " = " + IDLE + ";");
		else
			this.addCombinAssign(groupName, "      if (" + s8DataCntName + " == 2'b11) " + s8StateNextName + " = " + IDLE + ";");
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
		// generate re/we assigns directly or from delayed versions if clock gating is enabled
		assignReadWriteRequests(s8pioInterfaceReName, s8pioInterfaceWeName);
		
	}

	/** add ring16 pio interface */  
	private void genRing16PioInterface(RegProperties topRegProperties) {  
		//System.out.println("SystemVerilogDecodeModule: generating decoder with external interface, id=" + topRegProperties.getInstancePath());
		// add interface signals - convert serial interface to internal interface
		// create module IOs
		String prefix = (topRegProperties == null)? "r16" : "d2r_" + topRegProperties.getBaseName();  
		String ring16CmdValidName = prefix + "_cmd_valid";                      
		String ring16CmdDataName = prefix + "_cmd_data";                      	
		prefix = (topRegProperties == null)? "r16" : "r2d_" + topRegProperties.getBaseName();
		String ring16ResValidName = prefix + "_res_valid";                      
		String ring16ResDataName = prefix + "_res_data";                      
		
		//  inputs
		this.addScalarFrom(SystemVerilogBuilder.PIO, ring16CmdValidName);     // stays high while all cmd addr/data/cntl xferred 
		this.addVectorFrom(SystemVerilogBuilder.PIO, ring16CmdDataName, 0, 16);  

		// outputs  
		this.addScalarTo(SystemVerilogBuilder.PIO, ring16ResValidName);     // stays high while all res cntl/data xferred
		this.addVectorTo(SystemVerilogBuilder.PIO, ring16ResDataName, 0, 16);     
		
		// create the block base address
		this.addVectorWire("block_base_address", 0, ExtParameters.getLeafAddressSize());  //  base address of block 
		
		RegNumber baseAddr = new RegNumber(ExtParameters.getLeafBaseAddress());  // topRegProperties.getFullBaseAddress()
		baseAddr.setVectorLen(ExtParameters.getLeafAddressSize());
		if (topRegProperties != null) baseAddr = topRegProperties.getFullBaseAddress();  // override with local address is a child
		// if base address is a parameter use it
		if (builder.addBaseAddressParameter()) 
		   this.addWireAssign("block_base_address = BASE_ADDR;");  // parameter define
		else  
		   this.addWireAssign("block_base_address = " + baseAddr.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");  // constant define
		
		// calculate max number of 16b xfers required for address 
		int addressWidth = builder.getMapAddressWidth();
		int maxAddrXferCount = (int) Math.ceil(ExtParameters.getLeafAddressSize()/16.0);  // max address xfers (not determined by local size)
		//System.out.println("SystemVerilogBuilder genRing16PioInterface: addr width=" + addressWidth + ", max addr count=" + maxAddrXferCount);
		
		// compute max transaction size in 32b words and number of bits to represent (4 max) 
		int regWidth = builder.getMaxRegWidth();
		int regWords = builder.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = (regWords > 1);  // if transaction sizes need to be sent/received
		//System.out.println("SystemVerilogBuilder genRing16PioInterface: max width=" + regWidth + ", top max=" + topRegProperties.getRegWidth() + ", words=" + regWords + ", useT=" + useTransactionSize);
		
		// now create state machine vars
		String r16StateName = "r16_state";                      
		String r16StateNextName = "r16_state_next";                      
		String r16DataCntName = "r16_data_cnt";                      
		String r16DataCntNextName = "r16_data_cnt_next"; 
		
		String groupName = "ring16 i/f";  
		int stateBits = 4;
		this.addVectorReg(r16StateName, 0, stateBits);  
		this.addVectorReg(r16StateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), r16StateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  r16StateName + " <= #1  " + r16StateNextName + ";");  

		// r16 cmd inputs will feed into sm (after cmd delay regs)
		this.addScalarWire(ring16CmdValidName);  
		this.addVectorWire(ring16CmdDataName, 0, 16); 
        // r16 res outputs will be set in sm (after res delay regs)
		this.addScalarWire(ring16ResValidName);  
		this.addVectorWire(ring16ResDataName, 0, 16); 

		// create out fifo signals  
		int outFifoSize = maxAddrXferCount + 1;  // max depth is addr words plus 1 cntl word
		String outFifoAdvanceName = "r16_out_fifo_advance";  // set in sm
		String [] outFifoDataName = new String [outFifoSize+1];
		for (int idx=0; idx<outFifoSize+1; idx++) {
			outFifoDataName[idx] = "r16_out_fifo_data_dly" + idx;
		}
		this.addScalarReg(outFifoAdvanceName); 
		this.addVectorReg(outFifoDataName[0], 0, 16); // out data 0 is set in state machine
		for (int idx=1; idx<outFifoSize+1; idx++) {
			this.addVectorReg(outFifoDataName[idx], 0, 16); 
			this.addResetAssign(groupName, builder.getDefaultReset(), outFifoDataName[idx] + " <= #1  16'b0;");  
			this.addRegAssign(groupName,  "if (" + outFifoAdvanceName +") " + outFifoDataName[idx] + " <= #1  " + outFifoDataName[idx-1] + ";");  
		}

		// create delayed cmd signals
		int cmdDelayCount = (int) Math.ceil(ExtParameters.sysVerRing16InterNodeDelay()/2.0);  // half of delay
		String [] cmdValidDlyName = new String [cmdDelayCount+1];
		String [] cmdDataDlyName = new String [cmdDelayCount+1];
		for (int idx=0; idx<cmdDelayCount+1; idx++) {
			cmdValidDlyName[idx] = "r16_cmdValid_dly" + idx;
			cmdDataDlyName[idx] = "r16_cmdData_dly" + idx;
		}
		this.addScalarWire(cmdValidDlyName[0]);  // cmd delay 0 is set from IO
		this.addVectorWire(cmdDataDlyName[0], 0, 16); 
		for (int idx=1; idx<cmdDelayCount+1; idx++) {
			this.addScalarReg(cmdValidDlyName[idx]);  
			this.addVectorReg(cmdDataDlyName[idx], 0, 16); 
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdDataDlyName[idx] + " <= #1  16'b0;");  
			this.addRegAssign(groupName,  cmdValidDlyName[idx] + " <= #1  " + cmdValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  cmdDataDlyName[idx] + " <= #1  " + cmdDataDlyName[idx-1] + ";");  
		}
        // assign r16 cmd inputs to predelay 
		this.addWireAssign(cmdValidDlyName[0] + " = " + ring16CmdValidName + ";");  
		this.addWireAssign(cmdDataDlyName[0] + " = " + ring16CmdDataName + ";");

		// create delayed res signals  
		int resDelayCount = (int) Math.floor(ExtParameters.sysVerRing16InterNodeDelay()/2.0);  // half of delay
		String [] resValidDlyName = new String [resDelayCount+1];
		String [] resDataDlyName = new String [resDelayCount+1];
		for (int idx=0; idx<resDelayCount+1; idx++) {
			resValidDlyName[idx] = "r16_resValid_dly" + idx;
			resDataDlyName[idx] = "r16_resData_dly" + idx;
		}
		
		this.addScalarReg(resValidDlyName[0]);  // res delay 0 is set in state machine
		this.addVectorReg(resDataDlyName[0], 0, 16); 
		for (int idx=1; idx<resDelayCount+1; idx++) {
			this.addScalarReg(resValidDlyName[idx]);  
			this.addVectorReg(resDataDlyName[idx], 0, 16); 
			this.addResetAssign(groupName, builder.getDefaultReset(), resValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), resDataDlyName[idx] + " <= #1  16'b0;");  
			this.addRegAssign(groupName,  resValidDlyName[idx] + " <= #1  " + resValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  resDataDlyName[idx] + " <= #1  " + resDataDlyName[idx-1] + ";");  
		}
        // assign r16 outputs outputs to delayed versions 
		this.addWireAssign(ring16ResValidName + " = " + resValidDlyName[resDelayCount] + ";");
		this.addWireAssign(ring16ResDataName + " = " + resDataDlyName[resDelayCount] + ";");

		// add address accumulate reg 
		String r16AddrAccumName = "r16_addr_accum";                      
		String r16AddrAccumNextName = "r16_addr_accum_next";                      
		if (addressWidth > 0) {
			this.addVectorReg(r16AddrAccumName, builder.getAddressLowBit(), addressWidth);  
			this.addVectorReg(r16AddrAccumNextName, builder.getAddressLowBit(), addressWidth);  
			this.addRegAssign(groupName,  r16AddrAccumName + " <= #1  " + r16AddrAccumNextName + ";");  
			this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), addressWidth);  
			this.addWireAssign(pioInterfaceAddressName + " = " + r16AddrAccumName + ";");  // input addr is set from accum reg			
		}

		// add write data accumulate reg
		String r16WrAccumName = "r16_wdata_accum";                      
		String r16WrAccumNextName = "r16_wdata_accum_next";                      
		this.addVectorReg(r16WrAccumName, 0, regWidth);  
		this.addVectorReg(r16WrAccumNextName, 0, regWidth);  
		this.addRegAssign(groupName,  r16WrAccumName + " <= #1  " + r16WrAccumNextName + ";");  
		this.addVectorWire(pioInterfaceWriteDataName, 0, regWidth);  
		this.addWireAssign(pioInterfaceWriteDataName + " = " + r16WrAccumName + ";");  // input data is set from accum reg
		
		// will need to capture cmd size
		String pioInterfaceTransactionSizeNextName = pioInterfaceTransactionSizeName + "_next";  // cmd size next will be set in sm
		if (useTransactionSize) {
			this.addVectorReg(pioInterfaceTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(pioInterfaceTransactionSizeNextName, 0, regWordBits);  // res size will be set in sm
			this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceTransactionSizeName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  pioInterfaceTransactionSizeName + " <= #1  " + pioInterfaceTransactionSizeNextName + ";");  
		}

		// add capture reg for write transaction indicator
		String r16WrStateCaptureName = "r16_wr_state_capture";                      
		String r16WrStateCaptureNextName = "r16_wr_state_capture_next";                      
		this.addScalarReg(r16WrStateCaptureName);  
		this.addScalarReg(r16WrStateCaptureNextName);  
		this.addRegAssign(groupName,  r16WrStateCaptureName + " <= #1  " + r16WrStateCaptureNextName + ";");  

		// add capture reg for read data
		String r16RdCaptureName = "r16_rdata_capture";                      
		String r16RdCaptureNextName = "r16_rdata_capture_next";                      
		this.addVectorReg(r16RdCaptureName, 0, regWidth);  
		this.addVectorReg(r16RdCaptureNextName, 0, regWidth);  
		this.addRegAssign(groupName,  r16RdCaptureName + " <= #1  " + r16RdCaptureNextName + ";"); 

		// address xfer count
		String r16AddrCntName = "r16_addr_cnt";                      
		String r16AddrCntNextName = "r16_addr_cnt_next";                      
		int addrXferCountBits = Utils.getBits(maxAddrXferCount+1);  // always > 0 unlike serial case / need + 1 since comparing w next count
		this.addVectorReg(r16AddrCntName, 0, addrXferCountBits);  
		this.addVectorReg(r16AddrCntNextName, 0, addrXferCountBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), r16AddrCntName + " <= #1  " + addrXferCountBits + "'b0;");  
		this.addRegAssign(groupName,  r16AddrCntName + " <= #1  " + r16AddrCntNextName + ";");  
		
		// add capture reg for address xfer count
		String r16AddrCntCaptureName = "r16_addr_cnt_capture";                      
		String r16AddrCntCaptureNextName = "r16_addr_cnt_capture_next";                      
		this.addVectorReg(r16AddrCntCaptureName, 0, addrXferCountBits);  
		this.addVectorReg(r16AddrCntCaptureNextName, 0, addrXferCountBits);  
		this.addRegAssign(groupName,  r16AddrCntCaptureName + " <= #1  " + r16AddrCntCaptureNextName + ";"); 
		
		// add ring msb no match indicator
		String r16NotMineName = "r16_not_mine";                      
		String r16NotMineNextName = "r16_not_mine_next";                      
		this.addScalarReg(r16NotMineName);  
		this.addScalarReg(r16NotMineNextName);  
		this.addResetAssign(groupName, builder.getDefaultReset(), r16NotMineName + " <= #1  1'b0;");  
		this.addRegAssign(groupName,  r16NotMineName + " <= #1  " + r16NotMineNextName + ";");  
		
		// data byte count
		int maxDataXferCount = regWords * 2;
		int maxDataXferCountBits = Utils.getBits(maxDataXferCount);
		this.addVectorReg(r16DataCntName, 0, maxDataXferCountBits);  
		this.addVectorReg(r16DataCntNextName, 0, maxDataXferCountBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), r16DataCntName + " <= #1  " + maxDataXferCountBits + "'b0;");  
		this.addRegAssign(groupName,  r16DataCntName + " <= #1  " + r16DataCntNextName + ";"); 

		// define internal interface signals that will be set in sm 
		String r16pioInterfaceReName = "r16_" + pioInterfaceReName;
		String r16pioInterfaceWeName = "r16_" + pioInterfaceWeName;
		this.addScalarReg(r16pioInterfaceReName);
		this.addScalarReg(r16pioInterfaceWeName);
		
		// state machine init values
		this.addCombinAssign(groupName,  r16StateNextName + " = " + r16StateName + ";");  
		this.addCombinAssign(groupName,  resValidDlyName[0] + " =  1'b0;");  // return valid
		this.addCombinAssign(groupName,  resDataDlyName[0] + " =  16'b0;");   // return data
		this.addCombinAssign(groupName,  r16pioInterfaceWeName + " =  1'b0;");  // write active
		this.addCombinAssign(groupName,  r16pioInterfaceReName + " =  1'b0;");  // read active 
		this.addCombinAssign(groupName,  r16WrStateCaptureNextName + " = " + r16WrStateCaptureName + ";");  // write indicator
		this.addCombinAssign(groupName,  r16AddrCntCaptureNextName + " = " + r16AddrCntCaptureName + ";");  // address xfer count
		if (addressWidth > 0)
			this.addCombinAssign(groupName,  r16AddrAccumNextName + " = " + r16AddrAccumName + ";");  // address accumulate
		this.addCombinAssign(groupName,  r16NotMineNextName + " =  1'b0;");  // default to matching address
		this.addCombinAssign(groupName,  r16WrAccumNextName + " = " + r16WrAccumName + ";");  // write data accumulate
		this.addCombinAssign(groupName,  r16RdCaptureNextName + " = " + r16RdCaptureName + ";");  // read data capture
		// init cmd size capture
		if (useTransactionSize)
			this.addCombinAssign(groupName,  pioInterfaceTransactionSizeNextName + " = " + pioInterfaceTransactionSizeName + ";"); 
		// init counter values
		this.addCombinAssign(groupName,  r16AddrCntNextName + " = "  + addrXferCountBits + "'b0;");
		
		this.addCombinAssign(groupName,  r16DataCntNextName + " = "  + maxDataXferCountBits + "'b0;"); 
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
				
		this.addCombinAssign(groupName, "case (" + r16StateName + ")"); 

		// IDLE
		this.addCombinAssign(groupName,     "  " + IDLE + ": begin // IDLE");
		// init accumulator/capture regs
		this.addCombinAssign(groupName,     "      " + r16WrStateCaptureNextName + " = 1'b0;");  
		if (addressWidth > 0)
			this.addCombinAssign(groupName, "      " + r16AddrAccumNextName + " = " + addressWidth + "'b0;");
		this.addCombinAssign(groupName,     "      " + r16WrAccumNextName + " = " + regWidth + "'b0;");
		// go on cmd valid - capture r/w indicator, addr size, and transaction size
		this.addCombinAssign(groupName,     "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		this.addCombinAssign(groupName,     "        " + outFifoAdvanceName + " =  1'b1;");  // save cntl word
		this.addCombinAssign(groupName,     "        " + r16WrStateCaptureNextName + " = " + cmdDataDlyName[cmdDelayCount] + "[7];");  // bit 7 = write 
		this.addCombinAssign(groupName,     "        " + r16AddrCntCaptureNextName + " = " + cmdDataDlyName[cmdDelayCount] + SystemVerilogSignal.genRefArrayString(4, addrXferCountBits) + ";");  // bits 6:4 = addr xfers
		if (useTransactionSize)
		    this.addCombinAssign(groupName, "        " + pioInterfaceTransactionSizeNextName + " = " + cmdDataDlyName[cmdDelayCount] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + ";");  // bits 3:0 = transaction size
		// if ack or nack are already set then skip
		this.addCombinAssign(groupName,     "        if (" + cmdDataDlyName[cmdDelayCount] + "[15] | " + cmdDataDlyName[cmdDelayCount] + "[14]) begin");  
		this.addCombinAssign(groupName,     "          if (~" +  r16WrStateCaptureNextName + ") " + r16StateNextName + " = " + RESPONSE_BYPASS + ";"); // bypass data also if a read response 
		this.addCombinAssign(groupName,     "          else " + r16StateNextName + " = " + RESPONSE_FLUSH + ";"); // flush cmd word from fifo if a write response 
		this.addCombinAssign(groupName,     "        end");  //cmd valid
        // if an address then get it next  
		this.addCombinAssign(groupName,     "        else if (" + r16AddrCntCaptureNextName + " > " + addrXferCountBits + "'d0) " + r16StateNextName + " = " + CMD_ADDR + ";");  
		// otherwise get cmd data if a write or assert read and wait for internal read response
		this.addCombinAssign(groupName,     "        else if (" + r16WrStateCaptureNextName + ") " + r16StateNextName + " = " + CMD_DATA + ";");  
		this.addCombinAssign(groupName,     "        else " + r16StateNextName + " = " + RES_WAIT + ";");  
		this.addCombinAssign(groupName,     "      end");  //cmd valid
		this.addCombinAssign(groupName,     "    end"); 
		
		// CMD_ADDR
		this.addCombinAssign(groupName,  "  " + CMD_ADDR + ": begin // CMD_ADDR");
		this.addCombinAssign(groupName,  "      " + r16AddrCntNextName + " = " + r16AddrCntName + ";"); // hold value
		this.addCombinAssign(groupName,  "      " + r16NotMineNextName + " = " + r16NotMineName + ";");  // hold matching address value 
		this.addCombinAssign(groupName,  "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		this.addCombinAssign(groupName,  "        " + outFifoAdvanceName + " =  1'b1;");  // save addr word
		this.addCombinAssign(groupName,  "        " + r16AddrCntNextName + " = " + r16AddrCntName + " + " + addrXferCountBits + "'b1;");
		// accumulate the address
		for (int idx=0; idx<maxAddrXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "        " + prefix + "if (" + r16AddrCntName + " == " + addrXferCountBits + "'d" + idx + ") begin");
			int lowbit = idx*16 + builder.getAddressLowBit();
			int lsbCheckSize = ((idx+1)*16 > addressWidth)? Math.max(0, addressWidth - idx*16) : 16;  // last valid local addr xfer is btwn 0 and 16b (0 after)
			int msbCheckSize = 16 - lsbCheckSize;  // for ring, we'll use base address to test uppr addr bits in this xfer, could be all check size
			// if msbs are in this xfer, then look for an address mismatch 
			if (msbCheckSize > 0) {
				if ((lowbit + 16) > ExtParameters.getLeafAddressSize()) 
					msbCheckSize = ExtParameters.getLeafAddressSize() - lowbit;
				//String matchStr = topRegProperties.getFullBaseAddress().getSubVector(lowbit + lsbCheckSize, msbCheckSize).toFormat(NumBase.Hex, NumFormat.Verilog);
				String matchStr = "block_base_address" + SystemVerilogSignal.genRefArrayString(lowbit + lsbCheckSize, msbCheckSize);
				//block_base_address
				this.addCombinAssign(groupName, "          " + r16NotMineNextName + " = " + r16NotMineName + " | (" + cmdDataDlyName[cmdDelayCount] + SystemVerilogSignal.genRefArrayString(lsbCheckSize, msbCheckSize) + " != " + matchStr + ");");  // default to matching address
			}
			//  only accumulate address if in local range
			int maxLocalAddrXferCount = (int) Math.ceil(addressWidth/16.0);  // max valid local address xfers
			if (idx < maxLocalAddrXferCount) {
				String addrRangeString = (addressWidth > 1)? SystemVerilogSignal.genRefArrayString(lowbit, lsbCheckSize) : "";
			    this.addCombinAssign(groupName, "          " + r16AddrAccumNextName + addrRangeString + " = " + cmdDataDlyName[cmdDelayCount] + SystemVerilogSignal.genRefArrayString(0, lsbCheckSize) + ";");		
			}
			this.addCombinAssign(groupName, "        end"); 
		}

		// if addr done, send data if a write or wait for read response 
		// if this xfer isn't for me then either capture write data or just send the accumulated data in the bypass fifo
		this.addCombinAssign(groupName, "        if (" + r16AddrCntNextName + " == " + r16AddrCntCaptureName + ") begin"); 
		this.addCombinAssign(groupName, "          if (" + r16NotMineNextName + ") begin");  
		this.addCombinAssign(groupName, "            " +  r16AddrCntNextName + " = "  + addrXferCountBits + "'b0;");
		this.addCombinAssign(groupName, "            if (" + r16WrStateCaptureNextName + ") " + r16StateNextName + " = " + CMD_BYPASS + ";");  
		this.addCombinAssign(groupName, "            else " + r16StateNextName + " = " + CMD_FLUSH + ";");  
		this.addCombinAssign(groupName, "          end"); 
		this.addCombinAssign(groupName, "          else if (" + r16WrStateCaptureName + ") " + r16StateNextName + " = " + CMD_DATA + ";");  
		this.addCombinAssign(groupName, "          else " + r16StateNextName + " = " + RES_WAIT + ";");
		this.addCombinAssign(groupName, "        end"); 

		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
		
		// CMD_DATA - capture write data
		this.addCombinAssign(groupName, "  " + CMD_DATA + ": begin // CMD_DATA");
		this.addCombinAssign(groupName,  "      " + r16DataCntNextName + " = " + r16DataCntName + ";");
		this.addCombinAssign(groupName, "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		//  bump the data count
		this.addCombinAssign(groupName,  "        " + r16DataCntNextName + " = " + r16DataCntName + " + " + maxDataXferCountBits + "'b1;");
		// get the data slice
		for (int idx=0; idx<maxDataXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "        "+ prefix + "if (" + r16DataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
			this.addCombinAssign(groupName, "          "  + r16WrAccumNextName + SystemVerilogSignal.genRefArrayString(16*idx, 16) + " = " + cmdDataDlyName[cmdDelayCount] + ";");
		}
		// if done, move to res wait
		String finalCntStr = "1'b1";
		if (useTransactionSize) finalCntStr = "{" + pioInterfaceTransactionSizeName + ", 1'b1}";
		this.addCombinAssign(groupName, "        if (" + r16DataCntName + " == " + finalCntStr + ")");
		this.addCombinAssign(groupName, "          " + r16StateNextName + " = " + RES_WAIT + ";");
		this.addCombinAssign(groupName, "      end"); 
		this.addCombinAssign(groupName, "    end"); 
			
		// RES_WAIT
		this.addCombinAssign(groupName, "  " + RES_WAIT + ": begin  // RES_WAIT");
		// activate either read or write request
		this.addCombinAssign(groupName, "      " + r16pioInterfaceWeName + " = " + r16WrStateCaptureName + ";");  // set internal write active
		this.addCombinAssign(groupName, "      " + r16pioInterfaceReName + " = ~" + r16WrStateCaptureName + ";");  // set internal read active
		// go on ack/nack, capture read data and set first word of response (contains ack/nack and return xfer size) 
		this.addCombinAssign(groupName,   "      " + "if (" + pioInterfaceAckName + " | " + pioInterfaceNackName + ") begin");  
		this.addCombinAssign(groupName,   "        " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		this.addCombinAssign(groupName,   "        " + r16RdCaptureNextName + " = " + pioInterfaceReadDataName + ";");  // capture read data  
		this.addCombinAssign(groupName,   "        " + resDataDlyName[0] + "[15] = " + pioInterfaceNackName + ";");  // set nack bit  
		this.addCombinAssign(groupName,   "        " + resDataDlyName[0] + "[14] = ~" + pioInterfaceNackName + ";");  // set ack bit  
		this.addCombinAssign(groupName,   "        " + resDataDlyName[0] + "[7] = " + r16WrStateCaptureName + ";");  // set write bit  
		if (useTransactionSize) {
			this.addCombinAssign(groupName,  "        " + resDataDlyName[0] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + " = " + pioInterfaceRetTransactionSizeName + ";");  
		}
		// if a read then send the data else we're done
		this.addCombinAssign(groupName,   "        if (~" + r16WrStateCaptureName + ") " + r16StateNextName + " = " + RES_READ + ";");  
		this.addCombinAssign(groupName,   "        else "+ r16StateNextName + " = " + IDLE + ";");
		this.addCombinAssign(groupName,   "      end"); 
		this.addCombinAssign(groupName,   "    end");
		
		// RES_READ - send read data
		this.addCombinAssign(groupName,     "  " + RES_READ + ": begin  // RES_READ");  
		this.addCombinAssign(groupName,     "      " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		//  bump the data count
		this.addCombinAssign(groupName,     "      " + r16DataCntNextName + " = " + r16DataCntName + " + " + maxDataXferCountBits + "'b1;");
		// send the data slice while 
		for (int idx=0; idx<maxDataXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "      "+ prefix + "if (" + r16DataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
			this.addCombinAssign(groupName, "        " + resDataDlyName[0] + " = " + r16RdCaptureName + SystemVerilogSignal.genRefArrayString(16*idx, 16) + ";");
		}
		// if final count we're done
		if (useTransactionSize)
			this.addCombinAssign(groupName, "      if (" + r16DataCntName + " == {" + pioInterfaceRetTransactionSizeName + ", 1'b1}) " + r16StateNextName + " = " + IDLE + ";");
		else
			this.addCombinAssign(groupName, "      if (" + r16DataCntName + " == 1'b1) " + r16StateNextName + " = " + IDLE + ";");
		this.addCombinAssign(groupName,     "    end"); 
		
		// CMD_BYPASS - feed write data into bypass fifo  
		this.addCombinAssign(groupName,     "  " + CMD_BYPASS + ": begin // CMD_BYPASS");
		this.addCombinAssign(groupName,     "      " + r16DataCntNextName + " = " + r16DataCntName + ";");
		this.addCombinAssign(groupName,     "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		//  bump the data count
		this.addCombinAssign(groupName,     "        " + r16DataCntNextName + " = " + r16DataCntName + " + " + maxDataXferCountBits + "'b1;");
        // advance the fifo and set output valid
		this.addCombinAssign(groupName,     "        " + outFifoAdvanceName + " =  1'b1;");  // advance fifo
		this.addCombinAssign(groupName,     "        " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		// if done, move to flush
		this.addCombinAssign(groupName,     "        if (" + r16DataCntName + " == " + finalCntStr + ")"); 
		this.addCombinAssign(groupName,     "          " + r16StateNextName + " = " + CMD_FLUSH + ";");
		this.addCombinAssign(groupName,     "      end"); 
		// set the RES_output to fifo element depending on xfer addr size 
		for (int idx=0; idx<maxAddrXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "      " + prefix + "if (" + r16AddrCntCaptureName + " == " + addrXferCountBits + "'d" + idx + ")");
			this.addCombinAssign(groupName, "        " + resDataDlyName[0] + " = " + outFifoDataName[idx+1] + ";");
		}
		this.addCombinAssign(groupName,     "    end"); 
		
		// CMD_FLUSH  - shift data out of bypass fifo 
		this.addCombinAssign(groupName,     "  " + CMD_FLUSH + ": begin  // CMD_FLUSH"); 
        // transfer out address xfer count + 1
		this.addCombinAssign(groupName,     "      " + outFifoAdvanceName + " =  1'b1;");  // save addr word
		this.addCombinAssign(groupName,     "      " + r16AddrCntNextName + " = " + r16AddrCntName + " + " + addrXferCountBits + "'b1;");
		this.addCombinAssign(groupName,     "      if (" + r16AddrCntName + " == " + r16AddrCntCaptureName + ") " + r16StateNextName + " = " + IDLE + ";");
		// set the RES_output to fifo element depending on xfer addr size 
		this.addCombinAssign(groupName,     "      " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		for (int idx=0; idx<maxAddrXferCount; idx++) {
			prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "      " + prefix + "if (" + r16AddrCntCaptureName + " == " + addrXferCountBits + "'d" + idx + ")");
			this.addCombinAssign(groupName, "        " + resDataDlyName[0] + " = " + outFifoDataName[idx+1] + ";");
		}
		this.addCombinAssign(groupName,     "    end"); 
		
		// RESPONSE_BYPASS - forward responses from previous nodes on the ring  
		this.addCombinAssign(groupName,     "  " + RESPONSE_BYPASS + ": begin // RESPONSE_BYPASS");
		this.addCombinAssign(groupName,     "      " + r16DataCntNextName + " = " + r16DataCntName + ";");
		this.addCombinAssign(groupName,     "      if (" + cmdValidDlyName[cmdDelayCount] + ") begin");  
		//  bump the data count
		this.addCombinAssign(groupName,     "        " + r16DataCntNextName + " = " + r16DataCntName + " + " + maxDataXferCountBits + "'b1;");
        //  set output valid
		this.addCombinAssign(groupName,     "        " + outFifoAdvanceName + " =  1'b1;");  // save word
		this.addCombinAssign(groupName,     "        " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		// if done, flush fifo and back to idle
		this.addCombinAssign(groupName,     "        if (" + r16DataCntName + " == " + finalCntStr + ")");
		this.addCombinAssign(groupName,     "          " + r16StateNextName + " = " + RESPONSE_FLUSH + ";");
		this.addCombinAssign(groupName,     "      end"); 
		this.addCombinAssign(groupName,     "      " + resDataDlyName[0] + " = " + outFifoDataName[1] + ";");
		this.addCombinAssign(groupName,     "    end"); 
		
		// RESPONSE_FLUSH  - shift data out of bypass fifo 
		this.addCombinAssign(groupName,     "  " + RESPONSE_FLUSH + ": begin  // RESPONSE_FLUSH"); 
        // transfer out 1 word
		this.addCombinAssign(groupName,     "      " + outFifoAdvanceName + " =  1'b1;");  // save addr word
		this.addCombinAssign(groupName,     "      " + r16StateNextName + " = " + IDLE + ";");
		// set the RES_output to fifo element depending on xfer addr size 
		this.addCombinAssign(groupName,     "      " + resValidDlyName[0] + " =  1'b1;");  // res is valid
		this.addCombinAssign(groupName,     "      " + resDataDlyName[0] + " = " + outFifoDataName[1] + ";");
		this.addCombinAssign(groupName,     "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + r16StateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 				

		this.addScalarWire(pioInterfaceReName);  //  read enable internal interface
		this.addScalarWire(pioInterfaceWeName);  //  write enable internal interface
		// generate re/we assigns directly or from delayed versions if clock gating is enabled
		assignReadWriteRequests(r16pioInterfaceReName, r16pioInterfaceWeName);
		
	}
	
	/** generate re/we assigns directly or from delayed versions if clock gating is enabled */
	private void assignReadWriteRequests(String readReqStr, String writeReqStr) {
		// if gated logic clock, create an enable output and delay read/write activation 
		if (ExtParameters.systemverilogUseGatedLogicClk()) {
			
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
				if (dly == 1) {
					this.addRegAssign("clock gate delay",  cgateDelayedReName + dly + " <= #1 " + readReqStr + ";");
					this.addRegAssign("clock gate delay",  cgateDelayedWeName + dly + " <= #1 " + writeReqStr + ";");
				}
				else {
					this.addRegAssign("clock gate delay",  cgateDelayedReName + dly + " <= #1 " + cgateDelayedReName + (dly - 1) + ";");
					this.addRegAssign("clock gate delay",  cgateDelayedWeName + dly + " <= #1 " + cgateDelayedWeName + (dly - 1) + ";");
				}
			}
			
			// create gate enable output on first delay
			String suffix = (builder.getBuilderID() > 0)? "_" + builder.getBuilderID() : "";
			this.addScalarTo(SystemVerilogBuilder.PIO, "gclk_enable" + suffix);  // clock enable output
			this.addWireAssign("gclk_enable" + suffix + " = " + cgateDelayedReName + "1 | " + cgateDelayedWeName + "1;");
			
			// assign the delayed read/write requests		
			this.addWireAssign(pioInterfaceReName + " = " + readReqStr + "&" + cgateDelayedReName + maxDelay + ";"); 
			this.addWireAssign(pioInterfaceWeName + " = " + writeReqStr + "&" + cgateDelayedWeName + maxDelay + ";");
			
		}
		// otherwise just generate request signals with no delay
		else {
			this.addWireAssign(pioInterfaceReName + " = " + readReqStr + ";");   
			this.addWireAssign(pioInterfaceWeName + " = " + writeReqStr + ";");
		}
	}


	// ------------------------------ decoder external hw interface methods -------------------------------
	
	/** generate common external interface constructs */
	public void generateBaseExternalInterface(AddressableInstanceProperties regProperties) {
		   
		   // detect if a DEFAULT interface type so default IO are created
		   boolean isDEFAULT = regProperties.hasExternalType(ExtType.DEFAULT);
			   
		   // create verilog names associated with external
		   String decodeToHwName = regProperties.getFullSignalName(DefSignalType.D2H_DATA);   // write data                   
		   String decodeToHwWeName = regProperties.getFullSignalName(DefSignalType.D2H_WE); // we
		   String decodeToHwReName = regProperties.getFullSignalName(DefSignalType.D2H_RE); // re
		   String hwToDecodeName = regProperties.getFullSignalName(DefSignalType.H2D_DATA); // read data
		   String hwToDecodeAckName = regProperties.getFullSignalName(DefSignalType.H2D_ACK); // ext ack
		   String hwToDecodeNackName = regProperties.getFullSignalName(DefSignalType.H2D_NACK); // ext nack
		   
		   // add default signals to module IO lists if DEFAULT
		   if (isDEFAULT) {
			   this.addVectorTo(SystemVerilogBuilder.HW, decodeToHwName, 0, regProperties.getMaxRegWidth());    // add write data to decode to hw signal list 
			   this.addScalarTo(SystemVerilogBuilder.HW, decodeToHwWeName);     
			   this.addScalarTo(SystemVerilogBuilder.HW, decodeToHwReName);     
			   this.addVectorFrom(SystemVerilogBuilder.HW, hwToDecodeName, 0, regProperties.getMaxRegWidth());    // add read data to hw to decode signal list 
			   this.addScalarFrom(SystemVerilogBuilder.HW, hwToDecodeAckName);     
			   this.addScalarFrom(SystemVerilogBuilder.HW, hwToDecodeNackName);     			   
		   }
		   
		   // register the outputs and assign output reg values
		   this.addVectorReg(decodeToHwName, 0, regProperties.getMaxRegWidth());  
		   this.addScalarReg(decodeToHwWeName);  
		   this.addScalarReg(decodeToHwReName);  

		   // add next signal assignments also
		   this.addVectorReg(decodeToHwName + "_next", 0, regProperties.getMaxRegWidth());  
		   this.addScalarReg(decodeToHwWeName + "_next");  
		   this.addScalarReg(decodeToHwReName + "_next");  
		   // reset output signals
		   this.addResetAssign("external i/f", builder.getDefaultReset(), decodeToHwWeName + " <= #1  1'b0;" );   
		   this.addResetAssign("external i/f", builder.getDefaultReset(), decodeToHwReName + " <= #1  1'b0;" );

		   String ackInhibitStr = "~" + hwToDecodeAckName + " & ~" + hwToDecodeNackName;
		   this.addRegAssign("external i/f",  decodeToHwName + " <= #1  " + decodeToHwName + "_next;");  // assign next to flop
		   this.addRegAssign("external i/f",  decodeToHwWeName + " <= #1  " + decodeToHwWeName + "_next & " + ackInhibitStr  + ";");  // assign next to flop
		   this.addRegAssign("external i/f",  decodeToHwReName + " <= #1  " + decodeToHwReName + "_next & " + ackInhibitStr  + ";");  // assign next to flop

		   // if size of external range is greater than one reg we'll need an external address  
		   if ( (regProperties.getExtAddressWidth() > 0)  && !regProperties.isSingleExtReg()) {  
			   String decodeToHwAddrName = regProperties.getFullSignalName(DefSignalType.D2H_ADDR); // address
			   if (isDEFAULT) this.addVectorTo(SystemVerilogBuilder.HW, decodeToHwAddrName, regProperties.getExtLowBit(), regProperties.getExtAddressWidth());     
			   this.addVectorReg(decodeToHwAddrName, regProperties.getExtLowBit(), regProperties.getExtAddressWidth());  
			   this.addVectorReg(decodeToHwAddrName + "_next", regProperties.getExtLowBit(), regProperties.getExtAddressWidth());  
			   this.addRegAssign("external i/f",  decodeToHwAddrName + " <= #1  " + decodeToHwAddrName + "_next"  + ";");  // assign next to flop
		   }	
		   
		   // if size of max pio transaction is greater than one word need to add transaction size/retry info 
		   if ( (regProperties.getMaxRegWidth() > ExtParameters.getMinDataSize()) && !regProperties.isSingleExtReg()) { 
			   String decodeToHwTransactionSizeName = regProperties.getFullSignalName(DefSignalType.D2H_SIZE);  // size of r/w transaction in words
			   String hwToDecodeTransactionSizeName = regProperties.getFullSignalName(DefSignalType.H2D_RETSIZE);  // size of return read transaction in words

			   if (isDEFAULT) this.addVectorTo(SystemVerilogBuilder.HW, decodeToHwTransactionSizeName, 0, Utils.getBits(regProperties.getMaxRegWordWidth()));     
			   this.addVectorReg(decodeToHwTransactionSizeName, 0, Utils.getBits(regProperties.getMaxRegWordWidth()));  
			   this.addVectorReg(decodeToHwTransactionSizeName + "_next", 0, Utils.getBits(regProperties.getMaxRegWordWidth()));  
			   this.addRegAssign("external i/f",  decodeToHwTransactionSizeName + " <= #1  " + decodeToHwTransactionSizeName + "_next"  + ";");  // assign next to flop

			   if (isDEFAULT) this.addVectorFrom(SystemVerilogBuilder.HW, hwToDecodeTransactionSizeName, 0, Utils.getBits(regProperties.getMaxRegWordWidth()));     
			   this.addVectorReg(hwToDecodeTransactionSizeName + "_d1", 0, Utils.getBits(regProperties.getMaxRegWordWidth()));  
			   this.addResetAssign("external i/f", builder.getDefaultReset(), hwToDecodeTransactionSizeName + "_d1 <= #1  " + Utils.getBits(regProperties.getMaxRegWordWidth()) +"'b0;");  // reset input size flop
			   this.addRegAssign("external i/f",  hwToDecodeTransactionSizeName + "_d1 <= #1  " + hwToDecodeTransactionSizeName + ";");  // assign input size to flop
		   }	
	}

	/** generate BBV5 external interface shim logic */  
	public void generateExternalInterface_BBV5(AddressableInstanceProperties addrInstProperties) {
		//Jrdl.warnMessage("SystemVerilogBuilder gen_BBV5, ext type=" + regProperties.getExternalType() + ", id=" + regProperties.getId());
		
		// create verilog names associated with external
		String decodeToHwName = addrInstProperties.getFullSignalName(DefSignalType.D2H_DATA);   // write data                   
		String decodeToHwWeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_WE); // we
		String decodeToHwReName = addrInstProperties.getFullSignalName(DefSignalType.D2H_RE); // re
		String hwToDecodeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_DATA); // read data
		String hwToDecodeAckName = addrInstProperties.getFullSignalName(DefSignalType.H2D_ACK); // ext ack
		String hwToDecodeNackName = addrInstProperties.getFullSignalName(DefSignalType.H2D_NACK); // ext nack
		
		// create incoming signals (default IOs that now need to be set)
		this.addVectorWire(hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		this.addScalarReg(hwToDecodeAckName);
		this.addScalarReg(hwToDecodeNackName);

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
		this.addScalarTo(SystemVerilogBuilder.HW, topBackboneReqName);     
		this.addScalarTo(SystemVerilogBuilder.HW, topBackboneRdName);     //1=read, 0=write
		this.addScalarTo(SystemVerilogBuilder.HW, topBackboneWrDvldName);     //write data valid
		this.addVectorTo(SystemVerilogBuilder.HW, topBackboneWrWidthName, 0, 4);    //number of words 
		this.addVectorTo(SystemVerilogBuilder.HW, topBackboneWrDataName, 0, 32);    // add write data to decode to hw signal list 
		this.addVectorTo(SystemVerilogBuilder.HW, topBackboneAddrName, 0, ExtParameters.getLeafAddressSize());    // address 

		//this.addScalarFrom(SystemVerilogBuilder.HW, backboneTopCmdReturnedName);   
		this.addScalarFrom(SystemVerilogBuilder.HW, backboneTopReqName);     // stays high while all res data xfered
		this.addScalarFrom(SystemVerilogBuilder.HW, backboneTopDvldName);     // data valid - ony high on valid 32b data 
		this.addScalarFrom(SystemVerilogBuilder.HW, backboneTopNack);     
		this.addScalarFrom(SystemVerilogBuilder.HW, backboneTopWrRetry);      
		//this.addVectorFrom(SystemVerilogBuilder.HW, backboneTopStatus, 0, 4);     // 3=retry, 2=ack, 1=error, 0=nack
		this.addVectorFrom(SystemVerilogBuilder.HW, backboneTopWidthName, 0, 4);   //number of words  
		this.addVectorFrom(SystemVerilogBuilder.HW, backboneTopDataName, 0, 32);   //data  
		
		// use a timeout input if option specified
		int backboneTopTimeoutBits = 8; // set timeout counter size - default to 8 bits
		String backboneTopTimeoutValue = "8'hFF";  
		if (ExtParameters.sysVerBBV5TimeoutInput()) {
			//System.out.println("SystemVerilogDecodeModule generateExternalInterface_BBV5: using timeout input for " + regProperties.getBaseName());
			backboneTopTimeoutBits = 12; // set timeout counter size
			backboneTopTimeoutValue = "bb2d_" + addrInstProperties.getBaseName() + "_timeout";  // use input signal for timeout
			this.addVectorFrom(SystemVerilogBuilder.HW, backboneTopTimeoutValue, 0, backboneTopTimeoutBits);   //timeout input  
		}
		// if size of external range is greater than one reg we'll need to set external address bits 
		RegNumber newBase = addrInstProperties.getFullBaseAddress();  
		//RegNumber newBase = new RegNumber(ExtParameters.getLeafBaseAddress());  
		//newBase.setVectorLen(ExtParameters.getLeafAddressSize());
		//newBase.add(regProperties.getBaseAddress());
		if ( (addrInstProperties.getExtAddressWidth() > 0)  && !addrInstProperties.isSingleExtReg()) {
			String decodeToHwAddrName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ADDR); // address
			//decodes.get(regProperties.getBaseName() + " (external BBV5)", topBackboneAddrName + " = " + 
			//    newBase.toFormat(NumBase.Hex, NumFormat.Verilog) + "& " + decodeToHwAddrName + ";");    
			this.addWireAssign(topBackboneAddrName + " = " + newBase.toFormat(NumBase.Hex, NumFormat.Verilog) + " | (" + decodeToHwAddrName + " << " + addrInstProperties.getExtLowBit() + ");");
			//System.out.println("SystemVerilogBuilder generateExternalInterface_BBV5:base address=" + regProperties.getBaseAddress());
		}
		else 
			this.addWireAssign(topBackboneAddrName + " = " + newBase.toFormat(NumBase.Hex, NumFormat.Verilog) + ";");

		
		// if size of max pio transaction is greater than one word need to set transaction size/retry info 
		int regWords = addrInstProperties.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		//System.out.println("SystemVerilogBuilder generateExternalInterface_BBV5: regwords=" + regWords + ", bits=" + regWordBits);
		if ((regWords > 1) && !addrInstProperties.isSingleExtReg()) {
			String decodeToHwTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_SIZE);  // size of r/w transaction in words
			String hwToDecodeTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE);  // size of return read transaction in words
			this.addWireAssign(topBackboneWrWidthName + " = 4'b0 | " + decodeToHwTransactionSizeName + ";");
			// create inbound size signal
			this.addVectorWire(hwToDecodeTransactionSizeName, 0, regWordBits);
			this.addWireAssign(hwToDecodeTransactionSizeName + " = " + backboneTopWidthName + SystemVerilogSignal.genRefArrayString(0, regWordBits) + ";");
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
		this.addWireAssign(hwToDecodeName + " = " + bbRdAccumName + ";");

		// if wide regs in ext region, need word count
		if (regWordBits > 0) {
			this.addVectorReg(bbWordCntName, 0, regWordBits);  
			this.addVectorReg(bbWordCntNextName, 0, regWordBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), bbWordCntName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  bbWordCntName + " <= #1  " + bbWordCntNextName + ";");  			
		}
		
		// if an 8 bit ring, need to count wait cycles
		boolean is8bit = (addrInstProperties.getExternalType().getParm1() == 8);
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
		this.addCombinAssign(groupName,  hwToDecodeAckName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  hwToDecodeNackName + " =  1'b0;"); 
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
		this.addCombinAssign(groupName, "      if (" + decodeToHwReName + ") begin");  
		this.addCombinAssign(groupName, "        " + topBackboneReqName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + topBackboneRdName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + bbStateNextName + " = " + RES_WAIT + ";");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "      else if (" + decodeToHwWeName + ") begin");  
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
				this.addCombinAssign(groupName, "        " + topBackboneWrDataName + " = " + decodeToHwName + "[" + (word*32 + 31) + ":" + (word*32) + "];"); 
			}
		}
		else  // otherwise just use lower 32b
			this.addCombinAssign(groupName, "      " + topBackboneWrDataName + " = " + decodeToHwName + "[31:0];"); 
			
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
		this.addCombinAssign(groupName, "      " + topBackboneRdName + " = " + decodeToHwReName + ";"); // assert if rd  
		this.addCombinAssign(groupName, "      " + "if ("  + bbTimeoutCntName + " != " + backboneTopTimeoutValue + ")"); 
		this.addCombinAssign(groupName, "        " + bbTimeoutCntNextName + " = " + bbTimeoutCntName + " + " + backboneTopTimeoutBits + "'b1;");  // run timeout counter
		if (regWordBits > 0)
			this.addCombinAssign(groupName,  bbWordCntNextName + " = "  + regWordBits + "'b0;"); // reset word count
		// if a write or nack/error/retry then we're done else wait for read data
		this.addCombinAssign(groupName, "      " + "if (" + backboneTopReqName + ") begin");  
		this.addCombinAssign(groupName, "        " + "if (" + backboneTopNack + ")");  
		this.addCombinAssign(groupName, "          " + bbStateNextName + " = " + RES_DONE_NACK + ";");  
		this.addCombinAssign(groupName, "        " + "else if (" + decodeToHwWeName + " | " + backboneTopWrRetry + ")");  
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
		this.addCombinAssign(groupName, "      " + hwToDecodeAckName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + bbStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_NACK
		this.addCombinAssign(groupName, "  " + RES_DONE_NACK + ": begin // RES_DONE_NACK");
		this.addCombinAssign(groupName, "      " + hwToDecodeNackName + " = 1'b1;");   
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
		
		// create verilog names associated with default external
		String decodeToHwName = addrInstProperties.getFullSignalName(DefSignalType.D2H_DATA);   // write data                   
		String decodeToHwWeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_WE); // we
		String decodeToHwReName = addrInstProperties.getFullSignalName(DefSignalType.D2H_RE); // re
		String hwToDecodeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_DATA); // read data
		String hwToDecodeAckName = addrInstProperties.getFullSignalName(DefSignalType.H2D_ACK); // ext ack
		String hwToDecodeNackName = addrInstProperties.getFullSignalName(DefSignalType.H2D_NACK); // ext nack
		String decodeToHwAddrName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ADDR); // address
		
		// create incoming signals (default IOs that now need to be set)
		this.addVectorWire(hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		this.addScalarReg(hwToDecodeAckName);
		this.addScalarReg(hwToDecodeNackName);

		// create module IOs
		String decodeToSrRdName = "d2sr_" + addrInstProperties.getBaseName() + "_rd";                      
		String decodeToSrWrName = "d2sr_" + addrInstProperties.getBaseName() + "_wr";                      
		String decodeToSrWrDataName = "d2sr_" + addrInstProperties.getBaseName() + "_wr_data";                      
		String decodeToSrAddrName = "d2sr_" + addrInstProperties.getBaseName() + "_addr"; 
		
		String srToDecodeAck = "sr2d_" + addrInstProperties.getBaseName() + "_ack";                      
		String srToDecodeNack = "sr2d_" + addrInstProperties.getBaseName() + "_nack";                      
		String srToDecodeRdDataName = "sr2d_" + addrInstProperties.getBaseName() + "_rd_data";  
		
		//  inputs
		this.addScalarFrom(SystemVerilogBuilder.HW, srToDecodeAck);   // ack
		this.addScalarFrom(SystemVerilogBuilder.HW, srToDecodeNack);   // nack
		this.addVectorFrom(SystemVerilogBuilder.HW, srToDecodeRdDataName, 0, addrInstProperties.getMaxRegWidth());   // read data  
		
		//  outputs
		this.addScalarTo(SystemVerilogBuilder.HW, decodeToSrRdName);  // read pulse    
		this.addScalarTo(SystemVerilogBuilder.HW, decodeToSrWrName);  // write pulse  
		this.addVectorTo(SystemVerilogBuilder.HW, decodeToSrWrDataName, 0, addrInstProperties.getMaxRegWidth());    // write data  
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
		this.addVectorTo(SystemVerilogBuilder.HW, decodeToSrAddrName, regWordBits + addrInstProperties.getExtLowBit(), srAddrBits);    // address  
		
		// assign wr_data and addr outputs
		this.addWireAssign(decodeToSrWrDataName + " = " + decodeToHwName +  ";");
		this.addWireAssign(decodeToSrAddrName + " = " + decodeToHwAddrName + SystemVerilogSignal.genRefArrayString(regWordBits + addrInstProperties.getExtLowBit(), srAddrBits) + ";");

		// generate valid_size by comparing decodeToHwTransactionSizeName value with regWordWidth - 1
		String srValidSizeName = "dsr_" + addrInstProperties.getBaseName() + "_valid_size"; 
		this.addScalarWire(srValidSizeName);
		if ((addrInstProperties.getMaxRegWordWidth() > 1) && !addrInstProperties.isSingleExtReg()) {
			String decodeToHwTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_SIZE);  // size of w transaction in words
			this.addWireAssign(srValidSizeName + " = (" + decodeToHwTransactionSizeName + " == " + srRegSize + ");");
		}
		else
			this.addWireAssign(srValidSizeName + " = 1'b1;");  // not a wide reg space, so always valid
		
		// set fixed external reg size
		if ((addrInstProperties.getMaxRegWordWidth() > 1) && !addrInstProperties.isSingleExtReg()) {
			String hwToDecodeTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE);  // size of return read transaction in words
			this.addVectorWire(hwToDecodeTransactionSizeName, 0, regWordBits);
			this.addWireAssign(hwToDecodeTransactionSizeName + " = " + srRegSize + ";");
		}
		
		// set rd_data input
		this.addWireAssign(hwToDecodeName + " = " + srToDecodeRdDataName +  ";");

		// generate valid_addr - compare decodeToSrAddrName to max address of space
		String srValidAddrName = "dsr_" + addrInstProperties.getBaseName() + "_valid_addr"; 
		this.addScalarWire(srValidAddrName);
		RegNumber extMaxAddr = builder.getExternalRegBytes().getSubVector(addrInstProperties.getExtLowBit(), addrInstProperties.getExtAddressWidth());
		if (extMaxAddr.isNonZero()) // if max addr is less than allowed range, add compare stmt
			this.addWireAssign(srValidAddrName + " = (" + decodeToHwAddrName + " < " + extMaxAddr.toFormat(NumBase.Hex, NumFormat.Verilog) + ");");
		else
			this.addWireAssign(srValidAddrName + " = 1'b1;");
		//System.out.println("SystemVerilogBuilder gen.._SRAM: ext size (Bytes)=" + getExternalRegBytes() + ", extMax=" + extMaxAddr.toFormat(NumBase.Hex, NumFormat.Verilog));  

		// generate rd/wr pulses if valid_size and valid_addr
		// generate ack/nack based on address compare, valid_size, valid_addr, rd_ack, and wr_status
		
		// create state machine vars
		String srStateName = "sr_" + addrInstProperties.getBaseName() + "_state";                      
		String srStateNextName = "sr_" + addrInstProperties.getBaseName() + "_state_next";                      

		String groupName = addrInstProperties.getBaseName() + " sr i/f";
		int stateBits = 2;
		this.addVectorReg(srStateName, 0, stateBits);  
		this.addVectorReg(srStateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), srStateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  srStateName + " <= #1  " + srStateNextName + ";");
		
		// state machine init values
		this.addCombinAssign(groupName,  srStateNextName + " = " + srStateName + ";");  
		this.addCombinAssign(groupName,  decodeToSrRdName + " =  1'b0;");  
		this.addCombinAssign(groupName,  decodeToSrWrName + " =  1'b0;");  
		this.addCombinAssign(groupName,  hwToDecodeAckName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  hwToDecodeNackName + " =  1'b0;"); 
		
		// state machine
		String IDLE = stateBits + "'h0"; 
		String ACK_WAIT = stateBits + "'h1"; 
		String NACK = stateBits + "'h2"; 
		String ACK = stateBits + "'h3";
		
		this.addCombinAssign(groupName, "case (" + srStateName + ")"); 
		
		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");
		this.addCombinAssign(groupName, "      if (" + decodeToHwReName + ") begin");  
		this.addCombinAssign(groupName, "        if (~" + srValidAddrName + ") " + srStateNextName + " = " + NACK + ";");  
		//this.addCombinAssign(groupName, "        else if (~" + srValidSizeName + ") " + srStateNextName + " = " + ACK + ";");  
		this.addCombinAssign(groupName, "        else begin");  
		this.addCombinAssign(groupName, "        " + decodeToSrRdName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + srStateNextName + " = " + ACK_WAIT + ";");  
		this.addCombinAssign(groupName, "        end");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "      else if (" + decodeToHwWeName + ") begin");  
		this.addCombinAssign(groupName, "        if (~" + srValidAddrName + ") " + srStateNextName + " = " + NACK + ";");  
		this.addCombinAssign(groupName, "        else if (~" + srValidSizeName + ") " + srStateNextName + " = " + ACK + ";");  
		this.addCombinAssign(groupName, "        else begin");  
		this.addCombinAssign(groupName, "        " + decodeToSrWrName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + srStateNextName + " = " + ACK_WAIT + ";");  
		this.addCombinAssign(groupName, "        end");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "    end"); 
		
		// ACK_WAIT
		this.addCombinAssign(groupName, "  " + ACK_WAIT + ": begin // ACK_WAIT");
		this.addCombinAssign(groupName, "      if (" + srToDecodeAck + ") begin");  
		this.addCombinAssign(groupName, "        " + hwToDecodeAckName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + srStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "      else if (" + srToDecodeNack + ") begin");  
		this.addCombinAssign(groupName, "        " + hwToDecodeNackName + " =  1'b1;");  
		this.addCombinAssign(groupName, "        " + srStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "      end");  
		this.addCombinAssign(groupName, "    end"); 
		
		// NACK
		this.addCombinAssign(groupName, "  " + NACK + ": begin // NACK");
		this.addCombinAssign(groupName, "      " + hwToDecodeNackName + " =  1'b1;");  
		this.addCombinAssign(groupName, "      " + srStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// ACK
		this.addCombinAssign(groupName, "  " + ACK + ": begin // ACK");
		this.addCombinAssign(groupName, "      " + hwToDecodeAckName + " =  1'b1;");  
		this.addCombinAssign(groupName, "      " + srStateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + srStateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 
		
	}

	/** generate SERIAL8 external interface shim logic */  
	public void generateExternalInterface_SERIAL8(AddressableInstanceProperties addrInstProperties) {   
		//Jrdl.warnMessage("SystemVerilogBuilder gen_SERIAL8, ext type=" + regProperties.getExternalType()  + ", id=" + regProperties.getId());
		
		// create verilog names associated with external
		String decodeToHwName = addrInstProperties.getFullSignalName(DefSignalType.D2H_DATA);   // write data                   
		String decodeToHwWeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_WE); // we
		String decodeToHwReName = addrInstProperties.getFullSignalName(DefSignalType.D2H_RE); // re
		String hwToDecodeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_DATA); // read data
		String hwToDecodeAckName = addrInstProperties.getFullSignalName(DefSignalType.H2D_ACK); // ext ack
		String hwToDecodeNackName = addrInstProperties.getFullSignalName(DefSignalType.H2D_NACK); // ext nack
		String decodeToHwAddrName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ADDR); // address
		String decodeToHwTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_SIZE);  // size of r/w transaction in words
		String hwToDecodeTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE);  // size of return read transaction in words
		
		// create incoming signals (default IOs that now need to be set)
		this.addVectorWire(hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		this.addScalarReg(hwToDecodeAckName);
		this.addScalarReg(hwToDecodeNackName);
		
		// create module IOs
		String serial8CmdValidName = "d2s_" + addrInstProperties.getBaseName() + "_cmd_valid";                      
		String serial8CmdDataName = "d2s_" + addrInstProperties.getBaseName() + "_cmd_data";                      
		
		String serial8ResValidName = "s2d_" + addrInstProperties.getBaseName() + "_res_valid";                      
		String serial8ResDataName = "s2d_" + addrInstProperties.getBaseName() + "_res_data";                      
		
		//  outputs
		this.addScalarTo(SystemVerilogBuilder.HW, serial8CmdValidName);     // stays high while all cmd addr/data/cntl xferred 
		this.addVectorTo(SystemVerilogBuilder.HW, serial8CmdDataName, 0, 8);  

		// inputs  
		this.addScalarFrom(SystemVerilogBuilder.HW, serial8ResValidName);     // stays high while all res cntl/data xferred
		this.addVectorFrom(SystemVerilogBuilder.HW, serial8ResDataName, 0, 8);     
		
		// calculate number of 8b xfers required for address (same for all transctions to this i/f)
		int addrXferCount = 0;
		if ( (addrInstProperties.getExtAddressWidth() > 0)  && !addrInstProperties.isSingleExtReg()) {
			addrXferCount = (int) Math.ceil(addrInstProperties.getExtAddressWidth()/8.0);
			//System.out.println("SystemVerilogBuilder generateExternalInterface_Serial8: addr width=" + regProperties.getExtAddressWidth() + ", addr count=" + addrXferCount);
		}
		
		// compute max transaction size in 32b words and number of bits to represent (4 max) 
		int regWords = addrInstProperties.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = (regWords > 1) && !addrInstProperties.isSingleExtReg();  // if transaction sizes need to be sent/received
		
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
		int delayCount = addrInstProperties.getExternalType().getParm1();
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
		this.addWireAssign(hwToDecodeName + " = " + s8RdAccumName + ";");
		
		//  will need to capture res width
		String hwToDecodeTransactionSizeNextName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE) + "_next";  // res size will be set in sm
		if (useTransactionSize) {
			this.addVectorReg(hwToDecodeTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(hwToDecodeTransactionSizeNextName, 0, regWordBits);  // res size will be set in sm
			this.addResetAssign(groupName, builder.getDefaultReset(), hwToDecodeTransactionSizeName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  hwToDecodeTransactionSizeName + " <= #1  " + hwToDecodeTransactionSizeNextName + ";");  
		}

		// will need to capture nack state
		String rxNackCaptureName = hwToDecodeNackName + "_early"; 
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
		int maxDataXferCount = regWords * 4;
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
		this.addCombinAssign(groupName,  hwToDecodeAckName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  hwToDecodeNackName + " =  1'b0;");
		this.addCombinAssign(groupName,  rxNackCaptureNextName + " = " + rxNackCaptureName + ";");  
		this.addCombinAssign(groupName,  s8RdAccumNextName + " = " + s8RdAccumName + ";");
		// init res size capture
		if (useTransactionSize)
			this.addCombinAssign(groupName,  hwToDecodeTransactionSizeNextName + " = " + hwToDecodeTransactionSizeName + ";"); 
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
		this.addCombinAssign(groupName, "      if (" + decodeToHwReName + " | " + decodeToHwWeName + ") begin");  
		this.addCombinAssign(groupName, "        " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set  
		this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + "[7] = " + decodeToHwWeName + ";");  // bit 7 = write
		this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + "[6:4] = 3'd" + addrXferCount + ";");  // bits 6:4 = address xfers  
		if (useTransactionSize)
		    this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + " = " + decodeToHwTransactionSizeName + ";");  // bits 3:0 = transaction size
        // if an address then send it next
		if (addrXferCount > 0) {  
			this.addCombinAssign(groupName, "        " + s8StateNextName + " = " + CMD_ADDR + ";");  
		}
		// otherwise send data if a write or wait for read response
		else {
			this.addCombinAssign(groupName, "        if (" + decodeToHwWeName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
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
					this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + " = " + decodeToHwAddrName + SystemVerilogSignal.genRefArrayString(lowbit, size) + ";");
				}
	
				// if addr done, send data if a write or wait for read response 
				this.addCombinAssign(groupName, "      if (" + s8AddrCntName + " == " + addrXferCountBits + "'d" + (addrXferCount-1) + ") begin");
				this.addCombinAssign(groupName, "        if (" + decodeToHwWeName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "        else " + s8StateNextName + " = " + RES_WAIT + ";");
				this.addCombinAssign(groupName, "      end"); 
			}
			// else a single addr xfer
			else {
				// set address
				this.addCombinAssign(groupName, "      " + cmdDataDlyName[0] + SystemVerilogSignal.genRefArrayString(0, addrInstProperties.getExtAddressWidth()) + " = " + decodeToHwAddrName  + ";");  
				// next send data if a write or wait for read response 
				this.addCombinAssign(groupName, "      if (" + decodeToHwWeName + ") " + s8StateNextName + " = " + CMD_DATA + ";");  
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
			this.addCombinAssign(groupName, "        "  + cmdDataDlyName[0] + " = " + decodeToHwName + SystemVerilogSignal.genRefArrayString(8*idx, 8) + ";");
		}
		// if done, move to res wait
		String finalCntStr = "2'b11";
		if (useTransactionSize) finalCntStr = "{" + decodeToHwTransactionSizeName + ", 2'b11}";
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
		this.addCombinAssign(groupName, "        if (" + decodeToHwReName + ") " + s8StateNextName + " = " + RES_READ + ";");  
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
		if (useTransactionSize) 
		    this.addCombinAssign(groupName, "      if (" + s8DataCntName + " == {" + hwToDecodeTransactionSizeName + ", 2'b11}) " + s8StateNextName + " = " + RES_DONE_ACK + ";");
		else
		    this.addCombinAssign(groupName, "      if (" + s8DataCntName + " == 2'b11) " + s8StateNextName + " = " + RES_DONE_ACK + ";");
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_ACK
		this.addCombinAssign(groupName, "  " + RES_DONE_ACK + ": begin // RES_DONE_ACK");
		this.addCombinAssign(groupName, "      " + hwToDecodeAckName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + s8StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_NACK
		this.addCombinAssign(groupName, "  " + RES_DONE_NACK + ": begin // RES_DONE_NACK");
		this.addCombinAssign(groupName, "      " + hwToDecodeNackName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + s8StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + s8StateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 				
	}

	/** generate RING16 external interface shim logic */    
	public void generateExternalInterface_RING16(AddressableInstanceProperties addrInstProperties) {   
		//Jrdl.warnMessage("SystemVerilogBuilder gen_RING16, ext type=" + regProperties.getExternalType()  + ", id=" + regProperties.getId());
		
		// create verilog names associated with external
		String decodeToHwName = addrInstProperties.getFullSignalName(DefSignalType.D2H_DATA);   // write data                   
		String decodeToHwWeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_WE); // we
		String decodeToHwReName = addrInstProperties.getFullSignalName(DefSignalType.D2H_RE); // re
		String hwToDecodeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_DATA); // read data
		String hwToDecodeAckName = addrInstProperties.getFullSignalName(DefSignalType.H2D_ACK); // ext ack
		String hwToDecodeNackName = addrInstProperties.getFullSignalName(DefSignalType.H2D_NACK); // ext nack
		String decodeToHwAddrName = addrInstProperties.getFullSignalName(DefSignalType.D2H_ADDR); // address
		String decodeToHwTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.D2H_SIZE);  // size of r/w transaction in words
		String hwToDecodeTransactionSizeName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE);  // size of return read transaction in words
		
		// create incoming signals (default IOs that now need to be set)
		this.addVectorWire(hwToDecodeName, 0, addrInstProperties.getMaxRegWidth());
		this.addScalarReg(hwToDecodeAckName);
		this.addScalarReg(hwToDecodeNackName);
		
		// create module IOs
		String ring16CmdValidName = "d2r_" + addrInstProperties.getBaseName() + "_cmd_valid";                      
		String ring16CmdDataName = "d2r_" + addrInstProperties.getBaseName() + "_cmd_data";                      
		
		String ring16ResValidName = "r2d_" + addrInstProperties.getBaseName() + "_res_valid";                      
		String ring16ResDataName = "r2d_" + addrInstProperties.getBaseName() + "_res_data";                      
		
		//  outputs
		this.addScalarTo(SystemVerilogBuilder.HW, ring16CmdValidName);     // stays high while all cmd addr/data/cntl xferred 
		this.addVectorTo(SystemVerilogBuilder.HW, ring16CmdDataName, 0, 16);  

		// inputs  
		this.addScalarFrom(SystemVerilogBuilder.HW, ring16ResValidName);     // stays high while all res cntl/data xferred
		this.addVectorFrom(SystemVerilogBuilder.HW, ring16ResDataName, 0, 16);     
		
		// calculate number of 8b xfers required for address (same for all transctions to this i/f)
		int addrXferCount = 0;
		if ( (addrInstProperties.getExtAddressWidth() > 0)  && !addrInstProperties.isSingleExtReg()) {
			addrXferCount = (int) Math.ceil(addrInstProperties.getExtAddressWidth()/16.0);
			//System.out.println("SystemVerilogBuilder generateExternalInterface_ring16: addr width=" + regProperties.getExtAddressWidth() + ", addr count=" + addrXferCount);
		}
		
		// compute max transaction size in 32b words and number of bits to represent (4 max) 
		int regWords = addrInstProperties.getMaxRegWordWidth();
		int regWordBits = Utils.getBits(regWords);
		boolean useTransactionSize = (regWords > 1) && !addrInstProperties.isSingleExtReg();  // if transaction sizes need to be sent/received
		
		// now create state machine vars
		String r16StateName = "r16_" + addrInstProperties.getBaseName() + "_state";                      
		String r16StateNextName = "r16_" + addrInstProperties.getBaseName() + "_state_next";                      
		String r16AddrCntName = "r16_" + addrInstProperties.getBaseName() + "_addr_cnt";                      
		String r16AddrCntNextName = "r16_" + addrInstProperties.getBaseName() + "_addr_cnt_next";                      
		String r16DataCntName = "r16_" + addrInstProperties.getBaseName() + "_data_cnt";                      
		String r16DataCntNextName = "r16_" + addrInstProperties.getBaseName() + "_data_cnt_next"; 

		String groupName = addrInstProperties.getBaseName() + " ring16 i/f";  
		int stateBits = 3;
		this.addVectorReg(r16StateName, 0, stateBits);  
		this.addVectorReg(r16StateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), r16StateName + " <= #1  " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  r16StateName + " <= #1  " + r16StateNextName + ";");  

		// create delayed cmd signals
		int delayCount = addrInstProperties.getExternalType().getParm1();
		String [] cmdValidDlyName = new String [delayCount+1];
		String [] cmdDataDlyName = new String [delayCount+1];
		for (int idx=0; idx<delayCount+1; idx++) {
			cmdValidDlyName[idx] = "r16_" + addrInstProperties.getBaseName() + "_cmdValid_dly" + idx;
			cmdDataDlyName[idx] = "r16_" + addrInstProperties.getBaseName() + "_cmdData_dly" + idx;
		}
		this.addScalarReg(cmdValidDlyName[0]);  // cmd delay 0 is set in state machine
		this.addVectorReg(cmdDataDlyName[0], 0, 16); 
		for (int idx=1; idx<delayCount+1; idx++) {
			this.addScalarReg(cmdValidDlyName[idx]);  
			this.addVectorReg(cmdDataDlyName[idx], 0, 16); 
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), cmdDataDlyName[idx] + " <= #1  16'b0;");  
			this.addRegAssign(groupName,  cmdValidDlyName[idx] + " <= #1  " + cmdValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  cmdDataDlyName[idx] + " <= #1  " + cmdDataDlyName[idx-1] + ";");  
		}
        // assign r16 outputs outputs to delayed versions 
		this.addScalarWire(ring16CmdValidName);  
		this.addVectorWire(ring16CmdDataName, 0, 16); 
		this.addWireAssign(ring16CmdValidName + " = " + cmdValidDlyName[delayCount] + ";");
		this.addWireAssign(ring16CmdDataName + " = " + cmdDataDlyName[delayCount] + ";");

		// create delayed res signals  
		String [] resValidDlyName = new String [delayCount+1];
		String [] resDataDlyName = new String [delayCount+1];
		for (int idx=0; idx<delayCount+1; idx++) {
			resValidDlyName[idx] = "r16_" + addrInstProperties.getBaseName() + "_resValid_dly" + idx;
			resDataDlyName[idx] = "r16_" + addrInstProperties.getBaseName() + "_resData_dly" + idx;
		}
		this.addScalarWire(resValidDlyName[0]);  // res delay 0 is set from IO
		this.addVectorWire(resDataDlyName[0], 0, 16); 
		for (int idx=1; idx<delayCount+1; idx++) {
			this.addScalarReg(resValidDlyName[idx]);  
			this.addVectorReg(resDataDlyName[idx], 0, 16); 
			this.addResetAssign(groupName, builder.getDefaultReset(), resValidDlyName[idx] + " <= #1  1'b0;");  
			this.addResetAssign(groupName, builder.getDefaultReset(), resDataDlyName[idx] + " <= #1  16'b0;");  
			this.addRegAssign(groupName,  resValidDlyName[idx] + " <= #1  " + resValidDlyName[idx-1] + ";");  
			this.addRegAssign(groupName,  resDataDlyName[idx] + " <= #1  " + resDataDlyName[idx-1] + ";");  
		}
        // assign r16 inputs to predelay 
		this.addWireAssign(resValidDlyName[0] + " = " + ring16ResValidName + ";");  
		this.addWireAssign(resDataDlyName[0] + " = " + ring16ResDataName + ";");
		
		// add read data accumulate reg
		String r16RdAccumName = "r16_" + addrInstProperties.getBaseName() + "_rdata_accum";                      
		String r16RdAccumNextName = "r16_" + addrInstProperties.getBaseName() + "_rdata_accum_next";                      
		this.addVectorReg(r16RdAccumName, 0, addrInstProperties.getMaxRegWidth());  
		this.addVectorReg(r16RdAccumNextName, 0, addrInstProperties.getMaxRegWidth());  
		this.addRegAssign(groupName,  r16RdAccumName + " <= #1  " + r16RdAccumNextName + ";");  
		this.addWireAssign(hwToDecodeName + " = " + r16RdAccumName + ";");
		
		//  will need to capture res width
		String hwToDecodeTransactionSizeNextName = addrInstProperties.getFullSignalName(DefSignalType.H2D_RETSIZE) + "_next";  // res size will be set in sm
		if (useTransactionSize) {
			this.addVectorReg(hwToDecodeTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(hwToDecodeTransactionSizeNextName, 0, regWordBits);  // res size will be set in sm
			this.addResetAssign(groupName, builder.getDefaultReset(), hwToDecodeTransactionSizeName + " <= #1  " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  hwToDecodeTransactionSizeName + " <= #1  " + hwToDecodeTransactionSizeNextName + ";");  
		}

        // address byte count
		int addrXferCountBits = Utils.getBits(addrXferCount);
		if (addrXferCountBits > 0) {
			this.addVectorReg(r16AddrCntName, 0, addrXferCountBits);  
			this.addVectorReg(r16AddrCntNextName, 0, addrXferCountBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), r16AddrCntName + " <= #1  " + addrXferCountBits + "'b0;");  
			this.addRegAssign(groupName,  r16AddrCntName + " <= #1  " + r16AddrCntNextName + ";");  			
		}
		
		// data byte count
		int maxDataXferCount = regWords * 2;
		int maxDataXferCountBits = Utils.getBits(maxDataXferCount);
		this.addVectorReg(r16DataCntName, 0, maxDataXferCountBits);  
		this.addVectorReg(r16DataCntNextName, 0, maxDataXferCountBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), r16DataCntName + " <= #1  " + maxDataXferCountBits + "'b0;");  
		this.addRegAssign(groupName,  r16DataCntName + " <= #1  " + r16DataCntNextName + ";"); 
		//System.out.println("SystemVerilogBuilder generateExternalInterface_ring16: max data xfers=" + maxDataXferCount);
				
		// state machine init values
		this.addCombinAssign(groupName,  r16StateNextName + " = " + r16StateName + ";");  
		this.addCombinAssign(groupName,  cmdValidDlyName[0] + " =  1'b0;");  
		this.addCombinAssign(groupName,  cmdDataDlyName[0] + " =  16'b0;");  
		this.addCombinAssign(groupName,  hwToDecodeAckName + " =  1'b0;"); 
		this.addCombinAssign(groupName,  hwToDecodeNackName + " =  1'b0;");
		this.addCombinAssign(groupName,  r16RdAccumNextName + " = " + r16RdAccumName + ";");
		// init res size capture
		if (useTransactionSize)
			this.addCombinAssign(groupName,  hwToDecodeTransactionSizeNextName + " = " + hwToDecodeTransactionSizeName + ";"); 
		// init counter values
		if (addrXferCountBits > 0) {
			this.addCombinAssign(groupName,  r16AddrCntNextName + " = "  + addrXferCountBits + "'b0;");
		}
		this.addCombinAssign(groupName,  r16DataCntNextName + " = "  + maxDataXferCountBits + "'b0;");
			
		// state machine 
		String IDLE = stateBits + "'h0"; 
		String CMD_ADDR = stateBits + "'h1"; 
		String CMD_DATA = stateBits + "'h2"; 
		String RES_WAIT = stateBits + "'h3";
		String RES_READ = stateBits + "'h4";
		String RES_DONE_ACK = stateBits + "'h5";
		String RES_DONE_NACK = stateBits + "'h6";
		
		this.addCombinAssign(groupName, "case (" + r16StateName + ")"); 

		// IDLE
		this.addCombinAssign(groupName, "  " + IDLE + ": begin // IDLE");
		this.addCombinAssign(groupName, "      " + r16RdAccumNextName + " = " + addrInstProperties.getMaxRegWidth() + "'b0;");
		// go on read or write request
		this.addCombinAssign(groupName, "      if (" + decodeToHwReName + " | " + decodeToHwWeName + ") begin");  
		this.addCombinAssign(groupName, "        " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set  
		this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + "[7] = " + decodeToHwWeName + ";");  // bit 7 = write
		this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + "[6:4] = 3'd" + addrXferCount + ";");  // bits 6:4 = address xfers  
		if (useTransactionSize)
		    this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + " = " + decodeToHwTransactionSizeName + ";");  // bits 3:0 = transaction size
        // if an address then send it next
		if (addrXferCount > 0) {  
			this.addCombinAssign(groupName, "        " + r16StateNextName + " = " + CMD_ADDR + ";");  
		}
		// otherwise send data if a write or wait for read response
		else {
			this.addCombinAssign(groupName, "        if (" + decodeToHwWeName + ") " + r16StateNextName + " = " + CMD_DATA + ";");  
			this.addCombinAssign(groupName, "        else " + r16StateNextName + " = " + RES_WAIT + ";");  
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
				this.addCombinAssign(groupName,  "      " + r16AddrCntNextName + " = " + r16AddrCntName + " + " + addrXferCountBits + "'b1;");
				// select address slice
				for (int idx=0; idx<addrXferCount; idx++) {
					String prefix = (idx == 0)? "" : "else ";
					this.addCombinAssign(groupName, "      " + prefix + "if (" + r16AddrCntName + " == " + addrXferCountBits + "'d" + idx + ")");
					int lowbit = idx*16 + addrInstProperties.getExtLowBit();
					int size = ((idx+1)*16 > addrInstProperties.getExtAddressWidth())? addrInstProperties.getExtAddressWidth() - idx*16 : 16;  // last xfer can be smaller
					int addrPadLowbit = lowbit + addrInstProperties.getExtAddressWidth();  // for ring, we'll use base address to fill all addr bits in this xfer
					int addrPadSize = Math.min(ExtParameters.getLeafAddressSize() - addrPadLowbit, 16 - size);  // for ring, we'll use base address to fill all addr bits in this xfer
					int zeroPadSize = 16 - size - addrPadSize;
					String zeroPadStr = (zeroPadSize>0)? zeroPadSize + "'d0, " : "";
					if (addrPadSize>0) {
						String addrPadStr = addrInstProperties.getFullBaseAddress().getSubVector(addrPadLowbit, addrPadSize).toFormat(NumBase.Hex, NumFormat.Verilog);
						this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + " = {" + zeroPadStr + addrPadStr + ", " + decodeToHwAddrName + SystemVerilogSignal.genRefArrayString(lowbit, size) + "};");						
					}
					else
						this.addCombinAssign(groupName, "        " + cmdDataDlyName[0] + " = " + decodeToHwAddrName + SystemVerilogSignal.genRefArrayString(lowbit, size) + ";");						
					//this.addCombinAssign(groupName, "      end"); 
				}
	
				// if addr done, send data if a write or wait for read response 
				this.addCombinAssign(groupName, "      if (" + r16AddrCntName + " == " + addrXferCountBits + "'d" + (addrXferCount-1) + ") begin");
				this.addCombinAssign(groupName, "        if (" + decodeToHwWeName + ") " + r16StateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "        else " + r16StateNextName + " = " + RES_WAIT + ";");
				this.addCombinAssign(groupName, "      end"); 
			}
			// else a single addr xfer
			else {
				// set address - use upper bits from base addr
				int lowbit = addrInstProperties.getExtLowBit();
				int size = addrInstProperties.getExtAddressWidth();  // single xfer so just use addr size
				int addrPadLowbit = lowbit + addrInstProperties.getExtAddressWidth();  // for ring, we'll use base address to fill all addr bits in this xfer
				int addrPadSize = Math.min(ExtParameters.getLeafAddressSize() - addrPadLowbit, 16 - size);  // for ring, we'll use base address to fill all addr bits in this xfer
				if (addrPadSize>0) {
					String addrPadStr = addrInstProperties.getFullBaseAddress().getSubVector(addrPadLowbit, addrPadSize).toFormat(NumBase.Hex, NumFormat.Verilog);
					this.addCombinAssign(groupName, "      " + cmdDataDlyName[0] + " = {" + addrPadStr + ", " + decodeToHwAddrName + "};");						
				}
				else
					this.addCombinAssign(groupName, "      " + cmdDataDlyName[0] + " = " + decodeToHwAddrName + ";");						
				// next send data if a write or wait for read response 
				this.addCombinAssign(groupName, "      if (" + decodeToHwWeName + ") " + r16StateNextName + " = " + CMD_DATA + ";");  
				this.addCombinAssign(groupName, "      else " + r16StateNextName + " = " + RES_WAIT + ";");  
			}
			this.addCombinAssign(groupName, "    end"); 
		}
		
		// CMD_DATA
		this.addCombinAssign(groupName, "  " + CMD_DATA + ": begin // CMD_DATA");
		this.addCombinAssign(groupName, "      " + cmdValidDlyName[0] + " =  1'b1;");  // valid is set 
		//  bump the data count
		this.addCombinAssign(groupName,  "      " + r16DataCntNextName + " = " + r16DataCntName + " + " + maxDataXferCountBits + "'b1;");
		// select the data slice
		for (int idx=0; idx<maxDataXferCount; idx++) {
			String prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "      "+ prefix + "if (" + r16DataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
			this.addCombinAssign(groupName, "        "  + cmdDataDlyName[0] + " = " + decodeToHwName + SystemVerilogSignal.genRefArrayString(16*idx, 16) + ";");
		}
		// if done, move to res wait
		String finalCntStr = "1'b1";
		if (useTransactionSize) finalCntStr = "{" + decodeToHwTransactionSizeName + ", 1'b1}";
		this.addCombinAssign(groupName, "      if (" + r16DataCntName + " == " + finalCntStr + ")"); 
		this.addCombinAssign(groupName, "        " + r16StateNextName + " = " + RES_WAIT + ";");
		this.addCombinAssign(groupName, "    end"); 
			
		// RES_WAIT
		this.addCombinAssign(groupName,   "  " + RES_WAIT + ": begin  // RES_WAIT");
		// first word of response contains ack/nack and xfer size 
		this.addCombinAssign(groupName,   "      " + "if (" + resValidDlyName[delayCount] + ") begin");  // save bit15 nack, 3:0 size
		if (useTransactionSize) {
			this.addCombinAssign(groupName,  "        " + hwToDecodeTransactionSizeNextName + " = " + resDataDlyName[delayCount] + SystemVerilogSignal.genRefArrayString(0, regWordBits) + ";");  
		}
		// if a valid read response then get the data else ack/nack
		this.addCombinAssign(groupName,   "        if (" + decodeToHwReName + " & " + resDataDlyName[delayCount] + "[14] & ~" + resDataDlyName[delayCount] + "[15]) " + r16StateNextName + " = " + RES_READ + ";");  
		this.addCombinAssign(groupName,   "        else if (~" + resDataDlyName[delayCount] + "[14]) " + r16StateNextName + " = " + RES_DONE_NACK + ";"); // if no ack then nack it
		this.addCombinAssign(groupName,   "        else "+ r16StateNextName + " = " + RES_DONE_ACK + ";");  // write ack
		this.addCombinAssign(groupName,   "      end"); 
		this.addCombinAssign(groupName,   "    end"); 
		
		// RES_READ
		this.addCombinAssign(groupName,     "  " + RES_READ + ": begin  // RES_READ");  
		//  bump the data count
		this.addCombinAssign(groupName,     "      " + r16DataCntNextName + " = " + r16DataCntName + ";");
		// collect the data slice while res valid is asserted  
		this.addCombinAssign(groupName,     "      " + "if (" + resValidDlyName[delayCount] + ") begin");
		this.addCombinAssign(groupName,     "        " + r16DataCntNextName + " = " + r16DataCntName + " + " + maxDataXferCountBits + "'b1;");
		for (int idx=0; idx<maxDataXferCount; idx++) {
			String prefix = (idx == 0)? "" : "else ";
			this.addCombinAssign(groupName, "        "+ prefix + "if (" + r16DataCntName + " == " + maxDataXferCountBits + "'d" + idx + ")");  
			this.addCombinAssign(groupName, "          " + r16RdAccumNextName + SystemVerilogSignal.genRefArrayString(16*idx, 16) + " = " + resDataDlyName[delayCount] + ";");
		}
		this.addCombinAssign(groupName,     "      end"); 
		// move to ack when done 
		if (useTransactionSize) 
		    this.addCombinAssign(groupName, "      if (" + r16DataCntName + " == {" + hwToDecodeTransactionSizeName + ", 1'b1}) " + r16StateNextName + " = " + RES_DONE_ACK + ";");
		else
		    this.addCombinAssign(groupName, "      if (" + r16DataCntName + " == 1'b1) " + r16StateNextName + " = " + RES_DONE_ACK + ";");
		this.addCombinAssign(groupName,     "    end"); 
		
		// RES_DONE_ACK
		this.addCombinAssign(groupName, "  " + RES_DONE_ACK + ": begin // RES_DONE_ACK");
		this.addCombinAssign(groupName, "      " + hwToDecodeAckName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + r16StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// RES_DONE_NACK
		this.addCombinAssign(groupName, "  " + RES_DONE_NACK + ": begin // RES_DONE_NACK");
		this.addCombinAssign(groupName, "      " + hwToDecodeNackName + " = 1'b1;");   
		this.addCombinAssign(groupName, "      " + r16StateNextName + " = " + IDLE + ";");  
		this.addCombinAssign(groupName, "    end"); 
		
		// default
		this.addCombinAssign(groupName, "  default:");
		this.addCombinAssign(groupName, "    " + r16StateNextName + " = " + IDLE + ";");  

		this.addCombinAssign(groupName, "endcase"); 				
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

		// set decoder defaults by iterating through reglist  
		Iterator<AddressableInstanceProperties> it = decoderList.iterator();   
		writeStmt(indentLevel, "");
		while (it.hasNext()) {
			AddressableInstanceProperties elem = it.next();
			
			// generate default assigns for externals
			if (elem.isExternal()) {
				// generate reg write data assignment
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_DATA) + "_next = pio_dec_write_data_d1" + SystemVerilogSignal.genRefArrayString(0, elem.getMaxRegWidth()) + ";"); // regardless of transaction size assign based on regsize
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_WE) + "_next = 1'b0;");  // we defaults to 0
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_RE) + "_next = 1'b0;");  // we defaults to 0
				// if an address is required then add it 
				if ( (elem.getExtAddressWidth() > 0 ) && !elem.isSingleExtReg())
				   writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_ADDR) + "_next = pio_dec_address_d1 " + elem.getExtAddressArrayString() + ";");  // address is resistered value from pio
				// if data sizes are needed then add - also check for single register here and inhibit
				if ( (elem.getMaxRegWidth() > ExtParameters.getMinDataSize())  && !elem.isSingleExtReg()) {
				       int widthBits = Utils.getBits(elem.getMaxRegWordWidth());	   
					   writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_SIZE) + "_next = pio_dec_trans_size_d1" + SystemVerilogSignal.genRefArrayString(0, widthBits) + ";");
				}
			}
			// internal reg so init enables and data
			else {
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_DATA) + " = pio_dec_write_data_d1 " + SystemVerilogSignal.genRefArrayString(0, elem.getMaxRegWidth()) +";");  // regardless of transaction size assign based on regsize
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_WE) + " = 1'b0;");  // we defaults to 0
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2L_RE) + " = 1'b0;");  // re defaults to 0
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
						writeStmt(indentLevel, "pio_external_ack_next = " + elem.getFullSignalName(DefSignalType.H2D_ACK) + " | (pio_write_active & (pio_dec_trans_size_d1 < reg_width));"); 
					}
					
					// otherwise this is a larger ext region so use size io
					else {
						// write the size dependent assigns
						writeStmt(indentLevel, "reg_width" + SystemVerilogSignal.genRefArrayString(0, Utils.getBits(elem.getMaxRegWordWidth())) + " = " + elem.getFullSignalName(DefSignalType.H2D_RETSIZE) + "_d1;");  // set size for this register  
						// assign write enable 
						writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_WE) + "_next = pio_write_active & ~(pio_external_ack | pio_external_nack);");		// write goes thru even if invalid				
						writeStmt(indentLevel, "pio_external_ack_next = " + elem.getFullSignalName(DefSignalType.H2D_ACK) + ";"); 
					}
				   
				}
				else {
					writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_WE) + "_next = pio_write_active & ~(pio_external_ack | pio_external_nack);"); 
					writeStmt(indentLevel, "pio_external_ack_next = " + elem.getFullSignalName(DefSignalType.H2D_ACK) + ";"); 
				}
				
				// ext transaction is active if a valid read or write
				writeStmt(indentLevel, "external_transaction_active = pio_read_active | pio_write_active;"); 
				// create read enable
				writeStmt(indentLevel, elem.getFullSignalName(DefSignalType.D2H_RE) + "_next = pio_read_active & ~(pio_external_ack | pio_external_nack);"); 
				// capture ack/nack/read data
				writeStmt(indentLevel, "pio_external_nack_next = " + elem.getFullSignalName(DefSignalType.H2D_NACK) + ";");  
				writeStmt(indentLevel--, "dec_pio_read_data_next " + elem.getMaxRegArrayString() + " = " + elem.getFullSignalName(DefSignalType.H2D_DATA) + ";");
				writeStmt(indentLevel--, "end");  								
			}
			
			// else internal, so create internal ack, re/we to logic, and capture read data
			else {
				RegProperties regElem = (RegProperties) elem;  // internal must be a reg so cast
				// getNextAddress holds max value of address map at this point 
				if (mapHasMultipleAddresses()) writeStmt(indentLevel++, getIntDecodeAddressString(regElem) + ":");  
				writeStmt(indentLevel++, "begin");
				
	            // if this is a wide register 
				if (regElem.getMaxRegWordWidth() > 1) {
					String writeEnableString = "pio_write_active & ~" + pioInterfaceAckName + " & (pio_dec_trans_size_d1 >= reg_width)";  // suppress write if invalid trans size
					
					// write the size dependent assigns
					writeStmt(indentLevel, "reg_width = " + builder.getMaxWordBitSize() + "'d" + (regElem.getMaxRegWordWidth() - 1) + ";");  // set size for this register  						
					// create write enable
					writeStmt(indentLevel, regElem.getFullSignalName(DefSignalType.D2L_WE) + " = " + writeEnableString + ";");  
					// create read enable
					writeStmt(indentLevel, regElem.getFullSignalName(DefSignalType.D2L_RE) + " = pio_read_active & ~" + pioInterfaceAckName + ";");   					
				}
				else {
					writeStmt(indentLevel, regElem.getFullSignalName(DefSignalType.D2L_WE) + " = pio_write_active & ~" + pioInterfaceAckName + ";");  
					writeStmt(indentLevel, regElem.getFullSignalName(DefSignalType.D2L_RE) + " = pio_read_active & ~" + pioInterfaceAckName + ";");   					
				}
				// generate internal ack based on sw r/w settings of register
				if (!regElem.isSwWriteable()) 
				   writeStmt(indentLevel, "pio_internal_ack =  pio_read_active;");  
				else if (!regElem.isSwReadable()) 
				   writeStmt(indentLevel, "pio_internal_ack =  pio_write_active;");  
				else 
				   writeStmt(indentLevel, "pio_internal_ack =  pio_read_active | pio_write_active;"); 
				
				writeStmt(indentLevel--, "dec_pio_read_data_next " + regElem.getMaxRegArrayString() + " = " + regElem.getFullSignalName(DefSignalType.L2D_DATA) + ";");  
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
