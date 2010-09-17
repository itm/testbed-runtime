package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;


public class WiseMLTreeMergerException extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3527936436996918886L;
	private WiseMLTreeMerger merger;

	public WiseMLTreeMergerException(String message, Throwable cause, WiseMLTreeMerger merger) {
		super(message, cause);
		this.merger = merger;
	}

	public WiseMLTreeMerger getMerger() {
		return merger;
	}

}
