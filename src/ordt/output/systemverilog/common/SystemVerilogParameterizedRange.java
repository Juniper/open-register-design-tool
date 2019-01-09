package ordt.output.systemverilog.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ordt.output.common.MsgUtils;

/** parameterized IO signal range */
public class SystemVerilogParameterizedRange extends SystemVerilogRange {
	protected String leftExpr;
	protected String rightExpr;
	protected boolean valid = false;
	
	/** init the range using a colon separated range string */
	public SystemVerilogParameterizedRange(String range) {
	    Pattern p = Pattern.compile("(.*)\\:(.*)");
	    Matcher m = p.matcher(range);
	    if (m.matches()) {
	      this.leftExpr = m.group(1).trim();
	      this.rightExpr = m.group(2).trim();
	      this.valid = true;
	    }
	}
	
	private static void allDone() {
		try {
			throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
		}
		MsgUtils.errorExit("Parameterized IO signal range is unresolved.");
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	@Override
	public int getLowIndex() {
		allDone(); // TODO
		return 0;
	}

	@Override
	public int getHighIndex() {
		allDone(); // TODO
		return 0;
	}

	@Override
	public int getLeftIndex() {
		allDone(); // TODO
		return 0;
	}

	@Override
	public int getRightIndex() {
		allDone(); // TODO
		return 0;
	}

	@Override
	public int getSize() {
		allDone(); // TODO
		return 0;
	}
	
	/** return true if this io signal range is greater than 1 */
	@Override
	public boolean isVector() {
		return true;  // TODO - default is vector, but can set post-resolve
	}

	@Override
	public String getDefArray() {
	   	return " [" + leftExpr + ":" + rightExpr + "] ";
	}

}
