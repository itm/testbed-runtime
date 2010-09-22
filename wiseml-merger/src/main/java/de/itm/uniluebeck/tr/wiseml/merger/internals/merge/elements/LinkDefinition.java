package de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements;

import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkProperties;

public class LinkDefinition implements Comparable<LinkDefinition> {

	private final String source;
	private final String target;
	private final LinkProperties linkProperties;
	private final int inputIndex;
	
	private final String cmpstr;
	
	public LinkDefinition(
			final String source,
			final String target,
			final LinkProperties linkProperties, int inputIndex) {
		this.source = source;
		this.target = target;
		this.linkProperties = linkProperties;
		this.inputIndex = inputIndex;
		this.cmpstr = source + target;
	}

	@Override
	public int compareTo(LinkDefinition other) {
		return this.cmpstr.compareTo(other.cmpstr);
	}

	public LinkProperties getLinkProperties() {
		return linkProperties;
	}

	public int getInputIndex() {
		return inputIndex;
	}
	
	@Override
	public String toString() {
		return inputIndex+"#"+source+"<->"+target;
	}

	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}

}
