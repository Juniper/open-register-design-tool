/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.othertypes;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.PropertyList;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.extract.model.ModEnum;
import ordt.extract.model.ModEnumElement;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;
import ordt.output.OutputLine;
import ordt.output.RhsReference;
import ordt.output.FieldProperties.RhsRefType;
import ordt.output.InstanceProperties;
import ordt.parameters.ExtParameters;

public class XmlBuilder extends OutputBuilder {  
	private List<OutputLine> outputList = new ArrayList<OutputLine>();
	private int indentLvl = 0;
	private String commonRegAccess = null;  // detect common access mode of all fields
	
    //---------------------------- constructor ----------------------------------

    public XmlBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

	//---------------------------- OutputBuilder methods to create xml structures ----------------------------------------

	@Override
	public void addField() {
		addXmlElementStart("field");
		addXmlElement("id", fieldProperties.getPrefixedId());
		String textName = fieldProperties.getTextName();
		if (textName == null) textName = fieldProperties.getPrefixedId() + " field";
		textName = cleanXmlText(textName);
		if (textName.length() > 255) {
			Ordt.warnMessage("Field " + fieldProperties.getInstancePath() + " short text description exceeds 255 characters and will be truncated in xml.");
			textName = textName.substring(0, 255);
		}
		addXmlElement("shorttext", textName);
		addXmlElement("access", getFieldAccessType());
		if (fieldProperties.isSinglePulse()) addXmlElement("singlepulse", "");
		if (ExtParameters.xmlIncludeFieldHwInfo()) addHwInfo();  // include field hw info
		if (fieldProperties.getReset() != null) {
			addXmlElement("reset", fieldProperties.getReset().toFormat(NumBase.Hex, NumFormat.Int)); // output reset as hex string
		}
		//addXmlElementStart("position");
		addXmlElement("lowidx", fieldProperties.getLowIndex().toString());
		addXmlElement("width", fieldProperties.getFieldWidth().toString());
		//addXmlElementEnd("position");
		if (fieldProperties.hwChangesValue()) 
			addXmlElement("hwmod", "");
		if (fieldProperties.isDontCompare()) 
			addXmlElement("dontcompare", ""); 			
		if (fieldProperties.isCounter()) 
			addCounterInfo();
		if (fieldProperties.isInterrupt()) 
			addIntrInfo();
		if (fieldProperties.hasSubCategory()) 
			addXmlElement("subcatcode", String.valueOf(fieldProperties.getSubCategory().getValue()));  
		if (fieldProperties.getTextDescription() != null) 
			addXmlElement("longtext", wrapXmlText(fieldProperties.getTextDescription()));
		// if this instance has user defined properties, add them
		addUserDefinedPropertyElements(fieldProperties);
		// if this field has an enum encoding add it
		addFieldEncodeInfo(fieldProperties);
		
		addXmlElementEnd("field");
		// save the common access mode
		String fieldAccess = getFieldAccessType();
		if (commonRegAccess == null) commonRegAccess = fieldAccess;
		else if (!commonRegAccess.equals(fieldAccess)) commonRegAccess = "nope";
	}

	@Override
	public void addAliasField() {
		addField();
	}

	@Override
	public void addRegister() {
		commonRegAccess = null;  // init common access string before field processing
		addXmlElementStart("reg");
		addXmlElement("id", regProperties.getId());
		String parentPath = this.getParentInstancePath();
		if ((parentPath != null) && !parentPath.isEmpty()) addXmlElement("parentpath", parentPath);
		String textName = regProperties.getTextName();
		if (textName == null) textName = regProperties.getId() + " register";
		textName = cleanXmlText(textName);
		if (textName.length() > 255) {
			Ordt.warnMessage("Register " + regProperties.getInstancePath() + " short text description exceeds 255 characters and will be truncated in xml.");
			textName = textName.substring(0, 255);
		}
		addXmlElement("shorttext", textName);
		addXmlElement("baseaddr", regProperties.getFullBaseAddress().toString());
		addXmlElement("width", regProperties.getRegWidth().toString());
		if (regProperties.isReplicated()) {
			addXmlElement("reps", String.valueOf(regProperties.getRepCount()));
			addXmlElement("stride", regProperties.getAddrStride().toFormat(NumBase.Hex, NumFormat.Address));  // write stride in hex
		}
		if (regProperties.isRootExternal()) 
			addXmlElement("extroot", "");
		if (regProperties.isDontTest()) 
			addXmlElement("donttest", ""); 			
		if (regProperties.getTextDescription() != null) 
			addXmlElement("longtext", wrapXmlText(regProperties.getTextDescription()));
		// add infotext
		String infoStr = "";
		if (regProperties.isAlias()) {
			infoStr += "Alias of peer register " + regProperties.getAliasedId() + ".  ";
		} // intr
		if (!infoStr.isEmpty()) addXmlElement("infotext", infoStr);
		// 
		if (regProperties.isAlias()) addXmlElement("alias", "");  
	}

	@Override
	public void finishRegister() {
		// add category after all fields are processed
		if (regProperties.hasCategory()) 
			addXmlElement("catcode", String.valueOf(regProperties.getCategory().getValue()));  
		// add access mode after all fields processed
		if ((commonRegAccess != null) && (!commonRegAccess.equals("nope"))) addXmlElement("access", commonRegAccess);
		else {
			String acc = "";
			if (regProperties.isSwReadable()) acc += 'R';
			if (regProperties.isSwWriteable()) acc += 'W';
			addXmlElement("access", acc);
		}
		// if this instance has user defined properties, add them
		addUserDefinedPropertyElements(regProperties);
		addXmlElementEnd("reg");
	}

	@Override
	public void addRegSet() {
		// start this regset/addrmap element
		String rsType = (regSetProperties.isAddressMap())? "map" : "regset";
		// if a root instance add the version attribute
		if (regSetProperties.isRootInstance()) addXmlElementStart(rsType, "version", Ordt.getVersion());
		else addXmlElementStart(rsType);
		addXmlElement("id", regSetProperties.getId());
		String parentPath = this.getParentInstancePath();
		if ((parentPath != null) && !parentPath.isEmpty()) addXmlElement("parentpath", parentPath);
		String textName = regSetProperties.getTextName();
		if (textName == null) textName = regSetProperties.getId() + " registers";
		textName = cleanXmlText(textName);
		if (textName.length() > 255) {
			Ordt.warnMessage("Register set " + regSetProperties.getInstancePath() + " short text description exceeds 255 characters and will be truncated in xml.");
			textName = textName.substring(0, 255);
		}
		addXmlElement("shorttext", textName);
		// address
		if (regSetProperties.isRootInstance())
			addXmlElement("baseaddr", ExtParameters.getPrimaryBaseAddress().toString());
		else
			addXmlElement("baseaddr", regSetProperties.getFullBaseAddress().toString());
		// replication count
		if (regSetProperties.isReplicated()) {
			addXmlElement("reps", String.valueOf(regSetProperties.getRepCount()));
		}
		if (regSetProperties.getTextDescription() != null) 
			addXmlElement("longtext", wrapXmlText(regSetProperties.getTextDescription()));
		if (regSetProperties.isRootExternal()) 
			addXmlElement("extroot", "1");
	}

	@Override
	public void finishRegSet() {
		// process stride and highaddr which are only available once children are processed
		if (regSetProperties.isReplicated()) {
			addXmlElement("stride", getRegSetAddressStride().toString());  // write stride in hex
		}
		if (regSetProperties.getFullHighAddress() != null)
			addXmlElement("highaddr", regSetProperties.getFullHighAddress().toString());
		// if this instance has user defined properties, add them
		addUserDefinedPropertyElements(regSetProperties);
		// close up this regset/addrmap
		if (regSetProperties.isAddressMap()) addXmlElementEnd("map");
		else addXmlElementEnd("regset");
	}

	@Override
	public void addRegMap() {
		// issue warning message if non-aligned
		if (!ExtParameters.useJsAddressAlignment())
			Ordt.warnMessage("use of non-jspec alignment mode may cause incorrect addresses in xml model.");
		addXmlElementStart("map", "version", Ordt.getVersion());
		addXmlElement("id", getAddressMapName());
		addXmlElement("baseaddr", ExtParameters.getPrimaryBaseAddress().toString());
		String textName = regSetProperties.getTextName();
		if (textName == null) textName = getAddressMapName() + " registers";
		textName = cleanXmlText(textName);
		if (textName.length() > 255) {
			Ordt.warnMessage("Address map " + getAddressMapName() + " short text description exceeds 255 characters and will be truncated in xml.");
			textName = textName.substring(0, 255);
		}
		addXmlElement("shorttext", textName);
		if (regSetProperties.getTextDescription() != null) 
			addXmlElement("longtext", wrapXmlText(regSetProperties.getTextDescription()));
	}

	@Override
	public void finishRegMap() {
		addXmlElementEnd("map");
	}
	
	//--------------------------------------------------------------------

	/** write new element start with an attribute/value */
	private void addXmlElementStart(String elemName, String attrName, String attrVal) {
		outputList.add(new OutputLine(indentLvl++, "<" + elemName + " " + attrName + "=\"" + attrVal + "\">")); 		
	}

	/** write new element start */
	private void addXmlElementStart(String name) {
		outputList.add(new OutputLine(indentLvl++, "<" + name + ">")); 		
	}

	/** write new element end */
	private void addXmlElementEnd(String name) {
		indentLvl--;
		outputList.add(new OutputLine(indentLvl, "</" + name + ">"));  
	}

	/** write new element */
	private void addXmlElement(String name, String content) {
		outputList.add(new OutputLine(indentLvl, "<" + name + ">" + content + "</" + name + ">")); 		
	}

	/** generate field sw access type string (uvm_regs encoding) */
	private String getFieldAccessType() {
		String accessMode = "RO";
		// if read clear
		if (fieldProperties.isRclr()) {
			if (fieldProperties.isWoset()) accessMode = "W1SRC"; 
			else if (fieldProperties.isSwWriteable()) accessMode = "WRC";
		    else accessMode = "RC";
		}
		// if read set
		else if (fieldProperties.isRset()) {
			if (fieldProperties.isWoclr()) accessMode = "W1CRS"; 
			else if (fieldProperties.isSwWriteable()) accessMode = "WRS";
		    else accessMode = "RS";
		}
		// no read set/clr
		else {
			if (fieldProperties.isWoclr()) accessMode = "W1C";   
			else if (fieldProperties.isWoset()) accessMode = "W1S";
			else if (fieldProperties.isSwWriteable()) {
				if (fieldProperties.isSwReadable()) accessMode = "RW"; 
				else accessMode = "WO";
			}
		}
		return accessMode;
	}

	/** generate hw info tags */
	private void addHwInfo() {
		addXmlElementStart("hwinfo");
		addXmlElement("hwaccess", getFieldHwAccessType(fieldProperties));
		if (fieldProperties.hasWriteEnableH()) addXmlElement("we", "");
		else if (fieldProperties.hasWriteEnableL()) addXmlElement("wel", "");
		if (fieldProperties.hasHwSet()) addXmlElement("hwset", "");
		if (fieldProperties.hasHwClr()) addXmlElement("hwclr", "");
		if (fieldProperties.hasSwMod()) addXmlElement("swmod", "");
		if (fieldProperties.hasSwAcc()) addXmlElement("swacc", "");
		if (fieldProperties.hasSwWriteEnableH()) addXmlElement("swwe", "");
		else if (fieldProperties.hasSwWriteEnableL()) addXmlElement("swwel", "");
		if (fieldProperties.isAnded()) addXmlElement("anded", "");
		if (fieldProperties.isOred()) addXmlElement("ored", "");
		if (fieldProperties.isXored()) addXmlElement("xored", "");
		if (fieldProperties.hasRef(RhsRefType.NEXT)) {
			RhsReference ref = fieldProperties.getRef(RhsRefType.NEXT);
			if (ref != null) {
				String deRef = (ref.hasDeRef())? "(" + ref.getDeRef() + ")" : "";
				addXmlElement("nextassign", ref.getResolvedFieldWildcardPath(fieldProperties) + deRef);
			}
		}
		addXmlElementEnd("hwinfo");
	}

	/** return hw access type (rdl format) */
	private String getFieldHwAccessType(FieldProperties field) {
		String accessMode = "";
		if (fieldProperties.isHwReadable()) accessMode += "r";
		if (fieldProperties.isHwWriteable()) accessMode += "w";
		if (accessMode.isEmpty()) accessMode += "na";
		return accessMode;
	}

	/** generate informational string for counters/interrupts */
	@SuppressWarnings("unused")
	private String genInfoStr() {
		String infoStr = "";
		if (fieldProperties.isInterrupt()) {
			RhsReference ref = null;
			if (fieldProperties.hasRef(RhsRefType.NEXT)) ref = fieldProperties.getRef(RhsRefType.NEXT);
			else if (fieldProperties.hasRef(RhsRefType.INTR)) ref = fieldProperties.getRef(RhsRefType.INTR);
			if (ref != null) {
				if (ref.hasDeRef("intr")) infoStr += "Interrupt merge field set by register " + ref.getInstancePath() + ".  ";
				else infoStr += "Interrupt field set by " + ref.getResolvedFieldWildcardPath(fieldProperties) + ".  ";
			}
			if (fieldProperties.hasRef(RhsRefType.INTR_MASK)) infoStr += "Interrupt output masked by " + fieldProperties.getRef(RhsRefType.INTR_MASK).getResolvedFieldWildcardPath(fieldProperties) + ".  ";
			else if (fieldProperties.hasRef(RhsRefType.INTR_ENABLE)) infoStr += "Interrupt output enabled by " + fieldProperties.getRef(RhsRefType.INTR_ENABLE).getResolvedFieldWildcardPath(fieldProperties) + ".  ";
			if (fieldProperties.hasRef(RhsRefType.HALT_MASK)) infoStr += "Rdl halt output masked by " + fieldProperties.getRef(RhsRefType.HALT_MASK).getResolvedFieldWildcardPath(fieldProperties) + ".  ";
			else if (fieldProperties.hasRef(RhsRefType.HALT_ENABLE)) infoStr += "Rdl halt output enabled by " + fieldProperties.getRef(RhsRefType.HALT_ENABLE).getResolvedFieldWildcardPath(fieldProperties) + ".  ";
		} // intr
		if (fieldProperties.isCounter()) {
			if (fieldProperties.isIncrSatCounter()) infoStr += "Saturating counter.  ";
			//else infoStr += "Rollover counter.  ";
		}
		return infoStr;
	}

	/** add interrupt info detail if include_field_hw_info is set */
	private void addIntrInfo() {
		if (!ExtParameters.xmlIncludeFieldHwInfo()) addXmlElement("intr", "");
		else {
			addXmlElementStart("intr");
			addXmlElement("type", fieldProperties.getIntrType().name());
			addXmlElement("stickytype", fieldProperties.getIntrStickyType().name());
			if (fieldProperties.isMaskIntrBits()) addXmlElement("maskintrbits", "");
			// add cascaded intr references
			RhsReference ref = null;
			if (fieldProperties.hasRef(RhsRefType.NEXT)) ref = fieldProperties.getRef(RhsRefType.NEXT);
			else if (fieldProperties.hasRef(RhsRefType.INTR)) ref = fieldProperties.getRef(RhsRefType.INTR);
			if (ref != null) {
				if (ref.hasDeRef("intr")) addXmlElement("input", ref.getInstancePath());
				else addXmlElement("input", ref.getResolvedFieldWildcardPath(fieldProperties));
			}
			if (fieldProperties.hasRef(RhsRefType.INTR_MASK)) addXmlElement("mask", fieldProperties.getRef(RhsRefType.INTR_MASK).getResolvedFieldWildcardPath(fieldProperties));
			else if (fieldProperties.hasRef(RhsRefType.INTR_ENABLE)) addXmlElement("enable", fieldProperties.getRef(RhsRefType.INTR_ENABLE).getResolvedFieldWildcardPath(fieldProperties));
            // halt info
			if (fieldProperties.isHalt()) addXmlElement("halt", "");
			if (fieldProperties.hasRef(RhsRefType.HALT_MASK)) addXmlElement("haltmask", fieldProperties.getRef(RhsRefType.HALT_MASK).getResolvedFieldWildcardPath(fieldProperties));
			else if (fieldProperties.hasRef(RhsRefType.HALT_ENABLE)) addXmlElement("haltenable", fieldProperties.getRef(RhsRefType.HALT_ENABLE).getResolvedFieldWildcardPath(fieldProperties));
			addXmlElementEnd("intr");
		}
	}

	/** add counter info detail if include_field_hw_info is set */
	private void addCounterInfo() {
		if (!ExtParameters.xmlIncludeFieldHwInfo()) addXmlElement("counter", "");
		else {
			addXmlElementStart("counter");
			String satVal = fieldProperties.hasSaturateOutputs()? "has_output" : "no_output";
			   int fieldWidth = fieldProperties.getFieldWidth();
			   int countWidth = fieldWidth + 1;  // add a bit for over/underflow
			// add incr counter info
			if (fieldProperties.isIncrCounter()) {
				addXmlElementStart("incr");
				if (fieldProperties.hasOverflow())  addXmlElement("overflow", "");
				if (fieldProperties.isIncrSatCounter()) addXmlElement("saturate", satVal);
				// add incr value 
				if (fieldProperties.hasRef(RhsRefType.INCR_VALUE)) addXmlElement("incrvalue", "ref");
				else { 
					RegNumber regNum = fieldProperties.getIncrValue();
					if (regNum!=null) {
						regNum.setVectorLen(countWidth);
						addXmlElement("incrvalue", regNum.toFormat(NumBase.Hex, NumFormat.Address));
					}
				}
				// add saturate value 
				if (fieldProperties.hasRef(RhsRefType.INCR_SAT_VALUE)) addXmlElement("satvalue", "ref");
				else { 
					RegNumber regNum = fieldProperties.getIncrSatValue();
					if (regNum!=null) {
						regNum.setVectorLen(countWidth);
						addXmlElement("satvalue", regNum.toFormat(NumBase.Hex, NumFormat.Address));
					}
				}
				// add threshold value 
				if (fieldProperties.hasRef(RhsRefType.INCR_THOLD_VALUE)) addXmlElement("threshold", "ref");
				else { 
					RegNumber regNum = fieldProperties.getIncrTholdValue();
					if (regNum!=null) {
						regNum.setVectorLen(countWidth);
						addXmlElement("threshold", regNum.toFormat(NumBase.Hex, NumFormat.Address));
					}
				}
				addXmlElementEnd("incr");
			}
			// add decr counter info
			if (fieldProperties.isDecrCounter()) {
				addXmlElementStart("decr");
				if (fieldProperties.hasUnderflow())  addXmlElement("underflow", "");
				if (fieldProperties.isDecrSatCounter()) addXmlElement("saturate", satVal);
				// add decr value 
				if (fieldProperties.hasRef(RhsRefType.DECR_VALUE)) addXmlElement("decrvalue", "ref");
				else { 
					RegNumber regNum = fieldProperties.getDecrValue();
					if (regNum!=null) {
						regNum.setVectorLen(countWidth);
						addXmlElement("decrvalue", regNum.toFormat(NumBase.Hex, NumFormat.Address));
					}
				}
				// add saturate value 
				if (fieldProperties.hasRef(RhsRefType.DECR_SAT_VALUE)) addXmlElement("satvalue", "ref");
				else { 
					RegNumber regNum = fieldProperties.getDecrSatValue();
					if (regNum!=null) {
						regNum.setVectorLen(countWidth);
						addXmlElement("satvalue", regNum.toFormat(NumBase.Hex, NumFormat.Address));
					}
				}
				// add threshold value 
				if (fieldProperties.hasRef(RhsRefType.DECR_THOLD_VALUE)) addXmlElement("threshold", "ref");
				else { 
					RegNumber regNum = fieldProperties.getDecrTholdValue();
					if (regNum!=null) {
						regNum.setVectorLen(countWidth);
						addXmlElement("threshold", regNum.toFormat(NumBase.Hex, NumFormat.Address));
					}
				}
				addXmlElementEnd("decr");
			}
			addXmlElementEnd("counter");
		}	
	}

	/** remove xml arrows and amp from a string */
	private String cleanXmlText(String textDescription) {
		String outdesc = textDescription.replace('<', '-').replace('>', '-').replace('&', '+');  // xml parser doesnt like embedded html tags in text
		return outdesc;  
	}

	/** return a string with a CDATA wrapper */
	private String wrapXmlText(String textDescription) {
		textDescription = textDescription.replace("<APP", "-APP").replace("<app", "-app");
		textDescription = textDescription.replace("<SCRIPT", "-SCRIPT").replace("<script", "-script");
		String outdesc = "<![CDATA[" + textDescription.replace("]]>", "") + "]]>";  // use cdata to embedded html tags 
		return outdesc;  
	}

	/** add elements containing user defined properties */
	private void addUserDefinedPropertyElements(InstanceProperties instProperties) {
		if (!instProperties.hasUserDefinedProperties()) return;  // done if no external properties
		addXmlElementStart("user_properties");
		PropertyList pList = instProperties.getUserDefinedProperties();
		for (String name : pList.getProperties().keySet()) {
			String value = (pList.getProperty(name) == null)? "" : pList.getProperty(name);
			addXmlElement(name, value);
		}
		addXmlElementEnd("user_properties");
	}

	/** add field encode elements */
	private void addFieldEncodeInfo(FieldProperties fieldProperties) {
		if (fieldProperties.getEncoding()==null) return;  // exit if no encoding
		addXmlElementStart("enum_encode");
		ModEnum enumDef = fieldProperties.getEncoding();
		addXmlElement("enc_name", enumDef.getId());
		// check width
		Integer encodeWidth = enumDef.getWidth();
		//System.out.println("XmlBuilder addFieldEncodeInfo: encoding id=" + enumDef.getId() + ", enumElems=" + enumDef.getEnumElements().size());
		if ((encodeWidth != null && encodeWidth != fieldProperties.getFieldWidth())) 
			Ordt.errorMessage("Encoding width ("+ encodeWidth + ") does not match field width (" + fieldProperties.getFieldWidth() + ") in " + fieldProperties.getInstancePath());
		else {
			for (ModEnumElement enumElem : enumDef.getEnumElements()) {
				addXmlElementStart("enc_elem");
				enumElem.getValue().setNumFormat(RegNumber.NumFormat.Address);
				String elemName = (enumElem.getId() == null) ? "encode_" + enumElem.getValue() : enumElem.getId();
				addXmlElement("enc_elem_name", elemName);
				String elemValue = enumElem.getValue().toFormat(NumBase.Hex, NumFormat.Address);
				addXmlElement("enc_elem_value", elemValue);
				//if (enumElem.getName() != null) addXmlElement("enc_elem_info", enumElem.getName());
				addXmlElementEnd("enc_elem");	
			}
		}
		addXmlElementEnd("enum_encode");	
	}

	//---------------------------- methods to output verilog ----------------------------------------

	@Override
	protected void write(BufferedWriter bw) {
		bufferedWriter = bw;

		// write the output for each output group
		for (OutputLine jsLine: outputList) {
			//writeStmt(indentLevel, "/* registers in set=" + setName + " */");  		
			writeStmt(jsLine.getIndent(), jsLine.getLine());  
		}
	}

}
