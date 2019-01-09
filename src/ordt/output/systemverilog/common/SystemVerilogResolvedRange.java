package ordt.output.systemverilog.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** non-parameterized IO signal range */
public class SystemVerilogResolvedRange extends SystemVerilogRange {

	protected boolean leftMsb = true;  // msb is left position in range by default
	protected int lowIndex;
	protected int size = 0;

	public SystemVerilogResolvedRange(int lowIndex, int size) {
		this.lowIndex = lowIndex;
		this.size = size;
	}

	/** init the range using a colon separated int pair string */
	public SystemVerilogResolvedRange(String range) {
	    Pattern p = Pattern.compile("\\s*(\\d+)\\s*\\:\\s*(\\d+)\\s*");
	    Matcher m = p.matcher(range);
	    if (m.matches()) {
	      int leftval = Integer.valueOf(m.group(1));
	      int rightval = Integer.valueOf(m.group(2));
	      if (leftval < rightval) {
	  		this.lowIndex = leftval;
			this.size = rightval - leftval + 1;
			this.leftMsb = false;
	      }
	      else {
		  		this.lowIndex = rightval;
				this.size = leftval - rightval + 1;
	      }
	    }
	}

	/** return true if this range has been initialized with valid values */
	@Override
	public boolean isValid() {
		return size > 0;
	}

	/** get low index for this io signal */
	@Override
	public int getLowIndex() {
		return lowIndex;
	}

	/** get high index for this io signal */
	@Override
	public int getHighIndex() {
		return lowIndex + size -1;
	}

	/** get size in bits of this io signal range */
	@Override
	public int getSize() {
		return size;
	}
	
	/** return true if this io signal range is greater than 1 */
	@Override
	public boolean isVector() {
		return getSize() > 1;
	}
	
	/** return the array string used for definitions */
	@Override
	public String getDefArray() {
		if (getSize() < 2) return "";
	   	return " [" + getLeftIndex() + ":" + getRightIndex() + "] ";
	}

	@Override
	public int getLeftIndex() {
		if (leftMsb) return getHighIndex();
		return getLowIndex();
	}

	@Override
	public int getRightIndex() {
		if (leftMsb) return getLowIndex();
		return getHighIndex();
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (leftMsb ? 1231 : 1237);
		result = prime * result + lowIndex;
		result = prime * result + size;
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
		SystemVerilogResolvedRange other = (SystemVerilogResolvedRange) obj;
		if (leftMsb != other.leftMsb)
			return false;
		if (lowIndex != other.lowIndex)
			return false;
		if (size != other.size)
			return false;
		return true;
	}

}
