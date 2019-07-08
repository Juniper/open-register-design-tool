package ordt.output.systemverilog.decode;

import ordt.output.systemverilog.SystemVerilogBuilder;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals;
import ordt.output.systemverilog.common.SystemVerilogModule;
import ordt.output.systemverilog.common.io.SystemVerilogIOSignalList;
import ordt.parameters.ExtParameters;

public abstract class SystemVerilogPioIfModule extends SystemVerilogModule {  
	
	protected SystemVerilogBuilder builder;  // builder creating this module  // TODO - remove and move to decoder/intf?
	protected SystemVerilogDecodeModule decoder;
	
	// define io locations
	protected static final Integer DECODE = SystemVerilogDefinedOrdtSignals.DECODE;
	protected static final Integer DECODE_PIO_IF = SystemVerilogDefinedOrdtSignals.DECODE_PIO_IF;
	protected static final Integer PIO = SystemVerilogDefinedOrdtSignals.PIO;
	
	// define siglists used by this module
	protected SystemVerilogIOSignalList pioSigList = new SystemVerilogIOSignalList("pio");    // pio to/from this pio_if
	protected SystemVerilogIOSignalList decodeSigList = new SystemVerilogIOSignalList("decoder");    // decoder to/from pio_if
	
	// config values for this pio interface
    protected boolean usesDefaultClock = true;
    protected boolean usesDefaultReset = true;
    protected boolean needsEarlyAck = false;  // pio interface needs inputs for early ack/nack indication
    protected boolean supportsArbiterAtomics = false;  // pio interface can hold off peer interfaces for atomic operations
    protected boolean allowsWriteEnableMask = false;  // pio interface supports an write mask input
    
	// default interface signals to decoder
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
	
	public SystemVerilogPioIfModule(SystemVerilogBuilder builder, SystemVerilogDecodeModule decoder) {
		super(builder, DECODE_PIO_IF, SystemVerilogBuilder.getDecodeClk(), builder.getDefaultReset(), ExtParameters.sysVerUseAsyncResets());
		this.builder = builder;  // save reference to calling builder
		this.decoder = decoder;  // save reference to parent decoder
		this.useIOList(decodeSigList, DECODE); 
		this.useIOList(pioSigList, PIO); 
		this.addReset(builder.getDefaultReset(), builder.getDefaultResetActiveLow());  // TODO - move these to decoder?
		this.addDefaultIo();
		this.genPioInterface();
	}
	
	private void addDefaultIo() {
		// add default clock/reset inputs
		if (usesDefaultReset) decodeSigList.addSimpleScalar(DECODE, DECODE_PIO_IF, SystemVerilogBuilder.getDecodeClk());
		if (usesDefaultClock) decodeSigList.addSimpleScalar(DECODE, DECODE_PIO_IF, builder.getDefaultReset());
		// add default interface io set to decodeSigList
		decodeSigList.addSimpleScalar(DECODE_PIO_IF, DECODE, pioInterfaceReName);
		decodeSigList.addSimpleScalar(DECODE_PIO_IF, DECODE, pioInterfaceWeName);
		if (decoder.mapHasMultipleAddresses()) decodeSigList.addSimpleVector(DECODE_PIO_IF, DECODE, pioInterfaceAddressName, builder.getAddressLowBit(), builder.getMapAddressWidth());
		decodeSigList.addSimpleVector(DECODE_PIO_IF, DECODE, pioInterfaceWriteDataName, 0, builder.getMaxRegWidth());
		if (SystemVerilogDecodeModule.hasWriteEnables()) decodeSigList.addSimpleVector(DECODE_PIO_IF, DECODE, pioInterfaceWriteEnableName, 0, decoder.getWriteEnableWidth());
		decodeSigList.addSimpleScalar(DECODE_PIO_IF, DECODE, arbiterAtomicName);
	    if (builder.getMaxRegWordWidth() > 1) {
			decodeSigList.addSimpleVector(DECODE_PIO_IF, DECODE, pioInterfaceTransactionSizeName, 0, builder.getMaxWordBitSize());
			decodeSigList.addSimpleVector(DECODE, DECODE_PIO_IF, pioInterfaceRetTransactionSizeName, 0, builder.getMaxWordBitSize());
	    }
		decodeSigList.addSimpleVector(DECODE, DECODE_PIO_IF, pioInterfaceReadDataName, 0, builder.getMaxRegWidth());
		decodeSigList.addSimpleScalar(DECODE, DECODE_PIO_IF, pioInterfaceAckName);
		decodeSigList.addSimpleScalar(DECODE, DECODE_PIO_IF, pioInterfaceNackName);
		if (needsEarlyAck) {
			decodeSigList.addSimpleScalar(DECODE, DECODE_PIO_IF, pioInterfaceAckNextName);
			decodeSigList.addSimpleScalar(DECODE, DECODE_PIO_IF, pioInterfaceNackNextName);
		}
		
	    /* 
				
		// create p1, p2 interface outputs
		this.addVectorWire(p1ReadDataName, 0, builder.getMaxRegWidth());  //  read data output  
		if (builder.getMaxRegWordWidth() > 1) this.addVectorWire(p1RetTransactionSizeName, 0, builder.getMaxWordBitSize());  //  register the size
		this.addVectorWire(p2ReadDataName, 0, builder.getMaxRegWidth());  //  read data output  
		if (builder.getMaxRegWordWidth() > 1) this.addVectorWire(p2RetTransactionSizeName, 0, builder.getMaxWordBitSize());  //  register the size
		
		// create common internal i/f inputs
		if (builder.getMaxRegWordWidth() > 1) this.addVectorReg(pioInterfaceTransactionSizeName, 0, builder.getMaxWordBitSize());  //  internal transaction size
		if (hasWriteEnables()) this.addVectorReg(pioInterfaceWriteEnableName, 0, getWriteEnableWidth()); 
	
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
		*/

	}

	/** add pio interface */
	abstract protected void genPioInterface(); 

}
