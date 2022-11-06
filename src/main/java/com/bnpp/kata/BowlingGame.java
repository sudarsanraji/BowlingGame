package com.bnpp.kata;

import static java.util.Arrays.stream;
import java.util.ArrayList;
import java.util.List;
import static com.bnpp.kata.FrameDTO.STRIKE_SIGNAL;

class BowlingGame {

	private static final String EMPTY = "";

	private List<FrameDTO> buildFrames(String[] records) {
	    List<FrameDTO> frames = new ArrayList<>();
	    
	    int index = 0;
	    for (; index < records.length - 1;) {
	      frames.add(createFrame(records[index++], records[index++], false));
	      String record = records[index++];
	      if (STRIKE_SIGNAL.equals(record)) {
	        frames.add(createFrame(record, ZERO, false));
	      } else {
	        frames.add(createFrame(record, records[index++], false));
	      
	      }
	    }
	    if (hasBonus(index, records.length)) {
	      frames.add(createFrame(records[index], ZERO, true));
	    
	private int calculateScoreByFrame(List<Frame> frames, int index) {
		FrameDTO nextFrame = frames.get(index + 1);
		return frame.calculateScore() + nextFrame.getFirstScore();

		if (frame.isStrike()) {
			FrameDTO nextFrame = frames.get(index + 1);
			return frame.calculateScore() + nextFrame.getFirstScore() + nextFrame.getSecondScore();
		}

		return frame.calculateScore();

	}
	    
}
	}	