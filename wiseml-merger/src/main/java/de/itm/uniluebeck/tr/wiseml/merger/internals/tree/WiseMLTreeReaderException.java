package de.itm.uniluebeck.tr.wiseml.merger.internals.tree;

public class WiseMLTreeReaderException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1439028716391133856L;
	private WiseMLTreeReader reader;

	public WiseMLTreeReaderException(String message, Throwable cause, WiseMLTreeReader reader) {
		super(message, cause);
		this.reader = reader;
	}

	public WiseMLTreeReader getReader() {
		return reader;
	}
	
	

}
