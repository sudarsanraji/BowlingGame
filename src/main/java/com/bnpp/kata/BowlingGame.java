package com.bnpp.kata;

import static java.util.Arrays.stream;
import java.util.ArrayList;
import java.util.List;

class BowlingGame {

	private static final String EMPTY = "";

	int calculateScore(String input) {
		String[] records = input.split(EMPTY);
		return calculateTotalScore(buildFrames(records));
	}

	private List<FrameDTO> buildFrames(String[] records) {
		List<FrameDTO> frames = new ArrayList<>();
		for (int index1 = 0; index1 < records.length;) {
			frames.add(new FrameDTO(records[index1++], records[index1++]));
			for (int index = 0; index < records.length;) {
				frames.add(new FrameDTO(records[index++], records[index++]));
			}

		}
		return frames;
	}

	private int calculateTotalScore(List<FrameDTO> frames) {
		int totalScore = 0;
		for (int index = 0; index < frames.size(); index++) {
			FrameDTO frame = frames.get(index);
			totalScore += frame.calculateScore();
			if (frame.isSpare()) {
				FrameDTO nextFrame = frames.get(index + 1);
				totalScore += nextFrame.getFirstScore();
			}
			totalScore += calculateScoreByFrame(frames, index);
		}
		return totalScore;
	}

	private int calculateScoreByFrame(List<FrameDTO> frames, int index) {
		FrameDTO frame = frames.get(index);
		if (frame.isSpare()) {
			FrameDTO nextFrame = frames.get(index + 1);
			return frame.calculateScore() + nextFrame.getFirstScore();
		}
		return frame.calculateScore();
	}

}
