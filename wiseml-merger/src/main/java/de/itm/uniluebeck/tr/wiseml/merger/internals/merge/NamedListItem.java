package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import de.itm.uniluebeck.tr.wiseml.merger.internals.tree.WiseMLTreeReader;

public class NamedListItem implements Comparable<NamedListItem> {
	
	private final String strID;
	private final long numID;
	
	private final WiseMLTreeReader associatedReader;
	
	public NamedListItem(String strID, WiseMLTreeReader associatedReader) {
		this.strID = strID;
		this.associatedReader = associatedReader;
		this.numID = 0;
	}

	public NamedListItem(long numID, WiseMLTreeReader associatedReader) {
		this.numID = numID;
		this.associatedReader = associatedReader;
		this.strID = null;
	}

	@Override
	public int compareTo(NamedListItem o) {
		if (strID == null) {
			long diff = numID - o.numID;
			if (diff < Integer.MIN_VALUE) {
				return Integer.MIN_VALUE;
			}
			if (diff > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			}
			return (int)diff;
		}
		return strID.compareTo(o.strID);
	}

	public WiseMLTreeReader getAssociatedReader() {
		return associatedReader;
	}

	public String getID() {
		if (strID == null) {
			return Long.toString(numID);
		}
		return strID;
	}
	
}
