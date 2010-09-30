package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements.LinkPropertiesTransformer;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements.NodePropertiesTransformer;
import de.itm.uniluebeck.tr.wiseml.merger.structures.TimeInfo;

public class MergerResources {
	/*
	private int inputCount;
	private NodeProperties[] inputDefaultNodes;
	private LinkProperties[] inputDefaultLinks;
	private Coordinate[] inputOrigins;
	
	private Coordinate outputOrigin;
	private NodeProperties outputDefaultNode;
	private LinkProperties outputDefaultLink;
	*/
	private NodePropertiesTransformer nodePropertiesTransformer;
	private LinkPropertiesTransformer linkPropertiesTransformer;
	private TimeInfo timeInfo;
	/*
	public void setOrigins(Coordinate[] inputOrigins, Coordinate outputOrigin) {
		this.inputOrigins = inputOrigins;
		this.outputOrigin = outputOrigin;
	}
	
	public void setDefaultNodes(NodeProperties[] inputDefaultNodes, NodeProperties outputDefaultNode) {
		this.inputDefaultNodes = inputDefaultNodes;
		this.outputDefaultNode = outputDefaultNode;
	}
	
	public void setDefaultLinks(LinkProperties[] inputDefaultLinks, LinkProperties outputDefaultLink) {
		this.inputDefaultLinks = inputDefaultLinks;
		this.outputDefaultLink = outputDefaultLink;
	}
*/
	public NodePropertiesTransformer getNodePropertiesTransformer() {
		return nodePropertiesTransformer;
	}

	public void setNodePropertiesTransformer(
			NodePropertiesTransformer nodePropertiesTransformer) {
		this.nodePropertiesTransformer = nodePropertiesTransformer;
	}

	public LinkPropertiesTransformer getLinkPropertiesTransformer() {
		return linkPropertiesTransformer;
	}

	public void setLinkPropertiesTransformer(
			LinkPropertiesTransformer linkPropertiesTransformer) {
		this.linkPropertiesTransformer = linkPropertiesTransformer;
	}

	public TimeInfo getTimeInfo() {
		return timeInfo;
	}

	public void setTimeInfo(TimeInfo timeInfo) {
		this.timeInfo = timeInfo;
	}
	
}
