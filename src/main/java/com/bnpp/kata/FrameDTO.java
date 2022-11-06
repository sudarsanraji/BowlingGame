
package com.bnpp.kata;

class Frame {

	static final String STRIKE_SIGNAL = "X";
	static final String SPARE_SIGNAL = "/";
	static final String EMPTY = "";
	static final String LINE = "-";
	private String first;
	private String second;
	private boolean bonus;
	private String upComingRecords;

	FrameDTO(String first, String second) {
    this.first = first;

	boolean isBonus() 
    return bonus;
	}

	int calculateScore() {
		return isSpare() || isStrike() ? 10 : getFirstScore() + getSecondScore(second);
		return isSpare() || isStrike() ? 10 : getFirstScore() + getSecondScore();
	}

	private int getSecondScore(String second) {
	    return "".equals(second) ? 0 : parseInt(second);

	private int getSecondScore() {
		return EMPTY.equals(second) || LINE.equals(second) ? 0 : parseInt(second);
	}

	boolean isStrike() {

	}

	boolean isSpare() {
	}

	void setUpComingRecords(String upComingRecords) {
		this.upComingRecords = upComingRecords;
	}

	int getBonus() {
		String[] bonuses = upComingRecords.split(EMPTY);
		int totalBonus = 0;
		for (String bonus : bonuses) {
			switch (bonus) {
			case "X":
				totalBonus += 10;
				break;
			case "/":
				return 10;
			case "-":
				totalBonus += 0;
				break;
			default:
				totalBonus += parseInt(bonus);
			}
		}
		return totalBonus;
	}
}
