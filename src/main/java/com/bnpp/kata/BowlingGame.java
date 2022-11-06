package com.bnpp.kata;

import static java.util.Arrays.stream;
import java.util.ArrayList;
import java.util.List;
import static com.bnpp.kata.FrameDTO.STRIKE_SIGNAL;

class BowlingGame {

	private static final String EMPTY = "";
	private static final String ZERO = "0";

	int calculateScore(String input) {

	int getScore(String input) {
		String[] records = input.split(EMPTY);
		return calculateTotalScore(buildFrames(records));
	}

	private List<FrameDTO> buildFrames(String[] records) {
		List<FrameDTO> frames = new ArrayList<>();
		int index = 0;
		for (; index < records.length - 1;) {
			String record = records[index++];
			if (STRIKE_SIGNAL.equals(record)) {
				frames.add(createFrame(record, ZERO, false));
			} else {
				frames.add(createFrame(record, records[index++], false));
			}
		}
		if (hasBonus(index, records.length)) {
			frames.add(createFrame(records[index], ZERO, true));
		}
		return frames;
	}

	private boolean hasBonus(int index, int length) {
		return length > index;
		FramesBuilder framesBuilder = new FramesBuilder();
		BowlingCalculator bowlingCalculator = new BowlingCalculator();
		return bowlingCalculator.calculate(framesBuilder.build(records));
	}

	private FrameDTO createFrame(String first, String second, boolean bonus) {
		FrameDTO frame = new FrameDTO(first, second);
		frame.setBonus(bonus);
		return frame;
	}

	private int calculateTotalScore(List<FrameDTO> frames) {
		int totalScore = 0;
		for (int index = 0; index < frames.size(); index++) {
			totalScore += calculateScoreByFrame(frames, index);
		}
		return totalScore;
	}

	private int calculateScoreByFrame(List<FrameDTO> frames, int index) {
		FrameDTO frame = frames.get(index);
		if (frame.isBonus()) {
			return 0;
		}
		if (frame.isSpare()) {
			FrameDTO nextFrame = frames.get(index + 1);
			return frame.calculateScore() + nextFrame.getFirstScore();
		}
		if (frame.isStrike()) {
			FrameDTO nextFrame = frames.get(index + 1);
			return frame.calculateScore() + nextFrame.getFirstScore() + nextFrame.getSecondScore();
		}
		return frame.calculateScore();
	}

}