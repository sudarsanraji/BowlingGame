package com.bnpp.kata;

import java.util.ArrayList;
import java.util.List;

import static com.bnpp.kata.FrameDTO.STRIKE_SIGNAL;

class FramesBuilder {

	private static final String ZERO = "0";
	private static final String EMPTY = "";

	List<FrameDTO> build(String[] records) {
		List<FrameDTO> frames = new ArrayList<>();
		int index = 0;
		for (; index < records.length - 1;) {
			String record = records[index++];
			FrameDTO frame;
			if (isStrike(record)) {
				frame = createFrame(record, EMPTY, false);
			} else {
				frame = createFrame(record, records[index++], false);
			}
			frames.add(frame);
		}
		if (hasBonus(index, records.length)) {
			frames.add(createFrame(records[index], EMPTY, true));
		}
		return frames;
	}

	private boolean isStrike(String record) {
		return STRIKE_SIGNAL.equals(record);
	}

	private boolean hasBonus(int index, int length) {
		return length > index;
	}

	private FrameDTO createFrame(String first, String second, boolean bonus) {
		FrameDTO frame = new FrameDTO(first, second);
		frame.setBonus(bonus);
		return frame;
	}

}
