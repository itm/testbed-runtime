package de.uniluebeck.itm.tr.common;

import eu.wisebed.api.v3.common.NodeUrn;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public abstract class ServedNodeUrnsProviders {

	public static ServedNodeUrnsProvider of(final Iterable<NodeUrn> nodeUrns) {
		return new ServedNodeUrnsProvider() {
			private Set<NodeUrn> providerNodeUrns = newHashSet(nodeUrns);
			@Override
			public Set<NodeUrn> get() {
				return providerNodeUrns;
			}
		};
	}
}
