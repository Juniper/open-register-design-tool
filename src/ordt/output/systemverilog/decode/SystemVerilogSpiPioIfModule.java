package ordt.output.systemverilog.decode;

import ordt.output.common.MsgUtils;
import ordt.output.systemverilog.SystemVerilogBuilder;
import ordt.output.systemverilog.common.SystemVerilogModule;
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
		TODO - data/address gen for r/w
		TODO - data capture sequence for read                
		 */

		/*
		// disable atomic request to arbiter  TODO - handle mapping to module
		if (hasSecondaryInterface()) {
			this.addScalarWire(arbiterAtomicName);   
			this.addWireAssign(arbiterAtomicName + " = 1'b0;");
		}

		// generate re/we assigns - use delayed versions if this is a single primary
		assignReadWriteRequests(s8pioInterfaceReName, s8pioInterfaceWeName, pioInterfaceReName, pioInterfaceWeName, !hasSecondaryInterface());	
		*/
	}

}
