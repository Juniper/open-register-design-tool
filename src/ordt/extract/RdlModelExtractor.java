/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.extract;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import ordt.extract.model.ModAddressableInstance;
import ordt.extract.model.ModComponent;
import ordt.extract.model.ModEnum;
import ordt.extract.model.ModEnumElement;
import ordt.extract.model.ModIndexedInstance;
import ordt.extract.model.ModInstance;
import ordt.extract.model.ModRootComponent;
import ordt.extract.model.ModSignal;
import ordt.parameters.ExtParameters;
import ordt.parameters.Utils;
import ordt.parse.systemrdl.SystemRDLBaseListener;
import ordt.parse.systemrdl.SystemRDLLexer;
import ordt.parse.systemrdl.SystemRDLParser;
import ordt.parse.systemrdl.SystemRDLParser.Component_inst_elemContext;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/** extended SystemRDLBaseListener class (antlr) that extracts component/instance model from parse tree */
public class RdlModelExtractor extends SystemRDLBaseListener implements RegModelIntf{

	private ModRootComponent root = new ModRootComponent();     // the root element of the model
	private HashSet<Integer> activeRules = new HashSet<Integer>();  // hash of active parse rules   
	private Stack<ModComponent> activeCompDefs = new Stack<ModComponent>();   // stack of active component definitions 
	
	private ModInstance activeInstance;   // active RegInstance
	private ModEnum activeEnum;   // active RegEnum
	@SuppressWarnings("unused")
	private ModEnumElement activeEnumElement;   // active RegEnumElement
	private String rdlInputFile;                 // input rdl file name used for model extract 
	
	private PropertyList savedParms = new PropertyList();   // structure for saving parameters for assignment to comp/instances
	
	private static int anonCompId = 0;   // id for anonymous components
	
	private InstanceRef rhsInstanceRef = null;  // rhs ref info for assignment checking
	
	private String usrPropertyName, usrPropertyType, usrPropertyDefault;  // temp vars for capturing user defined properties
	private List<String> usrPropertyComponents;

	// structures to pre-build a list of user-defined signals
	private List<ModSignal> usrSignals = new ArrayList<ModSignal>();  // list of all signals in the model
	private HashSet<String> usrSignalNames = new HashSet<String>();  // full list of extracted signal names

	private Stack<Integer> fieldOffsets = new Stack<Integer>(); // stack of offsets used to calculate fieldset widths

	/** create data model from rdl file 
	 * @param rdlFile to be parsed
	 * @param moduleName to be used as default name for addrmap instances
	 * */
	public RdlModelExtractor (String rdlFile) {
        this.rdlInputFile = rdlFile;  // save rdl file
       
        try {
        	InputStream is = System.in;
        	if ( rdlFile!=null ) is = new FileInputStream(rdlInputFile);
        
        	ANTLRInputStream input = new ANTLRInputStream(is);
        	SystemRDLLexer lexer = new SystemRDLLexer(input);

        	// check input post lexer
        	//List<? extends Token> toks = lexer.getAllTokens();
        	//for(Token t: toks)
        	//    System.out.println(t);
        	//System.out.println("tokenizer size=" + toks.size());

        	CommonTokenStream tokens = new CommonTokenStream(lexer);

        	// check input post tokenizer
        	//List<Token> toks2 = tokens.getTokens();
        	//System.out.println("buffered size=" + toks2.size());

        	SystemRDLParser parser; // = new SystemRDLParser(null);
        	parser = new SystemRDLParser(tokens);

        	ParseTree tree = parser.root(); //compilationUnit(); // parse
        	//System.out.println(tree.toStringTree());
        	ParseTreeWalker walker = new ParseTreeWalker(); // create standard
        	walker.walk(this, tree); // initiate walk of tree with listener
        	if (parser.getNumberOfSyntaxErrors() > 0) {
        		Ordt.errorExit("RDL parser errors detected.");  
        	}
        	
        	// if components are specified for processing, find each and create an instance  
        	if (ExtParameters.hasRdlProcessComponents()) processComponents();
        	else if (root.getFirstChildInstance() == null)
        		Ordt.errorExit("No rdl structures instanced or typedefs specified for processing.");

        	//root.display(true);

        } catch (FileNotFoundException e) {
        	Ordt.errorExit("rdl file not found. "  + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}
	

	/** if comps are specified for processing, find each and create an instance  */  
	private void processComponents() {
   	    List<String> typedefNames = ExtParameters.getRdlProcessComponents();
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
    			//processComp.addInstanceOf(newInstance);
    			//System.out.println("RdlModelExtract processComponents: id=" + typedefName + ", isRootInstance=" + newInstance.isRootInstance());
    		}
    		else Ordt.errorExit("Unable to find specified component (" + typedefName + ") for processing.");        			
			
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
        		else Ordt.errorExit("Unable to find specified component (" + typedefName + ") for processing.");        			
    		}
    	}
	}

	//----------------------------------begin SystemRDLBaseListener overrides-----------------------------------
	
	/*
	 		anonymous_component_inst_elems
	 		  : (external_clause)?
	 		    component_inst_elem
	 		    (COMMA component_inst_elem)*
	 		  ;
	 */
	@Override public void enterAnonymous_component_inst_elems(@NotNull SystemRDLParser.Anonymous_component_inst_elemsContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		//System.out.println(repeat(' ', ctx.depth()) + "anonymous_comp_instance_elem (d=" + ctx.depth() + ") " + ctx.getText());
		savedParms.clear();
		// check for external modifier
		String firstToken = ctx.getChild(0).getText();
        if ("external_decode".equals(firstToken)) {     // save the external tag (use in component_inst_elem)
        	savedParms.setProperty("external", "EXTERNAL_DECODE");  // external_decode is handled as external special case
        }
        else if (firstToken.startsWith("external")) {     // save the external tag (use in component_inst_elem)
        	// if a special ext type is specified use it
        	if (ctx.getChild(0).getChildCount() > 2) savedParms.setProperty("external", ctx.getChild(0).getChild(2).getText());
        	else savedParms.setProperty("external", "DEFAULT");
        }
	}
	
	/**
	 * exit anonymous instance elem.
	 */
	@Override public void exitAnonymous_component_inst_elems(@NotNull SystemRDLParser.Anonymous_component_inst_elemsContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/*
	component_def
	  : ( 'addrmap' | 'regfile' | 'regset' |  'reg' | 'field' | 'signal' )
	    ( id
	    |
	    )
	    LBRACE
	      ( component_def
	      | explicit_component_inst
	      | property_assign
	      | enum_def
	      )*
	    RBRACE
	    ( anonymous_component_inst_elems )?
	    SEMI
	  ;*/

	/** Extract component definition
	 */
	@Override public void enterComponent_def(@NotNull SystemRDLParser.Component_defContext ctx) {
		activeRules.add(ctx.getRuleIndex());
		
		//System.out.println(repeat(' ', ctx.depth()) + "--> (" + ctx.depth() + ") " + ctx.getText());
		//System.out.println(repeat(' ', ctx.depth()) + "   par=" + ctx.getParent());
		
		// build structure by type
		String cd_type = ctx.getChild(0).getText();  // get component type
		String secondToken = ctx.getChild(1).getText();
		ModComponent rElem = ModComponent.createModComponent(cd_type);  
		//System.out.println("RdlModelExtractor enterComponent_def: line=" + ctx.getStart().getLine() + ", type=" + cd_type + ", 2ndtoken=" + secondToken);
		if (rElem != null) {
			if (rElem.isFieldSet()) {
				// initialize accumulated child widths for this fieldset component at zero 
				// these are relative offsets - final values resolved at builder
				fieldOffsets.push(0); // init fieldOffset for this fieldset
			}

			// set id of new component if specified
			if (!"{".equals(secondToken)) rElem.setId(secondToken);
			// otherwise give this anonymous component an id
			else rElem.setId("aNON" + anonCompId++);
			
			// save line number of component definition
			rElem.setInputLineNumber(ctx.getStart().getLine()); 

			// add as child of parent
			ModComponent activeParent = activeCompDefs.peek();   // snoop the stack to get parent component if any
			activeCompDefs.push(rElem);   // push onto the active definition stack
			if (activeParent != null) {
				activeParent.addChildComponent(rElem);
				rElem.setParent(activeParent);
				rElem.updateDefaultProperties(activeParent.getDefaultProperties());  // pick up defaults from parent
			}
						
			// save list of defined signals - will need this to resolve rdl signals vs fields in assignments
			if ("signal".equals(cd_type)) {
				usrSignals.add((ModSignal) rElem);
				//System.out.println("RdlModelExtract enterComponent_def: added signal comp id=" + rElem.getId());
			}
			//System.out.println(repeat(' ',ctx.depth()) + "  added new " + cd_type);
			//System.out.println("RdlModelExtractor enterComponent_def: new comp id=" + rElem.getId() + ", type=" + rElem.getBaseComponentTypeName());
		}
		else {
			Ordt.errorExit("component_def type=" + cd_type + " not implemented");
		}

	}
	
	/**
	 *   component creation is complete 
	 */
	@Override public void exitComponent_def(@NotNull SystemRDLParser.Component_defContext ctx) {
		activeRules.remove(ctx.getRuleIndex());
		//System.out.println("RdlModelExtractor exitComponent_def: line=" + ctx.getStart().getLine() + ", " + ctx.getText());
		
		ModComponent pe = activeCompDefs.pop();   // pop the stack since this definition is done
		
		// if a fieldstruct then save accumulated size
		if ((pe != null) && pe.isFieldSet()) {
			Integer childOffset = fieldOffsets.pop(); // grab the accumulated width of this fieldset from child offset
			Integer currentWidth = pe.getIntegerProperty("fieldstructwidth");
			// if accumulated width is bigger than current (if defined) then update
			if (currentWidth == null)
			    pe.setProperty("fieldstructwidth", childOffset.toString(), 0);  // save the fieldset width as a property in the component
			else if (childOffset>currentWidth)
				Ordt.errorExit("Specified fieldstructwidth (" + currentWidth + ") in fieldstruct " + pe.getId() + " is smaller than total of child widths (" + childOffset + ")");
			else
				pe.setProperty("fieldstructwidth", currentWidth.toString(), 0);  // save the fieldset width as a property in the component
			//System.out.println("RdlModelExtractor exitComponent_def: Fieldstruct " + pe.getId() + ", width=" + pe.getIntegerProperty("fieldstructwidth")); 

		}
		
		//if (pe != null) {
		//	System.out.println(repeat(' ',ctx.depth()) + "  created " + pe.getClass() + ", sub-defs=" + pe.getCompDefinitions().size() 
		//			+ ", sub-insts=" + pe.getCompInstances().size() + ", id=" + pe.getId());			
		//}
	}

	/*
	        component_inst_elem
	          : id
	            (array)?
	            (EQ  num)?   // reset
	            (AT  num)?   // address
	            (INC num)? //addr inc
	            (MOD num)?  //addr mod
	          ;
	 */
	@Override public void enterComponent_inst_elem(@NotNull SystemRDLParser.Component_inst_elemContext ctx) {
		activeRules.add(ctx.getRuleIndex());
		//System.out.println("RdlModelExtractor enterComponent_inst_elem: text=" + ctx.getText());
		//displayCtxChildren(ctx);

		// set compoent being instanced and parent based on inst type
		String compId = "";
		ModComponent modComp;  // component is the one being actively defined 
		ModComponent parentDef;  // parent compenent of this instance
		// if an anonymous component instance, then add to parent of current element
		if (activeRules.contains(SystemRDLParser.RULE_anonymous_component_inst_elems)) {
			modComp = activeCompDefs.peek();  // component is the one being actively defined 
			parentDef = modComp.getParent();
		}
		// an explicit comp instance so add to current active comp definintion 
		else {
			// find the specified component in scope
			compId = savedParms.getProperty("compId");  // get saved component id (used for multiple instance elems)
			modComp = activeCompDefs.peek().findCompDef(compId);  // find this component in scope
			parentDef = activeCompDefs.peek();  // parent is the active comp def
		}
		
		// create a new instance
		if (modComp != null) {
			activeInstance = modComp.createNewInstance();  
			activeInstance.updateProperties(savedParms.getProperties());   // add external, internal, alias_id settings from anon_comp_inst and expl_comp_inst
			activeInstance.setInputLineNumber(ctx.getStart().getLine());
			// extract parameters and update instance
			activeInstance.setId(ctx.getChild(0).getText());  // set id to first child
			extractInstanceAddressInfo(ctx);  // update activeInstance TODO - need to split into addressable/indexed
			//activeInstance.display();

			if (parentDef != null) {
				parentDef.addCompInstance(activeInstance);   // not anonymous, so add to currently active definition
				activeInstance.setParent(parentDef);  // instance is referenced by currently active comp definition 
				activeInstance.updateProperties(modComp.getProperties());   //  properties from component type of this instance
				activeInstance.updateProperties(parentDef.getDefaultProperties());   // default properties from ancestors 
			}
		}
		else Ordt.errorMessage("unable to find component " + compId + ((parentDef != null) ? " instanced in " + parentDef.getId() : ""));

	}
	
	/**
	 * exit Component_inst_elem
	 */
	@Override public void exitComponent_inst_elem(@NotNull SystemRDLParser.Component_inst_elemContext ctx) {
		activeRules.remove(ctx.getRuleIndex());
		//System.out.println(repeat(' ',ctx.depth()) + "  created RegInstance, id=" + activeInstance.getId() + ", rcomp=" + activeInstance.getRegComp());			
		activeInstance = null;
	}
	
	/**
	 * Concat
	 */
	@Override public void enterConcat(@NotNull SystemRDLParser.ConcatContext ctx) { 
		Ordt.warnMessage("concat not implemented");
	}
	
	/**
	 * exit Concat
	 */
	@Override public void exitConcat(@NotNull SystemRDLParser.ConcatContext ctx) { 
	}
	
	/*
	default_property_assign
	  : 'default'
	    explicit_property_assign
	  ;		
	 */
	@Override public void enterDefault_property_assign(@NotNull SystemRDLParser.Default_property_assignContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		//System.out.println(repeat(' ', ctx.depth()) + "default_property_assign (d=" + ctx.depth() + ") " + ctx.getText());	
	}

	/**
	 * exit Property_assign
	 */
	@Override public void exitDefault_property_assign(@NotNull SystemRDLParser.Default_property_assignContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/*
		enum_def
		  : 'enum' id
		    enum_body
		    SEMI
		  ;
	 */
	@Override public void enterEnum_def(@NotNull SystemRDLParser.Enum_defContext ctx) {  
		activeRules.add(ctx.getRuleIndex());
		//System.out.println(repeat(' ', ctx.depth()) + "--> (" + ctx.depth() + ") " + ctx.getText());
		
		// create a new enum
		activeEnum = new ModEnum();   
		activeEnum.setId(ctx.getChild(1).getText());  // id is in child(1)
		// add as child of parent component
		ModComponent activeParent = activeCompDefs.peek();   // snoop the stack to get parent component
		if (activeParent != null) {
			activeParent.addCompEnum(activeEnum);
			activeEnum.setParent(activeParent);
		}			
	}
	
	/**
	 * exit Enum_def
	 */
	@Override public void exitEnum_def(@NotNull SystemRDLParser.Enum_defContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}

	/*
		enum_entry
		  : id
		    EQ num
		    ( LBRACE (enum_property_assign)* RBRACE )?   <= omly name and description
		    SEMI
		  ;
    */
	@Override public void enterEnum_entry(@NotNull SystemRDLParser.Enum_entryContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		//System.out.println(repeat(' ', ctx.depth()) + "enum_entry (d=" + ctx.depth() + ") " + ctx.getText());	

		// create a new enumElement
		ModEnumElement newEnumElement = new ModEnumElement();
		// extract id and value of this element
		if (ctx.getChildCount()<3) Ordt.errorExit("Error parsing enum near line " + ctx.getStart().getLine());
		newEnumElement.setId(ctx.getChild(0).getText());
		newEnumElement.setValue(ctx.getChild(2).getText()); 
		// extract name and description
		//displayCtxChildren(ctx);
		int propCount = ctx.getChildCount() - 6;  // get number of enum_property_assigns
		for (int i=4; i<propCount+4; i++) {
			if (ctx.getChild(i).getChild(0).getText().equals("name")) 
				newEnumElement.setName(ctx.getChild(i).getChild(2).getText().replace("\"", ""));
			if (ctx.getChild(i).getChild(0).getText().equals("desc")) 
				newEnumElement.setDesc(ctx.getChild(i).getChild(2).getText().replace("\"", ""));
		}
		// add as child of parent enum
		if (activeEnum != null) {
			activeEnum.addEnumElement(newEnumElement);
		}			
		activeEnumElement = newEnumElement;  // save as current active enum element
	}

	/**
	 * exit Enum_entry
	 */
	@Override public void exitEnum_entry(@NotNull SystemRDLParser.Enum_entryContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}

	/** fires on all rules - for debug */
    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        //System.out.println("exit "+ctx.getRuleIndex());
    }

	/*
		explicit_property_assign 
		  : property_modifier   
		    property
		  | property
		    ( EQ property_assign_rhs )?   // added ? here to allow properties w/o an EQ
		  ;	
	 */
	@Override public void enterExplicit_property_assign(@NotNull SystemRDLParser.Explicit_property_assignContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		//System.out.println(repeat(' ', ctx.depth()) + "explicit_property_assign (d=" + ctx.depth() + ") " + ctx.getText());	
		//System.out.println("RegExtractor: explicit_property_assign, " + ctx.getText());	
		//System.out.println(repeat(' ', ctx.depth()) + "   " + ctx + "   " + ctx.getRuleIndex()+ "   " + SystemRDLParser.ruleNames[ctx.getRuleIndex()]);	
		//displayCtxChildren(ctx);

		// extract property name/value and add to component properties
		String property = ctx.getChild(0).getText();
		// boolean property assignment
		if (ctx.getChildCount() == 1)                       // boolean property
			if (activeRules.contains(SystemRDLParser.RULE_default_property_assign))  // default
			   activeCompDefs.peek().setDefaultProperty(property, "true");  
			else
			   activeCompDefs.peek().setProperty(property, "true", 0);  
		// property with assigned value
		else if ("=".equals(ctx.getChild(1).getText()))    // property with assigned value
			if (activeRules.contains(SystemRDLParser.RULE_default_property_assign))
			   activeCompDefs.peek().setDefaultProperty(property, ctx.getChild(2).getText().replace("\"",""));  
			else
			   activeCompDefs.peek().setProperty(property, ctx.getChild(2).getText().replace("\"",""), 0);  
		// property with property modifier (only applies to intr property which uses posedge/negedge/bothedge/level)
		else if ("intr".equals(ctx.getChild(1).getText())) {
			String modifier = ctx.getChild(0).getText();  // save the modifier
			if (activeRules.contains(SystemRDLParser.RULE_default_property_assign)) {  // if default property
				   activeCompDefs.peek().setDefaultProperty("intr", "true");  
				   activeCompDefs.peek().setDefaultProperty(modifier, "true");  // save the modifier also
			}
			else {  
				   activeCompDefs.peek().setProperty("intr", "true", 0);  
				   activeCompDefs.peek().setProperty(modifier, "true", 0);  // save the modifier also
			}
			
		}
		else Ordt.warnMessage("explicit property assignment form not supported: " + ctx.getText());   
	}
	
	/**
	 * exit Explicit_property_assign
	 */
	@Override public void exitExplicit_property_assign(@NotNull SystemRDLParser.Explicit_property_assignContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/*
	 		explicit_component_inst
			  : ( external_clause 
			    | 'internal' 
			    | 'alias' id )?
	 		    id                 
	 		    component_inst_elem
	 		    (COMMA component_inst_elem)*
	 		    SEMI
	 		  ;
	 */
	@Override public void enterExplicit_component_inst(@NotNull SystemRDLParser.Explicit_component_instContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		//System.out.println(repeat(' ', ctx.depth()) + "explicit_component_inst (d=" + ctx.depth() + ") " + ctx.getText());	
		
		savedParms.clear();
		// check for external/alias modifiers
		String firstToken = ctx.getChild(0).getText();
		String secondToken = ctx.getChild(1).getText();
        if ("external_decode".equals(firstToken)) {    // save the internal tag and component id
        	savedParms.setProperty("external", "EXTERNAL_DECODE");  // external_decode is handled as external special case
        	savedParms.setProperty("compId", secondToken);
        }
        else if (firstToken.startsWith("external")) {    // save the external tag and component id
        	savedParms.setProperty("compId", secondToken);
        	// if a special ext type is specified use it
        	if (ctx.getChild(0).getChildCount() > 2) savedParms.setProperty("external", ctx.getChild(0).getChild(2).getText());
        	else savedParms.setProperty("external", "DEFAULT");
        }
        else if ("internal".equals(firstToken)) {    // save the internal tag and component id
        	//savedParms.setProperty("internal", "true");
        	savedParms.setProperty("compId", secondToken);
        }
        else if ("alias".equals(firstToken)) {    // save the aliased instance and component id
        	// check for valid alias instance
			String aliasId = noEscapes(secondToken);
        	if (activeCompDefs.peek().findLocalInstance(aliasId) != null) {
        		savedParms.setProperty("aliasedId", aliasId);   // pass aliasedId tag to instance(s)
        		savedParms.setProperty("compId", ctx.getChild(2).getText());  // compId is third token
    			//Jrdl.infoMessage("found alias target instance " + aliasId + " in local context");   
        	}
        	else {
    			Ordt.errorMessage("unable to find alias target instance " + aliasId + " in local context");
        	}
        }
        // otherwise save the component Id
        else savedParms.setProperty("compId", firstToken);
		
	}
	
	/**
	 * end Explicit_component_inst
	 */
	@Override public void exitExplicit_component_inst(@NotNull SystemRDLParser.Explicit_component_instContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());	
	}
	
	/**
	 * enterInstance_ref
	 */
	@Override public void enterInstance_ref(SystemRDLParser.Instance_refContext ctx) {
		activeRules.add(ctx.getRuleIndex());
		// if this reg is on rhs of an assign, save it
		if (activeRules.contains(SystemRDLParser.RULE_post_property_assign) &&
			activeRules.contains(SystemRDLParser.RULE_property_assign_rhs)) {
		   rhsInstanceRef = new InstanceRef(ctx, false);
		}
	}

	/**
	 * exitInstance_ref
	 */
	@Override public void exitInstance_ref(SystemRDLParser.Instance_refContext ctx) {
		activeRules.remove(ctx.getRuleIndex());
	}
    	
	/*
		post_property_assign   
           : instance_ref ( EQ property_assign_rhs )
           | simple_instance_ref ( EQ verilog_expression )  // allow vlog expression use in signal assigns 
		  ;
	 */
	@Override public void enterPost_property_assign(@NotNull SystemRDLParser.Post_property_assignContext ctx) {
		activeRules.add(ctx.getRuleIndex());
		//System.out.println("RdlModelExtractor enterPost_property_assign: " + ctx.getText());
		//displayCtxChildren(ctx);
		int postPropertyAssignChildren = ctx.getChildCount();
		InstanceRef lhsInstanceRef = new InstanceRef(ctx.getChild(0), true);
		String property = lhsInstanceRef.getProperty();
		String instPathStr = lhsInstanceRef.getInstPathStr();
		rhsInstanceRef = null;  // clear out the rhsRef
		
		//System.out.println("RdlModelExtractor enterPost_property_assign: dref instance " + instPathStr + ", property=" + property + ", wildcards=" + lhsInstanceRef.hasWildcard()); 
		// exit with error if an array indexed value is used on lhs	
		if ((instPathStr!=null) && instPathStr.contains("[")) {
			Ordt.errorMessage("property assignment using indexed values in lhs not supported: " + ctx.getText());
			return;
		}
		
		// if an instance ref of form: path -> property
		if (lhsInstanceRef.hasDeRef()) { 
			// search for instance having this path in the current component definition
			ModComponent comp = activeCompDefs.peek();
			//System.out.println("RegExtractor: post prop assign active component =" + comp.getId()); 
			ModInstance regInst = comp.findInstance(lhsInstanceRef.getInstPath());   // search for specified instance recursively  
			if (regInst == null)
				Ordt.errorMessage("unable to find lhs instance in property assignment: " + ctx.getText());
		    // if this instance is found 
			else {
				//System.out.println("RdlModelExtractor enterPost_property_assign: found lhs instance "+ instPathStr); 
				// if no field wildcard then assign value to this instance
				if (!lhsInstanceRef.hasWildcard()) {
					String rhsValue = (postPropertyAssignChildren>1) ? noEscapes(ctx.getChild(2).getText().replace("\"","")) : "true"; // check for right hand assignment
					comp.addParameter(instPathStr, property, rhsValue);
				}
				// field wildcard, so assign to all child instances
				else {
					ModComponent regComp = regInst.getRegComp();
					for (ModInstance regI : regComp.getChildInstances()) {
						//	System.out.println("setting property=" + property + " to " + ctx.getChild(2).getText() + ", was " + regInst.getProperty(property)+ ", in " + regInst.getId());
						String rhsValue = (postPropertyAssignChildren>1) ? noEscapes(ctx.getChild(2).getText().replace("\"","")) : "true"; 
						String wildcardInstPathStr = (instPathStr.length() > 0) ? instPathStr + "." + regI.getId() : regI.getId();
						comp.addParameter(wildcardInstPathStr, property, rhsValue);
					}
				}
			}
		}
		
		// parse instance ref of form: instance_ref_elem(or *)+ (treat as a signal assign)
		else if (lhsInstanceRef.isValid()) { 
			//System.out.println("RdlModelExtractor enterPost_property_assign: sig instance " + instPathStr + ", property=" + property); 

			// search for instance having this path in the current component definition
			ModComponent comp = activeCompDefs.peek();
			//System.out.println("RegExtractor: post prop assign active component =" + comp.getId()); 
			ModInstance regInst = comp.findInstance(lhsInstanceRef.getInstPath());   // search for specified instance recursively  
			if (regInst == null)
				Ordt.errorMessage("unable to find lhs instance or property in assignment: " + ctx.getText());
		    // if this instance is found in local component then save the prop assignment
			else {
				//System.out.println("RdlModelExtractor enterPost_property_assign: found lhs instance "+ instPathStr); 
				String rhsValue = (postPropertyAssignChildren>1) ? noEscapes(ctx.getChild(2).getText().replace("\"","")) : "true"; // check for right hand assignment
				comp.addParameter(instPathStr, property, rhsValue);
			}
		}

		// unsupported 
		else {
			Ordt.warnMessage("instance_ref " + ctx.getChild(0).getText() + " not supported");			
		}
	}


	/**
	 * exit Post_property_assign
	 */
	@Override public void exitPost_property_assign(@NotNull SystemRDLParser.Post_property_assignContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
		// check for valid rhs references
		if ((rhsInstanceRef != null) && rhsInstanceRef.isValid()) {
			//System.out.println("RdlModelExtractor exitPost_property_assign: instance " + rhsInstanceRef.getInstPathStr() + ", property=" + rhsInstanceRef.getProperty() + ", wildcards=" + rhsInstanceRef.hasWildcard()); 
			ModComponent comp = activeCompDefs.peek();
			//System.out.println("RegExtractor: post prop assign active component =" + comp.getId()); 
			ModInstance regInst = comp.findInstance(rhsInstanceRef.getInstPath());   // search for specified instance recursively  
			if (regInst == null)
				Ordt.errorMessage("unable to find rhs instance in dynamic property assignment: " + ctx.getText());
			//else
			//	Jrdl.infoMessage("found rhs instance in dynamic property assignment: " + ctx.getText());
		}
	}
	
	/**
	 * Property_assign
	 */
	@Override public void enterProperty_assign(@NotNull SystemRDLParser.Property_assignContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
	}
	
	/**
	 * exit Property_assign
	 */
	@Override public void exitProperty_assign(@NotNull SystemRDLParser.Property_assignContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/**
	 * enterProperty_assign_rhs
	 */
	@Override public void enterProperty_assign_rhs(SystemRDLParser.Property_assign_rhsContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
	}
	
	/**
	 * exitProperty_assign_rhs
	 */
	@Override public void exitProperty_assign_rhs(SystemRDLParser.Property_assign_rhsContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}

	/**
	 * Property_definition
	 */
	@Override public void enterProperty_definition(@NotNull SystemRDLParser.Property_definitionContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		//System.out.println("RdlModelExtractor enterProperty_definition: " + ctx.getText());
		usrPropertyName = ctx.getChild(1).getText();  // save prop name
		// init type, defauld, components for this property
		usrPropertyType = null;
		usrPropertyDefault = null;
		usrPropertyComponents = new ArrayList<String>();
	}
	
	/**
	 * exit Property_definition
	 */
	@Override public void exitProperty_definition(@NotNull SystemRDLParser.Property_definitionContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());	
		//System.out.println("RdlModelExtractor exitProperty_definition: name=" + usrPropertyName + ", type=" + usrPropertyType + ", default=" + usrPropertyDefault+ ", comps=" + usrPropertyComponents);
        // add the new property to defined list
		DefinedProperties.addUserProperty(usrPropertyName, usrPropertyType, usrPropertyDefault, usrPropertyComponents);
	}

	/**
	 * set user defined property type
	 */
	@Override public void enterProperty_type(SystemRDLParser.Property_typeContext ctx) { 	
		activeRules.add(ctx.getRuleIndex());
		//System.out.println("RdlModelExtractor enterProperty_type: " + ctx.getText());
		usrPropertyType = ctx.getChild(2).getText();  // save prop type
	}
	
	@Override public void exitProperty_type(SystemRDLParser.Property_typeContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/**
	 * set user defined property default value
	 */
	@Override public void enterProperty_default(SystemRDLParser.Property_defaultContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		//System.out.println("RdlModelExtractor enterProperty_default: " + ctx.getText());
		usrPropertyDefault = ctx.getChild(2).getText();  // save prop default
	}
	
	@Override public void exitProperty_default(SystemRDLParser.Property_defaultContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}
	
	/**
	 * set user defined property usage
	 * 'component' EQ property_component (OR property_component)* SEMI
	 */
	@Override public void enterProperty_usage(SystemRDLParser.Property_usageContext ctx) { 
		activeRules.add(ctx.getRuleIndex());
		//System.out.println("RdlModelExtractor enterProperty_usage: " + ctx.getText());
		int numComponents = (ctx.getChildCount() - 1) / 2;
		for (int cnum = 1; cnum<=numComponents; cnum++) {
			usrPropertyComponents.add(ctx.getChild(cnum*2).getText());  // save each prop component			
		}
	}
	
	@Override public void exitProperty_usage(SystemRDLParser.Property_usageContext ctx) { 
		activeRules.remove(ctx.getRuleIndex());
	}

	/**
	 *  initialize root element
	 */
	@Override public void enterRoot(@NotNull SystemRDLParser.RootContext ctx) { 
		// add the root element
		root = (ModRootComponent) ModComponent.createModComponent("root");
		activeCompDefs.push(root);   // push onto the active definition stack
	}
	
	/**
	 *  exiting file 
	 */
	@Override public void exitRoot(@NotNull SystemRDLParser.RootContext ctx) { 
		// create list of defined signals recursively from each leaf instance since signal defines are sparse
		for (ModSignal sig: usrSignals)
			sig.getDefinedSignalNames(usrSignalNames); 
		//System.out.println("RdlModelExtract exitRoot: found " + usrSignals.size() + " defined signal nodes with " + usrSignalNames.size() + " defined signals");
		//for (String sigName: usrSignalNames)
			//System.out.println("RdlModelExtract exitRoot: found signal " + sigName);
		//if (sigName.contains("int_detected_cas_tx_afifo2_mem_0")) System.out.println("RdlModelExtract exitRoot: found signal " + sigName);
	}

	/**
	 * Detect unimplemented properties
	 */
	@Override public void enterUnimplemented_property(@NotNull SystemRDLParser.Unimplemented_propertyContext ctx) {
		Ordt.warnMessage("property " + ctx.getText() + " not implemented");
	}

	// ------------------- inline parameter define methods
	
	/**
	 * Assign global parameters
	 */
	@Override public void enterGlobal_parm_assign(@NotNull SystemRDLParser.Global_parm_assignContext ctx) {
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());
	}
	
	/**
	 * Assign rdl input parameters
	 */
	@Override public void enterRdl_in_parm_assign(@NotNull SystemRDLParser.Rdl_in_parm_assignContext ctx) { 
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());			
	}
	
	/**
	 * Assign systemverilog output parameters
	 */
	@Override public void enterSystemverilog_out_parm_assign(@NotNull SystemRDLParser.Systemverilog_out_parm_assignContext ctx) {
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign rdl output parameters
	 */
	@Override public void enterRdl_out_parm_assign(@NotNull SystemRDLParser.Rdl_out_parm_assignContext ctx) { 
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());				
	}
	
	/**
	 * Assign jspec output parameters
	 */
	@Override public void enterJspec_out_parm_assign(@NotNull SystemRDLParser.Jspec_out_parm_assignContext ctx) {
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign reglist output parameters
	 */
	@Override public void enterReglist_out_parm_assign(@NotNull SystemRDLParser.Reglist_out_parm_assignContext ctx) {
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign uvmregs output parameters
	 */
	@Override public void enterUvmregs_out_parm_assign(@NotNull SystemRDLParser.Uvmregs_out_parm_assignContext ctx) {
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign bench output parameters
	 */
	@Override public void enterBench_out_parm_assign(@NotNull SystemRDLParser.Bench_out_parm_assignContext ctx) {
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}
	
	/**
	 * Assign xml output parameters
	 */
	@Override public void enterXml_out_parm_assign(SystemRDLParser.Xml_out_parm_assignContext ctx) {
		ExtParameters.assignParameter(ctx.getChild(0).getText(), ctx.getChild(2).getText());		
	}

	/**
	 * Capture annotation command  
		 annotation_command
		   : ('set_reg_property' | 'set_field_property')
		     ID EQ STR
		     ('instances' | 'components')
		     STR
	 */
	@Override public void enterAnnotation_command(@NotNull SystemRDLParser.Annotation_commandContext ctx) {
		ExtParameters.processAnnotationCommand(ctx);
	}

    // ------------------------------ RegModelExtractor interface methods --------------------------------------
	
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
		return rdlInputFile;
	}

	/** return true if field offsets are relative to zero or max reg/fieldset width (rdl=true, jspec=false) **/
	public boolean fieldOffsetsFromZero() {
		return true;
	}

	@Override
	/** return true if specified name is a user-defined signal name */
	public boolean isUserDefinedSignal(String name) {
		return usrSignalNames.contains(name);  
	}

    // ------------------------------------------------------------------------------------------------
	
	/** inner class for carrying extracted rdl instance_ref info */
    class InstanceRef {
    	private boolean isValid = false;
    	private boolean hasWildcard = false;
    	private boolean hasDeRef = false;
    	private String property = null;  // ref is invalid if null
		private List<String> instPath = new ArrayList<String>();
  	
		public InstanceRef(ParseTree instRefTree, boolean isLhs) {
			// extract deRef, property
			int instanceRefChildren = instRefTree.getChildCount();
			// parse an instance ref of form: instance_ref_elem(or *)+ -> property
			if ((instanceRefChildren>=3) && "->".equals(instRefTree.getChild(instanceRefChildren-2).getText())) { // DREF is at size - 2
				isValid = true;
				hasDeRef = true;
				property = instRefTree.getChild(instanceRefChildren-1).getText();  // set the property from deref
				// build an instance path list and detect field wildcards
				for (int idx=0; idx<instanceRefChildren-2; idx += 2) {
					String instRef = instRefTree.getChild(idx).getText();
					if ("*".equals(instRef)) hasWildcard = true;  // mark field wildcard, but dont save instref
					else instPath.add(noEscapes(instRef));
				}
			}
			// else parse instance ref of form: instance_ref_elem(or *)+ - treat as a signal assign
			else if (instanceRefChildren>=1) { 
				isValid = isLhs || (instanceRefChildren>1);  // for now rhs singletons are marked is invalid to inhibit checks
				property = isLhs? "signalAssign" :  null;  // property is signal assignment if lhs and no deRef				
				// build an instance path list 
				for (int idx=0; idx<instanceRefChildren; idx += 2) {
					String instRef = instRefTree.getChild(idx).getText();
					if ("*".equals(instRef)) hasWildcard = true;  // mark field wildcard, but dont save instref
					else instPath.add(noEscapes(instRef));
				}
			}
		}
		
		public boolean isValid() {
			return isValid;
		}
		
		public boolean hasWildcard() {
			return hasWildcard;
		}
		
		public boolean hasDeRef() {
			return hasDeRef;
		}
		
		public String getProperty() {
			return property;
		}
		
		public List<String> getInstPath() {
			return instPath;
		}
		
		public String getInstPathStr() {
			return toPath(instPath);
		}
   	
    }
    
    // ------------------------------------------------------------------------------------------------
	
	/** update activeInstance with extracted address/repcount info
	 * 
	 * @param ctx - instance element context
	 */
	private void extractInstanceAddressInfo(Component_inst_elemContext ctx) {
		// loop through ctx children, set address/reset parms, and count processed children
		//System.out.println("RdlModelExrtactor extractInstanceAddressInfo: ctx=" + ctx.getText() + ", childCount="+ ctx.getChildCount());
		int childrenFound = 1;  // init to 1 for id 
		for (int idx=1; idx<ctx.getChildCount(); idx++) {
			if ("=".equals(ctx.getChild(idx).getText())) {
				activeInstance.setProperty("reset", ctx.getChild(idx+1).getText(), 0); childrenFound += 2;
			}
			else if ("@".equals(ctx.getChild(idx).getText())) {
				if (activeInstance.isAddressable()) ((ModAddressableInstance) activeInstance).setAddress(ctx.getChild(idx+1).getText()); 
				childrenFound += 2;
			}
			else if (">>".equals(ctx.getChild(idx).getText())) {
				if (activeInstance.isAddressable()) ((ModAddressableInstance) activeInstance).setAddressShift(ctx.getChild(idx+1).getText()); 
				childrenFound += 2;
			}
			else if ("+=".equals(ctx.getChild(idx).getText())) {
				if (activeInstance.isAddressable()) ((ModAddressableInstance) activeInstance).setAddressIncrement(ctx.getChild(idx+1).getText()); 
				childrenFound += 2;
			}
			else if ("%=".equals(ctx.getChild(idx).getText())) {
				if (activeInstance.isAddressable()) ((ModAddressableInstance) activeInstance).setAddressModulus(ctx.getChild(idx+1).getText()); 
				childrenFound += 2;
			}
		}
		// extract array values if they exist after processing address/reset assigns    array [ nnn : nnn ] 
		//System.out.println("RdlModelExrtactor extractInstanceAddressInfo: childrenFound="+ childrenFound);
		if (ctx.getChildCount()>childrenFound) {
			Integer leftIdx = Utils.numStrToPosInteger(ctx.getChild(1).getChild(1).getText(), " in instance " + activeInstance.getId());
			// save repcount (fieldsets not allowed in rdl so only addressable can be replicated)
			if (activeInstance.isAddressable()) 
				activeInstance.setRepCount(leftIdx); // left index will set repcount  
			// otherwise if a field or signal extract width/offset
			else if (activeInstance.isIndexed()) {
				ModIndexedInstance activeInst = ((ModIndexedInstance) activeInstance);
				//System.out.println("RdlModelExtractor extractInstanceAddressInfo: getBaseComponentTypeName=" + activeInstance.getRegComp().getBaseComponentTypeName() );

				// if a second index, set explicit offset and width
				if (ctx.getChild(1).getChildCount()>3) {
					Integer rightIdx = Utils.numStrToPosInteger(ctx.getChild(1).getChild(3).getText(), " in instance " + activeInst.getId());
					activeInst.setWidth(leftIdx - rightIdx + 1);  // TODO assumes lsb0						
					activeInst.setOffset(rightIdx);  						
				}
				// otherwise set width only if field/reps only if fieldstruct, offset is unknown
				else {
					if (activeInst.getRegComp().isFieldSet()) {
						activeInst.setRepCount(leftIdx); // interpret single fieldstruct index as repcount
					}
					else activeInst.setWidth(leftIdx);
					activeInst.setOffset(null);  						
				}
				//System.out.println("RdlModelExtractor extractInstanceAddressInfo: getWidth=" + ((ModIndexedInstance) activeInstance).getWidth() + ", getOffset=" + ((ModIndexedInstance) activeInstance).getOffset());
			}
		}
		
		// add width of this instance to its parent on top of fieldOffset stack
		if (activeInstance.isIndexed() && !fieldOffsets.isEmpty()) {
			ModIndexedInstance activeInst = ((ModIndexedInstance) activeInstance);
			Integer parentOffset = fieldOffsets.pop();
			// fieldstruct width is stored in component fieldstructwidth property
			Integer activeInstWidth = activeInst.getRegComp().isFieldSet()? activeInst.getRegComp().getIntegerProperty("fieldstructwidth") :activeInst.getWidth();
			Integer activeInstOffset = activeInst.getOffset();
			// if active instance has an offset, then use it's offset width
			if (activeInstOffset != null) {
				Integer indexedWidth = activeInstOffset + activeInstWidth;
				if (indexedWidth >=  parentOffset)
					fieldOffsets.push(indexedWidth);  // use width implied by indexed component
				else
					fieldOffsets.push(parentOffset);  // just restore previous value
				//	Ordt.errorExit("Instance " + activeInst.getId() + " does not fit in fieldstruct " + activeInst.getParent().getId());
			}
			else
				fieldOffsets.push(parentOffset + activeInstWidth * activeInst.getRepCount());
		}
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

	/** convert a string ins list to a path */
	private static String toPath(List<String> instPath) {
		String pathStr = "";
		Iterator<String> it = instPath.iterator();
		while (it.hasNext()) {
			String inst = it.next();
			pathStr = (it.hasNext()) ? pathStr + inst + "." : pathStr + inst;
		}
		return pathStr;
	}
	
}
