package ordt.output.drvmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	
	/** get list of children with specified mapId or all children if mapId is null */
	private List<DrvModBaseInstance> getChildren(Integer mapId) {
		List<DrvModBaseInstance> outList = new ArrayList<DrvModBaseInstance>();
		for (DrvModBaseInstance inst: childMaps.keySet()) {
			//System.out.println("DrvModRegSetInstance getChildren: inst.name=" + inst.getName() + ", inst.map=" + childMaps.get(inst) + ", mapId=" + mapId);
			if ((mapId==null) || ((childMaps.get(inst) & (1<<mapId))>0)) outList.add(inst);
		}
		/* sort the child list by address so equals behaves consistently
		Collections.sort(outList, new Comparator<DrvModBaseInstance>() {
		        @Override
		        public int compare(DrvModBaseInstance inst1, DrvModBaseInstance inst2)
		        { return  ((Long) inst2.getAddressOffset()).compareTo(inst1.getAddressOffset());}
		    });*/
		
		return outList;
	}

	/** add new child or add mapId to encoded value if a dup */
	public void addChild(DrvModBaseInstance child, int mapId) {
		Integer currentMap = childMaps.get(child);
		/*if (currentMap == null) {
			System.out.println("DrvModRegSetInstance addChild: adding new child, child.getName=" + child.getName() + ", mapId=" + mapId + ", my hash=" + hashCode() + ", new child hash=" + child.hashCode());
			for(DrvModBaseInstance inst: childMaps.keySet())
				System.out.println("  inst.name=" + inst.getName() + ", map=" + childMaps.get(inst) + ", inst hash=" + inst.hashCode());

		}*/
		int newMap = (currentMap == null)? (1<<mapId) : currentMap | (1<<mapId);
		/*
		if (currentMap==null) childMaps.put(child, newMap);
		else */childMaps.put(child, newMap);
		/*if (currentMap == null) {
			System.out.println("DrvModRegSetInstance addChild:  after child add, child.getName=" + child.getName() + ", newMap=" + newMap + ", my hash=" + hashCode());
		}*/
	}
	
	@Override
	/** walk tree and process instances matching map/reg criteria */
	public void process(Integer mapId, boolean regsOnly) {
		List<DrvModBaseInstance> children = getChildren(mapId);
		//System.out.println("DrvModRegSetInstance process: name=" + name + ", mapId=" + mapId + ", children=" + children.size()+ ", childMaps sz=" + childMaps.size());
		for(DrvModBaseInstance inst: children) inst.process(mapId, regsOnly);  // process children recursively
		if (!regsOnly) builder.processInstance();
	}

	@Override
	public int hashCode() {
		int hcode = hashCode(false, true, true);  // do not include address info, include child regsets, include reg info
		return hcode;  
	}

	@Override
	public int hashCode(boolean includeAddrInfo, boolean includeChildRegsets, boolean includeRegInfo) {
		final int prime = 31;
		int result = super.hashCode(includeAddrInfo);  // do not include address info
		if (includeChildRegsets) {
			result = prime * result + getChildListHashCode();  // include address info + no children
		}
		//System.out.println("DrvModRegSetInstance hashCode:  return=" + result + ", name=" + getName() + ", includeAddrInfo=" + includeAddrInfo);
		return result;
	}

	/** compute children hashCode  */
	private int getChildListHashCode() {
		  int hashCode = 1;
		  for (DrvModBaseInstance e : getChildren())
		      hashCode = 31*hashCode + (e==null ? 0 : e.hashCode(true, true, true));  // include address info, include child regsets, include reg info
		  return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		return equals(obj, false, true, true);  // do not include address info, include child regsets, include reg info
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
			if (!children.get(idx).equals(otherList.get(idx), true, true, true)) return false;  // include address info, include child regsets, include reg info
		return true;
	}

}
