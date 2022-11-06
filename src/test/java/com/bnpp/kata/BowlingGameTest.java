package com.bnpp.kata;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BowlingGameTest {

	private BowlingGame bowlingGame;

	@Before
	public void setUp() throws Exception {
		bowlingGame = new BowlingGame();
	}

	@Test
	public void noSpareAndNoStrike() throws Exception {
		BowlingGame bowlingGame = new BowlingGame();

		int score = bowlingGame.calculateScore("12345123451234512345");

		assertThat(score, is(0));
	}

	@Test
	public void oneSpareInFirstFrame() throws Exception {
		int score = bowlingGame.calculateScore("5/345123451234512345");

		assertThat(score, is(70));
	}

	@Test
	public void oneSpareInLastFrame() throws Exception {
		int score = bowlingGame.calculateScore("1234512345123451235/5");

		assertThat(score, is(66));
	}

	@Test
	public void oneStrikeInFirstFrame() throws Exception {
		int score = bowlingGame.calculateScore("X345123451234512345");

		assertThat(score, is(74));
	}

	@Test
	public void allStrikes() throws Exception {
		int score = bowlingGame.getScore("XXXXXXXXXXXX");

		assertThat(score, is(300));
	}

	@Test
	public void allSpares() throws Exception {
		int score = bowlingGame.getScore("5/5/5/5/5/5/5/5/5/5/5");

		assertThat(score, is(150));
	}

	@Test
	public void ninePinsDownEachRound() throws Exception {
		int score = bowlingGame.getScore("9-9-9-9-9-9-9-9-9-9-");

		assertThat(score, is(90));
	}

}
