package de.uniluebeck.itm.tr.iwsn.portal.api.rest.v1.providers;

import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.apache.cxf.jaxrs.ext.ParameterHandler;

import javax.ws.rs.ext.Provider;

@Provider
public class NodeUrnPrefixParameterHandler implements ParameterHandler<NodeUrnPrefix> {

	@Override
	public NodeUrnPrefix fromString(final String s) {
		return new NodeUrnPrefix(s);
	}
}