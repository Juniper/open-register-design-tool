/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.othertypes;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;
import ordt.output.OutputLine;
import ordt.output.RhsReference;
import ordt.output.FieldProperties.RhsRefType;
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
	    setAllowLocalMapInternals(true);  // cascaded addrmaps will result in local non-ext regions   
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

	//---------------------------- OutputBuilder methods to create xml structures ----------------------------------------

	@Override
	public void addSignal() {
		/*addXmlElementStart("signal");
		addXmlElement("id", signalProperties.getId());
		addXmlElementEnd("signal");*/
	}

	@Override
	public void addField() {
		addXmlElementStart("field");
		addXmlElement("id", fieldProperties.getId());
		String textName = fieldProperties.getTextName();
		if (textName == null) textName = fieldProperties.getId() + " field";
		addXmlElement("shorttext", cleanXmlText(textName));
		addXmlElement("access", getFieldAccessType(fieldProperties));
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
			addXmlElement("counter", "");
		if (fieldProperties.isInterrupt()) 
			addXmlElement("intr", "");
		if (fieldProperties.hasSubCategory()) 
			addXmlElement("subcatcode", String.valueOf(fieldProperties.getSubCategory().getValue()));  
		if (fieldProperties.getTextDescription() != null) 
			addXmlElement("longtext", wrapXmlText(fieldProperties.getTextDescription()));
		// add infotext
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
		if (!infoStr.isEmpty()) addXmlElement("infotext", infoStr);
		
		addXmlElementEnd("field");
		/*
		if (regProperties.getId().startsWith("features")) {
			System.out.println("XmlBuilder addField: reg=" + regProperties.getId() + ", field=" + fieldProperties.getId());
			System.out.println("   hwChanges=" + fieldProperties.hwChangesValue() + ", counter=" + fieldProperties.isCounter() + ", intr=" + fieldProperties.isInterrupt());	
			System.out.println("   isHwWriteable=" + fieldProperties.isHwWriteable() + ", hasWe=" + fieldProperties.hasWriteEnable() );	
		}*/
		// save the common access mode
		String fieldAccess = getFieldAccessType(fieldProperties);
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
		addXmlElement("shorttext", cleanXmlText(textName));
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
		addXmlElementEnd("reg");
	}

	@Override
	public void addRootExternalRegisters() {
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
		addXmlElement("shorttext", cleanXmlText(textName));
		// address
		if (regSetProperties.isRootInstance())
			addXmlElement("baseaddr", ExtParameters.getLeafBaseAddress().toString());
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
			addXmlElement("stride", getRegSetAddressStride(true).toString());  // write stride in hex
		}
		if (regSetProperties.getFullHighAddress() != null)
			addXmlElement("highaddr", regSetProperties.getFullHighAddress().toString());
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
		addXmlElement("baseaddr", ExtParameters.getLeafBaseAddress().toString());
		String textName = regSetProperties.getTextName();
		if (textName == null) textName = getAddressMapName() + " registers";
		addXmlElement("shorttext", cleanXmlText(textName));
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
