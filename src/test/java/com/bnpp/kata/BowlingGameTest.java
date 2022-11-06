package com.bnpp.kata;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class BowlingGameTest {

  @Test
  public void noSpareAndNoStrike() throws Exception {
	  BowlingGame game = new BowlingGame();
		int gameScore = game.getGameScore();

		assertEquals(0, gameScore);
  }

}
