/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved. 
 */
package ordt.output.othertypes;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ordt.extract.DefinedProperties;
import ordt.extract.DefinedProperty;
import ordt.output.common.MsgUtils;
import ordt.extract.PropertyList;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.model.ModComponent;
import ordt.extract.model.ModEnum;
import ordt.extract.model.ModEnumElement;
import ordt.extract.model.ModRegister;
import ordt.output.AddressableInstanceProperties;
import ordt.output.FieldProperties;
import ordt.output.InstanceProperties;
import ordt.output.OutputBuilder;
import ordt.output.RhsReference;
import ordt.output.common.OutputLine;
import ordt.parameters.ExtParameters;

public class RdlBuilder extends OutputBuilder {
	
	private List<OutputLine> outputList = new ArrayList<OutputLine>();
	private static HashSet<String> escapedIds = new HashSet<String>(); // set of keywords needing to be escaped
	private int indentLvl = 0;
	
    //---------------------------- constructor ----------------------------------

    public RdlBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    setEscapedIds();
		RhsReference.setInstancePropertyStack(instancePropertyStack);  // update pointer to the instance stack for rhs reference evaluation
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

	//---------------------------- OutputBuilder methods to load rdl structures ----------------------------------------

	@Override
	public void addField() {
	}

	@Override
	public void addAliasField() {
		// handle same as non-aliased field
		addField();
	}

	@Override
	public void addRegister() {
	}

	@Override
	public void finishRegister() {
		// only add the register if it is sw accessible
		if (regProperties.isSwWriteable() || regProperties.isSwReadable()) {
			//if (regProperties.isAlias()) System.out.println("RdlBuilder finishRegister: Id=" + regProperties.getId() + ", aliasedId=" + regProperties.getAliasedId());
			// build the register header
			String aliasComp = regProperties.isAlias()? regProperties.getId() + "_ALIAS" : "";
			buildRegHeader(aliasComp);
			// add field info
			buildFields();  
			// close out the register definition
			// if not an alias, just generate the instance
			if (!regProperties.isAlias()) outputList.add(new OutputLine(--indentLvl, "} " + getInstanceStr(regProperties) + ";"));
			// otherwise make the reg a component define and instance as an alias
			else {
				outputList.add(new OutputLine(--indentLvl, "};"));
				outputList.add(new OutputLine(indentLvl, ""));	
				outputList.add(new OutputLine(indentLvl, "alias " + regProperties.getAliasedId() + " " + aliasComp + " " + getInstanceStr(regProperties) + ";"));	
			}
			outputList.add(new OutputLine(indentLvl, ""));	
		}
	}

	@Override
	public void addRegSet() {
		// if this is the root instance add any enum defs at root level
		if (regSetProperties.isRootInstance() && !ExtParameters.rdlNoRootEnumDefs()) buildEnumDefs(model.getRoot()); 
		// add any user property definitions and get list of js passthru properties
		if (regSetProperties.isRootInstance()){
			buildPropertyDefs();
		}
		// if a container for multiple rdl component defs, generate no output
		if ("TYPEDEF_CONTAINER".equals(regSetProperties.getId())) {
			//buildEnumDefs(regSetProperties.getExtractInstance().getRegComp()); // just build the enums
			return; // do not output the typedef container used in translate from jspec
		}
		// build the register header
		buildRegSetHeader();  
		//System.out.println("RdlBuilder addRegSet: " + regSetProperties.getInstancePath() + ", addr=" + regSetProperties.getBaseAddress());
	}

	@Override
	public void finishRegSet() {  
		// close out the register set instance
		if ("TYPEDEF_CONTAINER".equals(regSetProperties.getId())) return; // do not output the typedef container used in translate
		// if root regset(s) are not to be instanced then inhibit
		//System.out.println("RdlBuilder finishRegSet: ext root isntanced=" + ExtParameters.rdlRootComponentIsInstanced() + ", id=" + regSetProperties.getId() + ", is root inst=" + regSetProperties.isRootInstance());  
		boolean noComponentInstance = (!ExtParameters.rdlRootComponentIsInstanced() && regSetProperties.isRootInstance()) || regSetIsTypedefContainerInstance();
		String instanceStr = noComponentInstance ? "" : getInstanceStr(regSetProperties);  
		outputList.add(new OutputLine(--indentLvl, "} " + instanceStr + ";"));
		outputList.add(new OutputLine(indentLvl, ""));	
	}

	/** process root instanced address map */
	@Override
	public void addRegMap() { 
		// add any enum defs at root level
		if (!ExtParameters.rdlNoRootEnumDefs()) buildEnumDefs(model.getRoot());
		// add any user property definitions
		buildPropertyDefs();
		
		outputList.add(new OutputLine(indentLvl, "// address map " + getAddressMapName()));  
		//System.out.println("RdlBuilder addRegMap: addrmap regSetProperties, id=" +
		//       ((regSetProperties == null) ? "null" : regSetProperties.getId()) + ", inst=" + getInstancePath());
		// if root is not to be instanced then add component name
		String compName = (!ExtParameters.rdlRootComponentIsInstanced() && regSetProperties.isRootInstance()) ? getAddressMapName() + " " : "";  
		outputList.add(new OutputLine(indentLvl, "addrmap " + compName + "{"));
		outputList.add(new OutputLine(indentLvl++, ""));
		buildCommonAssigns(regSetProperties);  // set params for child addrmaps
		// if this is the root instanced addrmap (sv builder 0), display enums defs in root instanced component
		buildEnumDefs(model.getRootInstancedComponent());  
	}

	/** finish root address map  */
	@Override
	public  void finishRegMap() {	
		// compute total size of this address map/root register set
		String mapId = getAddressMapName();
		// close out the addrmap definition
		//if (regSetProperties == null) System.out.println("RdlBuilder finishRegMap: no regSetProperties for addrmap " + mapId);
		//else System.out.println("RdlBuilder finishRegMap: addrmap resSetProperties, id=" + regSetProperties.getId() + ", base=" + regSetProperties.getBaseAddress());
		String instanceStr =  mapId;  // base addrmap has no incoming props was  : getInstanceStr(regSetProperties)
		// if root is not to be instanced then inhibit
		//System.out.println("RdlBuilder finishRegMap: ext root isntanced=" + ExtParameters.rdlRootComponentIsInstanced() + ", id=" + regSetProperties.getId() + ", is root inst=" + regSetProperties.isRootInstance());  
		if (!ExtParameters.rdlRootComponentIsInstanced() && regSetProperties.isRootInstance()) instanceStr = "";  
		outputList.add(new OutputLine(--indentLvl, "} " + instanceStr + ";"));   
		outputList.add(new OutputLine(indentLvl, ""));	
	}

    //---------------------------- rdl gen methods ----------------------------------------

    /** build instance string for a regfile or reg
     * 
     * @param properties - register or regfile properties
     * @return instance string
     */
	private String getInstanceStr(AddressableInstanceProperties properties) {
		String prefix = (properties.isRootExternal() && properties.isExternalDecode()) ? "external_decode " : "";
		String instanceStr = prefix + escapedId(properties.getId());
		int repCount = properties.getExtractInstance().getRepCount();   // add replication count
		if (repCount > 1) instanceStr = instanceStr + "[" + repCount + "]";
		instanceStr = instanceStr + " @" + properties.getRelativeBaseAddress();
		RegNumber addressInc = properties.getExtractInstance().getAddressIncrement();  // add addr increment
        if ((repCount > 1) && (addressInc != null)) instanceStr = instanceStr + " += " + addressInc;
		return instanceStr;
	}

	/** build a rdl header for current register set instance */ 
	private void buildRegSetHeader() {
		outputList.add(new OutputLine(indentLvl, "// register set " + regSetProperties.getId()));
		// if root is not to be instanced then add component name
		boolean noComponentInstance = (!ExtParameters.rdlRootComponentIsInstanced() && regSetProperties.isRootInstance()) || regSetIsTypedefContainerInstance();
		String compName = noComponentInstance ? regSetProperties.getId() + " " : ""; 
		outputList.add(new OutputLine(indentLvl, "regfile " + compName + "{"));
		outputList.add(new OutputLine(indentLvl++, ""));
		// add name, description
		buildCommonAssigns(regSetProperties);
		//  display enums defs in this regset
		buildEnumDefs(regSetProperties.getExtractInstance().getRegComp());
	}
   
	/** return true if current regset is intanced by a typedef container used for js translation */
	private boolean regSetIsTypedefContainerInstance() {
		ModComponent parent = regSetProperties.getExtractInstance().getParent();
		if ((parent != null) && ("TYPEDEF_CONTAINER".equals(parent.getId()))) return true;
		return false;
	}

	/** build aa rdl header for current register instance 
	 * @param componentName - if not empty, this name will be used as the component name of this register (otherwise anonymous) */ 
	private void buildRegHeader(String componentName) {
		if (componentName.isEmpty()) {
			outputList.add(new OutputLine(indentLvl, "// register " + regProperties.getId())); 
			outputList.add(new OutputLine(indentLvl, "reg {"));
		}
		else {
			outputList.add(new OutputLine(indentLvl, "// register define " + componentName));
			outputList.add(new OutputLine(indentLvl, "reg " + componentName + " {"));
		}
		outputList.add(new OutputLine(indentLvl++, ""));	
		//System.out.println("RdlBuilder buildRegHeader: id=" + regProperties.getId() + ", name=" + regProperties.getTextName());
		//regProperties.display();
		buildCommonAssigns(regProperties); 
		// show category
		if (regProperties.hasCategory())
	        outputList.add(new OutputLine(indentLvl, "category = \"" + regProperties.getCategory() + "\";")); 
		// set jspec attributes property if specified
		if (ExtParameters.rdlOutputJspecAttributes() && regProperties.getJspecAttributes() != null) {
			// if do not test override is applied, make sure it's included in js_attributes
			String doNotTestStr = "";
			if (regProperties.isDontTest() && !regProperties.getJspecAttributes().contains("DO_NOT_TEST")) {
				//System.out.println("RdlBuilder buildRegHeader: missing dont-test attribute in " + regProperties.getInstancePath());
				doNotTestStr = "|JS_ATTRIB_DO_NOT_TEST";
			}
	        outputList.add(new OutputLine(indentLvl, "js_attributes = \"" + regProperties.getJspecAttributes() + doNotTestStr + "\";")); 
		}
		// add regwidth if not default
		if (regProperties.getRegWidth() != ModRegister.defaultWidth)
			outputList.add(new OutputLine(indentLvl, "regwidth = " + regProperties.getRegWidth() + ";"));
		//  display enums defs in this reg
		buildEnumDefs(regProperties.getExtractInstance().getRegComp());
	}

	/** display name and description assigns
	 * 
	 * @param properties
	 */
	private void buildCommonAssigns(InstanceProperties properties) {
		// set name and description
		String textName = properties.getTextName();
		String textDescription = properties.getTextDescription();
        if (textName != null) outputList.add(new OutputLine(indentLvl, "name = \"" + textName + "\";")); 
        if (textDescription != null) {
        	textDescription = textDescription.replaceAll("\"", "\\\\\\\"");  // TODO
        	outputList.add(new OutputLine(indentLvl, "desc = \"" + textDescription + "\";")); 		
        }
		// basic test attributes
		if (properties.isDontTest())
	        outputList.add(new OutputLine(indentLvl, "donttest;"));  
		else if (properties.isDontCompare())
	        outputList.add(new OutputLine(indentLvl, "dontcompare;"));  
		// add any jspec passthru attributes
		buildJsPassthruAssigns(properties);
	}

	/** build jspec for current register fields */
	private void buildFields() {  
		// traverse field list from high bit to low
		while (fieldList.size() > 0) {
			FieldProperties field = fieldList.remove();  // get next field
			buildField(field); 
		}
	}
	
	/** build enum def rdl for all enum children in a component
	 * 
	 * @param modComp - component whose child enum defs will be generated
	 */
	private void buildEnumDefs(ModComponent modComp) { 
		if (modComp == null) return;
		List<ModEnum> enumList = modComp.getCompEnumList();
		// display child enums
		for (ModEnum modEnum: enumList) {
			buildEnumDef(modEnum);
		}			
	}
	
	/** build enum rdl statements
	 * 
	 * @param enumComp - enum component to be generated
	 */
	private void buildEnumDef(ModEnum enumComp) { 
		if (enumComp == null) return;
		// get enum name/description text
		String enumId = enumComp.getId();
        // gen enum header
		outputList.add(new OutputLine(indentLvl++, "enum " + enumId + " {"));
		// add enum elements
		for (ModEnumElement enumElem : enumComp.getEnumElements()) {
			enumElem.getValue().setNumFormat(RegNumber.NumFormat.Address);
			String enumNameStr = (enumElem.getName() == null) ? "" : " { name = \"" + enumElem.getName() + "\";}";
			outputList.add(new OutputLine(indentLvl, enumElem.getId() + " = " + enumElem.getValue() + enumNameStr + ";"));
		}
		outputList.add(new OutputLine(--indentLvl, "};"));  // finish up enum def
		outputList.add(new OutputLine(indentLvl, ""));	
	}
	
	/** build any user property definitions */
	private void buildPropertyDefs() {
		List<DefinedProperty> usrPropList = DefinedProperties.getUserDefinedProperties();
		if (!usrPropList.isEmpty()) {
			for (DefinedProperty usrProp : usrPropList) {
				outputList.add(new OutputLine(indentLvl, usrProp.getRdlDefineString()));
			}
			outputList.add(new OutputLine(indentLvl, ""));	 // finish up property defs			
		}	
	}

	/** build any jspec passthru assignments by directly pulling from model instance */
	private void buildJsPassthruAssigns(InstanceProperties inst) {
		if (!inst.hasJsPassthruProperties()) return;
		PropertyList pList = inst.getJsPassthruProperties();
		for (String name : pList.getProperties().keySet()) {
			String value = (pList.getProperty(name) == null)? "" : pList.getProperty(name);
			DefinedProperty prop = DefinedProperties.getProperty(name);
			if (prop!=null) outputList.add(new OutputLine(indentLvl, name + " = " + prop.getRdlValue(value) + ";")); 		
		}
	}

	/** build field stmts  */
	private void buildField(FieldProperties field) {   
		outputList.add(new OutputLine(indentLvl++, "field {"));
		// add name, description
		buildCommonAssigns(field); 
		buildFieldParams(field);  
		// create field instance line
		Integer lowIdx = field.getLowIndex();
		Integer highIdx = lowIdx + field.getFieldWidth() - 1;
		//System.out.println("RdlBuilder buildField: ----- null field index");
		String instanceStr = escapedId(field.getPrefixedId()) + " [" + highIdx + ":" + lowIdx + "]";
		// if a reset is defined set it
		if (field.getReset() != null) {
			RegNumber resetVal = new RegNumber(field.getReset());
			if (resetVal.isDefined()) {
				instanceStr += " =" + resetVal.toFormat(RegNumber.NumBase.Hex, RegNumber.NumFormat.Address);
			}
			else MsgUtils.warnMessage("Unable to resolve reset value for field " + field.getInstancePath() + ", value=" + field.getReset());
		}
		outputList.add(new OutputLine(--indentLvl, "} " + instanceStr + ";"));
		outputList.add(new OutputLine(indentLvl, ""));	
	}

	/** create field parameter stmts */ 
	private void buildFieldParams(FieldProperties field) {
		// set the read/write status
		String accessModeStr = null;
		if (field.isSwReadable()) accessModeStr = "r";
		if (field.isSwWriteable()) accessModeStr += "w";
		if (accessModeStr == null) accessModeStr = "na";
		accessModeStr = "sw=" + accessModeStr + "; ";
		// set read/write clear/set options
		if (field.isWoclr()) accessModeStr += "woclr;";   
		else if (field.isWoset()) accessModeStr += "woset;";    
		if (field.isRclr()) accessModeStr += "rclr;"; 
		outputList.add(new OutputLine(indentLvl, accessModeStr));
		// set subcategory if specified
		if (field.hasSubCategory())
	        outputList.add(new OutputLine(indentLvl, "sub_category = \"" + field.getSubCategory() + "\";")); 
		// if field has enum encoding provide a ref
		if (field.getEncoding() != null)
			outputList.add(new OutputLine(indentLvl, "encode = " + field.getEncoding().getId() + ";"));
	}

    /** create set of id keywords that need to be escaped */
    private static String escapedId(String id) {
    	if (escapedIds.contains(id)) return "\\" + id;
    	return id;
	}

    /** create set of id keywords that need to be escaped */
    private static void setEscapedIds() {
    	escapedIds.add("counter");
    	escapedIds.add("enable");
    	escapedIds.add("intr");
    	escapedIds.add("level");
    	escapedIds.add("mask");
    	escapedIds.add("number");
    	escapedIds.add("overflow");
    	escapedIds.add("reset");
    	escapedIds.add("type");
    	escapedIds.add("underflow");
    	escapedIds.add("threshold");
	}
    
    //---------------------------- output write methods ----------------------------------------

	@Override
	public void write(BufferedWriter bw) {
		bufferedWriter = bw;

		// write the output for each output group
		for (OutputLine jsLine: outputList) {
			//writeStmt(indentLevel, "/* registers in set=" + setName + " */");  		
			writeStmt(jsLine.getIndent(), jsLine.getLine());  
		}
	}

}
