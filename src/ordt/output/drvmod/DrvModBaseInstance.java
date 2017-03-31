package ordt.output.drvmod;

/** base class for storing driver model reg/regset instances */
public abstract class DrvModBaseInstance {

	protected static DrvModBuilder builder;
	private static int instanceCount = 0;
	
	protected String name; // instance name
	protected int mapId = 0;  // id of register map/overlay that created this instance
	protected int instId = 0;  // unique instance id for inst name generation
	protected long addressOffset = 0;  // relative address offset of this instance (64b max addr size)
	protected int reps;  // number of replications of this instance
	protected Long addressStride;  // address stride if a replicated instance (64b max addr size
	protected boolean hasBeenProcessed = false;  // set to true if this instance has been processed
	
	protected DrvModBaseInstance(String name, int mapId, long addressOffset, int reps, Long addressStride) {
		super();
		this.name = name;
		this.mapId = mapId;
		this.instId = instanceCount++;  // bump unique instance count
		this.addressOffset = addressOffset;
		this.reps = reps;
		this.addressStride = (addressStride == null)? 0 : addressStride;
		//System.out.println("DrvModBaseInstance: creating name=" + name + ", mapId=" + mapId + ", addressOffset=" + addressOffset + ", reps=" + reps + ", addressStride=" + addressStride);
	}

	public static DrvModBuilder getBuilder() {
		return builder;
	}

	public static void setBuilder(DrvModBuilder builder) {
		DrvModBaseInstance.builder = builder;
	}

	public String getName() {
		return name;
	}

	public String getInstName() {
		return name + '_' + instId;
	}

	public int getMapId() {
		return mapId;
	}

	public long getAddressOffset() {
		return addressOffset;
	}

	public int getReps() {
		return reps;
	}

	public long getAddressStride() {
		return addressStride;
	}

	/** includes name always, mapId never, address info optionally */
	public int hashCode(boolean includeAddrInfo) {  
		final int prime = 31;
		int result = 1;
		if (includeAddrInfo) {
			result = prime * result + (int) (addressOffset ^ (addressOffset >>> 32));
			result = prime * result + ((addressStride == null) ? 0 : addressStride.hashCode());
			result = prime * result + reps;
		}
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		//System.out.println("Super hashCode:  return=" + result + ", name=" + getName() + ", includeAddrInfo=" + includeAddrInfo);
		return result;
	}

	/** includes name always, mapId never, address info optionally */
	public boolean equals(Object obj, boolean includeAddrInfo) {   
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrvModBaseInstance other = (DrvModBaseInstance) obj;
		if (includeAddrInfo) {
			if (addressOffset != other.addressOffset)
				return false;
			if (addressStride == null) {
				if (other.addressStride != null)
					return false;
			} else if (!addressStride.equals(other.addressStride))
				return false;
			if (reps != other.reps)
				return false;
		}
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	abstract public int hashCode(boolean includeAddrInfo, boolean includeChildRegsets, boolean includeRegInfo);
	abstract public boolean equals(Object obj, boolean includeAddrInfo, boolean includeChildRegsets, boolean includeRegInfo);
	abstract public void process(Integer mapId, boolean regsOnly, boolean processOnce);

}
