package de.itm.uniluebeck.tr.wiseml.merger.enums;

public enum Interpolation {
	
	none(0),
	linear(1),
	polynomial(3),
	cubic(2),
	spline(4),
	;
	
	private final int quality;
	
	Interpolation(int quality) {
		this.quality = quality;
	}
	
	public int getQuality() {
		return quality;
	}

}
