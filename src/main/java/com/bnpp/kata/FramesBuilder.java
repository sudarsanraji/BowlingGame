package com.bnpp.kata;

import java.util.ArrayList;
import java.util.List;

import static com.bnpp.kata.FrameDTO.STRIKE_SIGNAL;

class FramesBuilder {

	private static final String ZERO = "0";
	private static final String EMPTY = "";

	List<FrameDTO> build(String[] records) {

	List<FrameDTO> build(String input) {
	    String[] records = input.split(EMPTY);
	    List<FrameDTO> frames = new ArrayList<>();
	    int index = 0;
	    for (; index < records.length - 1; index++) {

	private FrameDTO createBonusFrame(String[] records, int index) {
	}

	private FrameDTO buildFrame(String[] records, int index) {
		FrameDTO frame;
		if (isStrike(records[index])) {
			return createFrame(records[index], EMPTY, false);
			frame = createFrame(records[index], EMPTY, false);
		} else {
			frame = createFrame(records[index], records[index + 1], false);
		}
		return createFrame(records[index], records[index + 1], false);
		if (frame.isStrike()) {
			frame.setUpComingRecords(records[index + 1] + records[index + 2]);
		}
		if (frame.isSpare()) {
			frame.setUpComingRecords(records[index + 1]);
		}
		return frame;
	}

	private boolean isStrike(String record) {
		return STRIKE_SIGNAL.equals(record);
	}

}
