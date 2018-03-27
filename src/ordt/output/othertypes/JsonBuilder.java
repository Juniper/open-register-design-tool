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
import ordt.parameters.ExtParameters;

public class JsonBuilder extends OutputBuilder {  
	private List<OutputLine> outputList = new ArrayList<OutputLine>();
	private int indentLvl = 0;
	private String commonRegAccess = null;  // detect common access mode of all fields
	
    //---------------------------- constructor ----------------------------------

    public JsonBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

	//---------------------------- OutputBuilder methods to load jspec structures ----------------------------------------

	@Override
	public void addField() {
        // build element for reg instance 
		String textName = fieldProperties.getTextName();
		if (textName == null) textName = fieldProperties.getPrefixedId() + " field";
		else textName = cleanJsonText(textName);
		String textDescription = fieldProperties.getTextName();
		if (textDescription != null) textDescription = cleanJsonText(textDescription);
		addJsonElementStart(fieldProperties.getPrefixedId(), "field", textName, textDescription);
		// address
		addJsonStringElement("accessMode", getFieldAccessType(fieldProperties));
		if (fieldProperties.getReset() != null) 
			addJsonStringElement("reset", fieldProperties.getReset().toFormat(NumBase.Hex, NumFormat.Address)); // output reset 
		//addJsonElementStart("position");
		addJsonNumElement("lsb", fieldProperties.getLowIndex().toString());
		int hiIdx = fieldProperties.getLowIndex() + fieldProperties.getFieldWidth() - 1;
		addJsonNumElement("msb", String.valueOf(hiIdx));
		addJsonElementEnd(false);  // end field element
		/*
		if (regProperties.getId().startsWith("features")) {
			System.out.println("JsonBuilder addField: reg=" + regProperties.getId() + ", field=" + fieldProperties.getPrefixedId());
			System.out.println("   hwChanges=" + fieldProperties.hwChangesValue() + ", counter=" + fieldProperties.isCounter() + ", intr=" + fieldProperties.isInterrupt());	
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
        // build element for reg instance 
		String textName = regProperties.getTextName();
		if (textName == null) textName = regProperties.getId() + " register";
		else textName = cleanJsonText(textName);
		String textDescription = regProperties.getTextName();
		if (textDescription != null) textDescription = cleanJsonText(textDescription);
		addJsonElementStart(regProperties.getId(), "register", textName, textDescription);
		// address
		addJsonNumElement("address", regProperties.getRelativeBaseAddress().toFormat(NumBase.Dec, NumFormat.Int));
		// replication count
		if (regSetProperties.isReplicated()) {
			addJsonNumElement("repeat", String.valueOf(regProperties.getRepCount()));
			addJsonNumElement("stride", regProperties.getAddrStride().toFormat(NumBase.Hex, NumFormat.Address));  // TODO ok? write stride in hex
		}
		// width
		addJsonNumElement("width", regProperties.getRegWidth().toString());
		// add contents elem start
		addJsonElementStart("contents", null, null, null);
	}

	@Override
	public void finishRegister() {
		addJsonNumElement("parent", null, true);  // add bogus parent to close out contents element
		addJsonElementEnd(false);  // end the contents element
		// add category after all fields are processed
		if (regProperties.hasCategory()) 
			addJsonStringListElement("category", regProperties.getCategory().toString());  
		// add access mode after all fields processed
		/*if ((commonRegAccess != null) && (!commonRegAccess.equals("nope"))) addJsonStringElement("access", commonRegAccess);
		else {
			String acc = "";
			if (regProperties.isSwReadable()) acc += 'R';
			if (regProperties.isSwWriteable()) acc += 'W';
			addJsonStringElement("access", acc);
		}*/   // TODO
		addJsonElementEnd(false);  // end of reg
	}

	@Override
	public void addRegSet() {
		if (regSetProperties.isRootInstance()) addTopLevelElementStart();  // start root element
        // now build element for regset instance 
		String textName = regSetProperties.getTextName();
		if (textName == null) textName = regSetProperties.getId() + " registers";
		else textName = cleanJsonText(textName);
		String textDescription = regSetProperties.getTextName();
		if (textDescription != null) textDescription = cleanJsonText(textDescription);
		addJsonElementStart(regSetProperties.getId(), "register_set", textName, textDescription);
		// address
		if (regSetProperties.isRootInstance())
			addJsonNumElement("address", ExtParameters.getPrimaryBaseAddress().toFormat(NumBase.Dec, NumFormat.Int));
		else
			addJsonNumElement("address", regSetProperties.getRelativeBaseAddress().toFormat(NumBase.Dec, NumFormat.Int));
		// replication count
		if (regSetProperties.isReplicated()) {
			addJsonNumElement("repeat", String.valueOf(regSetProperties.getRepCount()));
		}
		// add contents elem start
		addJsonElementStart("contents", null, null, null);
	}

	@Override
	public void finishRegSet() {
		addJsonNumElement("parent", null, true);  // add bogus parent to close out contents element
		addJsonElementEnd(false);  // end the contents element
		// add size which is only available once children are processed
		addJsonStringElement("size", getRegSetAddressStride().toFormat(NumBase.Dec, NumFormat.Int), true);  // write stride in dec 
		if (regSetProperties.isRootInstance()) {
			addJsonElementEnd(true);  // end the root contents elem
			addJsonElementEnd(true);  // end root element
		}
	}

	@Override
	public void addRegMap() {
		addTopLevelElementStart();   // start root element
        // now build element for root map instance 
		String textName = regSetProperties.getTextName();
		if (textName == null) textName = getAddressMapName() + " registers";
		else textName = cleanJsonText(textName);
		String textDescription = regSetProperties.getTextName();
		if (textDescription != null) textDescription = cleanJsonText(textDescription);
		addJsonElementStart(getAddressMapName(), "register_set", textName, textDescription);
		// address
		addJsonNumElement("address", ExtParameters.getPrimaryBaseAddress().toFormat(NumBase.Dec, NumFormat.Int));
		// add contents elem start
		addJsonElementStart("contents", null, null, null);
	}

	@Override
	public void finishRegMap() {
		addJsonNumElement("parent", null, true);  // add bogus parent to close out contents element
		addJsonElementEnd(false);  // end the contents element
		// add size which is only available once children are processed
		addJsonNumElement("size", getNextAddress().toFormat(NumBase.Dec, NumFormat.Int), true);  // write size in dec
		addJsonElementEnd(true);  // end the addr map
		addJsonElementEnd(true);  // end the root contents elem
		addJsonElementEnd(true);  // end the root json elem
	}
	
	//--------------------------------------------------------------------

	/** add root json element start */
	private void addTopLevelElementStart() {
		// issue warning message if non-aligned
		if (!ExtParameters.useJsAddressAlignment())
			Ordt.warnMessage("use of non-jspec alignment mode may cause incorrect addresses in json model.");
		addJsonElementStart("JSPEC - jrdl " + Ordt.getVersion(), "top-level", null, null);
		addJsonElementStart("contents", null, null, null);
	}

	/** write new element start */
	private void addJsonElementStart(String name, String type, String summary, String description) {
		outputList.add(new OutputLine(indentLvl++, "top-level".equals(type)? "{" : "\"" + name + "\" : {"));
		if ((name != null) && !"contents".equals(name)) addJsonStringElement("name", name);
		if (summary != null) addJsonStringElement("summary", summary);
		if (type != null) addJsonStringElement("type", type);
		if (description != null) addJsonStringElement("description", description);
	}

	/** write new element end */
	private void addJsonElementEnd(boolean isLast) {
		indentLvl--;
		String suffix = isLast ? "" : ",";
		outputList.add(new OutputLine(indentLvl, "}" + suffix));  
	}

	/** write new string element */
	private void addJsonStringElement(String name, String content, boolean isLast) {
		String suffix = isLast ? "" : ",";
		outputList.add(new OutputLine(indentLvl, "\"" + name + "\" : \"" + content + "\"" + suffix)); 		
	}
	
	/** write new string element - include comma suffix */
	private void addJsonStringElement(String name, String content) {
		addJsonStringElement(name, content, false);
	}

	/** write new num element */
	private void addJsonNumElement(String name, String content, boolean isLast) {
		String suffix = isLast ? "" : ",";
		outputList.add(new OutputLine(indentLvl, "\"" + name + "\" : " + content + suffix)); 		
	}
	
	/** write new num element */
	private void addJsonNumElement(String name, String content) {
		addJsonNumElement(name, content, false);
	}

	/** write new string list element (single element case) */
	private void addJsonStringListElement(String name, String content, boolean isLast) {
		String suffix = isLast ? "" : ",";
		outputList.add(new OutputLine(indentLvl, "\"" + name + "\" : [\"" + content + "\"]" + suffix)); 		
	}
	
	/** write new string list element (single element case) - include comma suffix */
	private void addJsonStringListElement(String name, String content) {
		addJsonStringListElement(name, content, false);
	}

	/** generate field access type string 
	 * @param field */
	private String getFieldAccessType(FieldProperties field) {
		String accessMode = "READ_ONLY";
		if (field.isWoclr()) accessMode = "WRITE_ONE_TO_CLEAR";   
		else if (field.isWoset()) accessMode = "WRITE_ONE_TO_SET";   
		else if (field.isRclr()) accessMode = "READ_TO_CLEAR";
		else if (field.isSwWriteable()) {
			if (field.isSwReadable()) accessMode = "READ_WRITE"; 
			else accessMode = "WRITE_ONLY";
		}
		return accessMode;
	}

	/* generate field access type string   TODO - use uvmregs format?
	 * @param field *
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
	}*/

	private String cleanJsonText(String textDescription) {
		String outdesc = textDescription.replace('{', '-').replace('}', '-');
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
