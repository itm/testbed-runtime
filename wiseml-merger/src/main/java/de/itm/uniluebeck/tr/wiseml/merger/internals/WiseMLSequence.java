package de.itm.uniluebeck.tr.wiseml.merger.internals;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum WiseMLSequence {
	
	SetupNode(WiseMLTag.node),
	NodePropertiesCapability(WiseMLTag.capability),
	SetupLink(WiseMLTag.link),
	LinkPropertiesCapability(WiseMLTag.capability),
	Scenario(WiseMLTag.scenario),
	ScenarioItem(
			WiseMLTag.timestamp,
			WiseMLTag.enableNode,
			WiseMLTag.disableNode,
			WiseMLTag.enableLink,
			WiseMLTag.disableLink,
			WiseMLTag.node),
	Trace(WiseMLTag.trace),
	TraceItem(
			WiseMLTag.timestamp,
			WiseMLTag.node,
			WiseMLTag.link);
	
	
	
	private Set<WiseMLTag> tagSet;
	
	WiseMLSequence(WiseMLTag... tags) {
		Set<WiseMLTag> set = new HashSet<WiseMLTag>();
		for (WiseMLTag tag : tags) {
			set.add(tag);
		}
		this.tagSet = Collections.unmodifiableSet(set);
	}

	public Set<WiseMLTag> getTagSet() {
		return tagSet;
	}

}
