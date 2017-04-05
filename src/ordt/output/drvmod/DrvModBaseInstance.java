package ordt.output.drvmod;

/** base class for storing driver model reg/regset instances */
public abstract class DrvModBaseInstance {

	protected static DrvModBuilder builder;
	private static int instanceCount = 0;
	
	protected String name; // instance name
	protected int mapId = 0;  // id of register map/overlay that created this instance
	protected int instId = 0;  // unique instance id for inst name generation
	protected boolean hasBeenProcessed = false;  // set to true if this instance has been processed
	
	protected DrvModBaseInstance(String name, int mapId) {
		super();
		this.name = name;
		this.mapId = mapId;
		this.instId = instanceCount++;  // bump unique instance count
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

	/** includes name always, mapId never */
	@Override
	public int hashCode() {  
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		//System.out.println("Super hashCode:  return=" + result + ", name=" + getName() );
		return result;
	}

	/** includes name always, mapId never */
	@Override
	public boolean equals(Object obj) {   
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DrvModBaseInstance other = (DrvModBaseInstance) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	abstract public void process(Integer mapId, boolean regsOnly, boolean processOnce);

}
