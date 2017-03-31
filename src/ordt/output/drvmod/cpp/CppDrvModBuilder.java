package ordt.output.drvmod.cpp;

import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ordt.extract.RegModelIntf;
import ordt.output.drvmod.DrvModBaseInstance;
import ordt.output.drvmod.DrvModBuilder;
import ordt.output.drvmod.DrvModRegInstance;
import ordt.output.drvmod.DrvModRegInstance.DrvModField;
import ordt.output.drvmod.DrvModRegSetInstance;
import ordt.output.drvmod.cpp.CppBaseModClass.CppMethod;
import ordt.output.drvmod.cpp.CppBaseModClass.Vis;

/** generate C++ pio driver model */
public class CppDrvModBuilder extends DrvModBuilder {  // Note no OutputBuilder overrides in CppDrvModBuilder - these are in DrvModBuilder 
	private BufferedWriter commonHppBw;
	private BufferedWriter commonCppBw;
	private BufferedWriter hppBw;
	private BufferedWriter cppBw;

	private static CppBaseModClass rootClass;
	
	private static HashSet<String> reservedWords = getReservedWords();
	
	public CppDrvModBuilder(RegModelIntf model) {  
		super(model);
	}

    /** load C++ reserved words to be escaped */
	private static HashSet<String> getReservedWords() {
		HashSet<String> reservedWords = new HashSet<String>();
		//reservedWords.add("bit");
		return reservedWords;
	}
	
    /** escape string if a reserved words  */
	@SuppressWarnings("unused")
	private String escapeReservedString(String word) {
		if (reservedWords.contains(word)) return ("\\" + word + " ");  
		return word;
	}

	// ----------- DrvModBuilder overrides
	
	@Override
	/** add a regset instance to the build */
	public void processRegSetInstance(DrvModRegSetInstance rsInst) {
		CppMethod rootBuild = rootClass.getTaggedMethod("build");
		// create the new instance
		rootBuild.addStatement("std::shared_ptr<ordt_drv_regset> " + rsInst.getInstName() + 
				" = std::make_shared<ordt_drv_regset>(\"" + rsInst.getName() + "\", " + rsInst.getReps() + ", " + rsInst.getAddressOffset() + ", " + rsInst.getAddressStride() + ");");
		// add this instance's children
		HashMap<DrvModBaseInstance, Integer> children = rsInst.getChildMaps();
		for(DrvModBaseInstance child: children.keySet())
			rootBuild.addStatement(rsInst.getInstName() + "->add_child(" + children.get(child) + ", " + child.getInstName() + ");");
	}

	@Override
	public void processRegInstance(DrvModRegInstance rInst) {
		CppMethod rootBuild = rootClass.getTaggedMethod("build");
		// create the new instance
		rootBuild.addStatement("std::shared_ptr<ordt_drv_reg> " + rInst.getInstName() + 
				" = std::make_shared<ordt_drv_reg>(\"" + rInst.getName() + "\", " + rInst.getReps() + ", " + rInst.getAddressOffset() + ", " + rInst.getAddressStride() + ");");
		// add this instance's fields
		List<DrvModField> fields = rInst.getFields();
		for(DrvModField field: fields)
			rootBuild.addStatement(rInst.getInstName() + "->add_field(\"" + field.name + "\", " + field.lowIndex + ", " + field.width + ");");
	}
	
    //---------------------------- output write methods ----------------------------------------

    /** required default write method - not used in CppDrvModBuilder*/
	@Override
	public void write(BufferedWriter bw) {
	}
	
	/** write c++ output to specified output file(s)  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	@Override
	public void write(String outName, String description, String commentPrefix) {		

		File saveDir = new File(outName);
   	    saveDir.mkdirs();   // make sure directory exists
   	    
   	    // open the common hpp and cpp files and write
    	commonHppBw = openBufferedWriter(outName + "/ordt_pio_common.hpp", description);
    	commonCppBw = openBufferedWriter(outName + "/ordt_pio_common.cpp", description);
    	if ((commonHppBw != null) && (commonCppBw != null)) {

    		// write the hpp file header
    		writeHeader(commonHppBw, commentPrefix);
    		writeStmt(commonHppBw, 0, "#ifndef __ORDT_PIO_COMMON_HPP_INCLUDED__");	
    		writeStmt(commonHppBw, 0, "#define __ORDT_PIO_COMMON_HPP_INCLUDED__");
    		writeStmt(commonHppBw, 0, "");	
    		writeStmt(commonHppBw, 0, "#include <vector>");
    		writeStmt(commonHppBw, 0, "#include <iostream>");  
    		writeStmt(commonHppBw, 0, "#include <memory>");  // unique_ptr, etc g++  
    		writeStmt(commonHppBw, 0, "#include <cstdint>");  
    		writeStmt(commonHppBw, 0, "#include <algorithm>");  // min/max  
    		writeStmt(commonHppBw, 0, "#include <string>");    
    		writeStmt(commonHppBw, 0, "#include <sstream>");    
    	    writeStmt(commonHppBw, 0, "#define quote(x) #x"); // for debug display of vars
    		writeStmt(commonHppBw, 0, "");		   
   		
    		// write the cpp file header
    		writeHeader(commonCppBw, commentPrefix);
    		writeStmt(commonCppBw, 0, "#include \"ordt_pio_common.hpp\"");
    		writeStmt(commonCppBw, 0, "");
    		
    		// write the common data class
    		CppBaseModClass newClass = CppBaseModClass.getOrdtDataClass();
    		writeStmts(commonHppBw, newClass.genHeader(false)); // header with no include guards
    		writeStmts(commonHppBw, newClass.genMethods(true, true));  // template methods with namespace
    		writeStmts(commonCppBw, newClass.genMethods(true));  // non-template methods with namespace
    		
            // close up hpp file
    		writeStmt(commonHppBw, 0, "#endif // __ORDT_PIO_COMMON_HPP_INCLUDED__");
    		closeBufferedWriter(commonHppBw);
    		
    		// close up cpp file
    		closeBufferedWriter(commonCppBw);
    	}

   	    // open the driver model hpp and cpp files and write

   	    // open the hpp and cpp files
    	hppBw = openBufferedWriter(outName + "/ordt_pio_drv.hpp", description);
    	cppBw = openBufferedWriter(outName + "/ordt_pio_drv.cpp", description);
    	if ((hppBw != null) && (cppBw != null)) {

    		// write the hpp file header
    		writeHeader(hppBw, commentPrefix);
    		writeStmt(hppBw, 0, "#ifndef __ORDT_PIO_DRV_HPP_INCLUDED__");	
    		writeStmt(hppBw, 0, "#define __ORDT_PIO_DRV_HPP_INCLUDED__");
    		writeStmt(hppBw, 0, "");	
    		writeStmt(hppBw, 0, "#include <vector>");
    		writeStmt(hppBw, 0, "#include <iostream>");  
    		writeStmt(hppBw, 0, "#include <memory>");  // unique_ptr, etc g++ 
    		writeStmt(hppBw, 0, "#include <cstdint>");  
    		writeStmt(hppBw, 0, "#include <algorithm>");  // min/max  
    		writeStmt(hppBw, 0, "#include <string>");    
    		writeStmt(hppBw, 0, "#include <sstream>");    
    	    writeStmt(hppBw, 0, "#define quote(x) #x"); // for debug display of vars
    		writeStmt(hppBw, 0, "");		   
   		
    		// write the cpp file header
    		writeHeader(cppBw, commentPrefix);
    		writeStmt(cppBw, 0, "#include \"ordt_pio_common.hpp\"");
    		writeStmt(cppBw, 0, "#include \"ordt_pio_drv.hpp\"");
    		writeStmt(cppBw, 0, "");
    		
    		// write the driver model classes
    		writeClasses();
    		
            // close up hpp file
    		writeStmt(hppBw, 0, "#endif // __ORDT_PIO_DRV_HPP_INCLUDED__");
    		closeBufferedWriter(hppBw);
    		
    		// close up cpp file
    		closeBufferedWriter(cppBw);
    	}
	}

	/** write model classes */
	private void writeClasses() {
		// write base classes
		writeOrdtDrvBaseStructs();
		// write base classes
		writeOrdtDrvElementClass();
		writeOrdtDrvRegsetChildClass();
		writeOrdtDrvRegSetClass(); 
		writeOrdtDrvRegClass();
		// create the root class - will add model instantiations to build()
		rootClass = createOrdtDrvRootClass();  
		// add design specific class instances
		addDesignInstances();
		// add root the instances
		addRootInstances();
		// write the root class now that build method is populated
		writeOrdtDrvRootClass();  
	}

	/** add design specific class instances to root.build()  */
	private void addDesignInstances() {
		// process unique instances of each overlay
        for(int overlay=0; overlay<rootInstances.size();overlay++) {
    		rootInstances.get(overlay).process(overlay, false, true);  // process both regs and regsets, only process each elem once
        }
	}

    /** add root class children in build */
	private void addRootInstances() {
		CppMethod rootBuild = rootClass.getTaggedMethod("build");
        for(int overlay=0; overlay<rootInstances.size();overlay++) {
        	DrvModRegSetInstance inst = rootInstances.get(overlay);
			rootBuild.addStatement("add_child(" + (1 << overlay) + ", " + inst.getInstName() + ");");
        }
	}

	// ------------------------- write driver model base classes -------------------------
	
	private void writeOrdtDrvBaseStructs() {
		// path element class (vector of these are extracted from input path string)
		String className = "ordt_drv_path_element";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addDefine(Vis.PUBLIC, "std::string m_name");   // relative name of path element
		newClass.addDefine(Vis.PUBLIC, "int m_idx");   // index of path element if replicated
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name, int _m_idx)");  
		nMethod.addInitCall("m_name(_m_name)");  
		nMethod.addInitCall("m_idx(_m_idx)");
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
		
		// field info class 
		className = "ordt_drv_field";   // TODO - add readable/writeable booleans
		newClass = new CppBaseModClass(className);
		newClass.addDefine(Vis.PUBLIC, "std::string m_name");   // field name
		newClass.addDefine(Vis.PUBLIC, "int m_loidx");   // low index of field
		newClass.addDefine(Vis.PUBLIC, "int m_width");   // field width
		// constructors
		nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name, int _m_loidx, int _m_width)");  
		nMethod.addInitCall("m_name(_m_name)");  
		nMethod.addInitCall("m_loidx(_m_loidx)");
		nMethod.addInitCall("m_width(_m_width)");
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}

	/** create and write ordt_drv_element classes */    
	private void writeOrdtDrvElementClass() {
		String className = "ordt_drv_element";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addDefine(Vis.PUBLIC, "std::string m_name");
		newClass.addDefine(Vis.PROTECTED, "int m_reps");
		newClass.addDefine(Vis.PROTECTED, "uint64_t m_offset");
		newClass.addDefine(Vis.PROTECTED, "uint64_t m_stride");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name, int _m_reps, uint64_t _m_offset, uint64_t _m_stride)");  
		nMethod.addInitCall("m_reps(_m_reps)");  // number of reps in this instance
		nMethod.addInitCall("m_offset(_m_offset)");
		nMethod.addInitCall("m_stride(_m_stride)");
		nMethod.addInitCall("m_name(_m_name)");  // relative name of instance
		// methods
		newClass.addMethod(Vis.PUBLIC, "pure virtual uint64_t get_address(const int version, const std::vector<ordt_drv_path_element> &path)"); 
		//newClass.addMethod(Vis.PUBLIC, "pure virtual " + className + " get_element(const int version, const std::vector<ordt_drv_path_element> &path)");  // TODO move this to regset?
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}
		
	/** create and write ordt_drv_element and ordt_drv_regset_child classes */    
	private void writeOrdtDrvRegsetChildClass() {
		// regset child class 
		String className = "ordt_drv_regset_child";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addDefine(Vis.PROTECTED, "int m_map");  // encoded overlap map
		newClass.addDefine(Vis.PROTECTED, "std::shared_ptr<ordt_drv_element> m_child");  // pointer to child element
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(int _m_map, std::shared_ptr<ordt_drv_element> _m_child)");  
		nMethod.addInitCall("m_map(_m_map)");  
		nMethod.addInitCall("m_child(_m_child)");   
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}
	
	/** create and write ordt_drv_regset class */    
	private void writeOrdtDrvRegSetClass() {
		String className = "ordt_drv_regset";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addParent("ordt_drv_element");
		newClass.addDefine(Vis.PROTECTED, "std::vector<ordt_drv_regset_child> m_children");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name, int _m_reps, uint64_t _m_offset, uint64_t _m_stride)");  
		nMethod.addInitCall("ordt_drv_element(_m_name, _m_reps, _m_offset, _m_stride)");
		nMethod.addInitCall("m_children()");
		// methods
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual uint64_t get_address(const int version, const std::vector<ordt_drv_path_element> &path)");  
		nMethod.addStatement("return 0;");  // TODO
		nMethod = newClass.addMethod(Vis.PUBLIC, "void add_child(int _m_map, std::shared_ptr<ordt_drv_element> _m_child)");  
		nMethod.addStatement("ordt_drv_regset_child new_child(_m_map, _m_child);");  
		nMethod.addStatement("m_children.push_back(new_child);");  
		//newClass.addMethod(Vis.PUBLIC, "pure virtual " + className + " get_element(const int version, const std::vector<ordt_drv_path_element> &path)");  // TODO 
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}

	/** create and write ordt_drv_reg class */    
	private void writeOrdtDrvRegClass() {
		String className = "ordt_drv_reg";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addParent("ordt_drv_element");
		newClass.addDefine(Vis.PROTECTED, "std::vector<ordt_drv_field> m_fields");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name, int _m_reps, uint64_t _m_offset, uint64_t _m_stride)");  
		nMethod.addInitCall("ordt_drv_element(_m_name, _m_reps, _m_offset, _m_stride)");
		nMethod.addInitCall("m_fields()");
		// methods
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual uint64_t get_address(const int version, const std::vector<ordt_drv_path_element> &path)");  
		nMethod.addStatement("return 0;");  // TODO
		nMethod = newClass.addMethod(Vis.PUBLIC, "void add_field(std::string _m_name, int _m_loidx, int _width)");  
		nMethod.addStatement("ordt_drv_field new_field(_m_name, _m_loidx, _width);");  
		nMethod.addStatement("m_fields.push_back(new_field);");  
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}

	/** create ordt_drv_root class */    
	private CppBaseModClass createOrdtDrvRootClass() {
		String className = "ordt_drv_root";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addParent("ordt_drv_regset");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name, int _m_reps, uint64_t _m_offset, uint64_t _m_stride)");  
		nMethod.addInitCall("ordt_drv_regset(_m_name, _m_reps, _m_offset, _m_stride)");
		// methods
		nMethod = newClass.addMethod(Vis.PUBLIC, "void build()"); 
		newClass.tagMethod("build", nMethod);
		// return the root class
		return newClass;
	}
	
	/** write the ordt_drv_root class */    
	private void writeOrdtDrvRootClass() {
		writeStmts(hppBw, rootClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, rootClass.genMethods(true));  // methods with namespace
	}

}
