package ordt.output.drvmod;

import java.util.ArrayList;
import java.util.List;

public class DrvModRegSetInstance extends DrvModBaseInstance {
	
	List<DrvModBaseInstance> children = new ArrayList<DrvModBaseInstance>(); // children from same overlay as this instance 
	List<DrvModBaseInstance> altChildren = new ArrayList<DrvModBaseInstance>(); // children from an alternate overlay (different child w/ same name/address)
	
	DrvModRegSetInstance(String name, int mapId, long addressOffset, int reps, long addressStride) {
		super(name, mapId, addressOffset, reps, addressStride);
	}

	public List<DrvModBaseInstance> getChildren() {
		return children;
	}
	
	public List<DrvModBaseInstance> getAltChildren() {
		return altChildren;
	}

	public void addChild(DrvModBaseInstance child) {
		if (this.mapId==child.getMapId()) // add to primary list if child is from same overlay
		    this.children.add(child);
		else  // add to alternate list if child is from different overlay
		    this.altChildren.add(child);
	}

	@Override
	public int hashCode() {
		return hashCode(false, true);  // do not include address info, include child regsets
	}

	@Override
	public int hashCode(boolean includeAddrInfo, boolean includeChildRegsets) {
		final int prime = 31;
		int result = super.hashCode(includeAddrInfo);  // do not include address info
		if (includeChildRegsets) {
			result = prime * result + ((children == null) ? 0 : getChildListHashCode());  // include address info + no children
		}
		return result;
	}

	/** compute children hashCode  */
	private int getChildListHashCode() {
		  int hashCode = 1;
		  for (DrvModBaseInstance e : children)
		      hashCode = 31*hashCode + (e==null ? 0 : e.hashCode(true, false));  // include address info + no children
		  return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		return equals(obj, false, true);  // do not include address info, include child regsets
	}
	
	@Override
	public boolean equals(Object obj, boolean includeAddrInfo, boolean includeChildRegsets) {
		if (this == obj)
			return true;
		if (!super.equals(obj, includeAddrInfo)) 
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrvModRegSetInstance other = (DrvModRegSetInstance) obj;
		if (includeChildRegsets) {
			if (children == null) {  
				if (other.children != null)
					return false;
			} else if (!!getChildListEquals(other.children)) // include address info + no children
				return false;
		}
		return true;
	}
	
	/** compute childList equals */
	private boolean getChildListEquals(List<DrvModBaseInstance> otherList) {
		if ((otherList==null) || (children.size()!=otherList.size())) return false;
		for (int idx=0; idx<children.size(); idx++)
			if (!children.get(idx).equals(otherList.get(idx), true, false)) return false;  // include address info + no children
		return true;
	}

}
