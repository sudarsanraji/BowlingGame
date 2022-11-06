package com.bnpp.kata;

class BowlingGame {

	int getScore(String input) {
		FramesBuilder framesBuilder = new FramesBuilder();
		BowlingCalculator bowlingCalculator = new BowlingCalculator();
		return bowlingCalculator.calculate(framesBuilder.build(input));
	}
}