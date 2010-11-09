package de.itm.uniluebeck.tr.wiseml.merger.internals.merge;

import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements.LinkPropertiesTransformer;
import de.itm.uniluebeck.tr.wiseml.merger.internals.merge.elements.NodeItemTransformer;
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
	
	private NodeItemTransformer nodeItemTransformer;
	
	private TimeInfo[] inputTimeInfos;
	private TimeInfo outputTimeInfo;
	private boolean timestampOffsetDefined;
	
	public boolean isTimestampOffsetDefined() {
		return timestampOffsetDefined;
	}

	public void setTimestampOffsetDefined(boolean timestampOffsetDefined) {
		this.timestampOffsetDefined = timestampOffsetDefined;
	}

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

	public TimeInfo getOutputTimeInfo() {
		return outputTimeInfo;
	}

	public void setOutputTimeInfo(TimeInfo timeInfo) {
		this.outputTimeInfo = timeInfo;
	}

	public NodeItemTransformer getNodeItemTransformer() {
		return nodeItemTransformer;
	}

	public void setNodeItemTransformer(NodeItemTransformer nodeItemTransformer) {
		this.nodeItemTransformer = nodeItemTransformer;
	}

	public TimeInfo getInputTimeInfo(int inputIndex) {
		return inputTimeInfos[inputIndex];
	}

	public void setInputTimeInfos(TimeInfo[] infos) {
		this.inputTimeInfos = infos;
	}
	
}
