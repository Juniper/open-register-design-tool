package ordt.output.drvmod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DrvModRegSetInstance extends DrvModBaseInstance {
	
	HashMap<DrvModBaseInstance, Integer> childMaps = new HashMap<DrvModBaseInstance, Integer>(); // hash of child instances and encoded overlay use map 

	DrvModRegSetInstance(String name, int mapId, long addressOffset, int reps, long addressStride) {
		super(name, mapId, addressOffset, reps, addressStride);
	}

	/** get list of children with this instances mapId */
	public List<DrvModBaseInstance> getChildren() {
		return getChildren(this.mapId);
	}
	
	/** get list of children with specified mapId */
	private List<DrvModBaseInstance> getChildren(int mapId) {
		List<DrvModBaseInstance> outList = new ArrayList<DrvModBaseInstance>();
		for (DrvModBaseInstance inst: childMaps.keySet())
			if ((childMaps.get(inst) & (1<<mapId))>0) outList.add(inst);
		return outList;
	}

	/** add new child or add mapId to encoded value if a dup */
	public void addChild(DrvModBaseInstance child, int mapId) {
		Integer currentMap = childMaps.get(child);
		int newMap = (currentMap == null)? mapId : currentMap & (1<<mapId);
		childMaps.put(child, newMap);
	}

	@Override
	public int hashCode() {
		int hcode = hashCode(false, true, false);  // do not include address info, include child regsets, do not include reg info
		return hcode;  
	}

	@Override
	public int hashCode(boolean includeAddrInfo, boolean includeChildRegsets, boolean includeRegInfo) {
		final int prime = 31;
		int result = super.hashCode(includeAddrInfo);  // do not include address info
		if (includeChildRegsets) {
			result = prime * result + getChildListHashCode();  // include address info + no children
		}
		return result;
	}

	/** compute children hashCode  */
	private int getChildListHashCode() {
		  int hashCode = 1;
		  for (DrvModBaseInstance e : getChildren())
		      hashCode = 31*hashCode + (e==null ? 0 : e.hashCode(true, true, false));  // include address info, include child regsets, do not include reg info
		  return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		return equals(obj, false, true, false);  // do not include address info, include child regsets, do not include reg info
	}
	
	@Override
	public boolean equals(Object obj, boolean includeAddrInfo, boolean includeChildRegsets, boolean includeRegInfo) {
		if (this == obj)
			return true;
		if (!super.equals(obj, includeAddrInfo)) 
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrvModRegSetInstance other = (DrvModRegSetInstance) obj;
		if (includeChildRegsets) {
			if (!getChildListEquals(other.getChildren())) // include address info + no children
				return false;
		}
		return true;
	}
	
	/** compute childList equals */
	private boolean getChildListEquals(List<DrvModBaseInstance> otherList) {
		List<DrvModBaseInstance> children = getChildren();
		if ((otherList==null) || (children.size()!=otherList.size())) return false;
		for (int idx=0; idx<children.size(); idx++)
			if (!children.get(idx).equals(otherList.get(idx), true, true, false)) return false;  // include address info, include child regsets, do not include reg info
		return true;
	}

}
