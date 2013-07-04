package de.uniluebeck.itm.tr.federator.utils;

import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class URIToNodeUrnPrefixSetMap extends HashMap<URI, Set<NodeUrnPrefix>> {

	public URIToNodeUrnPrefixSetMap(final Map<URI, Set<NodeUrnPrefix>> map) {
		super(map);
	}
}
