
package com.bnpp.kata;

class Frame {

	static final String STRIKE_SIGNAL = "X";
	static final String SPARE_SIGNAL = "/";
	static final String EMPTY = "";
	private String first;
	private String second;
	private boolean bonus;
	private String upComingRecords;

	FrameDTO(String first, String second) {
    this.first = first;
    boolean isBonus() 
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
}