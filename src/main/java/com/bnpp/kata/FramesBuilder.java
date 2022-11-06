package com.bnpp.kata;

import java.util.ArrayList;
import java.util.List;

import static com.bnpp.kata.FrameDTO.STRIKE_SIGNAL;

class FramesBuilder {

	private static final String EMPTY = "";

	List<FrameDTO> build(String input) {
		String[] records = input.split(EMPTY);
		List<FrameDTO> frames = new ArrayList<>();
		int index = 0;
		for (; index < records.length - 1; index++) {
			if (frames.size() == 10) {
				break;
			}
			frames.add(buildFrame(records, index));
			if (!isStrike(records[index])) {
				index++;
			}
		}
		if (hasBonus(index, records.length)) {
			frames.add(createBonusFrame(records, index));
		}
		return frames;
	}

	private FrameDTO createBonusFrame(String[] records, int index) {
		String firstRecord = records[index++];
		String secondRecord = EMPTY;
		if (records.length > index) {
			secondRecord = records[index];
		}
		FrameDTO frame = new FrameDTO(firstRecord, secondRecord);
		frame.setBonus(true);
		return frame;
	}

	private FrameDTO buildFrame(String[] records, int index) {
		FrameDTO frame = new FrameDTO();
		frame.setFirst(records[index]);
		frame.setBonus(false);
		if (!isStrike(records[index])) {
			frame.setSecond(records[index + 1]);
		} else {
			frame.setUpComingRecords(records[index + 1] + records[index + 2]);
		}
		if (frame.isSpare()) {
			frame.setUpComingRecords(records[index + 2]);
		}
		return frame;
	}

	private boolean isStrike(String record) {
		return STRIKE_SIGNAL.equals(record);
	}

	private boolean hasBonus(int index, int length) {
		return length > index;
	}

}