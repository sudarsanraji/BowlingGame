
package com.bnpp.kata;

import static java.lang.Integer.parseInt;

class FrameDTO {

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
		this.second = second;
	}

	FrameDTO() {

	}

	int calculateScore() {
		return isSpare() || isStrike() ? 10 : getFirstScore() + getSecondScore();
	}

	private int getSecondScore() {
		return EMPTY.equals(second) || LINE.equals(second) ? 0 : parseInt(second);
	}

	boolean isStrike() {
		return STRIKE_SIGNAL.equals(first);
	}

	boolean isSpare() {
		return SPARE_SIGNAL.equals(second);
	}

	int getFirstScore() {
		return EMPTY.equals(first) || LINE.equals(first) ? 0 : parseInt(first);
	}

	void setBonus(boolean bonus) {
		this.bonus = bonus;
	}

	boolean isBonus() {
		return bonus;
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

	public void setFirst(String first) {
		this.first = first;
	}

	public void setSecond(String second) {
		this.second = second;
	}

}
