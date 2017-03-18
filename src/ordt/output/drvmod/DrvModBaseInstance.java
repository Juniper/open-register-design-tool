package ordt.output.drvmod;

/** base class for storing driver model reg/regset instances */
public class DrvModBaseInstance {

	String name; // instance name
	int mapId = 0;  // id of register map that created this instance
	long addressOffset = 0;  // relative address offset of this instance (64b max addr size)
	int reps;  // number of replications of this instance
	Long addressStride;  // address stride if a replicated instance (64b max addr size
	
	protected DrvModBaseInstance(String name, int mapId, long addressOffset, int reps, Long addressStride) {
		super();
		this.name = name;
		this.mapId = mapId;
		this.addressOffset = addressOffset;
		this.reps = reps;
		this.addressStride = addressStride;
		System.out.println("DrvModBaseInstance: creating name=" + name + ", mapId=" + mapId + ", addressOffset=" + addressOffset + ", reps=" + reps + ", addressStride=" + addressStride);
	}

	public String getName() {
		return name;
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
	
	
}
