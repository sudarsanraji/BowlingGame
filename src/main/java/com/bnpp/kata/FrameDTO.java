
package com.bnpp.kata;

import static java.lang.Integer.parseInt;

public class FrameDTO {
  private static final String SPARE_SIGNAL = "/";
  private String first;
  

  public FrameDTO(String first) {
	  
    this.first = first;
    
  }

  public int calculateScore() {
    if (isSpare()) {
      return 10;
    }
    return parseInt(first);
  }
  

  public boolean isSpare() {
    return SPARE_SIGNAL.endsWith(first);
  }

  public int getFirstScore() {
    return parseInt(first);
  }
}
