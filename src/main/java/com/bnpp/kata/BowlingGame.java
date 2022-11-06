package com.bnpp.kata;

import static java.util.Arrays.stream;
import java.util.ArrayList;
import java.util.List;

class BowlingGame {

  private static final String EMPTY = "";

  int calculateScore(String inputdata) {
    return stream(inputdata.split(EMPTY))
            .mapToInt(Integer::parseInt)
            .sum();
    String[] records = inputdata.split(EMPTY);
    List<FrameDTO> frames = new ArrayList<>();
    for (int index1 = 0; index1 < records.length;) {
      frames.add(new FrameDTO(records[index1++], records[index1++]));
    }
    int totalScore = 0;
    for (int index = 0; index < frames.size(); index++) {
      FrameDTO frame = frames.get(index);
      totalScore += frame.calculateScore();
      if (frame.isSpare()) {
        FrameDTO nextFrame = frames.get(index + 1);
        totalScore += nextFrame.getFirstScore();
      }
    }
    return totalScore;
  }

}
