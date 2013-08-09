package de.uniluebeck.itm.tr.common;

import eu.wisebed.api.v3.common.NodeUrnPrefix;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public abstract class ServedNodeUrnPrefixesProviders {

	public static ServedNodeUrnPrefixesProvider of(final NodeUrnPrefix nodeUrnPrefix) {
		return new ServedNodeUrnPrefixesProvider() {
			private final Set<NodeUrnPrefix> nodeUrns = newHashSet(nodeUrnPrefix);
			@Override
			public Set<NodeUrnPrefix> get() {
				return nodeUrns;
			}
		};
	}

	public static ServedNodeUrnPrefixesProvider of(final Iterable<NodeUrnPrefix> nodeUrnsIterable) {
		return new ServedNodeUrnPrefixesProvider() {
			private final Set<NodeUrnPrefix> nodeUrns = newHashSet(nodeUrnsIterable);
			@Override
			public Set<NodeUrnPrefix> get() {
				return nodeUrns;
			}
		};
	}

}
