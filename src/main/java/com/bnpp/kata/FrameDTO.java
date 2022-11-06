
package com.bnpp.kata;

import static java.lang.Integer.parseInt;

public class FrameDTO {
	private static final String SPARE_SIGNAL = "/";
	public static final String STRIKE_SIGNAL = "X";
	private String first;
	private String second;
	private boolean bonus;

	
	int calculateScore() {
	    return isSpare() ? 10 : parseInt(first) + parseInt(second);
	    
	  }

	
	public boolean isSpare() {
		return SPARE_SIGNAL.endsWith(second);
	}
	
	boolean isStrike() {
	    return STRIKE_SIGNAL.equals(first);
	  }

	public int getFirstScore() {
		return parseInt(first);
	}
	public int getSecondScore() {
	    return parseInt(second);
	  }
}
