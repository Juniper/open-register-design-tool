package ordt.output.systemverilog.decode;

import ordt.output.common.MsgUtils;
import ordt.output.common.OutputWriterIntf;
import ordt.output.systemverilog.SystemVerilogBuilder;
import ordt.output.systemverilog.SystemVerilogDefinedOrdtSignals;
import ordt.output.systemverilog.common.SystemVerilogModule;
import ordt.output.systemverilog.common.io.SystemVerilogIOSignalList;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;

public class SystemVerilogSpiPioIfModule extends SystemVerilogPioIfModule {
		
	public SystemVerilogSpiPioIfModule(SystemVerilogBuilder builder, SystemVerilogDecodeModule decoder) {
		super(builder, decoder);
		String modName = "spi_pio_if_" + builder.getBuilderID();  // unique module per builder
		this.setName(modName);
		SystemVerilogModule.addUniqueModule(modName, this);  // add module to the common list
	}
	
	/** add spi pio interface */
	protected void genPioInterface() {  // TODO add spi mode, cmd, address, return info controls
		// spi control info
		int spiMode = 0; // 0:cpol=0,cpha=0(rising capture)  1:cpol=0,cpha=1(falling capture)  2:cpol=1,cpha=0(falling capture)  3:cpol=1,cpha=1(rising capture)
		
		int spiWordSize = 8;  // 8b per std word
        int spiFirstWordSize = 3; // special first word  // TODO use a mode??
        boolean spiIndirectReadMode = true;  // address is not sent with r/w - separate write sets address first, waddr, raddr, then do indir reg write/read
		
		boolean risingCapture = (spiMode == 0) || (spiMode == 3);
		int maxSpiWordSize = (spiWordSize > spiFirstWordSize)? spiWordSize : spiFirstWordSize;
		int spiWordSizeBits = Utils.getBits(maxSpiWordSize);
				
		//System.out.println("SystemVerilogDecodeModule: generating decoder with spi interface, id=" + topRegProperties.getInstancePath());
		// set IO names
		String spiClkName = "spi_sclk";                      
		String spiMisoName = "spi_miso";                      
		String spiMisoEnableName = "spi_miso_enable";                      
		String spiMosiName = "spi_mosi";                      
		String spiSelectName = "spi_ss"; 
		
		// set internal names
		String spiMosiBitCountName = "spi_mosi_bit_count";  // count of bits in spi word
		String spiMosiBitCountNextName = "spi_mosi_bit_count_next";  // count of bits in spi word
		String spiMosiWordCountName = "spi_mosi_word_count";   // count of words in transaction                  
		String spiMosiWordCountNextName = "spi_mosi_word_count_next";   // count of words in transaction                  
		String spiMosiWordEndName = "spi_mosi_word_end";      // half cycle pulse                
		String spiMosiWordEndNextEdgeName = "spi_mosi_word_end_next_edge";      // half cycle delayed word_end for valid gen               
		String spiMosiCaptureName = "spi_mosi_capture";      // sclk flopped incoming data 
		
		String spiWordEndSync0Name = "spi_word_end_sync_0";      // valid sync flops                
		String spiWordEndSync1Name = "spi_word_end_sync_1";                  
		String spiWordEndSync2Name = "spi_word_end_sync_2";                 
		String spiWordEndSync3Name = "spi_word_end_sync_3";                 
		String spiWordEndSyncName = "spi_word_end_sync";      // resync'd valid                
	
		// tie off enables
		if (decoder.hasWriteEnables()) {
			MsgUtils.warnMessage("SPI decoder interface will not generate write data enables.");
			this.addVectorWire(pioInterfaceWriteEnableName, 0, decoder.getWriteEnableWidth()); 
			this.addWireAssign(pioInterfaceWriteEnableName + " = " + SystemVerilogBuilder.getHexOnesString(decoder.getWriteEnableWidth()) + ";");
		}
		
		// create module IOs
		//  inputs
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, spiClkName); 
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, spiMosiName); 
		this.addSimpleScalarFrom(SystemVerilogBuilder.PIO, spiSelectName); 

		// outputs  
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, spiMisoName);
		this.addSimpleScalarTo(SystemVerilogBuilder.PIO, spiMisoEnableName);
		
		// calculate min spi data word transactions
		if ((ExtParameters.getMinDataSize() % spiWordSize) != 0)
			MsgUtils.warnMessage("Specified SPI decoder interface word size (" + spiWordSize + ") must be a factor of minimum data size (" + ExtParameters.getMinDataSize() + ").");
		int minSpiDataWords = ExtParameters.getMinDataSize()/spiWordSize;

		// calculate max spi data word transactions
		int maxRegWords = builder.getMaxRegWordWidth();
		int maxSpiDataWords = minSpiDataWords * maxRegWords;
		int spiWordCountBits = Utils.getBits(maxSpiDataWords);
		
		// compute max transaction size in words and number of bits to represent (4b max)  // FIXME
		int maxRegWidth = builder.getMaxRegWidth();
		int regWordBits = Utils.getBits(maxRegWords);
		boolean useTransactionSize = (maxRegWords > 1);  // if transaction sizes need to be sent/received
		
		// calculate max number of 8b xfers required for address   // FIXME
		int addressWidth = builder.getMapAddressWidth();
		int addrXferCount = 0;
		if (addressWidth > 0) {
			addrXferCount = (int) Math.ceil(addressWidth/8.0);
			//System.out.println("SystemVerilogBuilder genSPIPioInterface: addr width=" + addressWidth + ", addr count=" + addrXferCount);
		}

		// count incoming sclk bit transactions
		String groupName = "spi i/f - mosi bit count and word end indication";
		this.addVectorReg(spiMosiBitCountName, 0, spiWordSizeBits); 
		this.addVectorReg(spiMosiBitCountNextName, 0, spiWordSizeBits); 
		this.addScalarReg(spiMosiWordEndName); 
		String syncAssignStr = " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " ";
		this.addResetAssign(groupName, builder.getDefaultReset(), spiClkName, !risingCapture, spiMosiBitCountName + syncAssignStr + spiWordSizeBits + "'d0;");
		this.addRegAssign(groupName, spiClkName, !risingCapture,  spiMosiBitCountName + syncAssignStr + spiMosiBitCountNextName + ";");
		
		groupName = "spi i/f - mosi bit count and word end indication (combin)";
		this.addCombinAssign(groupName, spiMosiWordEndName + " = 1'b0;");
		this.addCombinAssign(groupName, "if (" + spiSelectName + ") " + spiMosiBitCountNextName + " = " + spiWordSizeBits + "'d0;");  
		this.addCombinAssign(groupName, "else if (((" + spiMosiWordCountName + "==" + spiWordCountBits + "'d0) && (" + spiMosiBitCountName + " == " + spiWordSizeBits + "'d" + (spiFirstWordSize-1) + ")) || ");
		this.addCombinAssign(groupName, "         ((" + spiMosiWordCountName + "!=" + spiWordCountBits + "'d0) && (" + spiMosiBitCountName + " == " + spiWordSizeBits + "'d" + (spiWordSize-1) + "))) begin");
		this.addCombinAssign(groupName, "    " + spiMosiBitCountNextName + " = " + spiWordSizeBits + "'d0;");
		this.addCombinAssign(groupName, "    " + spiMosiWordEndName + " = 1'b1;");
		this.addCombinAssign(groupName, "end");
		this.addCombinAssign(groupName, "else " + spiMosiBitCountNextName + " = " + spiMosiBitCountNextName + " + " + spiWordSizeBits + "'d1;");  

		// create a half clock delayed word end signal (assumes word end valid in a half sclk cycle)
		groupName = "spi i/f - mosi word end delayed";
		this.addScalarReg(spiMosiWordEndNextEdgeName);  
		this.addRegAssign(groupName, spiClkName, risingCapture,  spiMosiWordEndNextEdgeName + syncAssignStr + spiMosiWordEndName + ";");
			
		// count incoming sclk words
		groupName = "spi i/f - mosi word count";
		this.addVectorReg(spiMosiWordCountName, 0, spiWordCountBits); // TODO single word case? 
		this.addVectorReg(spiMosiWordCountNextName, 0, spiWordCountBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), spiClkName, !risingCapture, spiMosiWordCountName + syncAssignStr + spiWordCountBits + "'d0;");
		this.addRegAssign(groupName, spiClkName, !risingCapture,  spiMosiWordCountName + syncAssignStr + spiMosiWordCountNextName + ";");
		
		this.addCombinAssign(groupName, spiMosiWordCountName + " = " + spiMosiWordCountNextName + " + " + spiWordCountBits + "'d1;");  
		this.addCombinAssign(groupName, "if (" + spiSelectName + ") " + spiMosiWordCountName + " = " + spiWordCountBits + "'d0;");  
		this.addCombinAssign(groupName, "else if (" + spiMosiWordEndName + ") " + spiMosiWordCountName + " = " + spiMosiWordCountNextName + " + " + spiWordCountBits + "'d1;");  

		groupName = "spi i/f - mosi capture";
		this.addVectorReg(spiMosiCaptureName, 0, spiWordSizeBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), spiClkName, !risingCapture, spiMosiCaptureName + syncAssignStr + spiWordSizeBits + "'b0;");
		String idxStr = "[" + spiMosiBitCountNextName + "]";
		this.addRegAssign(groupName, spiClkName, !risingCapture,  "if (!" + spiSelectName + ") " + spiMosiCaptureName + idxStr + syncAssignStr + spiMosiName + ";");  

		groupName = "spi i/f - sync word end";
		this.addScalarReg(spiWordEndSync0Name);  
		this.addScalarReg(spiWordEndSync1Name);  
		this.addScalarReg(spiWordEndSync2Name);  
		this.addScalarReg(spiWordEndSync3Name);  
		this.addResetAssign(groupName, builder.getDefaultReset(), spiWordEndSync0Name + syncAssignStr + "1'b0;");
		this.addRegAssign(groupName, spiWordEndSync0Name + syncAssignStr + spiMosiWordEndNextEdgeName + ";");
		this.addRegAssign(groupName, spiWordEndSync1Name + syncAssignStr + spiWordEndSync0Name + ";");
		this.addRegAssign(groupName, spiWordEndSync2Name + syncAssignStr + spiWordEndSync1Name + ";");
		this.addRegAssign(groupName, spiWordEndSync3Name + syncAssignStr + spiWordEndSync2Name + ";");
		this.addScalarReg(spiWordEndSyncName);  
		this.addCombinAssign(groupName, spiWordEndSyncName + " = " +  spiWordEndSync1Name + " & " + spiWordEndSync2Name + " & !" + spiWordEndSync3Name + ";");  
		
		/*
		String spiValidSync0Name = "spi_valid_sync_0");      // valid sync flops                
		String spiValidSync1Name = "spi_valid_sync_1");                  
		String spiValidSync2Name = "spi_valid_sync_2");                 
		String spiValidSync3Name = "spi_valid_sync_3");                 
		String spiValidSyncName = "spi_valid_sync");      // resync'd valid                
		 */

		/*
		// now create state machine vars
		String groupName = "serial8 i/f";  
		int stateBits = 3;
		this.addVectorReg(s8StateName, 0, stateBits);  
		this.addVectorReg(s8StateNextName, 0, stateBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), s8StateName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + stateBits + "'b0;");  
		this.addRegAssign(groupName,  s8StateName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + s8StateNextName + ";");  

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
			this.addRegAssign(groupName,  s8AddrAccumName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + s8AddrAccumNextName + ";");  
			this.addVectorWire(pioInterfaceAddressName, builder.getAddressLowBit(), addressWidth);  
			this.addWireAssign(pioInterfaceAddressName + " = " + s8AddrAccumName + ";");  // input addr is set from accum reg			
		}

		// add write data accumulate reg
		this.addVectorReg(s8WrAccumName, 0, regWidth);  
		this.addVectorReg(s8WrAccumNextName, 0, regWidth);  
		this.addRegAssign(groupName,  s8WrAccumName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + s8WrAccumNextName + ";");  
		this.addVectorWire(pioInterfaceWriteDataName, 0, regWidth);  
		this.addWireAssign(pioInterfaceWriteDataName + " = " + s8WrAccumName + ";");  // input data is set from accum reg
		
		// will need to capture cmd size
		String pioInterfaceTransactionSizeNextName = pioInterfaceTransactionSizeName + "_next";  // cmd size next will be set in sm
		if (useTransactionSize) {
			this.addVectorReg(pioInterfaceTransactionSizeName, 0, regWordBits);  
			this.addVectorReg(pioInterfaceTransactionSizeNextName, 0, regWordBits);  // res size will be set in sm
			this.addResetAssign(groupName, builder.getDefaultReset(), pioInterfaceTransactionSizeName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + regWordBits + "'b0;");  
			this.addRegAssign(groupName,  pioInterfaceTransactionSizeName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + pioInterfaceTransactionSizeNextName + ";");  
		}

		// add capture reg for write transaction indicator
		this.addScalarReg(s8WrStateCaptureName);  
		this.addScalarReg(s8WrStateCaptureNextName);  
		this.addRegAssign(groupName,  s8WrStateCaptureName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + s8WrStateCaptureNextName + ";");  

		// add capture reg for read data
		this.addVectorReg(s8RdCaptureName, 0, regWidth);  
		this.addVectorReg(s8RdCaptureNextName, 0, regWidth);  
		this.addRegAssign(groupName,  s8RdCaptureName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + s8RdCaptureNextName + ";"); 

		// address byte count
		int addrXferCountBits = Utils.getBits(addrXferCount);
		if (addrXferCountBits > 0) {
			this.addVectorReg(s8AddrCntName, 0, addrXferCountBits);  
			this.addVectorReg(s8AddrCntNextName, 0, addrXferCountBits);  
			this.addResetAssign(groupName, builder.getDefaultReset(), s8AddrCntName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + addrXferCountBits + "'b0;");  
			this.addRegAssign(groupName,  s8AddrCntName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + s8AddrCntNextName + ";");  			
		}
		
		// data byte count
		int maxDataXferCount = regWords * ExtParameters.getMinDataSize()/8;
		int maxDataXferCountBits = Utils.getBits(maxDataXferCount);
		this.addVectorReg(s8DataCntName, 0, maxDataXferCountBits);  
		this.addVectorReg(s8DataCntNextName, 0, maxDataXferCountBits);  
		this.addResetAssign(groupName, builder.getDefaultReset(), s8DataCntName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + maxDataXferCountBits + "'b0;");  
		this.addRegAssign(groupName,  s8DataCntName + " <= " + ExtParameters.sysVerSequentialAssignDelayString() + " " + s8DataCntNextName + ";"); 

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
		String finalCntStr = getSerialMaxDataCountStr(useTransactionSize, bytesInDataWord, pioInterfaceTransactionSizeName);  // get final data count compare string
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
		finalCntStr = getSerialMaxDataCountStr(useTransactionSize, bytesInDataWord, pioInterfaceRetTransactionSizeName);  // get final data count compare string
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
		*/
	}

}
