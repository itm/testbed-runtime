package de.uniluebeck.itm.tr.runtime.wsnapp;

import java.util.*;

public class UnknownNodeUrnsException extends Exception {

	private Set<String> nodeUrns;

	public UnknownNodeUrnsException(Collection<String> nodeUrns, String message) {
		super(message);
		this.nodeUrns = new HashSet<String>(nodeUrns);
	}

	public Set<String> getNodeUrns() {
		return nodeUrns;
	}
}
