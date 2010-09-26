package de.itm.uniluebeck.tr.wiseml.merger.enums;

public enum Indentation {
	
	NoneWithoutNewlines(null),
	NoneWithNewlines(""),
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
	
	public boolean preventWhitespace() {
		return string == null;
	}
}
