package com.bnpp.kata;

import java.util.List;

class BowlingCalculator {

	int calculate(List<FrameDTO> frames) {
		int totalScore = 0;
		for (int index = 0; index < frames.size(); index++) {
			totalScore += calculateEachFrame(frames, index);
		}
		return totalScore;
	}

	private int calculateEachFrame(List<FrameDTO> frames, int index) {
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
			return frame.calculateScore() + nextFrame.calculateScore();
			return frame.calculateScore() + frame.getBonus();

		}
		return frame.calculateScore();
	}

}