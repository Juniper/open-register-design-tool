/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.othertypes;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.HashSet;

import ordt.extract.DefinedProperties;
import ordt.extract.DefinedProperty;
import ordt.output.common.MsgUtils;
import ordt.extract.PropertyList;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.extract.model.ModEnumElement;
import ordt.extract.model.ModRegister;
import ordt.output.FieldProperties;
import ordt.output.InstanceProperties;
import ordt.output.OutputBuilder;
import ordt.output.RhsReference;
import ordt.output.common.OutputLine;
import ordt.parameters.ExtParameters;

public class JspecBuilder extends OutputBuilder {
	
	private List<OutputLine> outputList = new ArrayList<OutputLine>();
	private int indentLvl = 0;
	private static HashSet<String> reservedWords = getReservedWords();  // reserved jspec words
	
    // define inner class for storing fieldset start-end index info
	private int currentBaseOffset = 0;
	private int currentNestLevel = 0;
	
    private class FieldSetInfo {
		private String id, name;
		private int lowIndex = 0;
        private int highIndex = 0;
        private int nestLevel = 0;
        private FieldSetInfo(String id, String name, int lowIndex, int highIndex, int nestLevel) {
			this.id = id;
			this.name = name;
			this.lowIndex = lowIndex;
			this.highIndex = highIndex;
			this.nestLevel = nestLevel;
		}
		public int getHighIndex() {
			return highIndex;
		}
		public int getLowIndex() {
			return lowIndex;
		}
		public int getStartOrder() {
			return (100*highIndex) - nestLevel; // lower nest level starts first at each index
		}
		public int getEndOrder() {
			return (100*lowIndex) + nestLevel;  // higher nest level ends first at each index
		}
		public String getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		// create a header for this fieldset
		public void genHeader() {
			String textName = getName();
			if ((textName == null) || textName.isEmpty()) textName = getId() + " field_set";
			//System.out.println("JSpecBuilder FieldSetInfo genHeader: fieldset " + getId() + ", lowIndex=" + getLowIndex() + ", highIndex=" + getHighIndex());
			outputList.add(new OutputLine(indentLvl, ""));	
			outputList.add(new OutputLine(indentLvl++, "field_set " + getId() + " \"" + textName + "\" {"));
		}
		// create end of this fieldset
		public void genEnd() {
			//System.out.println("JSpecBuilder FieldSetInfo genEnd: fieldset " + getId() + ", lowIndex=" + getLowIndex() + ", highIndex=" + getHighIndex());
			outputList.add(new OutputLine(--indentLvl, "};"));
			outputList.add(new OutputLine(indentLvl, ""));	
		}    	
    }
    // process fieldset start locations ordered by descending high absolute index in register
    private PriorityQueue<FieldSetInfo> fieldSetStartList = new PriorityQueue<FieldSetInfo>(
    	8, new Comparator<FieldSetInfo>(){
        public int compare(FieldSetInfo a, FieldSetInfo b) {
            if (a.getStartOrder() < b.getStartOrder()) return 1;
            if (a.getStartOrder() == b.getStartOrder()) return 0;
            return -1;
        }
    });
    // process fieldset end locations ordered by descending low absolute index in register
    private PriorityQueue<FieldSetInfo> fieldSetEndList = new PriorityQueue<FieldSetInfo>(
    	8, new Comparator<FieldSetInfo>(){
        public int compare(FieldSetInfo a, FieldSetInfo b) {
            if (a.getEndOrder() < b.getEndOrder()) return 1;
            if (a.getEndOrder() == b.getEndOrder()) return 0;
            return -1;
        }
    });
	
    //---------------------------- constructor ----------------------------------

    public JspecBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals since jspec wont be used for code gen
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    // override fieldList comparator to generate descending ordered list by idx
	    fieldList = new PriorityQueue<FieldProperties>(
	    	128, new Comparator<FieldProperties>(){
            public int compare(FieldProperties a, FieldProperties b) {
                if (a.getLowIndex() < b.getLowIndex()) return 1;  // descending field list
                if (a.getLowIndex() == b.getLowIndex()) return 0;
                return -1;
            }
        }); 
		RhsReference.setInstancePropertyStack(instancePropertyStack);  // update pointer to the instance stack for rhs reference evaluation
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
    }

    /** load jspec reserved words */
	private static HashSet<String> getReservedWords() {
		HashSet<String> reservedWords = new HashSet<String>();
		reservedWords.add("nop");
		reservedWords.add("field_set");
		reservedWords.add("integer");
		reservedWords.add("register");
		reservedWords.add("register_set");
		return reservedWords;
	}

    //---------------------------- OutputBuilder methods to load jspec structures ----------------------------------------

	@Override
	public void addField() {
		// if name is a reserved keyword then exit
		if (reservedWords.contains(fieldProperties.getPrefixedId()))
			MsgUtils.errorMessage("field name " + fieldProperties.getPrefixedId() + " is a jspec reserved word");
	}

	@Override
	public void addAliasField() {
		// handle same as non-aliased field
		addField();
	}

	@Override
	public void addFieldSet() {
		// if name is a reserved keyword then exit
		if (reservedWords.contains(fieldSetProperties.getId()))
			MsgUtils.errorMessage("field_set name " + fieldSetProperties.getId() + " is a jspec reserved word");
		// compute absolute offsets and store fieldset info
		if (!ExtParameters.jspecKeepFsetHierarchy() || (fieldSetProperties.getFieldSetWidth()==0)) return;
		currentBaseOffset += fieldSetProperties.getOffset();
		currentNestLevel += 1;
		//System.out.println("JSpecBuilder addFieldset: fieldset " + fieldSetProperties.getId() + ", offset=" + fieldSetProperties.getOffset() + ", width=" + fieldSetProperties.getFieldSetWidth() + ", currentBaseOffset=" + currentBaseOffset+ ", currentNestLevel=" + currentNestLevel);
        FieldSetInfo fsInfo = new FieldSetInfo(fieldSetProperties.getId(), fieldSetProperties.getTextName(), currentBaseOffset, 
        		currentBaseOffset + fieldSetProperties.getFieldSetWidth() - 1, currentNestLevel);
		fieldSetStartList.add(fsInfo);
		fieldSetEndList.add(fsInfo);
	}

	@Override
	public void finishFieldSet() {
		// compute absolute offsets
		if (!ExtParameters.jspecKeepFsetHierarchy() || (fieldSetProperties.getFieldSetWidth()==0)) return;
		currentBaseOffset -= fieldSetProperties.getOffset();
		currentNestLevel -= 1;
	}

	@Override
	public void addRegister() {
		// check for misaligned base address here since jspec doesn't like
		if (regProperties.isReplicated()) {
			// compute total size of the register array
			RegNumber regArraySize = new RegNumber(regProperties.getRegByteWidth());
			regArraySize.setNumFormat(RegNumber.NumFormat.Address);
			RegNumber addressIncrement = regProperties.getExtractInstance().getAddressIncrement();
			if (addressIncrement != null) regArraySize = new RegNumber(addressIncrement);
			regArraySize.multiply(regProperties.getRepCount());
			
			// check for misaligned base address here since jspec doesn't like
			if (!regProperties.getBaseAddress().isModulus(regArraySize.getNextHighestPowerOf2())) 
				   MsgUtils.errorMessage("register array " + regProperties.getInstancePath() + 
						   " base address (" + regProperties.getBaseAddress() +") is not aligned on " + regArraySize.getNextHighestPowerOf2() +" boundary for jspec"); 
			//if (!regProperties.getRelativeBaseAddress().isModulus(regStride))  // TODO - is this check needed?? 
			//   Jrdl.errorMessage("replicated register " + regProperties.getInstancePath() + 
			//		   " jspec relative base address (" + regProperties.getRelativeBaseAddress() +") is not aligned with total size (" + regStride +")"); 
		}
		// initialize info for fieldset output
		currentBaseOffset = 0;
		fieldSetStartList.clear();
		fieldSetEndList.clear();
	}

	@Override
	public void finishRegister() {
		// only add the register if it is sw accessible
		if (regProperties.isSwWriteable() || regProperties.isSwReadable()) {
			// build the register header
			buildRegHeader();
			// add field info
			if (ExtParameters.jspecKeepFsetHierarchy()) buildHierFields();
			else buildFields();
			// close out the register definition
			outputList.add(new OutputLine(--indentLvl, "};"));
			outputList.add(new OutputLine(indentLvl, ""));	
			// if an alias register then add jspec test_group - only if a diag reg since js really only uses this
			if (regProperties.isAlias() && regProperties.getId().contains("diag")) {
				outputList.add(new OutputLine(indentLvl, "test_group { " + regProperties.getAliasedId() + ", " + regProperties.getId() + "; };"));
				outputList.add(new OutputLine(indentLvl, ""));					
			}
		}
	}

	@Override
	public void addRegSet() {
		// add any user property definitions and get list of js passthru properties
		if (regSetProperties.isRootInstance()){
			if (ExtParameters.jspecAddUserParamDefines()) buildUserParameterDefs();
		}
		// skip this regset if it's empty
		if (regSetProperties.getExtractInstance().getRegComp().hasChildInstances()) {
			// build the register header
			buildRegSetHeader();  
			//System.out.println("Register set: " + regSetProperties.getInstancePath() + ", addr=" + regSetProperties.getBaseAddress());
		}
	}

	@Override
	public void finishRegSet() {  
		// skip this regset if it's empty
		if (regSetProperties.getExtractInstance().getRegComp().hasChildInstances()) {
			// all jspec register sets must specify a size
			RegNumber regSetSize = getRegSetAddressStride(); // allowPrecomputedSize if replicated

			// check for empty register set
			if (!regSetSize.isGreaterThan(new RegNumber(0))) 
				   MsgUtils.warnMessage("register set " + regSetProperties.getInstancePath() + 
						   " has size 0B"); 
			else {
				// check for misaligned base address here since jspec doesn't like
				if (!regSetProperties.getBaseAddress().isModulus(regSetSize.getNextHighestPowerOf2())) 
					MsgUtils.errorMessage("register set " + regSetProperties.getInstancePath() + 
							" base address (" + regSetProperties.getBaseAddress() +") is not aligned on " + regSetSize.getNextHighestPowerOf2() +" boundary for jspec"); 
				// check for misaligned base addr 
				if (regSetProperties.isReplicated() && !regSetProperties.getRelativeBaseAddress().isModulus(regSetSize)) 
					MsgUtils.errorMessage("replicated register set " + regSetProperties.getInstancePath() + 
							" jspec relative base address (" + regSetProperties.getRelativeBaseAddress() +") is not aligned with increment value (" + regSetSize +")"); 
			}
			
			outputList.add(new OutputLine(indentLvl, "register_set_size = " + regSetSize + ";"));  
			// close out the register set definition
			outputList.add(new OutputLine(--indentLvl, "};"));
			outputList.add(new OutputLine(indentLvl, ""));	
		}
	}

	/** process root address map */
	@Override
	public void addRegMap() {
		// build any user-defined parameter defines and get list of js passthru properties
		if (ExtParameters.jspecAddUserParamDefines()) buildUserParameterDefs();
		
		// create text name and description if null
		String mapId = getAddressMapName();
		
		String textName = regSetProperties.getTextName();
		String textDescription = regSetProperties.getTextDescription();
		if (textName == null) textName = "Registers for " + mapId;
		
		// if root not to be instanced, make this a typedef
		//System.out.println("JSpecBuilder addRegMap: rootInst=" + ExtParameters.jspecRootRegsetIsInstanced());
		String tdefStr = rootHasTypedef()? "typedef " : "";
		outputList.add(new OutputLine(indentLvl, tdefStr + "register_set " + getRootDefName() + " \"" + textName + "\" {"));
		outputList.add(new OutputLine(indentLvl++, ""));	
		// set js pass-thru info
		buildJsPassthruAssigns(regSetProperties);
		// start address is 0
		outputList.add(new OutputLine(indentLvl, "address = 0x0;"));
		// set default reg width
	    outputList.add(new OutputLine(indentLvl, "register_width = " + ModRegister.defaultWidth + ";"));
		// add description for this reg set
		if (textDescription != null) { 
		   outputList.add(new OutputLine(indentLvl, "description = \"{"));
		   outputList.add(new OutputLine(++indentLvl, textDescription));
		   outputList.add(new OutputLine(--indentLvl, "}\";"));
	    }
	}

	//ExtParameters.jspecRootRegsetIsInstanced()
	/** finish root address map  */
	@Override
	public  void finishRegMap() {	
		// compute total size of this address map/root register set
	    outputList.add(new OutputLine(indentLvl, "register_set_size = " + getNextAddress() + ";"));
		// close out the register set definition
		outputList.add(new OutputLine(--indentLvl, "};"));
		outputList.add(new OutputLine(indentLvl, ""));	
		// if a typedef is defined and instantiation is specified, add the instance
		if (rootHasTypedef() && ExtParameters.jspecRootRegsetIsInstanced()) {
			String textName = regSetProperties.getTextName();
			if (textName == null) textName = " ";
			outputList.add(new OutputLine(indentLvl++, getRootDefName() + " " + getRootInstanceName() + " \"" + textName + "\" param {"));
			outputList.add(new OutputLine(indentLvl, "address = 0x0;"));
			if ((ExtParameters.getJspecRootInstanceRepeat()!=null) && (ExtParameters.getJspecRootInstanceRepeat()>1)) 
				outputList.add(new OutputLine(indentLvl, "repeat = " + ExtParameters.getJspecRootInstanceRepeat() + ";"));
			else if ((regSetProperties.getJspecInstanceRepeat()!=null) && (regSetProperties.getJspecInstanceRepeat()>1)) //  FIXME - remove
				outputList.add(new OutputLine(indentLvl, "repeat = " + regSetProperties.getJspecInstanceRepeat() + ";"));
			outputList.add(new OutputLine(--indentLvl, "};"));
			outputList.add(new OutputLine(indentLvl, ""));				
		}
	}

    //---------------------------- jspec gen methods ----------------------------------------
	
	/** return name used in the root definition - could be typedef or anonymous instance */
	private String getRootDefName() {
		//System.out.println("JspecBuilder getRootDefName: typedef  isNull=" + (regSetProperties.getJspecTypedefName()==null) + ", isEmpty=" + ((regSetProperties.getJspecTypedefName()==null)? "" : regSetProperties.getJspecTypedefName().isEmpty()));
		//System.out.println("JspecBuilder getRootDefName: instance isNull=" + (regSetProperties.getJspecInstanceName()==null) + ", isEmpty=" + ((regSetProperties.getJspecInstanceName()==null)? "" : regSetProperties.getJspecInstanceName().isEmpty()));
		if (ExtParameters.getJspecRootTypedefName()!=null) return ExtParameters.getJspecRootTypedefName();  // use typedef name override
		if (regSetProperties.getJspecTypedefName()!=null) return regSetProperties.getJspecTypedefName();  // use typedef name override  FIXME - remove
		if (!ExtParameters.jspecRootRegsetIsInstanced()) return getAddressMapName();  // else use addrmap name as typedef
		if (ExtParameters.getJspecRootInstanceName()!=null) return ExtParameters.getJspecRootInstanceName();  // use instance name override
		if (regSetProperties.getJspecInstanceName()!=null) return regSetProperties.getJspecInstanceName();  // use instance name override  FIXME - remove
		return getAddressMapName();   // else anonymous instance
	}

	/** return true if root regset hill have a defined typedef */
    private boolean rootHasTypedef() {
		//System.out.println("JspecBuilder rootHasTypedef: typedef  isNull=" + (regSetProperties.getJspecTypedefName()==null) + ", isEmpty=" + ((regSetProperties.getJspecTypedefName()==null)? "" : regSetProperties.getJspecTypedefName().isEmpty()));
		//System.out.println("JspecBuilder rootHasTypedef: jspecRootRegsetIsInstanced=" + ExtParameters.jspecRootRegsetIsInstanced());
		return (ExtParameters.getJspecRootTypedefName()!=null) || (regSetProperties.getJspecTypedefName()!=null) || !ExtParameters.jspecRootRegsetIsInstanced();  // FIXME - remove property
	}

    /** return instance name used in (non-anonymous) instance */
	private String getRootInstanceName() {
		if (ExtParameters.getJspecRootInstanceName()!=null) return ExtParameters.getJspecRootInstanceName();  // use instance name override
		return (regSetProperties.getJspecInstanceName()!=null)? regSetProperties.getJspecInstanceName() : getAddressMapName(); // FIXME - remove property
	}

	/** build a jspec header for current register set instance */ 
	private void buildRegSetHeader() {
		// create text name and description if null
		String id = regSetProperties.getId();
		String textName = regSetProperties.getTextName();
		String textDescription = regSetProperties.getTextDescription();
		if (textName == null) textName = id + " register_set";
		
		outputList.add(new OutputLine(indentLvl, "register_set " + id + " \"" + textName + "\" {"));
		// set address using reltive offset from parent base
		outputList.add(new OutputLine(++indentLvl, "address = " + regSetProperties.getRelativeBaseAddress() + ";"));
		// set js pass-thru info
		buildJsPassthruAssigns(regSetProperties);
		// get repcount for this reg set
		int repCount = regSetProperties.getExtractInstance().getRepCount();
		if (repCount > 1) 
			outputList.add(new OutputLine(indentLvl, "repeat = " + repCount + ";"));
		// add description for this reg set
		if (textDescription != null) { 
		   outputList.add(new OutputLine(indentLvl, "description = \"{"));
		   outputList.add(new OutputLine(++indentLvl, textDescription));
		   outputList.add(new OutputLine(--indentLvl, "}\";"));
	    }
		outputList.add(new OutputLine(indentLvl, ""));	
	}
	
	/** build any user parameter definitions */
	private void buildUserParameterDefs() {
		List<DefinedProperty> usrPropList = DefinedProperties.getJsUserDefinedProperties();
		if (!usrPropList.isEmpty()) {
			for (DefinedProperty usrProp : usrPropList) {
				outputList.add(new OutputLine(indentLvl, usrProp.getJsDefineString()));
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
			if (prop!=null) outputList.add(new OutputLine(indentLvl, name.substring(3) + " = " + prop.getJsValue(value) + ";")); 	// strip js_ prefix	
		}
	}

	/** build a jspec header for current register instance */ 
	private void buildRegHeader() {
		// create text name and description if null
		String id = regProperties.getId();
		String textName = regProperties.getTextName();
		String textDescription = regProperties.getTextDescription();
		if (textName == null) textName = id + " register";
		//else System.out.println("JSpecBuilder buildRegHeader: name=" + textName);
		
		outputList.add(new OutputLine(indentLvl, "register " + id + " \"" + textName + "\" {"));
		outputList.add(new OutputLine(++indentLvl, "address = " + regProperties.getRelativeBaseAddress() + ";"));
		
		// get the write status  // TODO - also allow READ_TO_CLEAR,WRITE_ONE_TO_CLEAR?
		String accessMode = "READ_ONLY";
		if (regProperties.isSwWriteable()) {
			if (regProperties.isSwReadable()) accessMode = "READ_WRITE"; 
			else accessMode = "WRITE_ONLY";
		}
		if (!accessMode.equals("READ_WRITE")) outputList.add(new OutputLine(indentLvl, "access_mode = " + accessMode + ";"));
		
		// set category/subcategory if specified
		if (regProperties.hasCategory())
	        outputList.add(new OutputLine(indentLvl, "category = { " + regProperties.getCategory() + " };")); 
		
		// set js attributes if specified   
		String attrStr = "";
		// if pass-thru attributes specified use these
		if (regProperties.getJspecAttributes() != null) attrStr = regProperties.getJspecAttributes();
		// otherwise extract attributes from rdl properties
		else {
			if (regProperties.isDontTest()) attrStr += " JS_ATTRIB_DO_NOT_TEST";  
			if (regProperties.isDontCompare()) attrStr += " JS_ATTRIB_TEST_ACCESS_ONLY";
			if (regProperties.hasInterruptFields() && (regProperties.getId().equals("status") || regProperties.getId().equals("diag"))) attrStr += " JS_ATTRIB_INT_STATUS";
		}
		if (!attrStr.isEmpty()) {
			attrStr = attrStr.trim().replace(" ", "|"); // create OR'd attr string
		    outputList.add(new OutputLine(indentLvl, "attributes = " + attrStr + ";"));
		}
		
		// set js pass-thru info
		buildJsPassthruAssigns(regProperties);

		// set reg width
		if (regProperties.getRegWidth() != ModRegister.defaultWidth)
			outputList.add(new OutputLine(indentLvl, "register_width = " + regProperties.getRegWidth() + ";"));
		// get repcount for this reg
		int repCount = regProperties.getExtractInstance().getRepCount();
		if (repCount > 1) 
			outputList.add(new OutputLine(indentLvl, "repeat = " + repCount + ";"));  // TODO - stride missing here??
		// add description for this reg
		if (textDescription != null) { 
		   outputList.add(new OutputLine(indentLvl, "description = \"{"));
		   outputList.add(new OutputLine(++indentLvl, textDescription));
		   outputList.add(new OutputLine(--indentLvl, "}\";"));
		}
		/* if a reset is defined display it
		if (regProperties.getReset() != null) {
			RegNumber resetVal = new RegNumber(regProperties.getReset());
			if (resetVal.isDefined()) {
				resetVal.setNumFormat(RegNumber.NumFormat.Address);
				outputList.add(new OutputLine(indentLvl, "reset = " + resetVal + ";"));
			}
		}*/
	}

	/** build jspec for current register fields */
	private void buildFields() {
		// traverse field list from high bit to low
		int currentBit = regProperties.getRegWidth();
		while (fieldList.size() > 0) {
			// get next field
			FieldProperties field = fieldList.remove();
			int fieldIdx = field.getLowIndex();
			int fieldWidth = field.getFieldWidth();
			// compute nop bits
			int nopBits = currentBit - (fieldIdx + fieldWidth);
			if (nopBits > 0)
				outputList.add(new OutputLine(indentLvl, "nop[" + nopBits + "];"));
            // display field as enum or int
			if (field.getEncoding() != null) buildEnumField(field, true); // use id prefix  
			else buildIntField(field, true);  // use id prefix 
			currentBit = fieldIdx;
		}
		// if still some unused bits add a nop
		if (currentBit > 0) {
			outputList.add(new OutputLine(indentLvl, "nop[" + currentBit + "];"));
		}	
	}

	/** build jspec for current register fields and fieldsets */  // FIXME
	private void buildHierFields() {
		// traverse field list from high bit to low
		int currentBit = regProperties.getRegWidth();
		while (fieldList.size() > 0) {
			// get next field
			FieldProperties field = fieldList.remove();
			int fieldIdx = field.getLowIndex();
			int fieldWidth = field.getFieldWidth();
			int fieldHighIdx = fieldIdx + fieldWidth - 1;
			// step thru both fset start and lists from top of register - at each index, close out then start new fset, return new currentBit 
			currentBit = buildFieldSets(fieldHighIdx, currentBit);  // generate any fieldset start/end structures down to specified fieldHighIdx
			// compute nop bits
			int nopBits = currentBit - (fieldHighIdx + 1);
			if (nopBits > 0)
				outputList.add(new OutputLine(indentLvl, "nop[" + nopBits + "];"));
            // display field as enum or int
			if (field.getEncoding() != null) buildEnumField(field, false); // no field id prefix  
			else buildIntField(field, false);  // no field id prefix 
			currentBit = fieldIdx;
		}
		// close out any open fieldsets
		currentBit = buildFieldSets(-1, currentBit);  // generate any fieldset start/end structures down to specified fieldHighIdx
		// if still some unused bits add a nop
		if (currentBit > 0) {
			outputList.add(new OutputLine(indentLvl, "nop[" + currentBit + "];"));
		}	
	}
	
	/** generate any fieldset start/end structure from specified currentBit down to new a field
	 * 
	 * @param fieldHighIdx - high index of new field that will be added
	 * @param currentBit - lowest index in reg above new field with a defined structure
	 * @return updated currentBit (lowest index in reg with a defined structure)
	 */
	private int buildFieldSets(int fieldHighIdx, int currentBit) {
		int newCurrentBit = currentBit;
		// continue processing down to new field index
		boolean done = false;
		while (!done) {
			// snoop current indices in the start and end queues
			boolean hasStartInfo = fieldSetStartList.peek() != null;
			boolean hasEndInfo = fieldSetEndList.peek() != null;
			if (!(hasStartInfo || hasEndInfo)) return newCurrentBit;  // no fieldset info so exit
			int nextStartIndex = hasStartInfo? fieldSetStartList.peek().getHighIndex() : -99;
			int nextEndIndex = hasEndInfo? fieldSetEndList.peek().getLowIndex() : -99;
			if ((nextStartIndex < fieldHighIdx) && (nextEndIndex <= fieldHighIdx)) done = true;  // no fieldset info before new field so exit
			// have something to process - process fs ends if next (at a higher index)
			else {
				if (nextStartIndex < nextEndIndex)
					newCurrentBit = buildFieldSetEnds(nextEndIndex, newCurrentBit);  // build all fs nops/ends at next idx and pop from queue
				else if (nextStartIndex > nextEndIndex)
					newCurrentBit = buildFieldSetStarts(nextStartIndex, newCurrentBit);  // build all fs nops/starts at next idx and pop from queue
				else {
					if (nextEndIndex > fieldHighIdx) newCurrentBit = buildFieldSetEnds(nextEndIndex, newCurrentBit);  // build all fs nops/ends at next idx and pop from queue
					newCurrentBit = buildFieldSetStarts(nextStartIndex, newCurrentBit);  // build all fs nops/starts at next idx and pop from queue
				}
			}
		}
		return newCurrentBit;
	}
	
	/** build all fs nops/starts at next idx and pop from queue
	 * 
	 * @param startIndex - create fieldset headers starting at this index 
	 * @param currentBit
	 * @return
	 */
	private int buildFieldSetStarts(int startIndex, int currentBit) {
		// add a nop if a gap is detected
		int nopBits = currentBit - (startIndex + 1);
		if (nopBits > 0)
			outputList.add(new OutputLine(indentLvl, "nop[" + nopBits + "];"));
		// process all fieldsets at this index
		boolean done = false;
		while (!done) {
			if ((fieldSetStartList.peek() == null) || (fieldSetStartList.peek().getHighIndex() != startIndex)) done = true;  // no fieldset info before new field so exit
			else {
				FieldSetInfo fsInfo = fieldSetStartList.remove();
				fsInfo.genHeader();
			}		
		}
		return startIndex + 1;
	}

	/** build all fs nops/ends at next idx and pop from queue 
	 * 
	 * @param endIndex - create fieldset headers ending at this index
	 * @param currentBit
	 * @return
	 */
	private int buildFieldSetEnds(int endIndex, int currentBit) {
		// add a nop if a gap is detected
		int nopBits = currentBit - endIndex;
		if (nopBits > 0)
			outputList.add(new OutputLine(indentLvl, "nop[" + nopBits + "];"));
		// process all fieldsets at this index
		boolean done = false;
		while (!done) {
			if ((fieldSetEndList.peek() == null) || (fieldSetEndList.peek().getLowIndex() != endIndex)) done = true;  // no fieldset info before new field so exit
			else {
				FieldSetInfo fsInfo = fieldSetEndList.remove();
				fsInfo.genEnd();
			}		
		}
		return endIndex;
	}

	/** build jspec enum field  */
	private void buildEnumField(FieldProperties field, boolean useIdPrefix) {  
		// get name/description text
		String id = useIdPrefix? field.getPrefixedId() : field.getId();
		String textName = field.getTextName();
		if (textName == null) textName = id + " field";
        // gen field header
		outputList.add(new OutputLine(indentLvl++, "enum " + id + "[" + field.getFieldWidth()  + "] \"" + textName + "\" {"));
		// check width
		Integer encodeWidth = field.getEncoding().getWidth();
		//System.out.println("-- buildEnumField: encoding id=" + field.getEncoding().getId() + ", enumElems=" + field.getEncoding().getEnumElements().size());
		if ((encodeWidth != null && encodeWidth != field.getFieldWidth())) 
			MsgUtils.errorMessage("Encoding width ("+ encodeWidth + ") does not match field width (" + field.getFieldWidth() + ") in " + field.getInstancePath());
		else {
			for (ModEnumElement enumElem : field.getEncoding().getEnumElements()) {
				enumElem.getValue().setNumFormat(RegNumber.NumFormat.Address);
				String enumName = (enumElem.getName() == null) ? "encode_" + enumElem.getValue() : enumElem.getName();
				outputList.add(new OutputLine(indentLvl, enumElem.getId() + " = " + enumElem.getValue() + " \"" + enumName + "\";"));  // TODO emum really should use prefixedId()
			}
		}
		indentLvl--;
		outputList.add(new OutputLine(indentLvl++, "} param {"));
		buildFieldParams(field);
		outputList.add(new OutputLine(--indentLvl, "};"));  // finish up field
	}

	/** build jspec int field  */
	private void buildIntField(FieldProperties field, boolean useIdPrefix) {   
		// get name/description text
		String id = useIdPrefix? field.getPrefixedId() : field.getId();
		String textName = field.getTextName();
		if (textName == null) textName = id + " field";
        // gen field header
		outputList.add(new OutputLine(indentLvl++, "integer " + id + "[" + field.getFieldWidth()  + "] \"" + textName + "\" param {"));
		buildFieldParams(field);
		outputList.add(new OutputLine(--indentLvl, "};"));
	}

	/** create field parameter jspac stmts */ 
	private void buildFieldParams(FieldProperties field) {
		String textDescription = field.getTextDescription();
		// add description for this reg
		if (textDescription != null) {
			outputList.add(new OutputLine(indentLvl, "description = \"{"));
			outputList.add(new OutputLine(++indentLvl, textDescription));
			outputList.add(new OutputLine(--indentLvl, "}\";"));
		}
		// get the write status
		String accessMode = "READ_ONLY";
		if (field.isWoclr()) accessMode = "WRITE_ONE_TO_CLEAR";   
		else if (field.isWoset()) accessMode = "WRITE_ONE_TO_SET";   
		else if (field.isRclr()) accessMode = "READ_TO_CLEAR";
		else if (field.isSwWriteable()) {
			if (field.isSwReadable()) accessMode = "READ_WRITE"; 
			else accessMode = "WRITE_ONLY";
		}
		outputList.add(new OutputLine(indentLvl, "access_mode = " + accessMode + ";"));
		
		// set category/subcategory if specified
		if (field.hasSubCategory())
	        outputList.add(new OutputLine(indentLvl, "sub_category = { " + field.getSubCategory().toString() + " };"));
		
		// if a reset is defined display it unless donttest_reset is set
		if ((field.getReset() != null) && field.getReset().isDefined() && !field.isDontCompare()) {
			outputList.add(new OutputLine(indentLvl, "reset = " + field.getReset().toFormat(NumBase.Hex, NumFormat.Address) + ";"));
		}
		else {
			//Jrdl.warnMessage("reset value not defined for field " + field.getInstancePath());
			outputList.add(new OutputLine(indentLvl, "reset = unknown;"));
		}
		
		// set js pass-thru info
		buildJsPassthruAssigns(field);
	}

	/** hack output string to remove embedded sl_comments from description */  
	@SuppressWarnings("unused")
	private static String clobberCommentss(String textDescription) {
		if (textDescription == null) return null;
        String newTextDescription = textDescription.replaceAll("\\n#[^\\n]*", "");
        /*if (!newTextDescription.equals(textDescription)) {
        	System.out.println("Changing str=" + textDescription);
        	System.out.println("      To str=" + newTextDescription);
        }*/
		return newTextDescription;
	}

    //---------------------------- output write methods ----------------------------------------

	@Override
	public void write(BufferedWriter bw) {
		bufferedWriter = bw;
		
		// add any includes specified
		List<String> includeFiles = ExtParameters.getJspecIncludeFiles();
		for (String incFile : includeFiles) {
			writeStmt(0, "#include \"" + incFile + "\"");
			writeStmt(0, "");
		}
		// write the output for each output group
		for (OutputLine jsLine: outputList) {
			//writeStmt(indentLevel, "/* registers in set=" + setName + " */");  		
			writeStmt(jsLine.getIndent(), jsLine.getLine());  
		}
	}

}
