package de.uniluebeck.itm.tr.runtime.wsnapp;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.*;

import static com.google.common.collect.Sets.newHashSet;

public class UnknownNodeUrnsException extends Exception {

	private Set<NodeUrn> nodeUrns;

	public UnknownNodeUrnsException(Collection<NodeUrn> nodeUrns, String message) {
		super(message);
		this.nodeUrns = newHashSet(nodeUrns);
	}

	public Set<NodeUrn> getNodeUrns() {
		return nodeUrns;
	}
}
