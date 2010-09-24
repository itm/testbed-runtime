package de.itm.uniluebeck.tr.wiseml.merger.enums;

public enum Indentation {
	
	None(""),
	Tabulator("\t"),
	OneSpace(" "),
	TwoSpaces("  "),
	FourSpaces("    "),
	;

	private String string;

	private Indentation(final String string) {
		this.string = string;
	}
	
	public String getIndentationElement() {
		return string;
	}
}
