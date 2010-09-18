package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import de.itm.uniluebeck.tr.wiseml.merger.structures.Coordinate;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Link;
import de.itm.uniluebeck.tr.wiseml.merger.structures.Node;

public class MergerResources {
	
	private int inputCount;
	private Node[] inputDefaultNodes;
	private Link[] inputDefaultLinks;
	private Coordinate[] inputOrigins;
	
	private Coordinate outputOrigin;
	private Node outputDefaultNode;
	private Link outputDefaultLink;
	
	public void setOrigins(Coordinate[] inputOrigins, Coordinate outputOrigin) {
		this.inputOrigins = inputOrigins;
		this.outputOrigin = outputOrigin;
	}
	
	public void setDefaultNodes(Node[] inputDefaultNodes, Node outputDefaultNode) {
		this.inputDefaultNodes = inputDefaultNodes;
		this.outputDefaultNode = outputDefaultNode;
	}
	
	public void setDefaultLinks(Link[] inputDefaultLinks, Link outputDefaultLink) {
		this.inputDefaultLinks = inputDefaultLinks;
		this.outputDefaultLink = outputDefaultLink;
	}
}
