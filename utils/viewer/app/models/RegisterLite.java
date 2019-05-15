package models;

import java.util.ArrayList;
import java.util.List;

/** Register class with alias and longText removed for smaller data transfer */
public class RegisterLite {

	public int id;
    public String name;
    public Long baseAddr;
    public Long highAddr;
    public Short width;
    public Boolean extRoot = false;
    public boolean isRegset = false;
    public Boolean isMap = false;
    public int reps = 1;
    public int stride = 4;
    public String shortText;
	public byte depth;
	public short loadId;
    public String access;
	public Integer catCode = 0;
    public Boolean donttest = false;
    public String parentPath;  // this is overridden in root level display to show load label

    /** create RegisterLite from a Register */
    public RegisterLite(Register reg, boolean includeParentPath) {
    	id = reg.id;
        name = reg.name;
        baseAddr = reg.baseAddr;
        highAddr = reg.highAddr;
        width = reg.width;
        extRoot = reg.extRoot;
        isRegset = reg.isRegset;
        isMap = reg.isMap;
        reps = reg.reps;
        stride = reg.stride;
        shortText = reg.shortText;
    	depth = reg.depth;
    	loadId = reg.loadId;
        access = reg.access;
        catCode = reg.catCode;
        donttest = reg.donttest;
        if (includeParentPath) parentPath = reg.parentPath;
    }
    
    /** create a list of RegisterLite from a RegisterList */
    public static List<RegisterLite> getList (List<Register> reglist, boolean includeParentPath) {
    	List<RegisterLite> newlist = new ArrayList<RegisterLite>();
    	for (Register reg : reglist) {
    		newlist.add(new RegisterLite(reg, includeParentPath));
    	}
    	return newlist;
    }
}
