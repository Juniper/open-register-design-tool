package ordt.output.drvmod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ordt.output.common.MsgUtils;

public class DrvModRegSetInstance extends DrvModBaseInstance {
	
	HashMap<DrvModRegSetChildInfo, Integer> childMaps = new HashMap<DrvModRegSetChildInfo, Integer>(); // hash of child instances and encoded overlay use map 

	DrvModRegSetInstance(String name, int mapId) {
		super(name, mapId);
	}

	/** get list of children with this instances mapId */
	public List<DrvModRegSetChildInfo> getChildren() {
		return getChildren(this.mapId);
	}
	
	/** get list of children with specified mapId or all children if mapId is null */  
	public List<DrvModRegSetChildInfo> getChildren(Integer mapId) {
		List<DrvModRegSetChildInfo> outList = new ArrayList<DrvModRegSetChildInfo>();
		for (DrvModRegSetChildInfo cinfo: childMaps.keySet()) {
			Integer childMap = childMaps.get(cinfo);
			if (childMap==null) System.out.println("DrvModRegSetInstance getChildren for failed on get of inst.name=" + cinfo.child.getName());
			//System.out.println("DrvModRegSetInstance getChildren: inst.name=" + inst.getName() + ", inst.map=" + childInfo.mapId + ", mapId=" + mapId);
			if ((mapId==null) || ((childMap & (1<<mapId))>0)) outList.add(cinfo);
		}
		/* sort the child list by address so equals behaves consistently
		Collections.sort(outList, new Comparator<DrvModBaseInstance>() {
		        @Override
		        public int compare(DrvModBaseInstance inst1, DrvModBaseInstance inst2)
		        { return  ((Long) inst2.getAddressOffset()).compareTo(inst1.getAddressOffset());}
		    });*/		
		return outList;
	}

	/** return the full hashmap of children  */
	public HashMap<DrvModRegSetChildInfo, Integer> getChildMaps() {
		return childMaps;
	}

	/** add new child or add mapId to encoded value if a dup */
	public void addChild(DrvModBaseInstance child, long addressOffset, int reps, Long addressStride, int mapId) {
		updateChild(child, addressOffset, reps, addressStride, mapId, true);
	}
	
	/** add new child or update mapId encoded value if a dup - issue error if allowAdd is false and not a dup
	 * 
	 * @param child - child instance
	 * @param addressOffset - relative address offset of child
	 * @param reps - number of replications of this child
	 * @param addressStride - stride of each replication 
	 * @param mapId - overlay map id to be added
	 * @param allowAdd - if true then adds to the child list are allowed, otherwise only map updates are allowed
	 */
	public void updateChild(DrvModBaseInstance child, long addressOffset, int reps, Long addressStride, int mapId, boolean allowAdd) {
		//if ("tdm_table".equals(child.getName())) System.out.println("DrvModRegSetInstance updateChild: adding new child, child.getName=" + child.getName() + ", mapId=" + mapId + ", allowAdd=" + allowAdd + ", offset=" + addressOffset + ", reps=" + reps + ", stride=" + addressStride+ ", hash=" + child.hashCode());
		DrvModRegSetChildInfo newChildInfo = new DrvModRegSetChildInfo(child, addressOffset, reps, addressStride);
		updateChild(newChildInfo, mapId, allowAdd);
	}

	public void updateChild(DrvModRegSetChildInfo cInfo, int mapId, boolean allowAdd) {
		Integer currentMap = childMaps.get(cInfo);
		// exit if not a dup and allowAdd false
		if (!allowAdd && (currentMap == null)) {
			//System.out.println("DrvModRegSetInstance updateChild: adding new child to " + getName() + " that should be a dup, child.getName=" + cInfo.child.getName() + ", mapId=" + mapId + ", my hash=" + hashCode() + ", new child hash=" + cInfo.child.hashCode());
			MsgUtils.errorExit("Problem detected in overlay file processing");
		}
		// compute new map encoding
		int newMap = (currentMap == null)? (1<<mapId) : currentMap | (1<<mapId);
		// add or update the map encoding for this child 
		childMaps.put(cInfo, newMap);
	}
	
	@Override
	/** walk tree and process instances matching map/reg criteria */
	public void process(Integer mapId, boolean regsOnly, boolean processOnce) {
		List<DrvModRegSetChildInfo> children = getChildren(mapId);
		//System.out.println("DrvModRegSetInstance process: name=" + name + ", mapId=" + mapId + ", children=" + children.size()+ ", childMaps sz=" + childMaps.size());
		for(DrvModRegSetChildInfo cInfo: children) cInfo.child.process(mapId, regsOnly, processOnce);  // process children recursively
		if ((!regsOnly) && (!processOnce || !hasBeenProcessed)) {
			builder.processRegSetInstance(this);
			hasBeenProcessed = true;
		}
	}

	// --------- internal class to hold child info
	public class DrvModRegSetChildInfo {
		public long addressOffset = 0;  // relative address offset of this child (64b max addr size)
		public int reps = 1;  // number of replications of this child
		public Long addressStride;  // address stride if a replicated child (64b max addr size
		public DrvModBaseInstance child;
		
		DrvModRegSetChildInfo(DrvModBaseInstance child, long addressOffset, int reps, Long addressStride) {
			super();
			this.child = child;
			this.addressOffset = addressOffset;
			this.reps = reps;
			this.addressStride = addressStride;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (addressOffset ^ (addressOffset >>> 32));
			result = prime * result + ((addressStride == null) ? 0 : addressStride.hashCode());
			result = prime * result + ((child == null) ? 0 : child.hashCode());
			result = prime * result + reps;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DrvModRegSetChildInfo other = (DrvModRegSetChildInfo) obj;
			if (addressOffset != other.addressOffset)
				return false;
			if (addressStride == null) {
				if (other.addressStride != null)
					return false;
			} else if (!addressStride.equals(other.addressStride))
				return false;
			if (child == null) {
				if (other.child != null)
					return false;
			} else if (!child.equals(other.child))
				return false;
			if (reps != other.reps)
				return false;
			return true;
		}
		
	}
	
	// --------- hashCode/equals
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();  
		result = prime * result + getChildListHashCode();  // include address info + no children
		//System.out.println("DrvModRegSetInstance hashCode:  return=" + result + ", name=" + getName() + ", includeAddrInfo=" + includeAddrInfo);
		return result;
	}

	/** compute children hashCode  */
	private int getChildListHashCode() {
		  int hashCode = 1;
		  for (DrvModRegSetChildInfo e : getChildren())  // only look at base overlay children when computing hash
		      hashCode = 31*hashCode + (e==null ? 0 : e.hashCode());  // include address info, include child regsets, include reg info
		  return hashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj)) 
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrvModRegSetInstance other = (DrvModRegSetInstance) obj;
		if (!getChildListEquals(other.getChildren())) // include address info + no children
			return false;
		return true;
	}
	
	/** compute childList equals */
	private boolean getChildListEquals(List<DrvModRegSetChildInfo> otherList) {
		List<DrvModRegSetChildInfo> children = getChildren();
		if ((otherList==null) || (children.size()!=otherList.size())) return false;
		for (int idx=0; idx<children.size(); idx++)
			if (!children.get(idx).equals(otherList.get(idx))) return false;  // include child regsets
		return true;
	}

}
