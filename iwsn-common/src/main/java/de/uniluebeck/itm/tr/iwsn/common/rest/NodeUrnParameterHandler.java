package de.uniluebeck.itm.tr.iwsn.common.rest;

import eu.wisebed.api.v3.common.NodeUrn;
import org.apache.cxf.jaxrs.ext.ParameterHandler;

import javax.ws.rs.ext.Provider;

@Provider
public class NodeUrnParameterHandler implements ParameterHandler<NodeUrn> {

	@Override
	public NodeUrn fromString(final String s) {
		return new NodeUrn(s);
	}
}