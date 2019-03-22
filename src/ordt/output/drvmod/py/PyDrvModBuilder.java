package ordt.output.drvmod.py;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;

import ordt.extract.Ordt;
import ordt.extract.RegModelIntf;
import ordt.extract.RegNumber.NumBase;
import ordt.extract.RegNumber.NumFormat;
import ordt.output.drvmod.DrvModBaseInstance;
import ordt.output.drvmod.DrvModBuilder;
import ordt.output.drvmod.DrvModRegInstance;
import ordt.output.drvmod.DrvModRegInstance.DrvModField;
import ordt.output.drvmod.DrvModRegSetInstance.DrvModRegSetChildInfo;
import ordt.output.drvmod.DrvModRegSetInstance;
import ordt.output.drvmod.py.PyBaseModClass.PyMethod;
import ordt.parameters.ExtParameters;

/** generate python pio driver model */
public class PyDrvModBuilder extends DrvModBuilder {  // Note no OutputBuilder overrides in PyDrvModBuilder - these are in DrvModBuilder 
	//private BufferedWriter hppBw;
	private BufferedWriter pyBw;

	private static PyBaseModClass rootClass;
		
	public PyDrvModBuilder(RegModelIntf model) {  
		super(model);
	}

	// ----------- DrvModBuilder overrides
	
	@Override
	/** add a regset instance to the build */
	public void processRegSetInstance(DrvModRegSetInstance rsInst) {
		PyMethod rootBuild = rootClass.getTaggedMethod("build");
		// create the new instance
		rootBuild.addStatement(rsInst.getInstName() + " = ordt_drv_regset('" + rsInst.getName() + "')");
		// add this instance's children
		HashMap<DrvModRegSetChildInfo, Integer> children = rsInst.getChildMaps();
		for(DrvModRegSetChildInfo childInfo: children.keySet()) {
			DrvModBaseInstance child = childInfo.child;
			rootBuild.addStatement(rsInst.getInstName() + ".add_child(" + children.get(childInfo) + ", " + child.getInstName() + ", " + childInfo.reps + ", " + childInfo.addressOffset + ", " + childInfo.addressStride + ")");
		}
	}

	@Override
	public void processRegInstance(DrvModRegInstance rInst) {
		PyMethod rootBuild = rootClass.getTaggedMethod("build");
		// create the new instance
		rootBuild.addStatement(rInst.getInstName() + " = ordt_drv_reg('" + rInst.getName() + "', " + rInst.getWidth() + ")");
		// add this instance's fields
		List<DrvModField> fields = rInst.getFields();
		for(DrvModField field: fields) {
			String resetStr = (field.reset == null)? "None" : field.reset.toFormat(NumBase.Dec, NumFormat.Int);
			rootBuild.addStatement(rInst.getInstName() + ".add_field('" + field.name + "', " + field.lowIndex + ", " + field.width +
					", " + toPyBool(field.isReadable()) + ", " + toPyBool(field.isWriteable()) + ", " + resetStr + ")");
		}
	}
	
    //---------------------------- output write methods ----------------------------------------

    private static String toPyBool(String javaBool) {
		return "true".equals(javaBool)? "True" : "False";
	}

	/** required default write method - not used in PyDrvModBuilder*/
	@Override
	public void write(BufferedWriter bw) {  // TODO use default builder write?
	}
	
	/** write py output to specified output file(s)  
	 * @param outName - output file or directory
	 * @param description - text description of file generated
	 * @param commentPrefix - comment chars for this file type */
	@Override
	public void write(String outName, String description, String commentPrefix) {		
   	    // open the py file
    	pyBw = openBufferedWriter(outName, description);
    	if (pyBw != null) {

    		// write the py file header
    		writeHeader(pyBw, commentPrefix);
    		writeStmt(pyBw, 0, "");
    		
    		// write enum defines
    		writeEnums();
    		
    		// write the driver model classes
    		writeClasses();
    		
    		// close up py file
    		closeBufferedWriter(pyBw);
    	}
	}

	/** write enum defines ***/
	private void writeEnums() {
		writeStmt(pyBw, 0, "from enum import Enum, auto");
		writeStmt(pyBw, 0, "");
		writeStmt(pyBw, 0, "class ordt_drv_error(Enum):");
		writeStmt(pyBw, 0, "    BAD_TAG = auto()");
		writeStmt(pyBw, 0, "    BAD_PATH = auto()");
		writeStmt(pyBw, 0, "    BAD_ADDRESS = auto()");
		writeStmt(pyBw, 0, "");
		writeStmt(pyBw, 0, "class ordt_drv_return_type(Enum):");
		writeStmt(pyBw, 0, "    REG = auto()");
		writeStmt(pyBw, 0, "    REGSET = auto()");
		writeStmt(pyBw, 0, "    FIELD = auto()");
		writeStmt(pyBw, 0, "");
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
		PyMethod rootBuild = rootClass.getTaggedMethod("build");
        for(int overlay=0; overlay<rootInstances.size();overlay++) {
        	DrvModRegSetInstance inst = rootInstances.get(overlay).instance;
        	Long offset = rootInstances.get(overlay).relativeAddress;
			rootBuild.addStatement("self.add_child(" + (1 << overlay) + ", " + inst.getInstName() + ", 1, " + offset + ", 0)"); 
        }
	}

	// ------------------------- write driver model base classes -------------------------
	
	private void writeOrdtDrvBaseStructs() {
		// path element class (list of these are extracted from input path string)
		String className = "ordt_drv_path_element";
		PyBaseModClass newClass = new PyBaseModClass(className);
		// constructors
		//PyMethod nMethod = newClass.addConstructor("__init__(self, name, idx)");  
		//nMethod.addStatement("self.name = name");  
		//nMethod.addStatement("self.idx = idx");
		//
		PyMethod nMethod = newClass.addConstructor("__init__(self, name_str)");  
		nMethod.addStatement("    sub_lst = name_str.split('[')");  // split name and index
		nMethod.addStatement("    if len(sub_lst)==2:");
		nMethod.addStatement("        self.name = sub_lst[0]");  
		nMethod.addStatement("        self.idx = sub_lst[1].rstrip(']')");
		nMethod.addStatement("    else:");
		nMethod.addStatement("        self.name = name_str");  
		nMethod.addStatement("        self.idx = 1");
		// write class
		writeStmts(pyBw, newClass.genPy()); // header 
		
		// field info class 
		className = "ordt_drv_field";
		newClass = new PyBaseModClass(className);
		// constructors
		nMethod = newClass.addConstructor("__init__(self, name, loidx, width, readable, writeable, reset)");  
		nMethod.addInitCall("self.name = name");  // field name
		nMethod.addInitCall("self.loidx = loidx"); // low index of field
		nMethod.addInitCall("self.width = width");  // field width
		nMethod.addInitCall("self.readable = readable");
		nMethod.addInitCall("self.writeable = writeable");
		nMethod.addInitCall("self.reset = reset");
		// write class
		writeStmts(pyBw, newClass.genPy()); // header
	}

	/** create and write ordt_drv_element classes */    
	private void writeOrdtDrvElementClass() {
		String className = "ordt_drv_element";
		PyBaseModClass newClass = new PyBaseModClass(className);
		newClass.addDefine("ORDT_PIO_DRV_VERBOSE = True");
		// constructors
		PyMethod nMethod = newClass.addConstructor("__init__(self, name)");  
		nMethod.addInitCall("self.name = name");  // relative name of instance
		//
		nMethod = newClass.addMethod("get_address_using_version(self, version, pathstr, address_in)"); 
		nMethod.addStatement("path = self.get_pathlist(pathstr)");
		nMethod.addStatement("if path:");  
		nMethod.addStatement("    return self.get_address_using_list(version, path, False, address_in)");  // match names by default
		nMethod.addStatement("if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("    print('--> invalid path: ' + pathstr)");
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_PATH}");  
		//
		nMethod = newClass.addMethod("get_path_instance_list_using_version(self, version, pathstr, path_instance_list_in)"); 
		nMethod.addStatement("path = self.get_pathlist(pathstr)");
		nMethod.addStatement("if path:");  
		nMethod.addStatement("    return self.get_path_instance_list_using_list(version, path, False, path_instance_list_in)");  // match names by default
		nMethod.addStatement("if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("    print('--> invalid path: ' + pathstr)");
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_PATH}");  
		//
		nMethod = newClass.addMethod("get_version(self, tag)");
		String baseTag = ExtParameters.hasPyDrvDefaultTagName()? ExtParameters.pyDrvDefaultTagName() : rootInstances.get(0).instance.getName();  // root instance name of overlay 0 will be used as tag unless specified
		nMethod.addStatement("if tag == '" + baseTag + "':");
		nMethod.addStatement("    return 0");
		int idx=1;
		for (String tag: Ordt.getOverlayFileTags()) {
			nMethod.addStatement("else if tag == '" + tag + "':");
		    nMethod.addStatement("    return " + idx++);
		}
		nMethod.addStatement("else:");  // invalid tag
		nMethod.addStatement("    return -1");
		//
		nMethod = newClass.addMethod("get_tags(self)");
		nMethod.addStatement("tags = []");
		nMethod.addStatement("tags.append('" + baseTag + "')");
		for (String tag: Ordt.getOverlayFileTags()) 
			nMethod.addStatement("tags.append('" + tag + "')");
		nMethod.addStatement("return tags");
		//
		nMethod = newClass.addMethod("get_address_using_tag(self, tag, pathstr, address_in)"); 
		nMethod.addStatement("version = self.get_version(tag)");
		nMethod.addStatement("if version<0:");  
		nMethod.addStatement("    if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("        print('--> invalid tag: ' + tag)");
		nMethod.addStatement("    return {'error':ordt_drv_error.BAD_TAG}");  
		nMethod.addStatement("return self.get_address_using_version(version, pathstr, address_in)");  
		//
		nMethod = newClass.addMethod("get_path_instance_list_using_tag(self, tag, pathstr, path_instance_list_in)"); 
		nMethod.addStatement("version = self.get_version(tag)");
		nMethod.addStatement("if version<0:");  
		nMethod.addStatement("    if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("        print('--> invalid tag: ' + tag)");
		nMethod.addStatement("    return {'error':ordt_drv_error.BAD_TAG}");  
		nMethod.addStatement("return self.get_path_instance_list_using_version(version, pathstr, path_instance_list_in)");  
		//
		nMethod = newClass.addMethod("get_pathlist(self, pathstr)");
		nMethod.addStatement("pathlist = []");
		nMethod.addStatement("lst = pathstr.split('.')");  // split
		nMethod.addStatement("for str_elem in lst:");
		nMethod.addStatement("    path_elem = ordt_drv_path_element(str_elem)");
		nMethod.addStatement("    pathlist.append(path_elem)");
		nMethod.addStatement("return pathlist");
		//
		nMethod = newClass.addMethod("get_path_using_tag(self, tag, address, path_in)"); 
		nMethod.addStatement("version = self.get_version(tag)");
		nMethod.addStatement("if version<0:");  
		nMethod.addStatement("    if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("        print('--> invalid tag: ' + tag)");
		nMethod.addStatement("    return {'error':ordt_drv_error.BAD_TAG}");  
		nMethod.addStatement("return self.get_path_using_version(version, address, path_in)");  
		// write class
		writeStmts(pyBw, newClass.genPy()); // header with no include guards
	}
		
	/** create and write ordt_drv_element and ordt_drv_regset_child classes */    
	private void writeOrdtDrvRegsetChildClass() {
		// regset child class 
		String className = "ordt_drv_regset_child";
		PyBaseModClass newClass = new PyBaseModClass(className);
		// constructors
		PyMethod nMethod = newClass.addConstructor("__init__(self, version_map, child, reps, offset, stride)");  
		nMethod.addInitCall("self.version_map = version_map");  
		nMethod.addInitCall("self.child = child");   
		nMethod.addInitCall("self.reps = reps");  // number of reps in this instance
		nMethod.addInitCall("self.offset = offset");
		nMethod.addInitCall("self.stride = stride");
        // (match_addr, match_path) = child.find_offset(address_in)
		nMethod = newClass.addMethod("find_offset(self, address_in)");  // return matching offset and name (null name indicates no match)
		nMethod.addStatement("if not self.stride:");  // if no stride, this is root, so offset is zero
		nMethod.addStatement("    return (0, self.child.name)");
		nMethod.addStatement("if (address_in < self.offset) or (address_in >= self.offset + self.reps*self.stride):");  // address_in out of range
		nMethod.addStatement("    return (0, None)");
		nMethod.addStatement("if self.reps < 2:");  // no reps so no index
		nMethod.addStatement("    return (self.offset, '.' + self.child.name)");
		nMethod.addStatement("index = (address_in - self.offset) // self.stride");  // multiple reps, so get matching index
		nMethod.addStatement("return (self.offset + index*self.stride, '.' + self.child.name + '[' + str(index) + ']')");
		// write class
		writeStmts(pyBw, newClass.genPy()); // header
	}
	
	/** create and write ordt_drv_regset class */    
	private void writeOrdtDrvRegSetClass() {
		String className = "ordt_drv_regset";
		PyBaseModClass newClass = new PyBaseModClass(className);
		newClass.addParent("ordt_drv_element");
		// constructors
		PyMethod nMethod = newClass.addConstructor("__init__(self, name)");  
		nMethod.addInitCall("super().__init__(name)");
		nMethod.addInitCall("self.children = []");
		// get_address method
		nMethod = newClass.addMethod("get_address_using_list(self, version, path, bypass_names, address_in)");  
		nMethod.addStatement("if not path:");  // exit with error if no path
		nMethod.addStatement("    return {'error':ordt_drv_error.BAD_PATH}");
		nMethod.addStatement("pelem = path[0]");
		nMethod.addStatement("if not bypass_names:");  
		nMethod.addStatement("    path.pop(0)");  // remove element from front of path
		nMethod.addStatement("    if not path:");    // now if path is empty we're done
		nMethod.addStatement("        return {'type':ordt_drv_return_type.REGSET, 'address':address_in, 'children':self.get_child_names(version)}");
		nMethod.addStatement("    pelem = path[0]");  // get child element from front of path
		nMethod.addStatement("for child in self.children:");  // find matching child
		nMethod.addStatement("    if ((1<<version) & child.version_map) and (bypass_names or (pelem.name == child.child.name)):");  // if a matching child, update address and make recursive call
		nMethod.addStatement("        address = address_in + child.offset");  // first add this regsets offset
		nMethod.addStatement("        if child.reps > 1:");  // add index offset if a replicated instance
		nMethod.addStatement("            address += child.stride*int(pelem.idx)");  // add index offset if a replicated instance
		nMethod.addStatement("        return child.child.get_address_using_list(version, path, False, address)");  // recursive call to matching child
		nMethod.addStatement("if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("    print('--> unable to find child ' + pelem.name + ' in regset ' + self.name)" );
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_PATH}");  
		// get_path_instance_list_using_list method
		nMethod = newClass.addMethod("get_path_instance_list_using_list(self, version, path, bypass_names, path_instance_list_in)");  
		nMethod.addStatement("if not path:");  // exit with error if no path
		nMethod.addStatement("    return {'error':ordt_drv_error.BAD_PATH}");
		nMethod.addStatement("pelem = path[0]");
		nMethod.addStatement("if not bypass_names:");  
		nMethod.addStatement("    path.pop(0)");  // remove element from front of path
		nMethod.addStatement("    if not path:");    // now if path is empty we're done
		nMethod.addStatement("        return {'type':ordt_drv_return_type.REGSET, 'instances':path_instance_list_in}");
		nMethod.addStatement("    pelem = path[0]");  // get child element from front of path
		nMethod.addStatement("for child in self.children:");  // find matching child
		nMethod.addStatement("    if ((1<<version) & child.version_map) and (bypass_names or (pelem.name == child.child.name)):");  // if a matching child, add to list and make recursive call
		nMethod.addStatement("        path_instance_list = path_instance_list_in.copy()");
		nMethod.addStatement("        path_instance_list.append(child)");  // append child to list
		nMethod.addStatement("        return child.child.get_path_instance_list_using_list(version, path, False, path_instance_list)");  // recursive call to matching child
		nMethod.addStatement("if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("    print('--> unable to find child ' + pelem.name + ' in regset ' + self.name)" );
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_PATH}");  
		// add_child method
		nMethod = newClass.addMethod("add_child(self, version_map, child, reps, offset, stride)");
		nMethod.addStatement("new_child = ordt_drv_regset_child(version_map, child, reps, offset, stride)");  
		nMethod.addStatement("self.children.append(new_child)");  
		// get_child_names method
		nMethod = newClass.addMethod("get_child_names(self, version)");  
		nMethod.addStatement("childnames = []");
		nMethod.addStatement("for child in self.children:");
		nMethod.addStatement("    if (1<<version) & child.version_map:");  // only get children matching version
		nMethod.addStatement("        childnames.append(child.child.name)");
		nMethod.addStatement("return childnames");  
		//  get_path_using_version
		nMethod = newClass.addMethod("get_path_using_version(self, version, address_in, path_in)");  
		//nMethod.addStatement("print('--> reg set name, address, path=', self.name, address_in, path_in)");
		nMethod.addStatement("for child in self.children:");
		nMethod.addStatement("    if (1<<version) & child.version_map:");  // only get children matching version 
		//nMethod.addStatement("        print('----> child name, offset, reps, stride=', child.child.name, child.offset, child.reps, child.stride)");
		nMethod.addStatement("        (match_addr, match_path) = child.find_offset(address_in)");
		nMethod.addStatement("        if match_path:"); // if a match, add to path, subtract offset, and call child
		nMethod.addStatement("            return child.child.get_path_using_version(version, address_in - match_addr, path_in + match_path)");
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_ADDRESS}");
        //
		// write class
		writeStmts(pyBw, newClass.genPy()); // header with no include guards
	}

	/** create and write ordt_drv_reg class */    
	private void writeOrdtDrvRegClass() {
		String className = "ordt_drv_reg";
		PyBaseModClass newClass = new PyBaseModClass(className);
		newClass.addParent("ordt_drv_element");
		// constructors
		PyMethod nMethod = newClass.addConstructor("__init__(self, name, width)");  
		nMethod.addInitCall("super().__init__(name)");
		nMethod.addInitCall("self.fields = []");
		nMethod.addInitCall("self.width = width");
		// get_address method
		nMethod = newClass.addMethod("get_address_using_list(self, version, path, bypass_names, address_in)");  
		nMethod.addStatement("if not path:");  // exit with error if no path
		nMethod.addStatement("    return {'error':ordt_drv_error.BAD_PATH}");
		nMethod.addStatement("path.pop(0)");  // remove element from front of path
		nMethod.addStatement("if not path:");    // now if path is empty we're done
		nMethod.addStatement("    return {'type':ordt_drv_return_type.REG, 'address':address_in, 'width':self.width, 'fields':self.fields}");  
		nMethod.addStatement("if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("    pelem = path[0]");  // get child element from front of path
		nMethod.addStatement("    print('--> invalid child ' + pelem.name + ' specified in reg ' + self.name)");
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_PATH}");
		// get_path_instance_list method
		nMethod = newClass.addMethod("get_path_instance_list_using_list(self, version, path, bypass_names, path_instance_list_in)");  
		nMethod.addStatement("if not path:");  // exit with error if no path
		nMethod.addStatement("    return {'error':ordt_drv_error.BAD_PATH}");
		nMethod.addStatement("path.pop(0)");  // remove element from front of path
		nMethod.addStatement("if not path:");    // now if path is empty we're done
		nMethod.addStatement("    return {'type':ordt_drv_return_type.REG, 'instances':path_instance_list_in}");
		nMethod.addStatement("if len(path) == 1:");  
		nMethod.addStatement("    fname = path[0].name");  // get child element from front of path
		nMethod.addStatement("    fld = self.get_field_by_name(fname)");  
		nMethod.addStatement("    if fld:");  
		nMethod.addStatement("        path_instance_list = path_instance_list_in.copy()");
		nMethod.addStatement("        path_instance_list.append(fld)");  // append child to list
		nMethod.addStatement("        return {'type':ordt_drv_return_type.FIELD, 'instances':path_instance_list}");
		nMethod.addStatement("if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("    pelem = path[0]");  // get child element from front of path
		nMethod.addStatement("    print('--> invalid child ' + pelem.name + ' specified in reg ' + self.name)");
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_PATH}");
		//
		nMethod = newClass.addMethod("get_field_by_name(self, name)");
		nMethod.addStatement("for fld in self.fields:");  
		nMethod.addStatement("    if fld.name == name:");  
		nMethod.addStatement("        return fld");  
		//
		nMethod = newClass.addMethod("add_field(self, name, loidx, width, readable, writeable, reset)");
		nMethod.addStatement("new_field = ordt_drv_field(name, loidx, width, readable, writeable, reset)");  
		nMethod.addStatement("self.fields.append(new_field)");  
		//  get_path_using_version
		nMethod = newClass.addMethod("get_path_using_version(self, version, address_in, path_in)");  
		//nMethod.addStatement("print('--> reg name, address, path=', self.name, address_in, path_in)");
		nMethod.addStatement("return {'type':ordt_drv_return_type.REG, 'path':path_in}");  // just return input path since already a match  
		// write class
		writeStmts(pyBw, newClass.genPy()); // header with no include guards
	}

	/** create ordt_drv_root class */    
	private PyBaseModClass createOrdtDrvRootClass() {
		String className = "ordt_drv_root";
		PyBaseModClass newClass = new PyBaseModClass(className);
		newClass.addParent("ordt_drv_regset");
		newClass.addDefine("base_address = 0");  // call build in root constuctor
		// constructors
		PyMethod nMethod = newClass.addConstructor("__init__(self)");  
		nMethod.addInitCall("super().__init__('root')");
		nMethod.addStatement("self.build()");  // call build in root constuctor
		// methods
		nMethod = newClass.addMethod("build(self)"); 
		newClass.tagMethod("build", nMethod);
		// override of get_address_using_version to bypass names at root level
		nMethod = newClass.addMethod("get_address_using_version(self, version, pathstr, address_in)"); 
		nMethod.addStatement("path = self.get_pathlist(pathstr)");
		nMethod.addStatement("if path:");  
		nMethod.addStatement("    return self.get_address_using_list(version, path, True, address_in)");  // skip name match at root level
		nMethod.addStatement("if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("    print('--> invalid path: ' + pathstr)");
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_PATH}");  
		// override of get_path_instance_list_using_version to bypass names at root level
		nMethod = newClass.addMethod("get_path_instance_list_using_version(self, version, pathstr, path_instance_list_in)"); 
		nMethod.addStatement("path = self.get_pathlist(pathstr)");
		nMethod.addStatement("if path:");  
		nMethod.addStatement("    return self.get_path_instance_list_using_list(version, path, True, path_instance_list_in)");  // skip name match at root level
		nMethod.addStatement("if __class__.ORDT_PIO_DRV_VERBOSE:");  
		nMethod.addStatement("    print('--> invalid path: ' + pathstr)");
		nMethod.addStatement("return {'error':ordt_drv_error.BAD_PATH}");  
		//
		nMethod = newClass.addMethod("get_address(self, tag, pathstr)"); 
		nMethod.addStatement("'''return address given a path string and tag'''");  
		nMethod.addStatement("return self.get_address_using_tag(tag, pathstr, __class__.base_address)");  
		//
		nMethod = newClass.addMethod("get_path_instance_list(self, tag, pathstr)"); 
		nMethod.addStatement("'''return list of model elements given a path string and tag'''");  
		nMethod.addStatement("return self.get_path_instance_list_using_tag(tag, pathstr, [])");  // start with empty list
		//
		nMethod = newClass.addMethod("get_path(self, tag, address)"); 
		nMethod.addStatement("'''return path string given an address and tag'''");  
		nMethod.addStatement("return self.get_path_using_tag(tag, address, '')");  
		// return the root class
		return newClass;
	}
	
	/** write the ordt_drv_root class */    
	private void writeOrdtDrvRootClass() {
		writeStmts(pyBw, rootClass.genPy());
	}
}
