package ordt.output.drvmod.cpp;

import java.io.BufferedWriter;
import java.io.File;

import ordt.extract.RegModelIntf;
import ordt.output.drvmod.DrvModBuilder;
import ordt.output.drvmod.DrvModRegInstance;
import ordt.output.drvmod.DrvModRegSetInstance;

/** generate C++ pio driver model */
public class CppDrvModBuilder extends DrvModBuilder {  // Note no OutputBuilder overrides in CppDrvModBuilder - these are in DrvModBuilder 
	private BufferedWriter commonHppBw;
	private BufferedWriter commonCppBw;
	private BufferedWriter hppBw;
	private BufferedWriter cppBw;

	private static int count = 0;  // TODO - remove
	
	public CppDrvModBuilder(RegModelIntf model) {  
		super(model);
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
/*    		writeStmt(hppBw, 0, "#include <memory>");  // unique_ptr, etc g++  // TODO
    		writeStmt(hppBw, 0, "#include <cstdint>");  
    		writeStmt(hppBw, 0, "#include <algorithm>");  // min/max  
    		writeStmt(hppBw, 0, "#include <string>");    
    		writeStmt(hppBw, 0, "#include <sstream>");    
    		writeStmt(hppBw, 0, "#include <mutex>");    // reg mutex
    		writeStmt(hppBw, 0, "#include <atomic>");    // field data atomic
    	    writeStmt(hppBw, 0, "#define quote(x) #x"); // for debug display of vars*/
    		writeStmt(hppBw, 0, "");		   
   		
    		// write the cpp file header
    		writeHeader(cppBw, commentPrefix);
    		writeStmt(cppBw, 0, "#include \"ordt_pio_common.hpp\"");
    		writeStmt(cppBw, 0, "#include \"ordt_pio_drv.hpp\"");
    		writeStmt(cppBw, 0, "");
    		
    		// define r/w modes
    		//writeStmt(hppBw, 0, "enum read_mode_t : uint8_t {r_none, r_std, r_clr};");
    		//writeStmt(hppBw, 0, "enum write_mode_t : uint8_t {w_none, w_std, w_1clr, w_1set};");

    		// write the driver model classes
    		writeClasses();
    		
    		// write model instances
            for(int overlay=0; overlay<rootInstances.size();overlay++) {  // overlays have different roots
            	count=0;
        		rootInstances.get(overlay).process(overlay, false, true);  // process both regs and regsets, only process each elem once
           	    System.out.println("CppDrvModBuilder write: overlay=" + overlay + ", count=" + count);
            }

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
		//writeOrdtDrvBaseClass();  // TODO
		//writeOrdtDrvRegSetClass();  // TODO
		//writeOrdtDrvRootClass();  // TODO
		//writeOrdtDrvRegClass();  // TODO
		// write design specific classes
		//writeDesignSpecificClasses();
	}

	/** write all design-specific c++ classes   // TODO - use processInstance to create instances
	private void writeDesignSpecificClasses() {
		for (CppModClass mClass: modClasses) {
			// write class
			writeStmts(hppBw, mClass.genHeader(false)); // header with no include guards
			writeStmts(cppBw, mClass.genMethods(true));  // methods with namespace			
		}	
	}*/

	/** create and write OrdtDrvRoot class     
	private void writeOrdtDrvRootClass() {
		String className = "ordt_data";
		CppModClass newClass = new CppModClass(className);
		newClass.addParent("std::vector<uint32_t>");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "()");
		nMethod.addInitCall("std::vector<uint32_t>()");
		nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(int _size, uint32_t _data)");
		nMethod.addInitCall("std::vector<uint32_t>(_size, _data)");
		nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(const ordt_data& _data)");  // copy
		nMethod.addInitCall("std::vector<uint32_t>(_data)");
		
		// set_slice method for ordt_data
		nMethod = newClass.addMethod(Vis.PUBLIC, "void set_slice(int lobit, int size, const ordt_data& update)");
		nMethod.addStatement("int data_size = this->size() * 32;");
		//nMethod.addStatement("std::cout << \"set_slice: -------------------, lo bit=\" << lobit << \", size=\" << size << \"\\n\";");
		nMethod.addStatement("if ((lobit % 32) > 0) {");
		nMethod.addStatement("  std::cout << \"ERROR set_slice: non 32b aligned large fields are not supported\" << \"\\n\";");
		nMethod.addStatement("  return;");
		nMethod.addStatement("}");
		nMethod.addStatement("int hibit = lobit + size - 1;"); 
		nMethod.addStatement("int loword = lobit / 32;"); 
		nMethod.addStatement("int hiword = hibit / 32;"); 
		nMethod.addStatement("if (hibit > data_size - 1) {");
		nMethod.addStatement("  std::cout << \"ERROR set_slice: specified slice is not contained in data\" << \"\\n\";");
		nMethod.addStatement("  return;");
		nMethod.addStatement("}");
		nMethod.addStatement("int update_idx=0;");
		nMethod.addStatement("for (int idx=loword; idx < hiword + 1; idx++) {");
		//nMethod.addStatement("  std::cout << \"set_slice: old word=\" << this->at(idx) << \"\\n\";");
		nMethod.addStatement("  if (idx == hiword) {");
		nMethod.addStatement("     int modsize = hibit - hiword*32 + 1;");		
		nMethod.addStatement("     uint32_t mask = (modsize == 32)? 0xffffffff : (1 << modsize) - 1;");		
		nMethod.addStatement("     this->at(idx) = (this->at(idx) & ~mask) ^ (update.at(update_idx) & mask);");
		//nMethod.addStatement("  std::cout << \"set_slice: updating word=\" << idx  << \", mask=\" << mask << \"\\n\";");
		nMethod.addStatement("  }");
		nMethod.addStatement("  else this->at(idx) = update.at(update_idx);");
		nMethod.addStatement("  update_idx++;");		
		//nMethod.addStatement("  std::cout << \"set_slice: new word=\" << this->at(idx) << \"\\n\";");
		nMethod.addStatement("}");
		
		// set_slice template method
		nMethod = newClass.addTemplateMethod(Vis.PUBLIC, "void set_slice(int lobit, int size, const T& update)");
		nMethod.addStatement("int data_size = this->size() * 32;");
		//nMethod.addStatement("std::cout << \"set_slice: -------------------, lo bit=\" << lobit << \", size=\" << size << \"\\n\";");
		nMethod.addStatement("if (sizeof(T) > 8) {");
		nMethod.addStatement("  std::cout << \"ERROR set_slice: size of update type is greater than 64b\" << \"\\n\";");
		nMethod.addStatement("  return;");
		nMethod.addStatement("}");
		nMethod.addStatement("int hibit = lobit + size - 1;"); 
		nMethod.addStatement("int loword = lobit / 32;"); 
		nMethod.addStatement("int hiword = hibit / 32;"); 
		nMethod.addStatement("if (hibit > data_size - 1) {");
		nMethod.addStatement("  std::cout << \"ERROR set_slice: specified slice is not contained in data\" << \"\\n\";");
		nMethod.addStatement("  return;");
		nMethod.addStatement("}");
		nMethod.addStatement("int update_rshift=0;");
		nMethod.addStatement("for (int idx=loword; idx < hiword + 1; idx++) {");
		nMethod.addStatement("  int offset=idx*32;");
		nMethod.addStatement("  int lo_modbit = std::max(0, lobit - offset);");		
		nMethod.addStatement("  int hi_modbit = std::min(31, hibit - offset);");		
		nMethod.addStatement("  int modsize = hi_modbit - lo_modbit + 1;");		
		nMethod.addStatement("  uint32_t mask = (modsize == 32)? 0xffffffff : (1 << modsize) - 1;");		
		nMethod.addStatement("  uint32_t shifted_mask = mask << lo_modbit;");
		//nMethod.addStatement("  std::cout << \"set_slice: old word=\" << this->at(idx) << \"\\n\";");
		nMethod.addStatement("  this->at(idx) = (this->at(idx) & ~shifted_mask) ^ (((update >> update_rshift) & mask) << lo_modbit);");
		nMethod.addStatement("  update_rshift += modsize;");		
		//nMethod.addStatement("  std::cout << \"set_slice: updating word=\" << idx << \", lo bit=\" << lo_modbit << \", hi bit=\" << hi_modbit  << \", mask=\" << mask << \"\\n\";");
		//nMethod.addStatement("  std::cout << \"set_slice: new word=\" << this->at(idx) << \"\\n\";");
		nMethod.addStatement("}");
		
		// get_slice method for ordt_data
		nMethod = newClass.addMethod(Vis.PUBLIC, "void get_slice(int lobit, int size, ordt_data& slice_out) const");
		nMethod.addStatement("int data_size = this->size() * 32;");
		//nMethod.addStatement("std::cout << \"get_slice: -------------------, lo bit=\" << lobit << \", size=\" << size << \", slice_out=\" << slice_out << \", sizeof(T)=\" << sizeof(T) << \"\\n\";");
		nMethod.addStatement("if ((lobit % 32) > 0) {");
		nMethod.addStatement("  std::cout << \"ERROR set_slice: non 32b aligned large fields are not supported\" << \"\\n\";");
		nMethod.addStatement("  return;");
		nMethod.addStatement("}");
		nMethod.addStatement("slice_out.clear();"); 
		nMethod.addStatement("int hibit = lobit + size - 1;"); 
		nMethod.addStatement("int loword = lobit / 32;"); 
		nMethod.addStatement("int hiword = hibit / 32;"); 
		nMethod.addStatement("if (hibit > data_size - 1) {");
		nMethod.addStatement("  std::cout << \"ERROR set_slice: specified slice is not contained in data\" << \"\\n\";");
		nMethod.addStatement("  return;");
		nMethod.addStatement("}");
		nMethod.addStatement("int out_idx=0;");
		nMethod.addStatement("for (int idx=loword; idx < hiword + 1; idx++) {");
		//nMethod.addStatement("  std::cout << \"get_slice: current word[\" << idx << \"]=\" << this->at(idx) << \"\\n\";");
		nMethod.addStatement("  if (idx == hiword) {");
		nMethod.addStatement("     int modsize = hibit - hiword*32 + 1;");		
		nMethod.addStatement("     uint32_t mask = (modsize == 32)? 0xffffffff : (1 << modsize) - 1;");		
		nMethod.addStatement("     slice_out.at(out_idx) = (this->at(idx) & mask);");
		//nMethod.addStatement("  std::cout << \"get_slice: extracting word=\" << idx << \", mask=\" << mask << \"\\n\";");
		nMethod.addStatement("  }");
		nMethod.addStatement("  else slice_out.at(out_idx) = this->at(idx);");
		nMethod.addStatement("  out_idx++;");	
		//nMethod.addStatement("  std::cout << \"get_slice: slice=\" << slice_out[\" << out_idx << \"]\" << \"\\n\";");
		nMethod.addStatement("}");
		nMethod.addStatement("return;");
		
		// get_slice template method
		nMethod = newClass.addTemplateMethod(Vis.PUBLIC, "void get_slice(int lobit, int size, T& slice_out) const");
		nMethod.addStatement("int data_size = this->size() * 32;");
		//nMethod.addStatement("std::cout << \"get_slice: -------------------, lo bit=\" << lobit << \", size=\" << size << \", slice_out=\" << slice_out << \", sizeof(T)=\" << sizeof(T) << \"\\n\";");
		nMethod.addStatement("slice_out = 0;"); 
        nMethod.addStatement("if (sizeof(T) > 8) {");
		nMethod.addStatement("  std::cout << \"ERROR get_slice: size of return type is greater than 64b\" << \"\\n\";");
		nMethod.addStatement("  return;");
		nMethod.addStatement("}");
		nMethod.addStatement("int hibit = lobit + size - 1;"); 
		nMethod.addStatement("int loword = lobit / 32;"); 
		nMethod.addStatement("int hiword = hibit / 32;"); 
		nMethod.addStatement("if (hibit > data_size - 1) {");
		nMethod.addStatement("  std::cout << \"ERROR set_slice: specified slice is not contained in data\" << \"\\n\";");
		nMethod.addStatement("  return;");
		nMethod.addStatement("}");
		nMethod.addStatement("int ret_lshift=0;");
		nMethod.addStatement("for (int idx=loword; idx < hiword + 1; idx++) {");
		nMethod.addStatement("  int offset=idx*32;");
		nMethod.addStatement("  int lo_modbit = std::max(0, lobit - offset);");		
		nMethod.addStatement("  int hi_modbit = std::min(31, hibit - offset);");		
		nMethod.addStatement("  int modsize = hi_modbit - lo_modbit + 1;");		
		nMethod.addStatement("  uint32_t mask = (modsize == 32)? 0xffffffff : (1 << modsize) - 1;");		
		nMethod.addStatement("  uint32_t shifted_mask = mask << lo_modbit;");
		//nMethod.addStatement("  std::cout << \"get_slice: current word[\" << idx << \"]=\" << this->at(idx) << \"\\n\";");
		nMethod.addStatement("  slice_out |= ((this->at(idx) & shifted_mask) >> lo_modbit) << ret_lshift;");
		nMethod.addStatement("  ret_lshift += modsize;");		
		//nMethod.addStatement("  std::cout << \"get_slice: extracting word=\" << idx << \", lo bit=\" << lo_modbit << \", hi bit=\" << hi_modbit  << \", mask=\" << mask << \"\\n\";");
		//nMethod.addStatement("  std::cout << \"get_slice: slice=\" << slice_out << \"\\n\";");
		nMethod.addStatement("}");
		nMethod.addStatement("return;");
		
		// to_string method
		nMethod = newClass.addMethod(Vis.PUBLIC, "std::string to_string() const");
		nMethod.addStatement("std::stringstream ss;");
		nMethod.addStatement("ss << \"{\" << std::hex << std::showbase;");
		nMethod.addStatement("for (int idx=this->size() - 1; idx >= 0; idx--) ");
		nMethod.addStatement("   ss << \" \" << this->at(idx);");
		nMethod.addStatement("ss << \" }\";");
		nMethod.addStatement("return ss.str();");
		
		// = overload
		nMethod = newClass.addMethod(Vis.PUBLIC, "ordt_data& operator=(const uint32_t rhs)");
		//nMethod.addStatement("for (int idx=0; idx<this->size(); idx++) ");
		//nMethod.addStatement("   this->at(idx) = rhs;");
		nMethod.addStatement("   this->assign(this->size(), rhs);");
		nMethod.addStatement("return *this;");
		
		// ~ overload
		nMethod = newClass.addMethod(Vis.PUBLIC, "ordt_data operator~()");
		nMethod.addStatement("ordt_data temp;");
		nMethod.addStatement("for (int idx=0; idx<this->size(); idx++) ");
		nMethod.addStatement("   temp.at(idx) = ~ this->at(idx);");
		nMethod.addStatement("return temp;");
		
		// & overload
		nMethod = newClass.addMethod(Vis.PUBLIC, "ordt_data operator&(const ordt_data& rhs)");
		nMethod.addStatement("ordt_data temp;");
		nMethod.addStatement("for (int idx=0; idx<this->size(); idx++) ");
		nMethod.addStatement("   if (idx < rhs.size()) temp.at(idx) = this->at(idx) & rhs.at(idx);");
		nMethod.addStatement("   else temp.at(idx) = 0;");
		nMethod.addStatement("return temp;");
		
		// | overload
		nMethod = newClass.addMethod(Vis.PUBLIC, "ordt_data operator|(const ordt_data& rhs)");
		nMethod.addStatement("ordt_data temp;");
		nMethod.addStatement("for (int idx=0; idx<this->size(); idx++) ");
		nMethod.addStatement("   if (idx < rhs.size()) temp.at(idx) = this->at(idx) | rhs.at(idx);");
		nMethod.addStatement("   else temp.at(idx) = this->at(idx);");
		nMethod.addStatement("return temp;");

		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(hppBw, newClass.genMethods(true, true));  // template methods with namespace
		writeStmts(cppBw, newClass.genMethods(true));  // non-template methods with namespace
		
		//writeStmt(hppBw, 0, "");
		//writeStmt(hppBw, 0, "std::ostream& operator<<(std::ostream &strm, const ordt_data &d) {");
		//writeStmt(hppBw, 0, "  strm << \"{ 0x\" << std::hex;");
		//writeStmt(hppBw, 0, "  for(std::vector<int>::reverse_iterator rit = d.rbegin(); rit!= d.rend(); ++rit) strm << *rit << \" \";");
		//writeStmt(hppBw, 0, "  return strm << std::dec << \"}");
		//writeStmt(hppBw, 0, "}");
		//writeStmt(hppBw, 0, "");
	}*/

	// --- DrvModBuilder overrides
	
	@Override
	public void processRegSetInstance(DrvModRegSetInstance drvModRegSetInstance) {
		count++;	
	}

	@Override
	public void processRegInstance(DrvModRegInstance drvModRegInstance) {
		count++;
	}

}
