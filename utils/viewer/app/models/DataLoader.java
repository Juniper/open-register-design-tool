package models;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.UserPrincipal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Stack;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.w3c.dom.Node;

import play.db.jpa.JPA;
import play.libs.XML;

public class DataLoader {

	/** load specified xml file - return load id on success else null */
	public static Short load(String xmlFile, String label) {
		
		//System.out.println("DataLoader load file=" + xmlFile);
		if (xmlFile == null) return null;
		try {
        	InputStream inStream = System.in;
        	if ( xmlFile!=null ) inStream = new FileInputStream(xmlFile);

        	Document dom = XML.fromInputStream(inStream, "utf-8");
            //dom.getElementsByTagName("*")	

            // get the first element
            Element root = dom.getDocumentElement();
            
			// verify root is a map or regset element
	        String rootName = root.getNodeName();
			//System.out.println("DataLoader load: root elemet, name=" + rootName);
	        boolean isRegSet = "regset".equals(rootName);
	        boolean isMap = "map".equals(rootName);
	        if (!isRegSet && !isMap) return null;
            
	        // only allow load of jrdl xml beyond min version
	        Double minVersion = 160420.01;  // min allowed version for this xml loader
	        Attr versionNd = root.getAttributeNode("version");
	        if (versionNd != null) {
	        	//System.out.println("Dataloader: found version = " + versionNd.getTextContent());
	        	String versStr = versionNd.getTextContent();
	        	Double fileVersion = Double.valueOf(versStr);
	        	if (fileVersion < minVersion) return null;
	        }
            // create the load table entry
            Load ld = new Load();
            ld.file = trim255(xmlFile);
            ld.label = trim255(label);
            ld.user = trim255(getFileOwner(xmlFile));  
            Calendar calendar = Calendar.getInstance();
            ld.timestamp = new Timestamp(calendar.getTime().getTime());
    		JPA.em().persist(ld);  // add ld to the db (adding here for db integ checks)
        	//System.out.println("Dataloader: ld.id = " + ld.id);
    		
			// recursively add all reg elements
            Stack<Integer> ancestorIds = new Stack<Integer>();  // create empty ancestor stack
			Integer rootRegId = extractRegElements(root, true, isMap, ancestorIds, ld.id);  // root is a regset and has no ancestors
			//System.out.println("DataLoader: root id=" + rootRegId);
			// save the root id back into the load table
            if (rootRegId != null) {
            	ld.rootRegId = rootRegId;
            	return ld.id;
            }	
            // xml extract failed so clean up
            Load.deleteId(Integer.valueOf(ld.id));
            return null;
		}
		catch (Exception e) {
	         //e.printStackTrace();  // file not found only?
			return null;
		}
	}
	
	/* get the owner of specified input file */
	private static String getFileOwner(String xmlFileName) {
		//String retval = System.getProperty("user.name");
		File xmlFile = new File(xmlFileName);
		UserPrincipal user;
		try {
			user = java.nio.file.Files.getOwner(xmlFile.toPath());
		} catch (IOException e) {
			return "unknown";
		}
		return user.getName();
	}

	/** recursively extract all reg elements from dom and add to db 
	 * @param id 
	 * @return id of the root register/regset extracted */
	private static Integer extractRegElements(Node nd, boolean isRegset, boolean isMap, Stack<Integer> ancestorIds, short id) {
		//System.out.println("DataLoader extractRegElements: entered");
        // get the parent's db id and stack depth 
		Integer depth = ancestorIds.size();
        // save the current node to the db
		Register current = extractRegElement(nd, isRegset, isMap, depth, id);   
		if (current == null)  {
			System.err.println("Problem extracting xml reg/regset element");
			return null;
		}
		//System.out.println("DataLoader extractRegElements: reg/regset element id=" + current.id + ", name=" + current.name);

        // push current reg id onto the stack
		ancestorIds.push(current.id); 
		// now add all ancestors of this reg to descendants table
		for (Integer i: ancestorIds) { 
			add_descendant(i, current.id);  // current is descendant of all ids on the stack
		}
		
        // recursively process all children of this node
        NodeList nodes = nd.getChildNodes();  // get all child nodes
        for (int i = 0; i < nodes.getLength(); i++) {   
        	Node childNd = nodes.item(i);
            // only process reg and regset child elements
            if (childNd.getNodeType() == Node.ELEMENT_NODE) {
            	String ndText = childNd.getNodeName();
            	boolean childIsReg = "reg".equals(ndText);
            	boolean childIsMap = "map".equals(ndText);
            	boolean childIsRegset = childIsMap || "regset".equals(ndText);
            	if (childIsReg || childIsRegset) {  
            		Integer newId = extractRegElements(childNd, childIsRegset, childIsMap, ancestorIds, id );  // recursive call
            		if (newId == null) return null;  // abort on error
            	}
            }
        }
        // done with the children so pop the stack
		ancestorIds.pop(); 
		return current.id;
	}

	private static void add_descendant(int parent, int descendant) {
		Descendant dec = new Descendant();
		dec.parent = parent;
		dec.descendant = descendant;
		JPA.em().persist(dec);  // add this parent/descendant pair
	}

	/** extract a register object from a dom node  */ 
	private static Register extractRegElement(Node nd, boolean isRegset, boolean isMap, int depth, short loadId) {
		List<Field> fields = new ArrayList<Field>();
		Register reg = new Register();
		reg.isRegset = isRegset;
		reg.isMap = isMap;
		reg.depth = (byte) depth;
		reg.loadId = loadId;
		boolean idFound = false;
		// loop thru all reg child nodes and extract object info
        NodeList childNodes = nd.getChildNodes();  // get all child nodes
        for (int i = 0; i < childNodes.getLength(); i++) {
        	Node childNd = childNodes.item(i);
        	String ndText = childNd.getTextContent();
        	String ndName = childNd.getNodeName();
        	if ("id".equals(ndName)) {
        		idFound = true;
        		reg.name = trim255(ndText);
                //System.out.println("register id=" + reg.name);
        	}
        	else if ("baseaddr".equals(ndName)) {
        		reg.baseAddr = Long.valueOf(ndText.replace("0x", ""), 16);
                //System.out.println("register addr=" + reg.address);
        	}
        	else if ("highaddr".equals(ndName)) {
        		reg.highAddr = Long.valueOf(ndText.replace("0x", ""), 16);
        	}
        	else if ("width".equals(ndName)) {
        		reg.width = Short.valueOf(ndText);
        		reg.stride = reg.width/8;
                //System.out.println("register width=" + reg.width);
        	}
        	else if ("reps".equals(ndName)) {
        		reg.reps = Integer.valueOf(ndText);
        	}
        	else if ("extroot".equals(ndName)) {
        		reg.extRoot = true;
        	}
        	else if ("stride".equals(ndName)) {
        		reg.stride = Integer.valueOf(ndText.replace("0x", ""), 16);
        	}
        	else if ("shorttext".equals(ndName)) {
        		reg.shortText = trim255(ndText);
                //System.out.println("register text=" + reg.shortText);
        	}
        	else if ("longtext".equals(ndName)) {
        		reg.longText = reg.longText + ndText + "<br>";  
        		//if (childNd.hasChildNodes() && (childNd.getFirstChild().getNodeType() == Node.TEXT_NODE)) System.out.println("Text node n=" + childNd.getChildNodes().getLength());
        		//if (childNd.hasChildNodes() && (childNd.getFirstChild().getNodeType() == Node.CDATA_SECTION_NODE)) System.out.println("CDATA node n=" + childNd.getChildNodes().getLength());
         	}
        	else if ("infotext".equals(ndName)) {
        		reg.longText = reg.longText + ndText + "<br>";
        	}
        	else if ("alias".equals(ndName)) {
        		reg.alias = true;
        	}
        	else if ("donttest".equals(ndName)) {
        		reg.donttest = true;
        	}
        	else if ("access".equals(ndName)) {
        		reg.access = trim255(ndText);
        	}
        	else if ("catcode".equals(ndName)) {
        		reg.catCode = Integer.valueOf(ndText);
        	}
        	else if ("parentpath".equals(ndName)) {
        		reg.parentPath = trim255(ndText);
        	}
        	else if ("field".equals(ndName)) {
        		Field newField = extractFieldElement(childNd);
				if ((newField != null) && (idFound)) fields.add(newField);
				else System.err.println("problem extracting xml field element in register " + reg.name);

        	}
        	/*else if (ndName.contains("short")) {  
                System.out.println("unmatched elem=" + ndName + ", text=" + ndText);        		
        	} */
        	// TODO - set bits and fields based on returned field info?
       	}
        
        if (!idFound) return null;
        
        // if id was detected then add to db
		JPA.em().persist(reg);  // add reg/regset to the db
		// if fields found, add these also
		for (Field fld: fields) {
			fld.regId = reg.id;
			JPA.em().persist(fld);  // add field to the db  
		}

		return reg;
	}

	/** extract a field object from a dom node  */ 
	private static Field extractFieldElement(Node nd) {
		Field field = new Field();
		boolean idFound = false;
		// loop thru all field child nodes and extract object info
        NodeList childNodes = nd.getChildNodes();  // get all child nodes
        for (int i = 0; i < childNodes.getLength(); i++) {
        	Node childNd = childNodes.item(i);
        	String ndText = childNd.getTextContent();
        	String ndName = childNd.getNodeName();
        	if ("id".equals(ndName)) {
        		idFound = true;;
        		field.name = trim255(ndText);
                //System.out.println("field id=" + field.name);
        	}
        	else if ("lowidx".equals(ndName)) {
        		field.lowIdx = Short.valueOf(ndText);
                //System.out.println("field lowidx=" + field.lowIdx);
        	}
        	else if ("width".equals(ndName)) {
        		field.width = Short.valueOf(ndText);
        	}
        	else if ("access".equals(ndName)) {
        		field.access = trim255(ndText);
        	}
        	else if ("reset".equals(ndName)) {
        		field.rst = trim255(ndText);
        	}
        	else if ("hwmod".equals(ndName)) {
        		field.hwMod = true;
        	}
        	else if ("dontcompare".equals(ndName)) {
        		field.dontcompare = true;
        	}
        	else if ("subcatcode".equals(ndName)) {
        		field.subCatCode = Integer.valueOf(ndText);
        	}
        	else if ("shorttext".equals(ndName)) {
        		field.shortText = trim255(ndText);
                //System.out.println("register text=" + reg.shortText);
        	}
        	else if ("longtext".equals(ndName)) {
        		field.longText = ndText.replaceAll("-br-", "<br>") + "<br>" + field.longText;
        	}
        	else if ("infotext".equals(ndName)) {
        		field.longText = field.longText + ndText + "<br>";
        	}
        	else if ("hwinfo".equals(ndName)) {
        		String hwInfo = getHwInfo(childNd);
        		//System.out.println("found hwinfo, txt=" + hwInfo);
        		if (!hwInfo.isEmpty()) field.longText += hwInfo + "<br>";
        	}
        	else if ("enum_encode".equals(ndName)) {
        		String enumInfo = getEnumInfo(childNd);
        		//System.out.println("found enuminfo, txt=" + enumInfo);
        		if (!enumInfo.isEmpty()) field.longText += enumInfo + "<br>";
        	}
        	else if ("counter".equals(ndName)) {
        		field.counter = true;
        		String counterInfo = getCounterInfo(childNd);
        		//System.out.println("found counter, txt=" + counterInfo);
        		if (!counterInfo.isEmpty()) field.longText += counterInfo + "<br>";
        	}
        	else if ("intr".equals(ndName)) {
        		field.intr = true;
        		String intrInfo = getIntrInfo(childNd);
        		//System.out.println("found intr, txt=" + intrInfo);
        		if (!intrInfo.isEmpty()) field.longText += intrInfo + "<br>";
        	}
       	}
		return idFound? field : null;
	}

	/** create string description of hw info */
	private static String getHwInfo(Node infoNd) {
		String outStr="";
		// loop thru all child nodes and build description
        NodeList childNodes = infoNd.getChildNodes();  // get all child nodes
        for (int i = 0; i < childNodes.getLength(); i++) {
        	Node childNd = childNodes.item(i);
        	String ndText = childNd.getTextContent();
        	String ndName = childNd.getNodeName();
        	if (childNd.getNodeType()==Node.ELEMENT_NODE) {
            	if (outStr.isEmpty()) outStr = "Rdl hw info: ";
            	else outStr += ", ";
            	if ("nextassign".equals(ndName)) outStr += "next=" + ndText;
            	else if (ndText.isEmpty()) outStr +=  ndName;
            	else outStr += ndName + "=" + ndText;
        	}
       	}
		return outStr;
	}
	
	/** create string description of enum */
	private static String getEnumInfo(Node infoNd) {
		String outStr="";
		// loop thru all child nodes and build encode description
        NodeList childNodes = infoNd.getChildNodes();  // get all child nodes
        for (int i = 0; i < childNodes.getLength(); i++) {
        	Node childNd = childNodes.item(i);
        	String ndText = childNd.getTextContent();
        	String ndName = childNd.getNodeName();
        	if (childNd.getNodeType()==Node.ELEMENT_NODE) {
           	   if ("enc_name".equals(ndName)) outStr += "Encoding: name=" + ndText;
               else if ("enc_elem".equals(ndName)) outStr += getEnumElemInfo(childNd);
        	}
       	}
		return outStr;
	}

	/** return string description of enum element */
	private static String getEnumElemInfo(Node infoNd) {
		String outStr="";
		// loop thru all child nodes and build encode description
        NodeList childNodes = infoNd.getChildNodes();  // get all child nodes
        String currentElemName = "";
        for (int i = 0; i < childNodes.getLength(); i++) {
        	Node childNd = childNodes.item(i);
        	String ndText = childNd.getTextContent();
        	String ndName = childNd.getNodeName();
        	if (childNd.getNodeType()==Node.ELEMENT_NODE) {
           	   if ("enc_elem_name".equals(ndName)) currentElemName = ndText;
               else if ("enc_elem_value".equals(ndName)) outStr += "<br>*   " + ndText + ": " + currentElemName;
        	}
       	}
		return outStr;
	}

	/** create string description of counter info */
	private static String getIntrInfo(Node infoNd) {
		String outStr="";
		// loop thru all child nodes and build description
        NodeList childNodes = infoNd.getChildNodes();  // get all child nodes
        for (int i = 0; i < childNodes.getLength(); i++) {
        	Node childNd = childNodes.item(i);
        	String ndText = childNd.getTextContent();
        	String ndName = childNd.getNodeName();
        	if (childNd.getNodeType()==Node.ELEMENT_NODE) {
            	if (outStr.isEmpty()) outStr = "Rdl interrupt info: ";
            	else outStr += ", ";
            	if ("input".equals(ndName)) outStr += "intr input=" + ndText;
            	else if (ndText.isEmpty()) outStr +=  ndName.startsWith("halt")? ndName + "(poll)" : ndName;
            	else outStr += (ndName.startsWith("halt")? ndName + "(poll)" : ndName) + "=" + ndText;
        	}
       	}
		return outStr;
	}

	/** create string description of counter info */
	private static String getCounterInfo(Node infoNd) {
		String outStr="";
		// loop thru all child nodes and build description
        NodeList childNodes = infoNd.getChildNodes();  // get all child nodes
        for (int i = 0; i < childNodes.getLength(); i++) {
        	Node childNd = childNodes.item(i);
        	String ndText = childNd.getTextContent();
        	String ndName = childNd.getNodeName();
        	if (childNd.getNodeType()==Node.ELEMENT_NODE) {
            	if (outStr.isEmpty()) outStr = "Rdl counter info: ";
            	else outStr += ", ";
            	if ("incr".equals(ndName)) outStr += getIncDecCounterInfo(childNd, true);
            	else if ("decr".equals(ndName)) outStr += getIncDecCounterInfo(childNd, false);
            	else if (ndText.isEmpty()) outStr +=  ndName;
            	else outStr += ndName + "=" + ndText;
        	}
       	}
		return outStr;
	}

	/** create string description of incr/decr counter info */
	private static String getIncDecCounterInfo(Node infoNd, boolean isIncr) {
		String outStr="";
		// loop thru all child nodes and build description
        NodeList childNodes = infoNd.getChildNodes();  // get all child nodes
        for (int i = 0; i < childNodes.getLength(); i++) {
        	Node childNd = childNodes.item(i);
        	String ndText = childNd.getTextContent();
        	String ndName = childNd.getNodeName();
        	if (childNd.getNodeType()==Node.ELEMENT_NODE) {
            	if (!outStr.isEmpty()) outStr += ", ";
            	if (ndText.isEmpty()) outStr +=  ndName;
            	else outStr += ndName + "=" + ndText;
        	}
       	}
		return (isIncr? "incrementing (" : "decrementing (") + outStr + ")";
	}

	/** truncate a string to 255 characters for storage in varchar(255) */
	public static String trim255(String inStr) {
		if ((inStr==null) || (inStr.length()<255)) return inStr;
    	System.out.println("Dataloader truncating string to 255 characters: " + inStr);
		return inStr.substring(0, 255);
	}
}

