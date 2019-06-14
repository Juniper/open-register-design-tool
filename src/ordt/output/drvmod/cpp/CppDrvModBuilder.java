package ordt.output.drvmod.cpp;

import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.RegModelIntf;
import ordt.output.common.MsgUtils;
import ordt.output.drvmod.DrvModBaseInstance;
import ordt.output.drvmod.DrvModBuilder;
import ordt.output.drvmod.DrvModRegInstance;
import ordt.output.drvmod.DrvModRegInstance.DrvModField;
import ordt.output.drvmod.DrvModRegSetInstance.DrvModRegSetChildInfo;
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
				" = std::make_shared<ordt_drv_regset>(\"" + rsInst.getName() + "\");");
		// add this instance's children
		HashMap<DrvModRegSetChildInfo, Integer> children = rsInst.getChildMaps();
		for(DrvModRegSetChildInfo childInfo: children.keySet()) {
			DrvModBaseInstance child = childInfo.child;
			rootBuild.addStatement(rsInst.getInstName() + "->add_child(" + children.get(childInfo) + ", " + child.getInstName() + ", " + childInfo.reps + ", " + childInfo.addressOffset + ", " + childInfo.addressStride + ");");
		}
	}

	@Override
	public void processRegInstance(DrvModRegInstance rInst) {
		CppMethod rootBuild = rootClass.getTaggedMethod("build");
		// create the new instance
		rootBuild.addStatement("std::shared_ptr<ordt_drv_reg> " + rInst.getInstName() + 
				" = std::make_shared<ordt_drv_reg>(\"" + rInst.getName() + "\");");
		// add this instance's fields
		List<DrvModField> fields = rInst.getFields();
		for(DrvModField field: fields)
			rootBuild.addStatement(rInst.getInstName() + "->add_field(\"" + field.name + "\", " + field.lowIndex + ", " + field.width +
					", " + field.isReadable() + ", " + field.isWriteable() +");");
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
    		writeStmt(hppBw, 0, "#include <list>");
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
    		rootInstances.get(overlay).instance.process(overlay, false, true);  // process both regs and regsets, only process each elem once
        }
	}

    /** add root class children in build */
	private void addRootInstances() {
		CppMethod rootBuild = rootClass.getTaggedMethod("build");
        for(int overlay=0; overlay<rootInstances.size();overlay++) {
        	DrvModRegSetInstance inst = rootInstances.get(overlay).instance;
        	Long offset = rootInstances.get(overlay).relativeAddress;
			rootBuild.addStatement("add_child(" + (1 << overlay) + ", " + inst.getInstName() + ", 1, " + offset + ", 0);"); 
        }
	}

	// ------------------------- write driver model base classes -------------------------
	
	private void writeOrdtDrvBaseStructs() {
		// path element class (list of these are extracted from input path string)
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
		className = "ordt_drv_field";
		newClass = new CppBaseModClass(className);
		newClass.addDefine(Vis.PUBLIC, "std::string m_name");   // field name
		newClass.addDefine(Vis.PUBLIC, "int m_loidx");   // low index of field
		newClass.addDefine(Vis.PUBLIC, "int m_width");   // field width
		newClass.addDefine(Vis.PUBLIC, "bool m_readable");   
		newClass.addDefine(Vis.PUBLIC, "bool m_writeable");   
		// constructors
		nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name, int _m_loidx, int _m_width, bool _m_readable, bool _m_writeable)");  
		nMethod.addInitCall("m_name(_m_name)");  
		nMethod.addInitCall("m_loidx(_m_loidx)");
		nMethod.addInitCall("m_width(_m_width)");
		nMethod.addInitCall("m_readable(_m_readable)");
		nMethod.addInitCall("m_writeable(_m_writeable)");
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}

	/** create and write ordt_drv_element classes */    
	private void writeOrdtDrvElementClass() {
		String className = "ordt_drv_element";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addDefine(Vis.PUBLIC, "std::string m_name");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name)");  
		nMethod.addInitCall("m_name(_m_name)");  // relative name of instance
		// -- get_address methods
		nMethod = newClass.addMethod(Vis.PUBLIC, "pure virtual int get_address_using_list(const int version, std::list<ordt_drv_path_element> &path, const bool bypass_names, uint64_t &address, std::list<ordt_drv_field> &fields)"); 
		//
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual int get_address_using_version(const int version, const std::string pathstr, uint64_t &address, std::list<ordt_drv_field> &fields)"); 
		nMethod.addStatement("std::list<ordt_drv_path_element> path = get_path(pathstr);");
		nMethod.addStatement("if (path.size()>0) return get_address_using_list(version, path, false, address, fields);");  
		nMethod.addStatement("#ifdef ORDT_PIO_DRV_VERBOSE");  
		nMethod.addStatement("   std::cout << \"--> invalid path: \" << pathstr << \"\\n\";" );
		nMethod.addStatement("#endif");  
		nMethod.addStatement("return 4;");  
		//
		nMethod = newClass.addMethod(Vis.PROTECTED, "int get_version(const std::string tag)");
		if (rootInstances.isEmpty())
			MsgUtils.errorExit("No valid root instances found for C++ model generation");
		String baseTag = rootInstances.get(0).instance.getName();  // root instance name of overlay 0 will be used as tag
		nMethod.addStatement("if (tag == \"" + baseTag + "\") return 0;");
		int idx=1;
		for (String tag: Ordt.getOverlayFileTags()) 
			nMethod.addStatement("else if (tag == \"" + tag + "\") return " + idx++ + ";");
		nMethod.addStatement("else return -1;");  // invalid tag
		//
		nMethod = newClass.addMethod(Vis.PUBLIC, "std::vector<std::string> get_tags()");
		nMethod.addStatement("std::vector<std::string> tags;");
		nMethod.addStatement("tags.push_back(\"" + baseTag + "\");");
		for (String tag: Ordt.getOverlayFileTags()) 
			nMethod.addStatement("tags.push_back(\"" + tag + "\");");
		nMethod.addStatement("return tags;");
		//
		nMethod = newClass.addMethod(Vis.PUBLIC, "int get_address(const std::string tag, const std::string pathstr, uint64_t &address, std::list<ordt_drv_field> &fields)"); 
		nMethod.addStatement("int version = get_version(tag);");
		nMethod.addStatement("if (version<0) {");  
		nMethod.addStatement("#ifdef ORDT_PIO_DRV_VERBOSE");  
		nMethod.addStatement("   std::cout << \"--> invalid tag: \" << tag << \"\\n\";" );
		nMethod.addStatement("#endif");  
		nMethod.addStatement("  return 2;");  
		nMethod.addStatement("}");  
		nMethod.addStatement("return get_address_using_version(version, pathstr, address, fields);");  
		//
		nMethod = newClass.addMethod(Vis.PRIVATE, "std::list<std::string> split(const std::string &text, char sep, bool trim_rb)");
		nMethod.addStatement("std::list<std::string> tokens;");
		nMethod.addStatement("std::size_t start = 0, end = 0, end_adj = 0;");
		nMethod.addStatement("while ((end = text.find(sep, start)) != std::string::npos) {");
		nMethod.addStatement("  if (trim_rb && ((end_adj = text.find(']', start)) != std::string::npos) && (end_adj<end))");
		nMethod.addStatement("    tokens.push_back(text.substr(start, end_adj - start));");
		nMethod.addStatement("  else");
		nMethod.addStatement("    tokens.push_back(text.substr(start, end - start));");
		nMethod.addStatement("  start = end + 1;");
		nMethod.addStatement("}");
		nMethod.addStatement("if (trim_rb && ((end_adj = text.find(']', start)) != std::string::npos))");
		nMethod.addStatement("  tokens.push_back(text.substr(start, end_adj - start));");
		nMethod.addStatement("else");
		nMethod.addStatement("  tokens.push_back(text.substr(start));");
		nMethod.addStatement("return tokens;");
		//
		nMethod = newClass.addMethod(Vis.PROTECTED, "std::list<ordt_drv_path_element> get_path(const std::string pathstr)");
		nMethod.addStatement("std::list<ordt_drv_path_element> pathlist;");
		nMethod.addStatement("std::list<std::string> lst = split(pathstr, '.', true);");  // split and remove right bracket
		//nMethod.addStatement("std::cout << \"--> list size: \" << lst.size() << \"\\n\";" );
		nMethod.addStatement("for(auto const& str_elem: lst) {");
		nMethod.addStatement("   std::list<std::string> sub_lst = split(str_elem, '[', false);");  // now split name and index
		nMethod.addStatement("   if (sub_lst.size()==2) {");
		//nMethod.addStatement("     std::cout << \"   >\" << sub_lst.front() << \" - \" << sub_lst.back() << \"\\n\";" );
		nMethod.addStatement("     pathlist.emplace_back(sub_lst.front(), std::stoi(sub_lst.back()));");
		nMethod.addStatement("   }");
		nMethod.addStatement("   else {");
		//nMethod.addStatement("     std::cout << \"   >\" << sub_lst.front() << \"\\n\";" );
		nMethod.addStatement("     pathlist.emplace_back(sub_lst.front(), 1);");
		nMethod.addStatement("   }");
		nMethod.addStatement("}");
		nMethod.addStatement("return pathlist;");  // invalid tag
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}
		
	/** create and write ordt_drv_element and ordt_drv_regset_child classes */    
	private void writeOrdtDrvRegsetChildClass() {
		// regset child class 
		String className = "ordt_drv_regset_child";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addDefine(Vis.PUBLIC, "int m_map");  // encoded overlap map
		newClass.addDefine(Vis.PUBLIC, "std::shared_ptr<ordt_drv_element> m_child");  // pointer to child element
		newClass.addDefine(Vis.PUBLIC, "int m_reps");
		newClass.addDefine(Vis.PUBLIC, "uint64_t m_offset");
		newClass.addDefine(Vis.PUBLIC, "uint64_t m_stride");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(int _m_map, std::shared_ptr<ordt_drv_element> _m_child, int _m_reps, uint64_t _m_offset, uint64_t _m_stride)");  
		nMethod.addInitCall("m_map(_m_map)");  
		nMethod.addInitCall("m_child(_m_child)");   
		nMethod.addInitCall("m_reps(_m_reps)");  // number of reps in this instance
		nMethod.addInitCall("m_offset(_m_offset)");
		nMethod.addInitCall("m_stride(_m_stride)");
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}
	
	/** create and write ordt_drv_regset class */    
	private void writeOrdtDrvRegSetClass() {
		String className = "ordt_drv_regset";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addParent("ordt_drv_element");
		newClass.addDefine(Vis.PROTECTED, "std::list<ordt_drv_regset_child> m_children");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name)");  
		nMethod.addInitCall("ordt_drv_element(_m_name)");
		nMethod.addInitCall("m_children()");
		// get_address method
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual int get_address_using_list(const int version, std::list<ordt_drv_path_element> &path, const bool bypass_names, uint64_t &address, std::list<ordt_drv_field> &fields)");  
		nMethod.addStatement("if (path.empty())");  // exit with error if no path
		nMethod.addStatement("  return 8;");
		nMethod.addStatement("ordt_drv_path_element pelem = path.front();");
		nMethod.addStatement("if (!bypass_names) {");  
		nMethod.addStatement("  path.pop_front();");  // remove element from front of path
		nMethod.addStatement("  if (path.empty())");    // now if path is empty we're done
		nMethod.addStatement("    return 0;");  
		nMethod.addStatement("  pelem = path.front();");  // get child element from front of path
		nMethod.addStatement("}");  
		nMethod.addStatement("for (auto const &child: m_children) {");  // find matching child
		//nMethod.addStatement("  std::cout << \"  --> checking child in regset \" << m_name  << \", bypass=\" << bypass_names << \", cname=\" << child.m_child->m_name << \", cmap=\" << child.m_map << \"\\n\";" );
		// if a matching child, update address and make recursive call
		nMethod.addStatement("  if (((1<<version) & child.m_map) && (bypass_names || (pelem.m_name == child.m_child->m_name))) {");  
		nMethod.addStatement("    address += child.m_offset;");  // first add this regsets offset
		nMethod.addStatement("    if (child.m_reps>1) address += (child.m_stride*pelem.m_idx);");  // add index offset if a replicated instance
		nMethod.addStatement("    return child.m_child->get_address_using_list(version, path, false, address, fields);");  // recursive call to matching child
		nMethod.addStatement("  }");  
		nMethod.addStatement("}");  
		nMethod.addStatement("#ifdef ORDT_PIO_DRV_VERBOSE");  
		nMethod.addStatement("std::cout << \"--> unable to find child \" << pelem.m_name << \" in regset \" << m_name << \"\\n\";" );
		nMethod.addStatement("#endif");  
		nMethod.addStatement("return 8;");  
		// add_child method
		nMethod = newClass.addMethod(Vis.PUBLIC, "void add_child(int _m_map, std::shared_ptr<ordt_drv_element> _m_child, int _m_reps, uint64_t _m_offset, uint64_t _m_stride)");
		nMethod.addStatement("ordt_drv_regset_child new_child(_m_map, _m_child, _m_reps, _m_offset, _m_stride);");  
		nMethod.addStatement("m_children.push_back(new_child);");  
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}

	/** create and write ordt_drv_reg class */    
	private void writeOrdtDrvRegClass() {
		String className = "ordt_drv_reg";
		CppBaseModClass newClass = new CppBaseModClass(className);
		newClass.addParent("ordt_drv_element");
		newClass.addDefine(Vis.PROTECTED, "std::list<ordt_drv_field> m_fields");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(std::string _m_name)");  
		nMethod.addInitCall("ordt_drv_element(_m_name)");
		nMethod.addInitCall("m_fields()");
		// get_address method
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual int get_address_using_list(const int version, std::list<ordt_drv_path_element> &path, const bool bypass_names, uint64_t &address, std::list<ordt_drv_field> &fields)");  
		nMethod.addStatement("if (path.empty())");  // exit with error if no path
		nMethod.addStatement("  return 8;");
		nMethod.addStatement("path.pop_front();");  // remove element from front of path
		nMethod.addStatement("if (path.empty()) {");    // now if path is empty we're done
		nMethod.addStatement("  fields = m_fields;");  // return the field list
		nMethod.addStatement("  return 0;");  
		nMethod.addStatement("}");  
		nMethod.addStatement("#ifdef ORDT_PIO_DRV_VERBOSE");  
		nMethod.addStatement("ordt_drv_path_element pelem = path.front();");  // get child element from front of path
		nMethod.addStatement("std::cout << \"--> invalid child \" << pelem.m_name << \" specified in reg \" << m_name << \"\\n\";" );
		nMethod.addStatement("#endif");  
		nMethod.addStatement("return 8;");
		//
		nMethod = newClass.addMethod(Vis.PUBLIC, "void add_field(std::string _m_name, int _m_loidx, int _width, bool _m_readable, bool _m_writeable)");
		nMethod.addStatement("ordt_drv_field new_field(_m_name, _m_loidx, _width, _m_readable, _m_writeable);");  
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
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "()");  
		nMethod.addInitCall("ordt_drv_regset(\"root\")");
		nMethod.addStatement("build();");  // call build in root constuctor
		// methods
		nMethod = newClass.addMethod(Vis.PUBLIC, "void build()"); 
		newClass.tagMethod("build", nMethod);
		// override of get_address to bypass names
		//
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual int get_address_using_version(const int version, const std::string pathstr, uint64_t &address, std::list<ordt_drv_field> &fields)"); 
		nMethod.addStatement("address=0;");  // initialize address if calling from root
		nMethod.addStatement("fields.clear();");  // initialize field list if calling from root
		nMethod.addStatement("std::list<ordt_drv_path_element> path = get_path(pathstr);");
		nMethod.addStatement("if (path.size()>0) return get_address_using_list(version, path, true, address, fields);");  
		nMethod.addStatement("#ifdef ORDT_PIO_DRV_VERBOSE");  
		nMethod.addStatement("   std::cout << \"--> invalid path: \" << pathstr << \"\\n\";" );
		nMethod.addStatement("#endif");  
		nMethod.addStatement("return 4;");  
		// return the root class
		return newClass;
	}
	
	/** write the ordt_drv_root class */    
	private void writeOrdtDrvRootClass() {
		writeStmts(hppBw, rootClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, rootClass.genMethods(true));  // methods with namespace
	}

}
