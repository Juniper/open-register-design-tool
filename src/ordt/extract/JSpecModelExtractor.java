/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import ordt.extract.model.ModAddressableInstance;
import ordt.extract.model.ModBaseComponent;
import ordt.extract.model.ModComponent;
import ordt.extract.model.ModEnum;
import ordt.extract.model.ModEnumElement;
import ordt.extract.model.ModFieldSet;
import ordt.extract.model.ModIndexedInstance;
import ordt.extract.model.ModInstance;
import ordt.extract.model.ModRegister;
import ordt.extract.model.ModRootComponent;
import ordt.extract.model.ModComponent.CompType;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;
import ordt.parse.jspec.JSpecBaseListener;
import ordt.parse.jspec.JSpecLexer;
import ordt.parse.jspec.JSpecParser;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/** extended JSpecBaseListener class (antlr) that extracts component/instance model from parse tree */
public class JSpecModelExtractor extends JSpecBaseListener implements RegModelIntf{

	private ModRootComponent root = new ModRootComponent();     // the root element of the model
	private HashSet<Integer> activeRules = new HashSet<Integer>();  // hash of active parse rules   
	
	private Stack<ModComponent> activeCompDefs = new Stack<ModComponent>();   // stack of active component definitions 
	private Stack<Boolean> typeDefActiveStates = new Stack<Boolean>();   // stack of typedef active state corresponding to component stack
	private boolean typeDefFound = false;  // indication that type_definition rule is active when current comp definition occurs
	
	private Stack<ModInstance> activeInstances = new Stack<ModInstance>();   // stack of active instance defs
	private ModEnum activeEnumDef;   // active enum definition
	private String jspecInputFile;                 // input jspec file name used for model extract 
		
	//private static int anonCompId = 0;   // id for anonymous components
	private static int numExpessionDepth = 0;  // nest level of num_expressions
	private HashMap<String, RegNumber> numConstants = new HashMap<String, RegNumber>();  // defined numeric constants
	private RegNumber lastResolvedNum;  // value of last resolved integer num_expression
	private Integer lastResolvedArrayIndex;  // value of last resolved integer array index
	boolean inhibitNextResolveCheck = false;
	
	private Integer registerWidth;  // TODO- needed? now that aligned-size info in model? 
	private Stack<Integer> fieldOffsets = new Stack<Integer>(); // stack of offsets used to calculate fieldset widths
	private static int anonCompId = 0;
	private static HashSet<String> ignoredParameters = getIgnoredParameters();  // list of ignored jspec params
	
	private Integer accumulated_constant_width;
	private String activeConstantId;
	
	/** create data model from rdl file 
	 * @param rdlFile to be parsed
	 * @param moduleName to be used as default name for addrmap instances
	 * */
	public JSpecModelExtractor (String rdlFile) {
        this.jspecInputFile = rdlFile;  // save rdl file

        try {
        	InputStream is = System.in;
        	if ( rdlFile!=null ) is = new FileInputStream(jspecInputFile);
        
        	ANTLRInputStream input = new ANTLRInputStream(is);
        	JSpecLexer lexer = new JSpecLexer(input);

        	// check input post lexer
        	//List<? extends Token> toks = lexer.getAllTokens();
        	//for(Token t: toks)
        	//    System.out.println(t);
        	//System.out.println("tokenizer size=" + toks.size());

        	CommonTokenStream tokens = new CommonTokenStream(lexer);

        	// check input post tokenizer
        	//List<Token> toks2 = tokens.getTokens();
        	//System.out.println("buffered size=" + toks2.size());

        	JSpecParser parser; // = new SystemRDLParser(null);
        	parser = new JSpecParser(tokens);

        	ParseTree tree = parser.root(); //compilationUnit(); // parse
        	//System.out.println(tree.toStringTree());
        	ParseTreeWalker walker = new ParseTreeWalker(); // create standard
        	walker.walk(this, tree); // initiate walk of tree with listener
        	if (parser.getNumberOfSyntaxErrors() > 0) {
        		Ordt.errorExit("Jspec parser errors detected.");  
        	}

        	// if typedefs are specified for processing, find each and create an instance  
        	if (ExtParameters.hasJspecProcessTypedefs()) processTypedefs();
        	else if (root.getFirstChildInstance() == null)
        		Ordt.errorExit("No jspec structures instanced or typedefs specified for processing.");

        	//root.display(true);

        } catch (FileNotFoundException e) {
        	Ordt.errorExit("jspec file not found. "  + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}
	

	/** if typedefs are specified for processing, find each and create an instance.
	 *  root_regset_is_addrmap value will be ignored for typedefs - always false  */  
	private void processTypedefs() {
   	    List<String> typedefNames = ExtParameters.getJspecProcessTypedefs();
   	    // clear out the existing root child instance(s) 
		root.removeChildInstances();  
   	    // if only one typedef then just add it to root
		if (typedefNames.size() == 1) {
			String typedefName = typedefNames.get(0);
			ModComponent processComp = root.findLocalCompDef(typedefName);
    		if (processComp != null) {
    			// add root enums to new root component
    			//processComp.setCompEnumList(root.getCompEnumList());
    			// create a modInstance
    			ModInstance newInstance = processComp.createNewInstance();   // create using comp call
    			newInstance.setId(typedefName);  // set instance id
    			newInstance.updateProperties(processComp.getProperties());   // get properties from from component type of this instance 
    			// set parent component
    			root.addCompInstance(newInstance);   // add this instance to root or container
    			newInstance.setParent(root);
    		}
    		else Ordt.errorExit("Unable to find specified typedef (" + typedefName + ") for processing.");        			
			
		}
		// otherwise more than one typedef so create a container component
		else {
			// create container regfile
			ModComponent tdefContainer = ModComponent.createModComponent("regfile");
			tdefContainer.setId("TYPEDEF_CONTAINER");
			root.addChildComponent(tdefContainer);
			tdefContainer.setParent(root);
			// add root enums to new root
			//tdefContainer.setCompEnumList(root.getCompEnumList());

			ModInstance tdefContainerInst = tdefContainer.createNewInstance();   // create container instance
			tdefContainerInst.setId("TYPEDEF_CONTAINER");  // set instance id
			// set parent component
			root.addCompInstance(tdefContainerInst);   // add this instance to root
			tdefContainerInst.setParent(root);
			// process each typedef
    		for (String typedefName : typedefNames) {
        		ModComponent processComp = root.findLocalCompDef(typedefName);
        		if (processComp != null) {
        			// create a modInstance
        			ModInstance newInstance = processComp.createNewInstance();   // create using comp call
        			newInstance.setId(typedefName);  // set instance id
        			newInstance.updateProperties(processComp.getProperties());   // get properties from from component type of this instance 
        			// set parent component
        			tdefContainer.addCompInstance(newInstance);   // add this instance to root or container
        			newInstance.setParent(tdefContainer);
        		}
        		else Ordt.errorExit("Unable to find specified typedef (" + typedefName + ") for processing.");        			
    		}
    	}
	}

	//----------------------------------begin JSpecBaseListener overrides-----------------------------------

	/**
	array
	  : LSQ num_expression
	    (COLON num_expression)?
	    RSQ	 */
	@Override public void enterArray(@NotNull JSpecParser.ArrayContext ctx) { 
		activeRules.add(ctx.getRuleIndex());   
		
	}
	
	/**
	 */
	@Override public void exitArray(@NotNull JSpecParser.ArrayContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());	
		lastResolvedArrayIndex = (lastResolvedNum != null) ? lastResolvedNum.toInteger() : null;  // get last resolved integer index
	}

	/**
	enum_field_def
	   : 'enum' id? array str?
	     LBRACE
	     (enum_value_assign)+
	     RBRACE         
	     param_block?
	     SEMI 
	 */
	@Override public void enterEnum_field_def(@NotNull JSpecParser.Enum_field_defContext ctx) { 
		activeRules.add(ctx.getRuleIndex());   
		// create an enum component and set as activeEnumDef
		createEnumComponent(ctx); 
		// extract field header info indices
		boolean hasNoId = ctx.getChild(1).getText().startsWith("[");
		Integer idIdx = hasNoId ? null : 1;   
		Integer arrayIdx = hasNoId ? 1 : 2;   
		boolean hasName = (ctx.getChildCount() > arrayIdx + 2) && ctx.getChild(arrayIdx+1).getText().startsWith("\"");
		Integer nameIdx = hasName ? arrayIdx+1 : null;   
		// create the field component (and instance if not typedef) 
		enterComponentDefinition(ctx, "field", idIdx, nameIdx);
		// extract the field width and store in component
		String fieldWidthStr = ctx.getChild(arrayIdx).getChild(1).getText();
		RegNumber fieldWidth = resolveNumExpresion(fieldWidthStr);
		if (fieldWidth == null)  Ordt.errorExit("Width of field " + activeCompDefs.peek().getId() +
				" (value=" + fieldWidthStr + ") in " + activeCompDefs.peek().getParent().getId() + " could not be resolved.");
		activeCompDefs.peek().setProperty("fieldwidth", 
				fieldWidth.toFormat(RegNumber.NumBase.Dec, RegNumber.NumFormat.Int), 0);  // save width to component (also used to indicate field comp)
		if (!hasNoId) activeCompDefs.peek().setProperty("encode", ctx.getChild(1).getText(), 0);  // indicate field has enum encoding
		// if a new field instance was created, update it's field indices (relative - final vals resolved at builder)
		if (!typeDefActiveStates.peek()) updateFieldIndexInfo(fieldWidth.toInteger(), true);  
	}

	/**
	 */
	@Override public void exitEnum_field_def(@NotNull JSpecParser.Enum_field_defContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
		exitComponentDefinition();		
	}


	/**
	enum_value_assign
	   : id EQ num_expression str
	     SEMI 
	 */
	@Override public void enterEnum_value_assign(@NotNull JSpecParser.Enum_value_assignContext ctx) { 
	}
	
	/**
	 */
	@Override public void exitEnum_value_assign(@NotNull JSpecParser.Enum_value_assignContext ctx) { 
		String parm = ctx.getChild(0).getText();
		String value = ctx.getChild(2).getText();
		String nameStr = ctx.getChild(3).getText().replaceAll("\"", "");
		verifyNonNullAssignValue(lastResolvedNum, parm, value);
		// create a new enum element 
		ModEnumElement enumElem = new ModEnumElement();
		enumElem.setId(parm);
		enumElem.setValue(lastResolvedNum.toString());
		enumElem.setName(nameStr);
		// now add this to the active enum definition
		activeEnumDef.addEnumElement(enumElem);		
	}
	
	/**
	field_set_def
	   : 'field_set' id str
	     LBRACE
	     (value_assign | int_field_def | enum_field_def | nop_field_def | typedef_instance)+
	     RBRACE         
	     SEMI 
	 */
	@Override public void enterField_set_def(@NotNull JSpecParser.Field_set_defContext ctx) { 
		activeRules.add(ctx.getRuleIndex());   
		// create the component (and instance if not typedef) 
		enterComponentDefinition(ctx, "fieldset", 1, 2);		
		// save the offset for this fieldset if an instance
		// initialize accumulated child widths for this fieldset component at zero 
		// these are relative offsets - final values resolved at builder
		fieldOffsets.push(0); // init fieldOffset for this fieldset

	}
	/**
	 */
	@Override public void exitField_set_def(@NotNull JSpecParser.Field_set_defContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
		Integer childOffset = fieldOffsets.pop(); // grab the width of this fieldset from child offset
		// if this fieldset is a union, remove all except first child 
		ModFieldSet currentFSet = (ModFieldSet) activeCompDefs.peek();
		if (currentFSet.isUnion()) {
			//System.out.println("JSpecModelExtractor exitFieldSet: union id=" + currentFSet.getId() + ", original offset=" + childOffset);
			childOffset = currentFSet.cleanupUnion();
			//System.out.println("JSpecModelExtractor exitFieldSet:     new offset=" + childOffset);
		}
		if (!typeDefActiveStates.peek()) {
			updateFieldIndexInfo(childOffset, true);  // if an instance, update current offset
		}
		currentFSet.setProperty("fieldstructwidth", childOffset.toString(), 0);  // save the fieldset width as a property in the component
		exitComponentDefinition();		
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * The default implementation does nothing.
	 */
	@Override public void enterInteger_constant_assign(@NotNull JSpecParser.Integer_constant_assignContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
    }
	
	/**
	 * {@inheritDoc}
	 * <p/>
	 * The default implementation does nothing.
	 */
	@Override public void exitInteger_constant_assign(@NotNull JSpecParser.Integer_constant_assignContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/**
	int_field_def
	   : 'integer' id? array str?    
	     param_block?
	     SEMI 
	 */
	@Override public void enterInt_field_def(@NotNull JSpecParser.Int_field_defContext ctx) { 
		activeRules.add(ctx.getRuleIndex());   
		// extract field header info indices
		boolean hasNoId = ctx.getChild(1).getText().startsWith("[");
		Integer idIdx = hasNoId ? null : 1;   
		Integer arrayIdx = hasNoId ? 1 : 2;   
		boolean hasName = (ctx.getChildCount() > arrayIdx + 2) && ctx.getChild(arrayIdx+1).getText().startsWith("\"");
		Integer nameIdx = hasName ? arrayIdx+1 : null;   
		// create the component (and instance if not typedef) 
		enterComponentDefinition(ctx, "field", idIdx, nameIdx);		
		// extract the field width and store in component
		String fieldWidthStr = ctx.getChild(arrayIdx).getChild(1).getText();
		RegNumber fieldWidth = resolveNumExpresion(fieldWidthStr);
		if (fieldWidth == null)  Ordt.errorExit("Width of field " + activeCompDefs.peek().getId() +
				" (value=" + fieldWidthStr + ") in " + activeCompDefs.peek().getParent().getId() + " could not be resolved.");
		activeCompDefs.peek().setProperty("fieldwidth", 
				fieldWidth.toFormat(RegNumber.NumBase.Dec, RegNumber.NumFormat.Int), 0);  // save width to component  (also used to indicate field comp)
		// if a new field instance was created, update it's field indices (relative - finel vals resolved at builder)
		if (!typeDefActiveStates.peek()) updateFieldIndexInfo(fieldWidth.toInteger(), true);
	}
	
	/**
	 */
	@Override public void exitInt_field_def(@NotNull JSpecParser.Int_field_defContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
		exitComponentDefinition();		
	}
	
	/**
	nop_field_def
	   : 'nop' array
	     param_block?
	     SEMI 
	 */
	@Override public void enterNop_field_def(@NotNull JSpecParser.Nop_field_defContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		// extract the field width and store in component
		String fieldWidthStr = ctx.getChild(1).getChild(1).getText();
		RegNumber fieldWidth = resolveNumExpresion(fieldWidthStr);
		// update  field indices with no update for nop (relative - finel vals resolved at builder)
		if (fieldWidth != null) updateFieldIndexInfo(fieldWidth.toInteger(), false);
		else Ordt.errorExit("Width of nop field (value=" + fieldWidthStr + ") in " + activeCompDefs.peek().getId() + " could not be resolved.");
		
	}
	
	/**
	 */
	@Override public void exitNop_field_def(@NotNull JSpecParser.Nop_field_defContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
		//exitComponentDefinition();				
	}

	/**
	 num_constant_def
      : 'constant' id str         
        LBRACE
        ( integer_constant_assign   //  int constant value assign
        | width_constant_assign+  // enum or field constant used for WIDTH()
        )   
        RBRACE
        SEMI 
	 */
	@Override public void enterNum_constant_def(@NotNull JSpecParser.Num_constant_defContext ctx) {
		activeRules.add(ctx.getRuleIndex());
		accumulated_constant_width = 0;  // init constant width
		activeConstantId = ctx.getChild(1).getText();  // set active constant id 
    }
	
	@Override public void exitNum_constant_def(@NotNull JSpecParser.Num_constant_defContext ctx) {  
		activeRules.remove(ctx.getRuleIndex());
		
		ParseTree constAssignStmt = ctx.getChild(4);
		boolean isIntConstant = (constAssignStmt.getChildCount() > 2) && "integer".equals(constAssignStmt.getChild(0).getText()) && "=".equals(constAssignStmt.getChild(2).getText());
    	//if (!isIntConstant) System.out.println("JSpecModelExtractor exitNum_constant_def: stmt=" + constAssignStmt.getText());
		// if an integer assign then set value and resolve width
        if (isIntConstant) {
        	if (lastResolvedNum != null) {
        		numConstants.put(activeConstantId, lastResolvedNum);
        		if (lastResolvedArrayIndex != null) {
        			numConstants.get(activeConstantId).setVectorLen(lastResolvedArrayIndex);
        			//System.out.println("JSpecModelExtractor: exitNum_constant_def, setting width of " + activeConstantId +  " to " + lastResolvedArrayIndex);
        		}
        		else Ordt.warnMessage("Width of integer constant " + activeConstantId + " could not be resolved");
        	}
        	else Ordt.warnMessage("Value of integer constant " + activeConstantId + " could not be resolved");
        }
		// otherwise save accumulated_constant_width
        else if (accumulated_constant_width > 0) {
        		numConstants.put(activeConstantId, new RegNumber(0));  // constant value defualt is zero
        		numConstants.get(activeConstantId).setVectorLen(accumulated_constant_width);
        		//System.out.println("JSpecModelExtractor exitNum_constant_def, setting width of " + activeConstantId +  " to " + accumulated_constant_width);
        }
	}
	
	/**
   : 'typedef' 'param'
     id  { JSpecLexer.addUserParameter($id.text); }
     EQ ('string' | 'boolean' | 'integer')
     SEMI 
	 */
	@Override public void enterParam_type_definition(JSpecParser.Param_type_definitionContext ctx) {
		activeRules.add(ctx.getRuleIndex());
		String usrPropertyName = "js_" + ctx.getChild(2).getText();
		String usrPropertyType = ctx.getChild(4).getText();
		List<String> comps = new ArrayList<String>();
		comps.add("all");
        // add the new property to defined list
		DefinedProperties.addUserProperty(usrPropertyName, usrPropertyType, null, comps);  // js usr properties can apply to any comp type, have no default
	}
	
	@Override public void exitParam_type_definition(JSpecParser.Param_type_definitionContext ctx) {
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/**
     width_constant_assign
     : ( 'enum' array LBRACE ~(LBRACE|RBRACE)* RBRACE SEMI 
     | 'integer' id array  str SEMI
     |  id SEMI    // field or fieldset id
     )
	 */
	@Override public void enterWidth_constant_assign(JSpecParser.Width_constant_assignContext ctx) {
		activeRules.add(ctx.getRuleIndex());		
	}

	@Override public void exitWidth_constant_assign(JSpecParser.Width_constant_assignContext ctx) {
		activeRules.remove(ctx.getRuleIndex());
		
		boolean isIntConstant = (ctx.getChildCount() > 2) && "integer".equals(ctx.getChild(0).getText());
		boolean isEnumConstant = (ctx.getChildCount() > 2) && "enum".equals(ctx.getChild(0).getText());
		boolean isFieldConstant = (ctx.getChildCount() > 1) && !isIntConstant && !isEnumConstant;
    	//if (isEnumConstant) System.out.println("JSpecModelExtractor exitWidth_constant_assign: not int or fld, stmt=" + ctx.getText());
		// if this is a constant representing a field or fieldset for width resolution, bump constant width 
        if (isFieldConstant) {
        	String fldId = ctx.getChild(0).getText();
        	//System.out.println("JSpecModelExtractor exitWidth_constant_assign: field width assign detected, id=" + constId + ", fldid=" + fldId);
        	Integer fldWidth = findFieldWidth(fldId);
        	if (fldWidth != null) {
        		accumulated_constant_width += fldWidth;
   				//System.out.println("JSpecModelExtractor exitWidth_constant_assign: setting width of " + activeConstantId +  " to " + accumulated_constant_width);
        	}
        	else Ordt.warnMessage("Width of field/fieldset " + fldId + " in constant " + activeConstantId + " could not be resolved"); 
        }       
		// otherwise if an integer assign then set value and resolve width
        else if (isIntConstant) {
        		if (lastResolvedArrayIndex != null) {
            		accumulated_constant_width += lastResolvedArrayIndex;
        			//System.out.println("JSpecModelExtractor: exitWidth_constant_assign, setting width of " + activeConstantId +  " to " + accumulated_constant_width);
        		}
        		else Ordt.warnMessage("Width of integer constant " + activeConstantId + " could not be resolved");
        }
		// otherwise if an enum constant then set value to zero and resolve width
        else if (isEnumConstant) {
        		if (lastResolvedArrayIndex != null) {
            		accumulated_constant_width += lastResolvedArrayIndex;
        			//System.out.println("JSpecModelExtractor: exitWidth_constant_assign, setting width of " + activeConstantId +  " to " + accumulated_constant_width);
        		}
        		else Ordt.warnMessage("Width of enum constant " + activeConstantId + " could not be resolved");
        }	
	}

    /** returns an Integer representing the width of a field or fieldset having specified id.
     * Used for WIDTH() function resolution - in js, width property is always stored back into component
     *  If no field is found, null is returned
     * @param fldId - id of field to be found
     */
	private Integer findFieldWidth(String fldId) {
    	ModComponent fldComp = activeCompDefs.peek().findCompDef(fldId);
    	Integer fldWidth = null;
    	if (fldComp != null) {
    		if (fldComp.isField() && fldComp.hasProperty("fieldwidth")) fldWidth = Utils.strToInteger(fldComp.getProperty("fieldwidth"));
    		else if (fldComp.isFieldSet() && fldComp.hasProperty("fieldstructwidth")) fldWidth = Utils.strToInteger(fldComp.getProperty("fieldstructwidth"));
    	}
		return fldWidth;
	}


	/**
	 * numeric expression
	 */
	@Override public void enterNum_expression(@NotNull JSpecParser.Num_expressionContext ctx) {
		activeRules.add(ctx.getRuleIndex());
		numExpessionDepth++;
		if (numExpessionDepth == 1) {
			//System.out.println("JSpecModelExtractor: enterNum_expression, d=" + numExpessionDepth +  ", in=" + ctx.getText());
			lastResolvedNum = resolveNumExpresion(ctx.getText()); 
			//System.out.println("JSpecModelExtractor: enterNum_expression, d=" + numExpessionDepth +  ", in=" + ctx.getText() + ", out=" + lastResolvedNum);
		}
    }
	
	/**
	 */
	@Override public void exitNum_expression(@NotNull JSpecParser.Num_expressionContext ctx) {  
		activeRules.remove(ctx.getRuleIndex());
		numExpessionDepth--;
	}
	
	/**
	param_block
	   : 'param'
	     LBRACE
	     (value_assign)+
	     RBRACE         
	 */
	@Override public void enterParam_block(@NotNull JSpecParser.Param_blockContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		
	}
	
	/**
	 */
	@Override public void exitParam_block(@NotNull JSpecParser.Param_blockContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	
	}

	/**
	register_def
	   : 'register' id str
	     LBRACE
	     (value_assign | field_def | typedef_instance)+
	     RBRACE         
	     param_block?
	     SEMI 
	 */
	@Override public void enterRegister_def(@NotNull JSpecParser.Register_defContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		// create the component (and instance if not typedef) 
		enterComponentDefinition(ctx, "reg", 1, 2);
		// assume default width unless register_width assignment is made
		registerWidth = ModRegister.defaultWidth;  // TODO - really should restructure width assign since regsets can assign 
		//System.out.println("JSpecModelExtractor: enterRegister_def: --------  id=" + ctx.getChild(1).getText() );
		fieldOffsets.push(0); // init fieldOffset for this reg
	}
	
	/**
	 */
	@Override public void exitRegister_def(@NotNull JSpecParser.Register_defContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
		//activeCompDefs.peek().display(null, null, true);
		Integer bitsAssigned = fieldOffsets.peek();
		if (bitsAssigned != registerWidth) {
			Integer bitPadding = registerWidth - bitsAssigned;
			//System.out.println("JSpecModelExtractor exitRegister_def: padding needed for reg " + activeCompDefs.peek().getId() + ", w=" + registerWidth + ",assigned=" + bitsAssigned);
			if (bitPadding > 0) 
				((ModRegister) activeCompDefs.peek()).setPadBits(bitPadding);  // save req'd reg padding in comp - used in regProperties to set field offsets
		}
		exitComponentDefinition();
		fieldOffsets.pop();
	}

	/**
	register_set_def
	   : 'register_set' id str
	     LBRACE
	     (value_assign | register_set_def | register_def | typedef_instance)+
	     RBRACE         
	     param_block?
	     SEMI 
	 */
	@Override public void enterRegister_set_def(@NotNull JSpecParser.Register_set_defContext ctx) { 
		activeRules.add(ctx.getRuleIndex());   
		// create the component (and instance if not typedef) 
		enterComponentDefinition(ctx, "regset", 1, 2);
	}

	/**
	 */
	@Override public void exitRegister_set_def(@NotNull JSpecParser.Register_set_defContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
		exitComponentDefinition();
	}

	/**
	 *  initialize root element
	 */
	@Override public void enterRoot(@NotNull JSpecParser.RootContext ctx) { 
		// add the root element
		root = (ModRootComponent) ModComponent.createModComponent("root");
		activeCompDefs.push(root);   // push onto the active definition stack
		typeDefActiveStates.push(false);  // typedef state
	}
	
	/**
	 *  exiting file 
	 */
	@Override public void exitRoot(@NotNull JSpecParser.RootContext ctx) { 
	}

	/**
	 * unsupported constants (same hdr as num_constant_def)
	 */
	@Override public void enterString_constant_def(@NotNull JSpecParser.String_constant_defContext ctx) { 
		//System.out.println("JSpecModelExtractor: enterString_constant_def: --------  def=" + ctx.getText() );
		String constID = ctx.getChild(1).getText();
		Ordt.warnMessage("Definition of non-integer constant " + constID + " will be ignored");
		
	}
	
	/**
	 */
	@Override public void exitString_constant_def(@NotNull JSpecParser.String_constant_defContext ctx) { 
		
	}
	
	/**
	test_group_def
	   : 'test_group'
	     LBRACE 
	     id (COMMA id)* SEMI 
	     RBRACE
	     SEMI
	  ;
	 */
	@Override public void enterTest_group_def(JSpecParser.Test_group_defContext ctx) { // TODO
		activeRules.add(ctx.getRuleIndex());		
		//System.out.println("JSpecModelExtractor: enterTest_group_def: --------  ctx=" + ctx.getText() );
		// interpret test group as an aliased register set - assume second in set is alias of the first (eg diag)
		if (ctx.getChildCount()>5) {
			String firstID = ctx.getChild(2).getText();
			String secondID = ctx.getChild(4).getText();
			//System.out.println("JSpecModelExtractor: enterTest_group_def:  1=" + firstID );
			//System.out.println("JSpecModelExtractor: enterTest_group_def:  2=" + secondID );
	    	ModInstance aliasRegInst = activeCompDefs.peek().findLocalInstance(secondID);  // find instance with second name
	    	if (aliasRegInst != null) {
	    		aliasRegInst.setProperty("aliasedId", firstID, 0);
	    	}
		}
	}
	
	/**
	 */
	@Override public void exitTest_group_def(JSpecParser.Test_group_defContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/** issue unsupported msg for transaction */
	@Override public void enterTransaction_def(@NotNull JSpecParser.Transaction_defContext ctx) {
		Ordt.warnMessage("Jspec transaction definition not supported (line " + ctx.getStart().getLine() + ")"); 
	}

	/**
	type_definition
	   : 'typedef'
	     ( enum_field_def
	     | register_set_def 
	     | register_def 
	     | field_def 
	     )
	 */
	@Override public void enterType_definition(@NotNull JSpecParser.Type_definitionContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		typeDefFound = true;  // mark for subsequent component definition - value will be pushed onto typeDef stack for ea component def	
	}
	
	/**
	 */
	@Override public void exitType_definition(@NotNull JSpecParser.Type_definitionContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation does nothing.</p>
	 */
	
	/**
	typedef_instance
	   : id id str 
	     param_block?
	     SEMI
	 */
	@Override public void enterTypedef_instance(@NotNull JSpecParser.Typedef_instanceContext ctx) {   
		activeRules.add(ctx.getRuleIndex());
        // extract typedef name, id, name, and source line  // TODO add src line?
		String compId = ctx.getChild(0).getText();
		String id = ctx.getChild(1).getText();
		String name = ctx.getChild(2).getText();
		if (name.length() > 2) name = name.substring(1, name.length() - 1);  // remove quotes
		else {
			//System.err.println("JSpecModelExtractor enterTypedef_instance -------- id=" + id +  ", name=" + name);
			name = null;
		}
		//System.err.println("JSpecModelExtractor enterTypedef_instance -------- id=" + id +  ", name=" + name);

		// find the ModComponent referenced
		ModComponent comp = activeCompDefs.peek().findCompDef(compId);  // find this component in scope
		if (comp != null) {
			//if (id.equals("type")) {
				//System.out.println("JSpecModelExtractor enterTypedef_instance -------- id=" + id +  ", comp=" + compId +  ", activeInstance below:");
				//if (activeInstances.isEmpty()) System.out.println("JSpecModelExtractor enterTypedef_instance          activeInstance stack is empty!");
				//else activeInstances.peek().display(null, 0, true);
			//}

			createNewInstance(comp, id, activeCompDefs.peek(), name, ctx.getStart().getLine());
			ModInstance newInst = activeInstances.peek();
			if (newInst.isRootInstance() && ExtParameters.jspecRootRegsetIsAddrmap()) comp.setCompType(CompType.ADDRMAP);  // make root instanced comp an addrmap
			// update properties from component and ancestors
			newInst.updateProperties(comp.getProperties());   // get properties from from component type of this instance 
			newInst.updateProperties(newInst.getParent().getDefaultProperties());   // get default properties from ancestors
			// if a new field or fieldset instance, update field indices (relative - final vals resolved at builder)
			if (comp.isField() && comp.hasProperty("fieldwidth")) updateFieldIndexInfo(comp.getIntegerProperty("fieldwidth"), true);   
			else if (comp.isFieldSet() && comp.hasProperty("fieldstructwidth")) {
				updateFieldIndexInfo(comp.getIntegerProperty("fieldstructwidth"), true);  // use stored fieldset width to compute instance offsets
			}

		}
		else Ordt.errorExit("unable to find component " + compId + " instanced in " + activeCompDefs.peek().getId());
	}
	
	/**
	 */
	@Override public void exitTypedef_instance(@NotNull JSpecParser.Typedef_instanceContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
		if (!activeInstances.isEmpty()) activeInstances.pop();  // we're done with this instance
	}

	/**
	value_assign
	   : assign_parameter EQ
	     ( defined_attribute
	     | num_expression 
	     | str 
	     | jstr
	     | defined_attribute_set
	     )         
	     SEMI 
	 */
	@Override public void enterValue_assign(@NotNull JSpecParser.Value_assignContext ctx) {
		activeRules.add(ctx.getRuleIndex());
		String parm = ctx.getChild(0).getText();
        if (parm.equals("reset")) inhibitNextResolveCheck = true;
    }

	/**
	 */
	@Override public void exitValue_assign(@NotNull JSpecParser.Value_assignContext ctx) {  
		activeRules.remove(ctx.getRuleIndex());
		String parm = ctx.getChild(0).getText();
		String value = ctx.getChild(2).getText();
		saveJSpecParam(parm, value);  // save info corresponding to this jspac parameter in the model
	}


    // ------------------------------ RegModel interface methods --------------------------------------
	
    /** get root component
	 *  @return the root
	 */
	public ModComponent getRoot() {
		return root;
	}
	
	/** get root instance
	 *  @return the root
	 */
	public ModInstance getRootInstance() {
		return getRoot().getFirstChildInstance();
	}    
	
	/** get root instanced component (usually base addrmap)
	 *  @return the root
	 */
	public ModComponent getRootInstancedComponent() {
	    if (getRootInstance() != null) return getRootInstance().getRegComp();
	    return null;
	}
	
	/** get rdlInputFile name
	 *  @return the rdlInputFile
	 */
	public String getOrdtInputFile() {
		return jspecInputFile;
	}

	/** return true if field offsets are relative to zero or max reg/fieldset width (rdl=true, jspac=false) **/
	public boolean fieldOffsetsFromZero() {
		return false;
	}

	@Override
	public boolean isUserDefinedSignal(String name) {
		return false;  // always return false since jspec does not allow user-defined signals
	}

    // ------------------------------------------------------------------------------------------------
	
	/** set the width and offset for current field/fieldset instance
	 * also add width of this instance to parent on fieldOffset stack
	 * 
	 * @param fieldWidth - width of field/fieldset being added
	 * @param update - if true update field bit locations
	 */
	private void updateFieldIndexInfo(Integer fieldWidth, boolean update) {
		//if (fieldOffsets.isEmpty()) { System.out.println("JSpecModelExtractor updateFieldIndexInfo: fieldWidth=" + fieldWidth + ", update=" + update); /*System.exit(0)*/; }
		Integer fieldOffset = fieldOffsets.peek();
		// set field indices assuming full register space for now (any fieldset offset resolved at builder unroll)
		if (update) {
			//System.out.println("JSpecModelExtractor updateFieldIndexInfo, --- id=" + activeInstances.peek().getFullId() + " , width=" + fieldWidth);
		
			// save the offset and width of this field/fieldset
			((ModIndexedInstance) activeInstances.peek()).setWidth(fieldWidth);  // save field/fieldset width					
			((ModIndexedInstance) activeInstances.peek()).setOffset(fieldOffset);  // save field/fieldset offset	
			//System.out.println("JSpecModelExtractor updateFieldIndexInfo,     updated indices to width=" + fieldWidth + ", offset=" + offset);  
		}
		//else System.out.println("JSpecModelExtractor updateFieldIndexInfo, --- no index update, width=" + fieldWidth);  

		fieldOffsets.push(fieldOffsets.pop() + fieldWidth);
		//System.out.println("JSpecModelExtractor updateFieldIndexInfo,     updated offset to " + fieldOffsets.peek());  
	}
	
	/** save the given jspec parameter assignment in the model */
	private void saveJSpecParam(String parm, String value) {
		// get parse state booleans
		boolean isInNop = activeRules.contains(JSpecParser.RULE_nop_field_def);
		//boolean isInParamBlock = activeRules.contains(JSpecParser.RULE_param_block);
		boolean isTypeDefInstance = activeRules.contains(JSpecParser.RULE_typedef_instance);  // typedef is being instanced
		
		// ignore assignments no nop
		if (isInNop) return;
		
		//if (value.length() > 20) value = value.substring(0, 18) + "...";   
		//System.out.println("JSpecModelExtractor: saveJSpecParam, tdef=" + typeDefIsActive + ", pblock=" + isInParamBlock + ", p=" + parm + ",  v=" + value + ",  comp=" + activeCompDefs.peek().getFullId());
	
		// if this is a typedef_instance, apply parms to the active instance else apply to active component
		ModBaseComponent modComp;
		if (isTypeDefInstance) {
			if (activeInstances.isEmpty()) return;  // nothing on inst stack so exit
			modComp = activeInstances.peek();
		}
		else {
			if (activeCompDefs.isEmpty()) return;  // nothing on comp stack so exit
			modComp = activeCompDefs.peek();			
		}

        // parameters applying to component or instance
		if (parm.equals("description")) {
			if (value.length() >= 5) {
				value = value.substring(2, value.length() - 2);  // remove brackets
				modComp.setProperty("desc", value, 0);  // set desciption
			}
		}
		
		else if (parm.equals("category")) {
			value = value.substring(1, value.length() - 1);  // remove brackets
			modComp.setProperty("category", value, 0);  // set category
			// detect volatile categories
			if (value.contains("INTERRUPT")) modComp.setDefaultProperty("intr", "true");
			else if (value.contains("STATE")) modComp.setDefaultProperty("hw", "w");
			else if (value.contains("COUNTER")) modComp.setDefaultProperty("counter", "true");
		}
		
		else if (parm.equals("sub_category")) {
			value = value.substring(1, value.length() - 1);  // remove brackets
			modComp.setProperty("sub_category", value, 0);  // set sub_category  
		}
		
		// ------ addressing parms need to be handled differently if a typedef instance
		else if (parm.equals("address")) {
			verifyNonNullAssignValue(lastResolvedNum, parm, value);
			if (isTypeDefInstance) {
				if (activeInstances.peek().isAddressable()) ((ModAddressableInstance) activeInstances.peek()).setAddress(lastResolvedNum);
			}
			else modComp.setProperty("address", lastResolvedNum.toString(), 0);  // set address  
		}

		else if (parm.equals("register_set_size")) {
			verifyNonNullAssignValue(lastResolvedNum, parm, value);
			if (isTypeDefInstance) {
				if (activeInstances.peek().isAddressable()) ((ModAddressableInstance) activeInstances.peek()).setAddressIncrement(lastResolvedNum);
			}
			else if (((ModComponent) modComp).isRegSetOrAddressMap()) modComp.setProperty("addrinc", lastResolvedNum.toString(), 0);  
		}

		// -----
		
		else if (parm.equals("repeat")) {
			verifyNonNullAssignValue(lastResolvedNum, parm, value);
			Integer reps = lastResolvedNum.toInteger();
			if (isTypeDefInstance) {
				activeInstances.peek().setRepCount(reps);
				if (reps>ExtParameters.jspecExternalReplicationThreshold()) activeInstances.peek().setProperty("external", "DEFAULT", 0);
			}
			else {
				modComp.setProperty("repcount", reps.toString(), 0);  // set rep count 
				if (reps>ExtParameters.jspecExternalReplicationThreshold()) modComp.setDefaultProperty("external", "DEFAULT");
			}
		}

		//
		else if (parm.equals("register_width")) {
			verifyNonNullAssignValue(lastResolvedNum, parm, value);
			registerWidth = lastResolvedNum.toInteger();  // set width of reg for field bit calcs  
			modComp.setProperty("regwidth", registerWidth.toString(), 0);  // 
		}
		
		//
		else if (parm.equals("root")) {
			if ("true".equals(value.toLowerCase()) && ExtParameters.jspecRootIsExternalDecode()) {
				if (isTypeDefInstance) {
					activeInstances.peek().setProperty("external", "EXTERNAL_DECODE", 0);
				}
				else {
					activeCompDefs.peek().setDefaultProperty("external", "EXTERNAL_DECODE"); 
					//System.out.println("JSpecModelExtractor: saveJSpecParam, p=" + parm + ",  v=" + value); 
				}
			}
		}

		// field properties - if in a typedef instance apply directly else use as defaults
		else if (parm.equals("reset")) {
			if (!value.equals("unknown")) {
				RegNumber newNum = new RegNumber(lastResolvedNum);
				// if a typedef instance apply to the inst itself
				if (isTypeDefInstance) {
					if (!newNum.isDefined()) newNum = getEnumElementValue(activeInstances.peek().getRegComp(), value);  // try resolving enum value
					verifyNonNullAssignValue(newNum, parm, value);  
					verifyNonNullAssignValue(newNum.getValue(), parm, value);  
					//if (activeInstances.peek().getId().equals("type")) System.out.println("JSpecModelExtractor: found 'type' assign... val=" + value + ", defined=" + newNum);
					activeInstances.peek().setProperty("reset", newNum.toString(), 0);
				}
				// otherwise apply to component property
				else {
					if (!newNum.isDefined()) newNum = getEnumElementValue(activeCompDefs.peek(), value); // try resolving enum value
					verifyNonNullAssignValue(newNum, parm, value);
					verifyNonNullAssignValue(newNum.getValue(), parm, value);
					boolean fieldDefActive = activeRules.contains(JSpecParser.RULE_int_field_def) || activeRules.contains(JSpecParser.RULE_enum_field_def);
					if (fieldDefActive) activeCompDefs.peek().setProperty("reset", newNum.toString(), 0); 				
					else activeCompDefs.peek().setDefaultProperty("reset", newNum.toString()); 				
				}
			}
			// remove any default reset property if unknown is set
			else {
				//System.out.println("------------- reset=unknown detected ------------------");
				if (!isTypeDefInstance) {  // TODO not looking at instance parms case
					//activeCompDefs.peek().display(null, 0, false);
					activeCompDefs.peek().removeDefaultProperty("reset");  
					//activeCompDefs.peek().display(null, 0, false);
					if (activeCompDefs.peek().hasDefaultProperty("sw")) {  
						if (activeCompDefs.peek().getDefaultProperty("sw").equals("r")) {
							//System.out.println("------------- reset=unknown read-only hack invoked ------------------");
							activeCompDefs.peek().setDefaultProperty("hw", "rw");
							activeCompDefs.peek().setDefaultProperty("we", "true");
						}
					}
				}
				//else activeInstances.peek().display(null, 2, true);
				
			}
		}
		
		else if (parm.equals("access_mode")) {
			String swVal = null;
			String rVal = null;
			String wVal = null;
			//Boolean setHwWrite = false;
			switch (value) {
			case ("READ_WRITE"): 
				swVal = "rw";
			    break;
			case ("READ_ONLY"): 
				swVal = "r";
			    //setHwWrite = true;
			    break;
			case ("WRITE_ONLY"): 
				swVal = "w";
			    break;
			case ("CLEAR_ON_READ"): 
			case ("READ_TO_CLEAR"): 
				swVal = "rw";
			    rVal = "rclr";
			    break;
			case ("WRITE_ONE_TO_CLEAR"): 
				swVal = "rw";
		        wVal = "woclr";
			    break;
			case ("WRITE_ONE_TO_SET"): 
				swVal = "rw";
	            wVal = "woset";
			    break;
			default: 
				swVal = "rw";
	            Ordt.warnMessage("Assignment of jspec access_mode to " + value + " not supported. rw assumed.");		
			}
			
			if (isTypeDefInstance) {  // set as default if not an instance
				//System.out.println("JSpecModelExtractor: saveJSpecParam, p=" + parm + ",  v=" + value + ",  num=" + lastResolvedNum); 
				activeInstances.peek().setDefaultProperty("sw", swVal); // was setProperty(("sw", swVal, 0)
				if (rVal != null) activeInstances.peek().setDefaultProperty(rVal, "true"); 
				if (wVal != null) activeInstances.peek().setDefaultProperty(wVal, "true");
			}
			else {
				activeCompDefs.peek().setDefaultProperty("sw", swVal); 
				if (rVal != null) activeCompDefs.peek().setDefaultProperty(rVal, "true");
				if (wVal != null) activeCompDefs.peek().setDefaultProperty(wVal, "true");
			}
		}
		
		else if (parm.equals("attributes")) { 

			boolean donttest = value.contains("JS_ATTRIB_DO_NOT_TEST");
			boolean dontcompare = value.contains("JS_ATTRIB_TEST_ACCESS_ONLY");
			boolean outputAttrs = ExtParameters.rdlOutputJspecAttributes();
			
			if (isTypeDefInstance) {  // set as default if not an instance
				//System.out.println("JSpecModelExtractor: saveJSpecParam, p=" + parm + ",  v=" + value + ",  num=" + lastResolvedNum); 
				if (donttest) activeInstances.peek().setDefaultProperty("donttest", "true");    // was setDynamicProperty(
				if (dontcompare) activeInstances.peek().setDefaultProperty("dontcompare", "true"); // was setDynamicProperty(
			}
			else {
				if (donttest) activeCompDefs.peek().setDefaultProperty("donttest", "true"); 
				if (dontcompare) activeCompDefs.peek().setDefaultProperty("dontcompare", "true"); 
			}
					
			if (outputAttrs) modComp.setProperty("js_attributes", value, 0);
		}
		
		else if (parm.equals("union")) { 
			if ("true".equals(value.toLowerCase()) && !isTypeDefInstance && ((ModComponent) modComp).isFieldSet()) {   // only handle case of fieldset typedef
				//System.out.println("JSpecModelExtractor: saveJSpecParam, p=" + parm + ",  v=" + value + ",  num=" + lastResolvedNum); 
				((ModFieldSet) modComp).setUnion(true);
			}
		}
		
		// detect passthru properties (in defined properties list)
		else if (DefinedProperties.hasProperty("js_" + parm)) {
			//System.out.println("Found a passthru jspec property: " + parm + " = " + value + " in component " + activeCompDefs.peek().getId());
			modComp.setProperty("js_" + parm, value, 0);
		}
			
		// otherwise issue a warning message unless in the ignore list
		else if (!ignoredParameters.contains(parm)) {
			Ordt.warnMessage("Unsupported jspec assignment: " + parm + " = " + value + " in component " + activeCompDefs.peek().getId());
			//System.out.println("JSpecModelExtractor saveJSpecParam, unknown assignment: " + parm + " = " + value + " in " + activeCompDefs.peek().getFullId());
		}
		//if ((lastResolvedNum != null) && !value.equals(lastResolvedNum.toString())) 
		//	System.out.println("JSpecModelExtractor: saveJSpecParam, p=" + parm + ",  v=" + value + ",  num=" + lastResolvedNum);
	}


	/** use a dynamic property assign to handle parm assigns to register that should be pushed to fields */  // FIXME - wont work unless field depth vs cmd is known
	@SuppressWarnings("unused")
	private void setDynamicProperty(String parm, String value) {
		ModInstance currentInst = activeInstances.peek();
		ModComponent parent = currentInst.getParent();
		//System.out.println("JSpecModelExtractor: setDynamicProperty, inst=" + currentInst.getId() + ", parent=" + parent.getId() + ", p=" + parm + ",  v=" + value); 
		// add dynamic assigns for all fields to parent
		ModComponent regComp = currentInst.getRegComp(); // get instance children
		for (ModInstance childInst : regComp.getChildInstances()) {
			String instPathStr = currentInst.getId() + "." + childInst.getId();
			parent.addParameter(instPathStr, parm, value);
			//System.out.println("JSpecModelExtractor: setDynamicProperty, instPathStr=" + instPathStr); 
		}
	}
	/**
	 * return value for an enum value assignment if it exists
	 * @param regComp - component where assignment occurs
	 * @param valueStr - pre-resolution value string
	 * @return resolved enum value or null if not found
	 */
	private RegNumber getEnumElementValue(ModComponent regComp, String valueStr) {
		RegNumber newValue = null;
		if ((regComp != null) && (regComp.hasProperty("encode"))) {
			String enumId = regComp.getProperty("encode");
			ModEnum enumComp = regComp.findEnum(enumId);
			if (enumComp != null) {
				ModEnumElement enumElem = enumComp.findEnumElement(valueStr);  // find specific enum element referenced
				if (enumElem != null) newValue = enumElem.getValue();
				//System.out.println("JSpecModelExtractor verifyNonNullResetAssignValue: found enum reset encoding for value=" + valueStr + ", out=" + newValue);
			}
		}
		return newValue;
	}
	
	/**
	 * issue an error message if resolved value is null
	 * @param value - resolved value to be checked
	 * @param parm - parameter name
	 * @param valueStr - pre-resolution value string
	 */
	private void verifyNonNullAssignValue(Object value, String parm, String valueStr) {
		if (value == null)
			Ordt.errorExit("Unable to resolve assignment: " + parm + " = " + valueStr);
		
	}
	
	/**
	 * Create a new component (and instance if anonymous)  
	 * @param ctx - definition parse ctx
	 * @param cType - component type to be created
	 * @param nameIndex - child index of comp name if non null
	 */
	private void enterComponentDefinition(ParserRuleContext ctx, String cType, Integer idIndex, Integer nameIndex) {
		String id = "nop";
		String name = "nop field";  
        // if non-null (not a nop field) nameIndex extract component id, name
		if (nameIndex != null) {
			if (idIndex != null) id = ctx.getChild(1).getText();  // set name if specified else create one
			else id = cType + anonCompId++;
			
			name = ctx.getChild(nameIndex).getText(); 
			if (name.length() > 2) name = name.substring(1, name.length() - 1);  // remove quotes
			else name = null;
		}
		Integer lineNumber = ctx.getStart().getLine();  // extract source line
		// determine if this definition is a type definition
		// override id of new component to use anon id if this is not a typedef (no, use for both instance and comp id)
		//String newId = (!typeDefFound)? "aNON" + anonCompId++ : idToken;  // typeDefFound hasn't been pushed onto stack yet so cant use peek()
		// override ctype to addrmap if a regfile instance
		if (cType.equals("regset") && ExtParameters.jspecRootRegsetIsAddrmap() && !typeDefFound && activeCompDefs.peek().isRoot()) {  
			cType = "addrmap";
		}
		// create the new component and push comp/typedef stacks
		ModComponent comp = createNewComponent(id, cType, name, lineNumber);
		// if not a typedef, create a new instance
		if (!typeDefActiveStates.peek()) {
			createNewInstance(comp, id, comp.getParent(), name, lineNumber);
		}
		//else
		//	System.out.println("JSpecModelExtractor enterComponentDefinition: typeDefIsActive for " + idToken + " of " + cType + ", tdfound=" + typeDefFound + ", tdpeek=" + typeDefActiveStates.peek());
	}
	
	/** create a new enum component and assign as active
	 * @param ctx - current enum_def parse context
	 */
	private void createEnumComponent(ParserRuleContext ctx) {
        // extract enum component id, name, and source line
		String idToken = ctx.getChild(1).getText();
		String nameToken = ctx.getChild(3).getText();  
		Integer lineNumber = ctx.getStart().getLine();
		// override id of new component to use anon id if this is not a typedef (NO, for now use same for both instance and comp id)
		//String newId = (!typeDefFound)? "aNON" + anonCompId++ : idToken;  // typeDefFound hasn't been pushed onto stack yet so cant use peek()
		// override ctype to addrmap if first regfile instance
		// create the new active enum component 
		activeEnumDef = (ModEnum) ModComponent.createModComponent("enum");  
		activeEnumDef.setProperty("name", nameToken, 0);  
		// set id of new component 
		activeEnumDef.setId(idToken);			
		// save line number of component definition
		activeEnumDef.setInputLineNumber(lineNumber); // save line number 

		// add as child of parent
		ModComponent activeParent = activeCompDefs.peek();   // snoop the stack to get parent component if any
		if (activeParent != null) {
			activeParent.addCompEnum(activeEnumDef);
			activeEnumDef.setParent(activeParent);
			activeEnumDef.updateDefaultProperties(activeParent.getDefaultProperties());  // pick up defaults from parent
			//System.out.println("JSpecModelExtractor createEnumComponent: added new enum, id=" + activeEnumDef.getId() + " to " + activeParent.getId() );
		}					
	}
	
	/** 
	 * Create a new empty component of specified type
	 * @param idToken - name of component (if typedef active) or instance
	 * @param cType - string representing type of component being defined
	 * @param lineNumber - source file linenumber of this definition
	 */
	private ModComponent createNewComponent(String idToken, String cType, String name, Integer lineNumber) {
		// create the new component of specified type and push onto component stack
		ModComponent rElem = ModComponent.createModComponent(cType);  
		if (rElem != null) {
			rElem.setProperty("name", name, 0); 
			// set id of new component 
			rElem.setId(idToken);			
			// save line number of component definition
			rElem.setInputLineNumber(lineNumber); // save line number 

			// add as child of parent
			ModComponent activeParent = activeCompDefs.peek();   // snoop the stack to get parent component if any
			// push new component and typedef state onto stacks
			activeCompDefs.push(rElem);   
			typeDefActiveStates.push(typeDefFound);
			if (activeParent != null) {
				activeParent.addChildComponent(rElem);
				rElem.setParent(activeParent);
				rElem.updateDefaultProperties(activeParent.getDefaultProperties());  // pick up defaults from parent
			}					
			//System.out.println("JSpecModelExtractor createNewComponent: added new " + cType + ", id=" + rElem.getId());
			typeDefFound = false;   // done with this so set to false for next component def
			return rElem;
		}
		else {
			Ordt.errorExit("component_def type=" + cType + " not implemented");
		}		
		typeDefFound = false;   // done with this so set to false for next component def
		return null;
	}
	
	/**
	 * update instance properties and pop component stack
	 */
	private void exitComponentDefinition() {
		// if an instance was created then update its properties and pop from the stack
		if (!typeDefActiveStates.peek()) {
			// update properties from component and ancestors
			ModInstance newInst = activeInstances.peek();
			newInst.updateProperties(newInst.getRegComp().getProperties());   // get properties from from component type of this instance 
			newInst.updateProperties(newInst.getRegComp().getDefaultProperties());   // get default properties from ancestors
			if (!activeInstances.isEmpty()) activeInstances.pop();  // we're done with this instance
		}
		// pop the stacks since this definition is done
		activeCompDefs.pop();   		
		typeDefActiveStates.pop();
	}
	

	/**
	 * create a new component instance and push onto activeInstances stack
	 * @param comp - component being instanced
	 * @param id - id of new instance
	 * @param parentComp - parent component containing new instance
	 * @param name - name of new instance
	 * @param lineNum - source line number
	 */
	private void createNewInstance(ModComponent comp, String id, ModComponent parentComp, String name, int lineNum) {
		//System.out.println("JSpecModelExtractor createNewInstance: added new instance " + id + " of " + comp.getId() + " in " + parentComp.getId());
		// create a modInstance
		ModInstance newInstance = comp.createNewInstance();   // create using comp call
		newInstance.setId(id);  // set instance id
		if (name != null) newInstance.setProperty("name", name, 0);
		newInstance.setInputLineNumber(lineNum);
		// set parent component
		if (parentComp != null) {
			parentComp.addCompInstance(newInstance);   // add this instance to parent component
			newInstance.setParent(parentComp);  // instance is referenced in the currentCompParent
		}
		//System.out.println("JSpecModelExtractor createNewInstance: parent instance:");
		//if (!activeInstances.isEmpty()) activeInstances.peek().display(null, 0, true);
		activeInstances.push(newInstance);
	}
	
	/** 
	 * return resolved numeric value of a simple expression
	 * @param ctx context containing numeric expression
	 * @return resolved numeric value
	 */
	private RegNumber resolveNumExpresion(String exprString) {
		// extract extression tokens and resolve constants
		String [] expArray = tokenizeNumExprString(exprString);
		//System.out.println("JSpecModelExtractor: resolveNumExpresion, expArray=" + join(expArray));				
		// resolve expression to a regNumber;
		RegNumber regNum = new RegNumber(expArray);
		if (regNum != null) {
			if (!regNum.isDefined()) return null;
			regNum.setNumBase(RegNumber.NumBase.Hex);
			regNum.setNumFormat(RegNumber.NumFormat.Address);
		}
		//System.out.println("JSpecModelExtractor: resolveNumExpresion, new=" + regNum + ", expr=" + exprString);					
		return regNum;
	}

/*	
	private String join(String[] expArray) {
		if (expArray == null) return null;
		String str="";
		for (int i=0; i<expArray.length; i++) {
			str +=  " " + expArray[i];
		}
		return str;
	}
*/
	
	/** split a numeric expression string into a token array and resolve any constants/width functions
	 * 
	 * @param exprString - numeric expression string to be resolved
	 * @return string[] of tokens
	 */
	private String[] tokenizeNumExprString(String exprString) {
		boolean inhibitCheck = inhibitNextResolveCheck;
		inhibitNextResolveCheck = false;
		// split the string into a token list
		exprString = exprString.replace("+", " + "); // pad operators
		exprString = exprString.replace("-", " - ");
		exprString = exprString.replace("*", " * ");
		exprString = exprString.replace("/", " / ");
		exprString = exprString.replace("^", " ^ ");
		exprString = exprString.replace("<<", " << ");
		exprString = exprString.replace(">>", " >> ");
		exprString = exprString.replace("&", " & ");
		exprString = exprString.replace("|", " | ");
		exprString = exprString.replace("(", " ( ");
		exprString = exprString.replace(")", " ) ");
		exprString = exprString.trim();
		//System.out.println("JSpecModelExtractor tokenizeNumExprString: post replace=" + exprString);
		String [] expArray = exprString.split("\\s+");
		//System.out.println("JSpecModelExtractor tokenizeNumExprString: post create, expArray=" + join(expArray));				
		// resolve WIDTH function
		expArray = resolveNumWidths(expArray, inhibitCheck);
		//System.out.println("JSpecModelExtractor tokenizeNumExprString: post width, expArray=" + join(expArray));				
		if (expArray == null) return null;	
		// resolve any constants
		expArray = resolveNumConstants(expArray, inhibitCheck);
		//System.out.println("JSpecModelExtractor tokenizeNumExprString: post const, expArray=" + join(expArray));				
		if (expArray == null) return null;
		return expArray;
	}
	
	/** resolve width functions and return resolved token string array
	 * 
	 * @param expArray - input expession array to be resolved
	 * @return string array with resolved width functions
	 */
	private String[] resolveNumWidths(String[] expArray, boolean inhibitCheck) {
		List<String> newExpArrayList = new ArrayList<String>();
		// loop through string array and generate resolved list
		for (int idx=0; idx<expArray.length; idx++) {  
			String elem = expArray[idx];
			// if width keyword detected in first token, resolve, otherwise error
			if (elem.equals("WIDTH")) {
				//System.out.println("JSpecModelExtractor: tokenizeNumExprString, WIDTH detected, idx=" + idx + ", elem=" + elem + "."); 
				//if (idx == 0) {
					// evaluate width of the function parm
					String constId = expArray[idx + 2];
					// first check the set of defined constants for a match and use it's width if found
					RegNumber constVal = numConstants.get(constId);  // get defined constant value or null if not found
					if ((constVal != null) && constVal.isDefined()) {
						if (!constVal.isDefinedVector()) return null;  // no width defined
						Integer constWidth = constVal.getVectorLen();
						//System.out.println("JSpecModelExtractor: resolveNumWidths, WIDTH() of constant " + constId + " resolved to " + constWidth + "."); 
						elem = constWidth.toString(); // overwrite elem with resolved value 
						// skip past width width tokens
						idx += 3;
					}
					// else check for a valid field/fieldset width
					else {
			        	Integer width = findFieldWidth(constId);
			        	if (width != null) {
							//System.out.println("JSpecModelExtractor: resolveNumWidths, WIDTH() of field/fieldset " + constId + " resolved to " + width + "."); 
							elem = width.toString(); // overwrite elem with resolved value 
							// skip past width tokens
							idx += 3;
			        	}
			        	// no match found so exit with error 
						else {
							if (!inhibitCheck) Ordt.warnMessage("WIDTH(" + constId + ") could not be resolved."); 
							return null;
						}

					}
				//}
				//else {
				//	Jrdl.errorMessage("----> Non-standard WIDTH function could not be resolved.");  
				//	return null;  // WIDTH is only allowed as first token
				//}
			}
			newExpArrayList.add(elem);  //add the token to the list 
		} //for
		
		// return result as a string array
		String [] newExpArray = new String[newExpArrayList.size()];
		int idx=0;
		for (String str: newExpArrayList) newExpArray[idx++] = str;
		return newExpArray;
	}
	
	/** resolve numeric constants and return resloved expression string array
	 * 
	 * @param expArray - input expession array to be resolved
	 * @return string array with resolved width functions
	 */
	private String[] resolveNumConstants(String[] expArray, boolean inhibitCheck) {
		String [] newExpArray = expArray;
		// loop through string array and generate resolved list
		for (int idx=0; idx<expArray.length; idx++) {  
			String elem = expArray[idx];  
			// if not an operator or number then look for a defined constant
			if (!("+-*/()<<>>|&^".contains(elem) || elem.matches("^(0[bx][_0-9A-Fa-f]+|\\d+)")) ) {
				//System.out.println("JSpecModelExtractor: tokenizeNumExprString,    var=" + elem);	
				RegNumber constVal = numConstants.get(elem);  // get defined constant value or null if not found
				// if a defined constant, then update token
				if ((constVal != null) && constVal.isDefined()) {
					//System.out.println("JSpecModelExtractor resolveNumConstants: Constant " + elem + " resolved to " + constVal + ".");  
					newExpArray[idx] = constVal.toString();
				}
				else {
					if (!inhibitCheck) Ordt.errorMessage("Constant " + elem + " could not be resolved.");  
					return null;
				}
			}
		}  // for
		
		// return result as a string array
		return newExpArray;
	}

	
	/** remove escapes from instance/component id strings */
	public static String noEscapes(String ref) {
		String retStr = ref;
		if (ref != null) retStr = ref.replace("\\", "");  // remove escapes
		return retStr;
	}

	/** display children of context */
	@SuppressWarnings("unused")
	private static void displayCtxChildren(ParserRuleContext ctx) {
		System.out.println(Utils.repeat(' ', ctx.depth()) + "node children:" + ctx.getChildCount());
		for (int i=0; i<ctx.getChildCount(); i++ ) {
			System.out.println(Utils.repeat(' ', ctx.depth()) + "   "+ i + ": " + ctx.getChild(i).getText());
				for (int j=0; j<ctx.getChild(i).getChildCount(); j++ ) {
					System.out.println(Utils.repeat(' ', ctx.depth()) + "     "+ j + ": " + ctx.getChild(i).getChild(j).getText());
				}
		}	
	}
	
	/** init list of ignored jspec parameters */
	private static HashSet<String> getIgnoredParameters() {
		HashSet<String> iParms = new HashSet<String>();
		iParms.add("address_alignment");
		iParms.add("address_width");
		iParms.add("addr_decode_instances");
		iParms.add("autogen");
		iParms.add("anonymous");
		iParms.add("macro_mode");
		iParms.add("macro_name");
		iParms.add("output_split_mode");
		iParms.add("output_split_prefix");
		iParms.add("padding");
		iParms.add("repeat_max");
		iParms.add("pio_decoder");
		//iParms.add("union");
		return iParms;
	}

	// ------------------------------------------
	
	/*
	public static void main(String[] args) {
		JSpecModelExtractor mod = new JSpecModelExtractor();
		RegNumber num = mod.resolveNumExpresion("0x1 << 0x3");
		System.out.println("resolved to: " + num);
	}*/

}
