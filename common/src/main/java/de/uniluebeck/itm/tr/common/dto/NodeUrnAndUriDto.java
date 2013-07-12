package de.uniluebeck.itm.tr.common.dto;

import eu.wisebed.api.v3.common.NodeUrn;

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement
public class NodeUrnAndUriDto {

	private NodeUrn nodeUrn;

	private URI uri;

	@SuppressWarnings("unused")
	public NodeUrnAndUriDto() {
	}

	public NodeUrnAndUriDto(final NodeUrn nodeUrn, final URI uri) {
		this.nodeUrn = nodeUrn;
		this.uri = uri;
	}

	public NodeUrn getNodeUrn() {
		return nodeUrn;
	}

	public void setNodeUrn(final NodeUrn nodeUrn) {
		this.nodeUrn = nodeUrn;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(final URI uri) {
		this.uri = uri;
	}
}
