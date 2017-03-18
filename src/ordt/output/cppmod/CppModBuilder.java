/*
 * Copyright (c) 2016 Juniper Networks, Inc. All rights reserved.
 */
package ordt.output.cppmod;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import ordt.extract.RegModelIntf;
import ordt.output.FieldProperties;
import ordt.output.OutputBuilder;
import ordt.output.drvmod.cpp.CppBaseModClass.CppMethod;
import ordt.output.drvmod.cpp.CppBaseModClass.Vis;

public class CppModBuilder extends OutputBuilder {

	private BufferedWriter hppBw;
	private BufferedWriter cppBw;
	private Stack<CppModClass> activeModClasses = new Stack<CppModClass>(); // stack of active classes being defined
	private List<CppModClass> modClasses = new ArrayList<CppModClass>(); // ordered list of classes to be created
	
	private static HashSet<String> reservedWords = getReservedWords();
	
    //---------------------------- constructor ----------------------------------

    public CppModBuilder(RegModelIntf model) {
	    this.model = model;  // store the model ref
	    setVisitEachReg(false);   // only need to call once for replicated reg groups
	    setVisitEachRegSet(false);   // only need to call once for replicated reg set groups
	    setVisitExternalRegisters(true);  // we will visit externals 
	    setVisitEachExternalRegister(false);	    // handle externals as a group
	    setAllowLocalMapInternals(true);  // cascaded addrmaps will result in local non-ext regions   
	    model.getRoot().generateOutput(null, this);   // generate output structures recursively starting at model root
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

    //---------------------------- OutputBuilder methods to load structures ----------------------------------------

	@Override
	public void addField() {
		//System.out.println("CppModBuilder: addField id=" + fieldProperties.getPrefixedId() + ", reset=" + fieldProperties.getReset());
	}

	@Override
	public void addAliasField() {    
		// handle same as non-aliased field
		addField();
	}

	@Override
	public void addRegister() {
		//	System.out.println("CppModBuilder addRegister: " + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress());
		// create a new CppModClass corresponding to this reg and push it onto the active stack
		CppModClass newClass = CppModClass.createReg(getCppRegClassName());
		activeModClasses.push(newClass);			
	}

	@Override
	public void finishRegister() {
		//	System.out.println("CppModBuilder finishRegister: " + regProperties.getInstancePath() + ", base=" + regProperties.getBaseAddress());	
		// add field info to this reg using sorted fieldList
		addFieldInfo();
		// only add the register if it is sw accessible
		if (regProperties.isSwWriteable() || regProperties.isSwReadable()) {
			// done with this regset so pop it and add to the output list
			CppModClass currentModClass = activeModClasses.pop();
			// dont add reg to output if prune is set
			if (!regProperties.cppModPrune()) {
				modClasses.add(currentModClass); 
				//  add info for this reg to parent class 
				activeModClasses.peek().addChildRegInfo(getCppRegClassName(), regProperties.getId(), 
						regProperties.getRelativeBaseAddress(), regProperties.getRegByteWidth(), regProperties.getRepCount(), regProperties.getExtractInstance().getAddressIncrement());
			}
			//else
			//  System.out.println("CppModBuilder finishRegister: pruned reg found, ipath=" + regProperties.getInstancePath() + ", id=" + regProperties.getId() + ", base=" + regProperties.getFullBaseAddress());
		}
		// otherwise a non sw accessble reg, so just pop it from the stack
		else activeModClasses.pop();
	}

	@Override
	public void addRegSet() {
		//System.out.println("CppModBuilder addRegSet: " + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress() + ", stride=" + regSetProperties.getExtractInstance().getAddressIncrement());
		// create a new CppModClass corresponding to this regset and push it onto the active stack
		// if this is the root regset, treat special
		CppModClass newClass;
		if (activeModClasses.isEmpty())
		   newClass = CppModClass.createRootRegset(getCppRegsetClassName());
		else
		   newClass = CppModClass.createRegset(getCppRegsetClassName());
		activeModClasses.push(newClass);

	}

	@Override
	public void finishRegSet() {    
		//System.out.println("CppModBuilder finishRegSet: " + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", reps=" + regSetProperties.getRepCount() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress() + ", stride=" + regSetProperties.getExtractInstance().getAddressIncrement());
		// done with this regset so pop it and add to the output list
		CppModClass currentModClass = activeModClasses.pop();
		// if root, then update address range for constructor else add info for this regset to parent class 
		if (activeModClasses.isEmpty()) {
		    modClasses.add(currentModClass);
			currentModClass.addRootInitCall(getCppRegsetClassName(), regSetProperties.getFullBaseAddress(), regSetProperties.getFullHighAddress());
		}
		else if (currentModClass.hasChildren()) {  // only add regset if it contains some regs
		    modClasses.add(currentModClass);
			activeModClasses.peek().addChildRegsetInfo(getCppRegsetClassName(), regSetProperties.getId(), 
					regSetProperties.getRelativeBaseAddress(), regSetProperties.getAlignedSize(), regSetProperties.getRepCount(), regSetProperties.getExtractInstance().getAddressIncrement());			
		}
		//System.out.println("CppModBuilder finishRegSet: root found, ipath=" + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId() + ", base=" + regSetProperties.getFullBaseAddress() + ", high=" + regSetProperties.getFullHighAddress());
	}

	/** process root address map */
	@Override
	public void addRegMap() { 
		//System.out.println("CppModBuilder addRegMap: root found, ipath=" + regSetProperties.getBaseName() + ", id=" + regSetProperties.getId());
		addRegSet();
	}

	/** finish root address map  */
	@Override
	public  void finishRegMap() {
		finishRegSet();
	}

    //----------------------------  --------------------------------------

	/** get c++ class name for a regset from hierarchy name */
	private String getCppRegsetClassName() {
		String baseName = regSetProperties.getBaseName();
		String className = ((baseName == null) || (baseName.isEmpty()))? "ordt_root" : "ordt_rset_" + baseName;
		return className;
	}

	/** get c++ class name for a reg from hierarchy name */
	private String getCppRegClassName() {
		String baseName = regProperties.getBaseName();
		String className = "ordt_rg_" + baseName;
		return className;
	}

	/** add all fields info to active reg class using fieldList */
	private void addFieldInfo() {
		for (FieldProperties fld: fieldList) {
			// add info for this field to the parent reg
			activeModClasses.peek().addChildFieldInfo(fld.getPrefixedId(), fld.getLowIndex(), fld.getFieldWidth(), fld.getReset(),
					getFieldReadAccessType(fld), getFieldWriteAccessType(fld));
			//System.out.println("CppModBuilder addFieldInfo: ipath=" + fld.getBaseName() + ", id=" + fld.getPrefixedId() );
		}	
	}
	
    //----------------------------  ----------------------------------------

	/** generate field read access type string 
	 * @param field */
	private String getFieldReadAccessType(FieldProperties field) {
		if (field.isRclr()) return "r_clr";
		else if (field.isSwReadable()) return "r_std"; 
		return "r_none";
	}

	/** generate field write access type string 
	 * @param field */
	private String getFieldWriteAccessType(FieldProperties field) {
		if (field.isWoset()) return "w_1set"; 
		else if (field.isWoclr()) return "w_1clr"; 
		else if (field.isSwWriteable()) return "w_std";
		return "w_none";
	}

    //---------------------------- output write methods ----------------------------------------

    /** required default write method - not used in CppModBuilder*/
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

   	    // open the hpp and cpp files
    	hppBw = openBufferedWriter(outName + "/ordt_pio.hpp", description);
    	cppBw = openBufferedWriter(outName + "/ordt_pio.cpp", description);
    	if ((hppBw != null) && (cppBw != null)) {

    		// write the hpp file header
    		writeHeader(hppBw, commentPrefix);
    		writeStmt(hppBw, 0, "#ifndef __ORDT_PIO_HPP_INCLUDED__");	
    		writeStmt(hppBw, 0, "#define __ORDT_PIO_HPP_INCLUDED__");
    		writeStmt(hppBw, 0, "");	
    		writeStmt(hppBw, 0, "#include <vector>");
    		writeStmt(hppBw, 0, "#include <iostream>");  
    		writeStmt(hppBw, 0, "#include <memory>");  // unique_ptr, etc g++  
    		writeStmt(hppBw, 0, "#include <cstdint>");  
    		writeStmt(hppBw, 0, "#include <algorithm>");  // min/max  
    		writeStmt(hppBw, 0, "#include <string>");    
    		writeStmt(hppBw, 0, "#include <sstream>");    
    		writeStmt(hppBw, 0, "#include <mutex>");    // reg mutex
    		writeStmt(hppBw, 0, "#include <atomic>");    // field data atomic
    	    writeStmt(hppBw, 0, "#define quote(x) #x"); // for debug display of vars
    		writeStmt(hppBw, 0, "");		   
   		
    		// write the cpp file header
    		writeHeader(cppBw, commentPrefix);
    		writeStmt(cppBw, 0, "#include \"ordt_pio.hpp\"");
    		writeStmt(cppBw, 0, "");
    		
    		// define r/w modes
    		writeStmt(hppBw, 0, "enum read_mode_t : uint8_t {r_none, r_std, r_clr};");
    		writeStmt(hppBw, 0, "enum write_mode_t : uint8_t {w_none, w_std, w_1clr, w_1set};");

    		// write the model classes
    		writeClasses();
    		
            // close up hpp file
    		writeStmt(hppBw, 0, "#endif // __ORDT_PIO_HPP_INCLUDED__");
    		closeBufferedWriter(hppBw);
    		
    		// close up cpp file
    		closeBufferedWriter(cppBw);
    	}
	}

	/** write model classes */
	private void writeClasses() {
		// write base classes
		writeOrdtDataClass();
		writeOrdtAddrElemClass();
		writeOrdtRegsetClass();
		writeOrdtAddrElemArrayClass();
		writeOrdtRegClass();
		writeOrdtFieldClass();  
		// write design specific classes
		writeDesignSpecificClasses();
	}

	/** write all design-specific c++ classes */
	private void writeDesignSpecificClasses() {
		for (CppModClass mClass: modClasses) {
			// write class
			writeStmts(hppBw, mClass.genHeader(false)); // header with no include guards
			writeStmts(cppBw, mClass.genMethods(true));  // methods with namespace			
		}	
	}

	/** create and write OrdtData class  */   
	private void writeOrdtDataClass() {
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
		
		/*
		writeStmt(hppBw, 0, "");
		writeStmt(hppBw, 0, "std::ostream& operator<<(std::ostream &strm, const ordt_data &d) {");
		writeStmt(hppBw, 0, "  strm << \"{ 0x\" << std::hex;");
		writeStmt(hppBw, 0, "  for(std::vector<int>::reverse_iterator rit = d.rbegin(); rit!= d.rend(); ++rit) strm << *rit << \" \";");
		writeStmt(hppBw, 0, "  return strm << std::dec << \"}");
		writeStmt(hppBw, 0, "}");
		writeStmt(hppBw, 0, "");
		*/
	}

	/** create and write OrdtAddrElem class  */
	private void writeOrdtAddrElemClass() {
		String className = "ordt_addr_elem";
		CppModClass newClass = new CppModClass(className);
		newClass.addDefine(Vis.PROTECTED, "uint64_t m_startaddress");
		newClass.addDefine(Vis.PROTECTED, "uint64_t m_endaddress");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(uint64_t _m_startaddress, uint64_t _m_endaddress)");  
		nMethod.addInitCall("m_startaddress(_m_startaddress)");
		nMethod.addInitCall("m_endaddress(_m_endaddress)");
		// methods
		newClass.addMethod(Vis.PUBLIC, "pure virtual void write(const uint64_t &addr, const ordt_data &wdata)");
		newClass.addMethod(Vis.PUBLIC, "pure virtual void read(const uint64_t &addr, ordt_data &rdata)");  
		nMethod = newClass.addMethod(Vis.PUBLIC, "bool containsAddress(const uint64_t &addr)");
		//nMethod.addStatement("std::cout << \"ordt_addr_elem containsAddress: addr=\"<< addr << \" start=\" << m_startaddress << \" end=\" << m_endaddress << \"\\n\";");
		nMethod.addStatement("return ((addr >= m_startaddress) && (addr <= m_endaddress));");
		nMethod = newClass.addMethod(Vis.PUBLIC, "bool isBelowAddress(const uint64_t &addr)");
		//nMethod.addStatement("std::cout << \"ordt_addr_elem isBelowAddress: addr=\"<< addr << \" start=\" << m_startaddress << \" end=\" << m_endaddress << \"\\n\";");
		nMethod.addStatement("return (addr > m_endaddress);");
		nMethod = newClass.addMethod(Vis.PUBLIC, "bool isAboveAddress(const uint64_t &addr)");
		//nMethod.addStatement("std::cout << \"ordt_addr_elem isAboveAddress: addr=\"<< addr << \" start=\" << m_startaddress << \" end=\" << m_endaddress << \"\\n\";");
		nMethod.addStatement("return (addr < m_startaddress);");
		nMethod = newClass.addMethod(Vis.PUBLIC, "bool hasStartAddress(const uint64_t &addr)");
		//nMethod.addStatement("std::cout << \"ordt_addr_elem hasStartAddress: addr=\"<< addr << \" start=\" << m_startaddress << \" end=\" << m_endaddress << \"\\n\";");
		nMethod.addStatement("return (addr == m_startaddress);");
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void update_child_ptrs()");  // empty placeholder defined in design-specific regset classes - called after elem copy in jdrl_addr_elem_array to fix pointers  
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}

	/** create and write OrdtRegset class  */
	private void writeOrdtRegsetClass() {
		String className = "ordt_regset";
		CppModClass newClass = new CppModClass(className);
		newClass.addParent("ordt_addr_elem");
		newClass.addDefine(Vis.PROTECTED, "std::vector<ordt_addr_elem *>  m_children");
		newClass.addDefine(Vis.PRIVATE, "ordt_addr_elem* childElem");  
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(uint64_t _m_startaddress, uint64_t _m_endaddress)");  
		nMethod.addInitCall("ordt_addr_elem(_m_startaddress, _m_endaddress)");
		// address search method
		nMethod = newClass.addMethod(Vis.PRIVATE, "ordt_addr_elem* findAddrElem(const uint64_t &addr)");  
		nMethod.addStatement("int lo = 0;");
		nMethod.addStatement("int hi = m_children.size()-1;");
		nMethod.addStatement("int mid = 0;");
		//nMethod.addStatement("for (int idx=lo; idx <= hi; idx++) {");
		//nMethod.addStatement("   std::cout << \"findAddrElem: idx=\"<< idx << \"....\\n\";this->m_children.at(idx)->containsAddress(addr); }");
		nMethod.addStatement("while (lo <= hi) {");
		nMethod.addStatement("   mid = (lo + hi) / 2;");
		//nMethod.addStatement("   std::cout << \"findAddrElem: looking for addr=\"<< addr << \" at idx=\" << mid << \", lo=\" << lo << \", hi=\" << hi << \"\\n\";");
		nMethod.addStatement("   if (m_children[mid]->containsAddress(addr)) {");
		nMethod.addStatement("      //outElem = m_children[mid];");
		//nMethod.addStatement("      std::cout << \"findAddrElem: ordt_regset contained addr=\"<< addr << \" at idx=\" << mid << \"\\n\";");
		nMethod.addStatement("      return m_children[mid];");
		nMethod.addStatement("   }");
		nMethod.addStatement("   else if (m_children[mid]->isAboveAddress(addr))");
		nMethod.addStatement("      hi = mid - 1;");
		nMethod.addStatement("   else");
		nMethod.addStatement("      lo = mid + 1;");
		nMethod.addStatement("}");
		//nMethod.addStatement("std::cout << \"findAddrElem: did not find addr=\"<< addr << \"\\n\";");
		nMethod.addStatement("return nullptr;");
		// methods
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void write(const uint64_t &addr, const ordt_data &wdata)");  
		//nMethod.addStatement("   std::cout << \"regset write: ---- addr=\"<< addr << \", data=\" << wdata.to_string() << \"\\n\";");
		nMethod.addStatement("   if (this->containsAddress(addr)) {");
		//nMethod.addStatement("      std::cout << \"regset write: ordt_regset contains addr=\"<< addr << \"\\n\";");
		nMethod.addStatement("      childElem = this->findAddrElem(addr);");
		nMethod.addStatement("      if (childElem != nullptr) { childElem->write(addr, wdata); return; }");
		//nMethod.addStatement("      else std::cout << \"write: findAddrElem returned nullptr, addr=\"<< addr << \"\\n\";" );
		nMethod.addStatement("   }");
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void read(const uint64_t &addr, ordt_data &rdata)");  
		//nMethod.addStatement("   std::cout << \"regset read: ---- addr=\"<< addr << \"\\n\";");
		nMethod.addStatement("   if (this->containsAddress(addr)) {");
		//nMethod.addStatement("      std::cout << \"regset read: ordt_regset contains addr=\"<< addr << \"\\n\";");
		nMethod.addStatement("      childElem = this->findAddrElem(addr);");
		nMethod.addStatement("      if (childElem != nullptr) { childElem->read(addr, rdata); return; }");
		//nMethod.addStatement("      else std::cout << \"read: findAddrElem returned nullptr, addr=\"<< addr << \"\\n\";" );
		nMethod.addStatement("   }");
		//nMethod.addStatement("   else {");
		//nMethod.addStatement("      std::cout << \"regset read: \" << typeid(this).name() << \" does not contains addr=\"<< addr << \"\\n\";");
		//nMethod.addStatement("   }");
		nMethod.addStatement("   rdata.clear();");
        
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}

	/** create and write OrdtAddrElemArray class  */ 
	private void writeOrdtAddrElemArrayClass() {
		// this is a template class so write directly to hpp file
		writeStmt(hppBw, 0, "");
		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "class ordt_addr_elem_array : public std::vector<T>, public ordt_addr_elem {");  // mult inheritance ordt_array_elem??
		writeStmt(hppBw, 0, "  protected:");
		writeStmt(hppBw, 0, "    std::vector<T> vec;");  
		writeStmt(hppBw, 0, "    uint64_t m_stride;");  
		writeStmt(hppBw, 0, "  public:");
		writeStmt(hppBw, 0, "    ordt_addr_elem_array(uint64_t _m_startaddress, uint64_t _m_endaddress, int _reps, uint64_t _m_stride);");
		writeStmt(hppBw, 0, "    virtual void write(const uint64_t &addr, const ordt_data &wdata);");
		writeStmt(hppBw, 0, "    virtual void read(const uint64_t &addr, ordt_data &rdata);");
		writeStmt(hppBw, 0, "};");
		writeStmt(hppBw, 0, "");

		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "ordt_addr_elem_array<T>::ordt_addr_elem_array(uint64_t _m_startaddress, uint64_t _m_endaddress, int _reps, uint64_t _m_stride)");
		writeStmt(hppBw, 0, "   : ordt_addr_elem(_m_startaddress, _m_endaddress + (_m_stride * _reps)), m_stride(_m_stride) {");
		writeStmt(hppBw, 0, "   this->reserve(_reps);");
		writeStmt(hppBw, 0, "   uint64_t el_startaddress = _m_startaddress;");
		writeStmt(hppBw, 0, "   uint64_t el_endaddress = _m_endaddress;");
		writeStmt(hppBw, 0, "   for(int idx=0; idx<_reps; idx++) {");
		writeStmt(hppBw, 0, "      std::unique_ptr<T> new_elem(new T(el_startaddress, el_endaddress));");  // push_back is a copy so manage ptr
		writeStmt(hppBw, 0, "      this->push_back(*new_elem);");  // this copy messes up children ptr assigns made in constructor so need update 
		writeStmt(hppBw, 0, "      this->back().update_child_ptrs();");  // update ptrs
		writeStmt(hppBw, 0, "      el_startaddress += _m_stride;");
		writeStmt(hppBw, 0, "      el_endaddress += _m_stride;");
		writeStmt(hppBw, 0, "   }");
		writeStmt(hppBw, 0, "}");
		writeStmt(hppBw, 0, "");

		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "void ordt_addr_elem_array<T>::write(const uint64_t &addr, const ordt_data &wdata) {");
		//writeStmt(hppBw, 0, "   std::cout << \"addr_elem array write: ---- addr=\"<< addr << \", data=\" << wdata.to_string() << \"\\n\";");
		writeStmt(hppBw, 0, "   if (this->containsAddress(addr)) {");
		writeStmt(hppBw, 0, "      int idx = (addr - m_startaddress) / m_stride;");
		//writeStmt(hppBw, 0, "      std::cout << \"addr_elem array write: array contains addr=\" << addr << \" at idx=\" << idx << \"\\n\";");
		writeStmt(hppBw, 0, "      if (idx < this->size()) this->at(idx).write(addr, wdata);");
		writeStmt(hppBw, 0, "      return;");
		writeStmt(hppBw, 0, "   }");
		writeStmt(hppBw, 0, "}");
		writeStmt(hppBw, 0, "");
		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "void ordt_addr_elem_array<T>::read(const uint64_t &addr, ordt_data &rdata) {");
		//writeStmt(hppBw, 0, "   std::cout << \"addr_elem array read: ---- addr=\"<< addr << \", start=\" << m_startaddress << \", end=\" << m_endaddress<< \", stride=\" << m_stride<< \"\\n\";");
		writeStmt(hppBw, 0, "   if (this->containsAddress(addr)) {");
		writeStmt(hppBw, 0, "      int idx = (addr - m_startaddress) / m_stride;");
		//writeStmt(hppBw, 0, "      std::cout << \"addr_elem array read: array contains addr=\" << addr << \" at idx=\" << idx << \", array size=\" << this->size() << \"\\n\";");
		writeStmt(hppBw, 0, "      if (idx < this->size()) this->at(idx).read(addr, rdata);");
		writeStmt(hppBw, 0, "      return;");
		writeStmt(hppBw, 0, "   }");
		//writeStmt(hppBw, 0, "   else {");
		//writeStmt(hppBw, 0, "      std::cout << \"addr_elem array read: \" << typeid(this).name() << \" does not contains addr=\"<< addr << \"\\n\";");
		//writeStmt(hppBw, 0, "   }");
		writeStmt(hppBw, 0, "   rdata.clear();");
		writeStmt(hppBw, 0, "}");
		writeStmt(hppBw, 0, "");
	}

	/** create and write OrdtReg class  */  
	private void writeOrdtRegClass() {  
		String className = "ordt_reg";
		CppModClass newClass = new CppModClass(className);
		newClass.addParent("ordt_addr_elem");
		// define mutex for reg
		newClass.addDefine(Vis.PROTECTED, "std::mutex  m_mutex");
		// constructors
		CppMethod nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(uint64_t _m_startaddress, uint64_t _m_endaddress)");  
		nMethod.addInitCall("ordt_addr_elem(_m_startaddress, _m_endaddress)");  
		// redefine copy constructor since we have a mutex
		nMethod = newClass.addConstructor(Vis.PUBLIC, className + "(const " + className + " &_old)");  
		nMethod.addInitCall("ordt_addr_elem(_old)");  
		nMethod.addInitCall("m_mutex()");  
		// write methods
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void write(const ordt_data &wdata)");  // will be overriden by child classes
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void write(const uint64_t &addr, const ordt_data &wdata)");
		// read methods
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void read(ordt_data &rdata)");  // will be overriden by child classes
		nMethod = newClass.addMethod(Vis.PUBLIC, "virtual void read(const uint64_t &addr, ordt_data &rdata)");  
		// write class
		writeStmts(hppBw, newClass.genHeader(false)); // header with no include guards
		writeStmts(cppBw, newClass.genMethods(true));  // methods with namespace
	}

	/** create and write write OrdtField class  */  
	private void writeOrdtFieldClass() {
		// this is a template class so write directly to hpp file
		writeStmt(hppBw, 0, "");
		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "class ordt_field {");
		writeStmt(hppBw, 0, "  public:");
		writeStmt(hppBw, 0, "    int lobit, size;");
		writeStmt(hppBw, 0, "    T data;");
		//writeStmt(hppBw, 0, "    std::atomic<T> data;");  // wrap data in atomic
		writeStmt(hppBw, 0, "    read_mode_t r_mode;");
		writeStmt(hppBw, 0, "    write_mode_t w_mode;");
		writeStmt(hppBw, 0, "    ordt_field(int _lobit, int _size, int _vsize, uint32_t _data, read_mode_t _r_mode, write_mode_t _w_mode);"); // special case for wide fields
		writeStmt(hppBw, 0, "    ordt_field(int _lobit, int _size, T _init_data, read_mode_t _r_mode, write_mode_t _w_mode);");
		// read/write 
		writeStmt(hppBw, 0, "    void write(const ordt_data &wdata);");
		writeStmt(hppBw, 0, "    void read(ordt_data &rdata);");
		// clear 
		//writeStmt(hppBw, 0, "    void clear(ordt_data &data);");
		writeStmt(hppBw, 0, "    void clear();");
		writeStmt(hppBw, 0, "};");
		// constructors
		writeStmt(hppBw, 0, "");
		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "ordt_field<T>::ordt_field(int _lobit, int _size, int _vsize, uint32_t _data, read_mode_t _r_mode, write_mode_t _w_mode)");
		writeStmt(hppBw, 0, "   : lobit(_lobit), size(_size), data(_vsize, _data), r_mode(_r_mode), w_mode(_w_mode) {");
		writeStmt(hppBw, 0, "}");
		writeStmt(hppBw, 0, "");
		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "ordt_field<T>::ordt_field(int _lobit, int _size, T _init_data, read_mode_t _r_mode, write_mode_t _w_mode)");
		writeStmt(hppBw, 0, "   : lobit(_lobit), size(_size), data(_init_data), r_mode(_r_mode), w_mode(_w_mode) {");
		writeStmt(hppBw, 0, "}");
		writeStmt(hppBw, 0, "");
		// read/write
		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "void ordt_field<T>::write(const ordt_data &wdata) {");
		//writeStmt(hppBw, 0, "   std::cout << \"field write: \" << \", mode=\" << w_mode << \"\\n\";");
		writeStmt(hppBw, 0, "   if (w_mode == w_std) wdata.get_slice(lobit, size, data);");
		writeStmt(hppBw, 0, "   else if (w_mode == w_1set) {");
		writeStmt(hppBw, 0, "      T mask_data;");
		writeStmt(hppBw, 0, "      wdata.get_slice(lobit, size, mask_data);");
		writeStmt(hppBw, 0, "      data = data | mask_data;");
		writeStmt(hppBw, 0, "   }");
		writeStmt(hppBw, 0, "   else if (w_mode == w_1clr) {");
		writeStmt(hppBw, 0, "      T mask_data;");
		writeStmt(hppBw, 0, "      wdata.get_slice(lobit, size, mask_data);");
		writeStmt(hppBw, 0, "      data = data & ~mask_data;");
		writeStmt(hppBw, 0, "   }");
		writeStmt(hppBw, 0, "}");
		writeStmt(hppBw, 0, "");
		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "void ordt_field<T>::read(ordt_data &rdata) {");
		//writeStmt(hppBw, 0, "   std::cout << \"field read: \" << \"\\n\";");
		writeStmt(hppBw, 0, "   rdata.set_slice(lobit, size, data);");
		writeStmt(hppBw, 0, "   if (r_mode == r_clr) clear();");
		writeStmt(hppBw, 0, "}");
		// clear 
		writeStmt(hppBw, 0, "");
		writeStmt(hppBw, 0, "template<typename T>");
		writeStmt(hppBw, 0, "void ordt_field<T>::clear() {");
		writeStmt(hppBw, 0, "    data = 0;");
		writeStmt(hppBw, 0, "}");
		writeStmt(hppBw, 0, "");
	}
	
}
