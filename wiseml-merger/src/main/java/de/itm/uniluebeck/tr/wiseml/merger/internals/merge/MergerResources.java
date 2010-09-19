package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;
import de.itm.uniluebeck.tr.wiseml.merger.structures.LinkProperties;
import de.itm.uniluebeck.tr.wiseml.merger.structures.NodeProperties;

public class MergerResources {
	
	private int inputCount;
	private NodeProperties[] inputDefaultNodes;
	private LinkProperties[] inputDefaultLinks;
	private Coordinate[] inputOrigins;
	
	private Coordinate outputOrigin;
	private NodeProperties outputDefaultNode;
	private LinkProperties outputDefaultLink;
	
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
}
