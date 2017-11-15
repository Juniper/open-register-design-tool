/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.othertypes;

import java.io.BufferedWriter;
import java.util.ArrayList;

import java.util.List;

import ordt.extract.RegModelIntf;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;
import ordt.output.OutputLine;
import ordt.parameters.ExtParameters;

/**
 *  @author snellenbach      
 *  Jan 27, 2014
 *
 */
public class RegListBuilder extends OutputBuilder {  // TODO handle alias fields
	private List<OutputLine> outputList = new ArrayList<OutputLine>();
	private int indentLvl = 0;

	//---------------------------- constructors ----------------------------------
	
	public RegListBuilder(RegModelIntf model) {
		setBaseBuilderID();   // set unique ID of this instance
		this.model = model;
	    setVisitEachReg(true);   // gen code for each reg
	    setVisitEachRegSet(true);   // gen code for each reg set
	    setVisitExternalRegisters(ExtParameters.reglistDisplayExternalRegs());  //visit externals also?
	    setVisitEachExternalRegister(true);	    // treat external regs exactly as internals
		model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
	}
	
	//---------------------------- OutputBuilder methods to load verilog structures ----------------------------------------

	/** add a field for a particular output */
	@Override
	public  void addField() {
	}
	
	/** add a field for a particular output */
	@Override
	public  void addAliasField() {
		addField();
	}

	/** add a register for a particular output */
	@Override
	public  void addRegister() { 
	}
	
	/** finish a register for a particular output */
	@Override
	public  void finishRegister() {
		//if ("sopcf1_ctl".equals(regProperties.getId())) 
		//	System.out.println("RegListBuilder finishRegister: " + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress());
		
		// only show output if register name match is specified
		if ((ExtParameters.getReglistMatchInstance() != null) && !regProperties.getInstancePath().contains(ExtParameters.getReglistMatchInstance())) return;
		// write the register info
		String extStr = regProperties.isExternal() ? "ext" : "int";
		if (!ExtParameters.reglistShowRegType()) extStr = "";
		//System.out.println("RegListBuilder write: showRegType=" + ExtParameters.reglistShowRegType());
		outputList.add(new OutputLine(indentLvl, String.format("%-15s  size: %-3s  fields: %-3s  bits: %-3s  %-3s  %s", 
				regProperties.getFullBaseAddress(), regProperties.getRegWidth(), regProperties.getFieldCount(), regProperties.getFilledBits(), 
				extStr , regProperties.getInstancePath()))); 
		indentLvl++;
		// write field info
		if (ExtParameters.reglistShowFields())  {
			while (fieldList.size() > 0) {
				FieldProperties field = fieldList.remove();  // get next field
				String id = field.getPrefixedId() + field.getFieldArrayString();
				String access = getFieldAccessType(field);
				String reset = ((field.getReset() != null) && field.getReset().isDefined())? field.getReset().toString() : "";
				String indexedPath = getIndexedInstancePath() + "." + field.getPrefixedId();

				String fieldAssign = "( " + indexedPath + (reset.isEmpty()? "" : " = " + reset) + " )";
				outputList.add(new OutputLine(indentLvl, String.format("%-25s  mode: %-3s  reset: %-20s  %s" , 
						id, access, reset, fieldAssign))); 
			}			
		}
		indentLvl--;
	}
			
	/** add a register set for a particular output */
	@Override
	public  void addRegSet() {
	}

	/** finish a register set for a particular output */
	@Override
	public  void finishRegSet() {	
	}

	/** add a register map for a particular output */
	@Override
	public  void addRegMap() {
	}

	/** finish a register map for a particular output */
	@Override
	public  void finishRegMap() {
	}
	
	//--------------------------------------------------------------------
	
	/** generate field access type string 
	 * @param field */
	private String getFieldAccessType(FieldProperties field) {
		String accessMode = "RO";
		// if read clear
		if (field.isRclr()) {
			if (field.isWoset()) accessMode = "W1SRC"; 
			else if (field.isSwWriteable()) accessMode = "WRC";
		    else accessMode = "RC";
		}
		// if read set
		else if (field.isRset()) {
			if (field.isWoclr()) accessMode = "W1CRS"; 
			else if (field.isSwWriteable()) accessMode = "WRS";
		    else accessMode = "RS";
		}
		// no read set/clr
		else {
			if (field.isWoclr()) accessMode = "W1C";   
			else if (field.isWoset()) accessMode = "W1S";
			else if (field.isSwWriteable()) {
				if (field.isSwReadable()) accessMode = "RW"; 
				else accessMode = "WO";
			}
		}
		return accessMode;
	}

	//---------------------------- methods to output  ----------------------------------------

	/** write out the verilog 
	 * @param bw */
	public void write(BufferedWriter bw) {
		bufferedWriter = bw;
		
		// write the output for each output group
		for (OutputLine rLine: outputList) {
			writeStmt(rLine.getIndent(), rLine.getLine());  
		}
	}
}
